const mongoose = require('mongoose');

// Definimos el "molde" estricto para la base de datos
const usuarioSchema = new mongoose.Schema({
    username: {
        type: String,
        required: [true, 'El nombre de usuario es obligatorio'],
        unique: true, // No pueden existir dos personas con el mismo nombre
        trim: true    // Quita espacios en blanco al inicio y al final
    },
    password: {
        type: String,
        required: [true, 'La contraseña es obligatoria']
    },
    rol: {
        type: String,
        enum: ['lector', 'administrador', 'desarrollador'], // Los 3 roles oficiales
        default: 'lector'          // Todo el que se registre desde Java será lector por defecto
    }
}, {
    timestamps: true // Magia pura: MongoDB agregará automáticamente "fecha de creación" a cada registro
});

// Exportamos el modelo para poder usarlo en el resto del proyecto
module.exports = mongoose.model('Usuario', usuarioSchema);