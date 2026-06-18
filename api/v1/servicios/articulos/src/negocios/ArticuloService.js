/**
 * Metroid Wiki - ArticuloService (Capa de Artículos Conectada a la Nube)
 * API del Microservicio de Artículos con validación JWT, MongoDB Atlas y gRPC
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');

// LIBRERÍAS DE gRPC Y RUTAS
const path = require('path');
const fs = require('fs');
const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');

const config = {
  port: process.env.PORT || 3001,
  mongoUri: process.env.MONGO_URI
};

const router = express.Router();

// ==========================================
// VIEWS COOLDOWN (Per-article per-user)
// ==========================================
const VIEW_COOLDOWN_MS = 30 * 1000;
const viewCooldownStore = new Map();

function deriveUserKey(req) {
  try {
    const authHeader = req.headers['authorization'] || req.headers['Authorization'];
    if (authHeader && authHeader.toLowerCase().startsWith('bearer ')) {
      const token = authHeader.split(' ')[1];
      if (token) {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        if (decoded && (decoded.id || decoded._id || decoded.sub)) {
          return `user:${decoded.id || decoded._id || decoded.sub}`;
        }
      }
    }
  } catch (e) {
  }
  const ip = req.ip || req.connection && req.connection.remoteAddress || 'anonymous';
  return `ip:${ip}`;
}

async function handleArticleView(req, articleId) {
  const userKey = deriveUserKey(req);
  const now = Date.now();

  // Si el artículo no está en la memoria caché de vistas, lo agregamos
  if (!viewCooldownStore.has(articleId)) {
    viewCooldownStore.set(articleId, new Map());
  }

  const userMap = viewCooldownStore.get(articleId);
  const lastViewTime = userMap.get(userKey) || 0;

  // Si ya pasaron los 30 segundos de seguridad (Cooldown)
  if (now - lastViewTime > VIEW_COOLDOWN_MS) {
    userMap.set(userKey, now); 
    
    await ArticuloRepository.incrementarVistas(articleId);
    return true; 
  }
  
  // Si no pasaron 30 segundos, ignoramos la vista para evitar spam
  return false; 
}

// Este es el basurero automático que limpia la RAM de Node.js cada hora
setInterval(() => {
  const now = Date.now();
  const maxAge = 60 * 60 * 1000; 
  for (const [articleId, userMap] of viewCooldownStore.entries()) {
    for (const [userKey, ts] of userMap.entries()) {
      if (now - ts > maxAge) userMap.delete(userKey);
    }
    if (userMap.size === 0) viewCooldownStore.delete(articleId);
  }
}, 60 * 1000);

// ==========================================
// CONFIGURACIÓN DE gRPC
// ==========================================
const PROTO_PATH = path.join(__dirname, '../../proto/media.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});

const mediaProto = grpc.loadPackageDefinition(packageDefinition).media;

// ==========================================
// MIDDLEWARE DE SEGURIDAD
// ==========================================
const verificarPermisos = (rolesPermitidos) => {
  return (req, res, next) => {
    const authHeader = req.headers['authorization'] || req.headers['Authorization'];
    
    if (!authHeader) {
      return res.status(403).json({ message: "No se proporcionó un token de seguridad." });
    }

    try {
      let tokenLimpio = authHeader;
      
      if (authHeader.toLowerCase().startsWith('bearer ')) {
        const partes = authHeader.split(' ');
        tokenLimpio = partes[1]; 
      }

      if (!tokenLimpio) {
         return res.status(401).json({ message: "El token proporcionado está vacío o mal formado." });
      }

      const decoded = jwt.verify(tokenLimpio, process.env.JWT_SECRET);
      req.user = decoded; 

      if (rolesPermitidos && !rolesPermitidos.includes(decoded.rol)) {
        return res.status(403).json({ message: `Acceso denegado: Requiere rol [${rolesPermitidos.join(', ')}]. Tu rol es: ${decoded.rol}` });
      }
      
      next();
    } catch (error) {
      return res.status(401).json({ message: "Token inválido o expirado.", detalle: error.message });
    }
  };
};

// ==========================================
// CONFIGURACION DE SWAGGER
// ==========================================
const swaggerUi = require('swagger-ui-express');
const swaggerJsDoc = require('swagger-jsdoc');

const swaggerOptions = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Metroid Wiki Artículos API',
      version: '1.0.0',
      description: 'Documentación interactiva de los artículos y lore',
    },
    servers: [{ url: `http://localhost:${config.port}` }],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
        }
      }
    }
  },
  apis: ['./src/negocio/ArticuloService.js'], 
};

const swaggerDocs = swaggerJsDoc(swaggerOptions);
router.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocs));

// ==========================================
// 1. CONFIGURACION DE BASE DE DATOS (Mongoose + UML)
// ==========================================
const articleSchema = new mongoose.Schema({
  titulo: { type: String, required: true, unique: true, trim: true },
  descripcion: { type: String, required: true, trim: true },
  contenido: { type: String, required: true },
  categoria: { 
    type: String, 
    enum: ['Lore', 'Items', 'Enemigos', 'Ubicaciones', 'Personajes'], 
    required: true 
  },
  estado: { 
    type: String, 
    enum: ['EnBorrador', 'EnRevision', 'Publicado', 'Archivado'], 
    default: 'EnBorrador' 
  },
  vistas: { type: Number, default: 0 },
  imagen: { type: String }, 
  autorId: { type: mongoose.Schema.Types.ObjectId, required: true },
  autor: { type: String, required: true, default: 'Desconocido' } 
}, {
  timestamps: true 
});

const Articulo = mongoose.model('Articulo', articleSchema);

// ==========================================
// 2. EXCEPCIONES ESPECIFICAS Y DTOs
// ==========================================
class MetroidException extends Error {
  constructor(mensaje, statusCode) {
    super(mensaje);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
  }
}

class DocumentoNoEncontradoException extends MetroidException {
  constructor(id) {
    super(`No se encontro ningun articulo con el ID: ${id}`, 404);
  }
}

class FormatoIdInvalidoException extends MetroidException {
  constructor() {
    super('El formato del ID no es valido', 400);
  }
}

class CamposRequeridosFaltantesException extends MetroidException {
  constructor(campos) {
    super(`Los siguientes campos son requeridos: ${campos.join(', ')}`, 400);
  }
}

class ErrorBaseDeDatosException extends MetroidException {
  constructor(detalle) {
    super(`Error en la operacion de base de datos: ${detalle}`, 500);
  }
}

class ArticuloDTO {
  constructor(modeloMongoose) {
    this.id = modeloMongoose._id;
    this.titulo = modeloMongoose.titulo;
    this.categoria = modeloMongoose.categoria;
    this.descripcion = modeloMongoose.descripcion;
    this.contenido = modeloMongoose.contenido;
    this.estado = modeloMongoose.estado;
    this.vistas = modeloMongoose.vistas;
    this.imagen = modeloMongoose.imagen || '';
    this.autorId = modeloMongoose.autorId;
    this.autor = modeloMongoose.autor || 'Desconocido'; 
    this.fechaCreacion = modeloMongoose.createdAt;
    this.fechaActualizacion = modeloMongoose.updatedAt;
  }
}

// ==========================================
// 3. CAPA DE DATOS (Repositorio)
// ==========================================
const ArticuloRepository = {
  obtenerTodos: async () => {
    try {
      return await Articulo.find();
    } catch (error) {
      throw new ErrorBaseDeDatosException(error.message);
    }
  },

  obtenerPorId: async (id) => {
    try {
      return await Articulo.findById(id);
    } catch (error) {
      throw new ErrorBaseDeDatosException(error.message);
    }
  },

  crear: async (datosArticulo) => {
    try {
      const nuevoArticulo = new Articulo(datosArticulo);
      return await nuevoArticulo.save();
    } catch (error) {
      if (error.code === 11000) {
          throw new ErrorBaseDeDatosException("Ya existe un artículo con ese título.");
      }
      throw new ErrorBaseDeDatosException(error.message);
    }
  },

  // 🛠️ NUEVO: Método para actualizar en la base de datos
  actualizar: async (id, datosActualizados) => {
    try {
      return await Articulo.findByIdAndUpdate(id, datosActualizados, { new: true, runValidators: true });
    } catch (error) {
      if (error.code === 11000) {
          throw new ErrorBaseDeDatosException("Ya existe un artículo con ese título.");
      }
      throw new ErrorBaseDeDatosException(error.message);
    }
  }
};

ArticuloRepository.incrementarVistas = async (id) => {
  try {
    return await Articulo.findByIdAndUpdate(id, { $inc: { vistas: 1 } }, { new: true });
  } catch (error) {
    throw new ErrorBaseDeDatosException(error.message);
  }
};

// ==========================================
// 4. CAPA DE NEGOCIO (Servicio)
// ==========================================
const ArticuloLogicService = {
  obtenerArticulos: async () => {
    const articulos = await ArticuloRepository.obtenerTodos();
    return articulos.map(art => new ArticuloDTO(art));
  },

  obtenerArticuloPorId: async (id) => {
    if (!mongoose.Types.ObjectId.isValid(id)) {
      throw new FormatoIdInvalidoException();
    }
    const articulo = await ArticuloRepository.obtenerPorId(id);
    if (!articulo) {
      throw new DocumentoNoEncontradoException(id);
    }
    return new ArticuloDTO(articulo);
  },

  crearArticulo: async (datos) => {
    const requiredFields = ['titulo', 'categoria', 'descripcion', 'contenido', 'autorId', 'autor'];
    const missingFields = requiredFields.filter(field => !datos[field]);
    
    if (missingFields.length > 0) {
      throw new CamposRequeridosFaltantesException(missingFields);
    }

    if (datos.categoria) {
      datos.categoria = datos.categoria.charAt(0).toUpperCase() + datos.categoria.slice(1).toLowerCase();
    }
    
    const articuloGuardado = await ArticuloRepository.crear(datos);
    return new ArticuloDTO(articuloGuardado);
  },

  actualizarArticulo: async (id, datos) => {
    if (!mongoose.Types.ObjectId.isValid(id)) {
      throw new FormatoIdInvalidoException();
    }

    const articuloExistente = await ArticuloRepository.obtenerPorId(id);
    if (!articuloExistente) {
      throw new DocumentoNoEncontradoException(id);
    }

    if (datos.categoria) {
      datos.categoria = datos.categoria.charAt(0).toUpperCase() + datos.categoria.slice(1).toLowerCase();
    }

    const articuloActualizado = await ArticuloRepository.actualizar(id, datos);
    return new ArticuloDTO(articuloActualizado);
  }
};

// ==========================================
// 5. CAPA DE PRESENTACION (Controladores HTTP)
// ==========================================
function manejarExcepcionHTTP(error, res) {
  if (error instanceof MetroidException) {
    console.error(`🚨 ERROR RECHAZADO POR MONGO: ${error.message}`); 
    return res.status(error.statusCode).json({
      error: error.name,
      message: error.message
    });
  }
  console.error('Excepcion no controlada:', error);
  res.status(500).json({
    error: 'ErrorCriticoDelServidor',
    message: 'Se produjo un error inesperado en la ejecucion.'
  });
}

/**
 * @swagger
 * /articulos:
 *   get:
 *     summary: Obtiene todos los articulos de la Wiki (Público)
 *     responses:
 *       200:
 *         description: Exito. Devuelve el total y el arreglo de articulos.
 */
