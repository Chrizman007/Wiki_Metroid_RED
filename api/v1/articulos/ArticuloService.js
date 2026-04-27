/**
 * Metroid Wiki - ArticuloService
 * Servicio de API REST para recuperar y crear artículos de la Metroid Wiki
 * Se conecta a MongoDB y expone endpoints para la obtención de artículos por ID y la creación de los mismos
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');

// Configuration from own .env
const config = {
  port: process.env.PORT || 3001,
  mongoUri: process.env.MONGO_URI || 'mongodb://localhost:27017',
  dbName: process.env.DB_NAME || 'metroid_wiki_articulos'
};

// Create router
const router = express.Router();

// Article Schema
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

router.get('/', async (req, res) => {
  try {
    const articulos = await Articulo.find();

    res.json({
      total: articulos.length,
      articulos
    });
  } catch (error) {
    console.error('Error al obtener artículos:', error);
    res.status(500).json({
      error: 'Error interno del servidor',
      message: 'Error al obtener los artículos'
    });
  }
});

// GET /:id - Get article by unique ID
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Validate MongoDB ObjectId format
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ 
        error: 'ID inválido',
        message: 'El formato del ID no es válido' 
      });
    }
    
    const articulo = await Articulo.findById(id);
    
    if (!articulo) {
      return res.status(404).json({ 
        error: 'Artículo no encontrado',
        message: `No se encontró ningún artículo con el ID: ${id}` 
      });
    }
    
    res.json(articulo);
  } catch (error) {
    console.error('Error al obtener artículo:', error);
    res.status(500).json({ 
      error: 'Error interno del servidor',
      message: 'Error al obtener el artículo' 
    });
  }
});

// POST / - Create new article
router.post('/', async (req, res) => {
  console.log("POST /articulos hit");
  console.log("Body:", req.body);

  try {
    const { titulo, juego, categoria, descripcion, contenido, imagen } = req.body;
    
    // Validate required fields
    const requiredFields = ['titulo', 'juego', 'categoria', 'descripcion', 'contenido'];
    const missingFields = requiredFields.filter(field => !req.body[field]);
    
    if (missingFields.length > 0) {
      return res.status(400).json({ 
        error: 'Campos requeridos faltantes',
        message: `Los siguientes campos son requeridos: ${missingFields.join(', ')}` 
      });
    }
    
    // Create new article
    const nuevoArticulo = new Articulo({
      titulo,
      juego,
      categoria,
      descripcion,
      contenido,
      imagen: imagen || ''
    });
    
    const articuloGuardado = await nuevoArticulo.save();
    
    res.status(201).json({
      message: 'Artículo creado exitosamente',
      articulo: articuloGuardado
    });
  } catch (error) {
    console.error('Error al crear artículo:', error);
    res.status(500).json({ 
      error: 'Error interno del servidor',
      message: 'Error al crear el artículo' 
    });
  }
});

// Export router and connection function
module.exports = { router, config };

// Start standalone microservice
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
    });
  } catch (error) {
    console.error('Failed to start Article Service:', error);
    process.exit(1);
  }
}

// Start if run directly
if (require.main === module) {
  startServer();
}