/**
 * Metroid Wiki - ArticuloService (Refactorizado en 3 Capas + Swagger + JWT)
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');

const config = {
  port: process.env.PORT || 3001,
  mongoUri: process.env.MONGO_URI || 'mongodb://localhost:27017',
  dbName: process.env.DB_NAME || 'metroid_wiki_articulos'
};

const router = express.Router();

// ==========================================
// MIDDLEWARE DE SEGURIDAD (El Guardia Blindado)
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

      const decoded = jwt.verify(tokenLimpio, 'firma_super_secreta_metroid');
      req.user = decoded;

      if (rolesPermitidos && !rolesPermitidos.includes(decoded.rol)) {
        return res.status(403).json({ message: "Acceso denegado: No tienes los permisos necesarios." });
      }
      
      next();
    } catch (error) {
      return res.status(401).json({ message: "Token inválido o expirado.", detalle: error.message });
    }
  };
};

// ==========================================
// CONFIGURACION DE SWAGGER (CERRADURA OFICIAL)
// ==========================================
const swaggerUi = require('swagger-ui-express');
const swaggerJsDoc = require('swagger-jsdoc');

const swaggerOptions = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Metroid Wiki API',
      version: '1.0.0',
      description: 'Documentacion interactiva de los servicios REST',
    },
    servers: [
      { 
        url: 'http://localhost:3000',
        description: 'API Gateway Local'
      }
    ],
    // NUEVO: Le decimos a Swagger que usamos Tokens Bearer (Aparecerá el botón Authorize)
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
  apis: ['./ArticuloService.js'], 
};

const swaggerDocs = swaggerJsDoc(swaggerOptions);
router.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocs));

// ==========================================
// 1. CONFIGURACION DE BASE DE DATOS (Mongoose)
// ==========================================
const articleSchema = new mongoose.Schema({
  titulo: { type: String, required: true },
  juego: { type: String, required: true },
  categoria: { type: String, required: true },
  descripcion: { type: String, required: true },
  contenido: { type: String, required: true },
  imagen: { type: String },
  fechaCreacion: { type: Date, default: Date.now },
  fechaActualizacion: { type: Date, default: Date.now }
});

articleSchema.pre("save", function (next) {
  this.fechaActualizacion = Date.now();
  next();
});

articleSchema.pre("findOneAndUpdate", function (next) {
  this.set({ fechaActualizacion: Date.now() });
  next();
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
    this.juego = modeloMongoose.juego;
    this.categoria = modeloMongoose.categoria;
    this.descripcion = modeloMongoose.descripcion;
    this.contenido = modeloMongoose.contenido;
    this.imagen = modeloMongoose.imagen || '';
    this.fechaCreacion = modeloMongoose.fechaCreacion;
    this.fechaActualizacion = modeloMongoose.fechaActualizacion;
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
      throw new ErrorBaseDeDatosException(error.message);
    }
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
    const requiredFields = ['titulo', 'juego', 'categoria', 'descripcion', 'contenido'];
    const missingFields = requiredFields.filter(field => !datos[field]);
    
    if (missingFields.length > 0) {
      throw new CamposRequeridosFaltantesException(missingFields);
    }
    
    const articuloGuardado = await ArticuloRepository.crear(datos);
    return new ArticuloDTO(articuloGuardado);
  }
};

// ==========================================
// 5. CAPA DE PRESENTACION (Controladores HTTP)
// ==========================================
function manejarExcepcionHTTP(error, res) {
  if (error instanceof MetroidException) {
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
router.post('/', verificarPermisos(['Administrador', 'Editor']), async (req, res) => {
  try {
    const articuloDTO = await ArticuloLogicService.crearArticulo(req.body);
    res.status(201).json({
      message: 'Articulo creado exitosamente',
      articulo: articuloDTO
    });
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});

// ==========================================
// 6. INICIO DEL SERVIDOR
// ==========================================
const { iniciarServidorGrpc } = require('../../grpc/GrpcServer');
module.exports = { router, config };

async function startServer() {
  try {
    await mongoose.connect(`${config.mongoUri}/${config.dbName}`);
    
    const app = express();
    app.use(express.json());
    app.use(cors());
    app.use('/articulos', router);
    
    app.listen(config.port, () => {
      console.log(`Metroid Wiki Article Service running on port ${config.port}`);
      console.log(`Database: ${config.dbName}`);
      
      iniciarServidorGrpc();
    });
  } catch (error) {
    console.error('Failed to start Article Service:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  startServer();
}