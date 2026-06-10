const mongoose = require('mongoose');

// Definimos el "molde" estricto para los artículos de la Metroid Wiki
const articuloSchema = new mongoose.Schema({
    titulo: {
        type: String,
        required: [true, 'El título del artículo es obligatorio'],
        trim: true,
        unique: true
    },
    descripcion: {
        type: String,
        required: [true, 'La descripción corta es obligatoria'],
        trim: true
    },
    contenido: {
        type: String,
        required: [true, 'El contenido del artículo es obligatorio']
    },
    categoria: {
        type: String,
        enum: ['Lore', 'Items', 'Enemigos', 'ubicaciones', 'personajes'],
        required: [true, 'La categoría es obligatoria']
    },
    imagen: {
        type: String,
        default: ''
    },
    estado: {
        type: String,
        enum: ['EnBorrador', 'EnRevision', 'Publicado', 'Archivado'], 
        default: 'EnBorrador'
    },
    vistas: {
        type: Number,
        default: 0
    },
    autorId: {
        type: mongoose.Schema.Types.ObjectId,
        required: [true, 'El ID del autor es obligatorio']
    }
}, {
    timestamps: true // Genera automáticamente fechaCreacion (y fechaActualizacion)
});

// Exportamos el modelo
module.exports = mongoose.model('Articulo', articuloSchema);