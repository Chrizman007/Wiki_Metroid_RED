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

exports.ArticuloDTO = ArticuloDTO;