router.get('/', async (req, res) => {
  try {
    const articulosDTO = await ArticuloLogicService.obtenerArticulos();
    res.json({
      total: articulosDTO.length,
      articulos: articulosDTO
    });
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});

/**
 * @swagger
 * /articulos/{id}:
 *   get:
 *     summary: Obtiene un articulo por su ID (Público)
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: ID unico del articulo en MongoDB
 *     responses:
 *       200:
 *         description: Exito.
 *       404:
 *         description: Articulo no encontrado.
 */
router.get('/:id', async (req, res) => {
  try {
    const articuloDTO = await ArticuloLogicService.obtenerArticuloPorId(req.params.id);

    try {
      const incremented = await handleArticleView(req, req.params.id);
      if (incremented) {
        articuloDTO.vistas = (articuloDTO.vistas || 0) + 1;
      }
    } catch (viewErr) {
      console.warn('No se pudo procesar el contador de vistas:', viewErr.message);
    }

    res.json(articuloDTO);
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});

/**
 * @swagger
 * /articulos:
 *   post:
 *     summary: Crea un nuevo articulo en la base de datos (Protegido - Requiere Token)
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               titulo:
 *                 type: string
 *                 example: "Samus Aran"
 *               juego:
 *                 type: string
 *                 example: "Metroid Prime"
 *               categoria:
 *                 type: string
 *                 example: "Personaje"
 *               descripcion:
 *                 type: string
 *                 example: "Cazarrecompensas"
 *               contenido:
 *                 type: string
 *                 example: "Historia completa..."
 *     responses:
 *       201:
 *         description: Articulo creado exitosamente.
 *       403:
 *         description: Token no proporcionado o permisos insuficientes.
 */
router.post('/', verificarPermisos(['administrador']), async (req, res) => {
  try {
    req.body.autorId = req.user.id || req.user._id || req.user.sub; 
    req.body.autor = req.user.nombre || req.user.name || 'Administrador';

    const articuloDTO = await ArticuloLogicService.crearArticulo(req.body);
    res.status(201).json({
      message: 'Articulo creado exitosamente',
      articulo: articuloDTO
    });
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});

// 🛠️ NUEVO: RUTA PARA ACTUALIZAR (PUT)
/**
 * @swagger
 * /articulos/{id}:
 *   put:
 *     summary: Actualiza un articulo existente (Protegido - Requiere Token)
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: ID unico del articulo a modificar
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               titulo:
 *                 type: string
 *               categoria:
 *                 type: string
 *               estado:
 *                 type: string
 *                 example: "Publicado"
 *               descripcion:
 *                 type: string
 *               contenido:
 *                 type: string
 *     responses:
 *       200:
 *         description: Articulo actualizado exitosamente.
 *       403:
 *         description: Token no proporcionado o permisos insuficientes.
 *       404:
 *         description: Articulo no encontrado.
 */
router.put('/:id', verificarPermisos(['administrador']), async (req, res) => {
  try {
    const articuloDTO = await ArticuloLogicService.actualizarArticulo(req.params.id, req.body);
    res.status(200).json({
      message: 'Articulo actualizado exitosamente',
      articulo: articuloDTO
    });
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});


// ==========================================
// 6. SERVIDOR gRPC
// ==========================================
const subirImagenHandler = (call, callback) => {
  let archivoNombre = '';
  let articuloId = '';
  let chunks = [];

  call.on('data', (request) => {
    articuloId = request.articuloId;
    archivoNombre = request.nombreArchivo;
    chunks.push(request.chunk); 
  });

  call.on('end', async () => {
    try {
      const archivoCompleto = Buffer.concat(chunks);
      const directorioPublico = path.join(__dirname, '../../public/imagenes');
      
      if (!fs.existsSync(directorioPublico)) {
          fs.mkdirSync(directorioPublico, { recursive: true });
      }

      const rutaDestino = path.join(directorioPublico, archivoNombre);
      fs.writeFileSync(rutaDestino, archivoCompleto);
      
      console.log(`📦 [gRPC] Archivo guardado con éxito: ${archivoNombre}`);

      const rutaPublica = `http://localhost:${config.port}/articulos/public/imagenes/${archivoNombre}`;

      try {
        await Articulo.findByIdAndUpdate(articuloId, { imagen: rutaPublica });
        console.log(`✅ [gRPC] Registro actualizado con imagen URL: ${rutaPublica}`);
      } catch (updateError) {
        console.warn(`⚠️ No se pudo actualizar el artículo:`, updateError.message);
      }

      callback(null, {
        exito: true,
        mensaje: "Archivo recibido y guardado correctamente.",
        urlImagen: rutaPublica
      });

    } catch (error) {
      console.error(" Fallo en gRPC:", error);
      callback({
        code: grpc.status.INTERNAL,
        message: `Error interno al guardar: ${error.message}`
      });
    }
  });
};

// ==========================================
// 7. INICIO DE TODOS LOS SERVIDORES
// ==========================================
module.exports = { router, config };

async function startServer() {
  try {
    await mongoose.connect(config.mongoUri);
    console.log('¡Conectado exitosamente a MongoDB Atlas (Artículos)!');
    
    const app = express();
    app.use(express.json());
    app.use(cors());
    
    const directorioImagenes = path.join(__dirname, '../../public/imagenes');
    if (!fs.existsSync(directorioImagenes)) {
      fs.mkdirSync(directorioImagenes, { recursive: true });
    }
    app.use('/articulos/public/imagenes', express.static(directorioImagenes));

    app.use('/articulos', router);
    
    app.listen(config.port, () => {
      console.log(`🌐 Metroid Wiki Article Service (REST) running on port ${config.port}`);
      console.log(`📚 Swagger docs available at: http://localhost:${config.port}/articulos/api-docs`);
    });

    const grpcServer = new grpc.Server();
    grpcServer.addService(mediaProto.MediaService.service, { SubirImagen: subirImagenHandler });
    
    grpcServer.bindAsync('0.0.0.0:3003', grpc.ServerCredentials.createInsecure(), (err, port) => {
      if (err) {
        console.error('No se pudo arrancar el servidor gRPC:', err);
        return;
      }
      grpcServer.start();
      console.log(`📸 Servidor gRPC (Multimedia) corriendo en el puerto: ${port}`);
    });

  } catch (error) {
    console.error('Failed to start Article Service:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  startServer();
}