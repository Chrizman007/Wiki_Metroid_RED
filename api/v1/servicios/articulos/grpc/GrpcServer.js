const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const fs = require('fs');
const path = require('path');
const PROTO_PATH = path.join(__dirname, 'media.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});

const mediaProto = grpc.loadPackageDefinition(packageDefinition).media;
const UPLOAD_DIR = path.join(__dirname, '..', 'public', 'imagenes');

if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

const subirImagen = (call, callback) => {
  let nombreArchivo = '';
  let fileBuffer = [];

  call.on('data', (request) => {
    if (request.nombreArchivo && !nombreArchivo) {
      nombreArchivo = request.nombreArchivo;
    }
    if (request.chunk) {
      fileBuffer.push(request.chunk);
    }
  });

  call.on('end', () => {
    if (!nombreArchivo || fileBuffer.length === 0) {
      return callback({
        code: grpc.status.INVALID_ARGUMENT,
        details: 'El nombre del archivo y los datos son obligatorios.'
      });
    }

    const nombreSeguro = nombreArchivo.replace(/[^a-zA-Z0-9.\-_]/g, '_');
    const rutaCompleta = path.join(UPLOAD_DIR, nombreSeguro);
    const datosImagen = Buffer.concat(fileBuffer);

    fs.writeFile(rutaCompleta, datosImagen, (error) => {
      if (error) {
        console.error('Error del sistema de archivos:', error);
        return callback({
          code: grpc.status.INTERNAL,
          details: `Fallo al escribir el archivo: ${error.message}`
        });
      }

      callback(null, {
        exito: true,
        mensaje: 'Imagen subida correctamente a AWS',
        urlImagen: nombreSeguro
      });
    });
  });
};

const descargarImagen = (call) => {
  const nombreArchivo = call.request.nombreArchivo;

  if (!nombreArchivo) {
    call.emit('error', {
      code: grpc.status.INVALID_ARGUMENT,
      details: 'El nombre del archivo es obligatorio para descargar.'
    });
    return;
  }

  const nombreSeguro = nombreArchivo.replace(/[^a-zA-Z0-9.\-_]/g, '_');
  const rutaCompleta = path.join(UPLOAD_DIR, nombreSeguro);

  if (!fs.existsSync(rutaCompleta)) {
    call.emit('error', {
      code: grpc.status.NOT_FOUND,
      details: 'La imagen solicitada no existe en el servidor.'
    });
    return;
  }

  const readStream = fs.createReadStream(rutaCompleta, { highWaterMark: 1024 * 64 });

  readStream.on('data', (chunk) => {
    call.write({ chunk: chunk });
  });

  readStream.on('end', () => {
    call.end();
  });

  readStream.on('error', (error) => {
    console.error('Error al leer la imagen para descarga:', error);
    call.emit('error', {
      code: grpc.status.INTERNAL,
      details: 'Error interno al leer el archivo físico.'
    });
  });
};


const iniciarServidorGrpc = () => {
  const server = new grpc.Server();
  
  server.addService(mediaProto.MediaService.service, { 
    SubirImagen: subirImagen,
    DescargarImagen: descargarImagen 
  });
  
  const puertoGrpc = '0.0.0.0:50051';
  server.bindAsync(puertoGrpc, grpc.ServerCredentials.createInsecure(), (error, port) => {
    if (error) {
      console.error('Error al iniciar gRPC:', error);
      return;
    }
    console.log(`[gRPC] Metroid Wiki Media Server corriendo en ${puertoGrpc}`);
    console.log(`[gRPC] Almacenando multimedia en: ${UPLOAD_DIR}`);
  });
};

if (require.main === module) {
  iniciarServidorGrpc();
}

module.exports = { iniciarServidorGrpc };