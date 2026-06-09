require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');

const router = express.Router();

const config = {
  port: process.env.PORT || 3004,
  mongoUri: process.env.MONGO_URI
};

// =========================
// MONGOOSE SCHEMAS
// =========================
const commentCounterSchema = new mongoose.Schema({
  articleId: { type: mongoose.Schema.Types.ObjectId, required: true, unique: true },
  seq: { type: Number, default: 0 }
});

const comentarioSchema = new mongoose.Schema({
  articuloId: { type: mongoose.Schema.Types.ObjectId, required: true, index: true },
  autorId: { type: mongoose.Schema.Types.ObjectId, required: true },
  autorNombre: { type: String, required: true },
  contenido: { type: String, required: true },
  orden: { type: Number, required: true },
}, {
  timestamps: true
});

const CommentCounter = mongoose.model('CommentCounter', commentCounterSchema);
const Comentario = mongoose.model('Comentario', comentarioSchema);

// =========================
// EXCEPTIONS / DTO
// =========================
class ComentarioException extends Error {
  constructor(message, status) {
    super(message);
    this.name = this.constructor.name;
    this.statusCode = status || 500;
  }
}

class NotFoundException extends ComentarioException {
  constructor(id) { super(`No se encontró recurso con id: ${id}`, 404); }
}

class BadRequestException extends ComentarioException {
  constructor(msg) { super(msg || 'Solicitud inválida', 400); }
}

class ComentarioDTO {
  constructor(model) {
    this.id = model._id;
    this.articuloId = model.articuloId || model.articuloId;
    this.autorId = model.autorId;
    this.autorNombre = model.autorNombre;
    this.contenido = model.contenido;
    this.orden = model.orden;
    this.fechaCreacion = model.createdAt;
    this.fechaActualizacion = model.updatedAt;
  }
}

// =========================
// JWT MIDDLEWARE
// =========================
const verificarPermisos = (rolesPermitidos) => {
  return (req, res, next) => {
    const authHeader = req.headers['authorization'] || req.headers['Authorization'];
    if (!authHeader) return res.status(403).json({ message: 'No se proporcionó token' });

    try {
      let token = authHeader;
      if (authHeader.toLowerCase().startsWith('bearer ')) token = authHeader.split(' ')[1];
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      req.user = decoded;
      if (rolesPermitidos && !rolesPermitidos.includes(decoded.rol)) {
        return res.status(403).json({ message: `Acceso denegado: requiere rol [${rolesPermitidos.join(', ')}]` });
      }
      next();
    } catch (err) {
      return res.status(401).json({ message: 'Token inválido o expirado', detalle: err.message });
    }
  };
};

// =========================
// REPOSITORY - concurrency-safe creation
// =========================
const ComentarioRepository = {
  listarPorArticulo: async (articuloId, limit = 100, skip = 0) => {
    try {
      return await Comentario.find({ articuloId }).sort({ orden: 1 }).skip(skip).limit(limit);
    } catch (err) {
      throw new ComentarioException('Error al listar comentarios: ' + err.message);
    }
  },

  crearConSecuencia: async (articuloId, autorId, autorNombre, contenido) => {
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
      // Atomically increment (or create) the counter for this article
      const counter = await CommentCounter.findOneAndUpdate(
        { articleId: articuloId },
        { $inc: { seq: 1 } },
        { new: true, upsert: true, session }
      );

      const orden = counter.seq;

      const comentario = new Comentario({ articuloId, autorId, autorNombre, contenido, orden });
      await comentario.save({ session });

      await session.commitTransaction();
      session.endSession();

      return comentario;
    } catch (err) {
      await session.abortTransaction();
      session.endSession();
      throw new ComentarioException('Error al crear comentario: ' + err.message);
    }
  }
};

// =========================
// BUSINESS LOGIC
// =========================
const ComentarioService = {
  listarComentarios: async (articuloId, limit, skip) => {
    if (!mongoose.Types.ObjectId.isValid(articuloId)) throw new BadRequestException('ID de articulo inválido');
    const comentarios = await ComentarioRepository.listarPorArticulo(articuloId, limit, skip);
    return comentarios.map(c => new ComentarioDTO(c));
  },

  agregarComentario: async (articuloId, autorId, autorNombre, contenido) => {
    if (!mongoose.Types.ObjectId.isValid(articuloId)) throw new BadRequestException('ID de articulo inválido');
    if (!contenido || contenido.trim().length === 0) throw new BadRequestException('Contenido de comentario vacío');
    if (contenido.length > 512) throw new BadRequestException('Contenido de comentario excede el máximo de 512 caracteres');

    const comentario = await ComentarioRepository.crearConSecuencia(articuloId, autorId, autorNombre, contenido);
    return new ComentarioDTO(comentario);
  }
};

// =========================
// CONTROLLERS / ROUTES
// =========================
function manejarError(res, err) {
  if (err instanceof ComentarioException) {
    return res.status(err.statusCode || 500).json({ error: err.name, message: err.message });
  }
  console.error('Error no controlado comentarios:', err);
  return res.status(500).json({ error: 'ErrorInterno', message: 'Se produjo un error interno' });
}

/**
 * @swagger
 * /comentarios/{articuloId}:
 *   get:
 *     summary: Lista comentarios de un artículo
 */
router.get('/:articuloId', async (req, res) => {
  try {
    const { articuloId } = req.params;
    const limit = parseInt(req.query.limit) || 100;
    const skip = parseInt(req.query.skip) || 0;
    const comentarios = await ComentarioService.listarComentarios(articuloId, limit, skip);
    res.json({ total: comentarios.length, comentarios });
  } catch (err) {
    manejarError(res, err);
  }
});

/**
 * @swagger
 * /comentarios/{articuloId}:
 *   post:
 *     summary: Agrega un comentario a un artículo (Requiere token)
 */
router.post('/:articuloId', verificarPermisos(), async (req, res) => {
  try {
    const { articuloId } = req.params;
    const autorId = req.user.id;
    const autorNombre = req.user.nombre || req.user.name || 'Usuario';
    const { contenido } = req.body;

    const comentarioDTO = await ComentarioService.agregarComentario(articuloId, autorId, autorNombre, contenido);
    res.status(201).json({ message: 'Comentario creado', comentario: comentarioDTO });
  } catch (err) {
    manejarError(res, err);
  }
});

// =========================
// Server start/export
// =========================
module.exports = { router, config };

async function startServer() {
  try {
    await mongoose.connect(config.mongoUri);
    console.log('Conectado a MongoDB (Comentarios)');

    const app = express();
    app.use(express.json());
    app.use(cors());

    app.use('/comentarios', router);

    app.listen(config.port, () => {
      console.log(`Servicio de Comentarios (REST) corriendo en puerto ${config.port}`);
    });
  } catch (err) {
    console.error('Fallo al iniciar servicio de comentarios:', err);
    process.exit(1);
  }
}

if (require.main === module) startServer();
