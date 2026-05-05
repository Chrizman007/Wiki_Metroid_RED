const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const fs = require('fs');
const path = require('path');

// 1. Cargar el contrato .proto
const PROTO_PATH = path.join(__dirname, 'media.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});
const mediaProto = grpc.loadPackageDefinition(packageDefinition).metroid_media;

// 2. Ruta donde guardaremos las imágenes
const UPLOAD_DIR = path.join(__dirname, '..', 'public', 'imagenes');

// 3. Lógica Estricta de Subida de Archivos
const subirImagen = (call, callback) => {
  const { nombre_archivo, datos_imagen } = call.request;

  // Validación: Excepción específica si faltan datos
  if (!nombre_archivo || !datos_imagen || datos_imagen.length === 0) {
    return callback({
      code: grpc.status.INVALID_ARGUMENT,
      details: 'El nombre del archivo y los datos de la imagen son obligatorios.'
    });
  }

  // Limpiar el nombre del archivo por seguridad (quitar espacios o caracteres raros)
  const nombreSeguro = nombre_archivo.replace(/[^a-zA-Z0-9.\-_]/g, '_');
  const rutaCompleta = path.join(UPLOAD_DIR, nombreSeguro);

  // Escribir el archivo físico en la carpeta public/imagenes
  fs.writeFile(rutaCompleta, datos_imagen, (error) => {
    if (error) {
      console.error('Error del sistema de archivos:', error);
      // Excepción específica de sistema de archivos para gRPC
      return callback({
        code: grpc.status.INTERNAL,
        details: `Fallo al escribir el archivo en el disco: ${error.message}`
      });
    }

    // Éxito: Devolvemos el DTO de respuesta
    callback(null, {
      exito: true,
      mensaje: 'Imagen subida y guardada correctamente',
      ruta_archivo: `/public/imagenes/${nombreSeguro}`
    });
  });
};

// 4. Iniciar el Servidor gRPC
const iniciarServidorGrpc = () => {
  const server = new grpc.Server();
  server.addService(mediaProto.MediaService.service, { SubirImagen: subirImagen });
  
  const puertoGrpc = '0.0.0.0:50051';
  server.bindAsync(puertoGrpc, grpc.ServerCredentials.createInsecure(), (error, port) => {
    if (error) {
      console.error('Error al iniciar gRPC:', error);
      return;
    }
    console.log(`Metroid Wiki gRPC Media Server corriendo en el puerto ${port}`);
    console.log(`Las imágenes se guardarán en: ${UPLOAD_DIR}`);
  });
};

// Si se ejecuta directamente
if (require.main === module) {
  iniciarServidorGrpc();
}

module.exports = { iniciarServidorGrpc };
