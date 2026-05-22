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