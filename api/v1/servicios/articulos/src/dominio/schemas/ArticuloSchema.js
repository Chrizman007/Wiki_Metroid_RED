const mongoose = require("mongoose");

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