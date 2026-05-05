/**
 * Metroid Wiki - ArticuloService (Refactorizado en 3 Capas)
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');

const config = {
  port: process.env.PORT || 3001,
  mongoUri: process.env.MONGO_URI || 'mongodb://localhost:27017',
  dbName: process.env.DB_NAME || 'metroid_wiki_articulos'
};

const router = express.Router();

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
    super(`No se encontró ningún artículo con el ID: ${id}`, 404);
  }
}

class FormatoIdInvalidoException extends MetroidException {
  constructor() {
    super('El formato del ID no es válido', 400);
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
    message: 'Se produjo un error inesperado en la ejecución.'
  });
}

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

router.get('/:id', async (req, res) => {
  try {
    const articuloDTO = await ArticuloLogicService.obtenerArticuloPorId(req.params.id);
    res.json(articuloDTO);
  } catch (error) {
    manejarExcepcionHTTP(error, res);
  }
});

router.post('/', async (req, res) => {
  console.log("POST /articulos hit");
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
const { iniciarServidorGrpc } = require('./grpc/GrpcServer');
module.exports = { router, config };

async function startServer() {
  try {
    // Conectar a MongoDB
    await mongoose.connect(`${config.mongoUri}/${config.dbName}`);
    
    // Iniciar Express (REST)
    const app = express();
    app.use(express.json());
    app.use(cors());
    app.use('/articulos', router);
    
    app.listen(config.port, () => {
      console.log(`Metroid Wiki Article Service running on port ${config.port}`);
      console.log(`Database: ${config.dbName}`);
      
      // Iniciar gRPC una vez que Express y Mongo están listos
      iniciarServidorGrpc();
    });
  } catch (error) {
    // Aquí no usamos nuestra MetroidException porque es un error de arranque crítico, no de una petición HTTP
    console.error('Failed to start Article Service:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  startServer();
}