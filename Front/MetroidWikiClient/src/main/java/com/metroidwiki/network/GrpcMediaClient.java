package com.metroidwiki.network;

import com.metroidwiki.network.grpc.DescargaRequest;
import com.metroidwiki.network.grpc.DescargaResponse;
import com.metroidwiki.network.grpc.ImagenRequest;
import com.metroidwiki.network.grpc.ImagenResponse;
import com.metroidwiki.network.grpc.MediaServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GrpcMediaClient {

    private static final Logger logger = Logger.getLogger(GrpcMediaClient.class.getName());
    private static final String IP_AWS = "18.224.252.1";
    private static final int PUERTO_GRPC = 50051;

    public interface UploadListener {
        void onSuccess(String nombreArchivoSeguro);
        void onError(Throwable t);
    }

    public interface DownloadListener {
        void onSuccess(File archivoDescargado);
        void onError(Throwable t);
    }

    private final ManagedChannel channel;
    private final MediaServiceGrpc.MediaServiceStub stubAsincrono;

    public GrpcMediaClient() {
        channel = ManagedChannelBuilder.forAddress(IP_AWS, PUERTO_GRPC)
                .usePlaintext()
                .build();
        stubAsincrono = MediaServiceGrpc.newStub(channel);
    }

    public void subirImagenArticulo(String idArticulo, File archivoImagen, UploadListener listener) {
        AtomicReference<String> urlImagenEnServidor = new AtomicReference<>(null);

        StreamObserver<ImagenResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ImagenResponse response) {
                urlImagenEnServidor.set(response.getUrlImagen());
                logger.info("[gRPC] Node.js dice: " + response.getMensaje());
                logger.info("Nombre guardado en BD: " + response.getUrlImagen());
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onSuccess(response.getUrlImagen()));
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "[gRPC] Falló al enviar la imagen", t);
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onError(t));
                }
                apagarCanal();
            }

            @Override
            public void onCompleted() {
                logger.info("[gRPC] Subida finalizada.");
                apagarCanal();
            }
        };

        StreamObserver<ImagenRequest> requestObserver = stubAsincrono.subirImagen(responseObserver);

        try (FileInputStream fis = new FileInputStream(archivoImagen)) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;

            logger.info("[gRPC] Enviando archivo: " + archivoImagen.getName());

            while ((bytesLeidos = fis.read(buffer)) != -1) {
                ImagenRequest request = ImagenRequest.newBuilder()
                        .setArticuloId(idArticulo)
                        .setNombreArchivo(archivoImagen.getName())
                        .setChunk(ByteString.copyFrom(buffer, 0, bytesLeidos))
                        .build();

                requestObserver.onNext(request);
            }

            requestObserver.onCompleted();

        } catch (Exception e) {
            requestObserver.onError(e);
            logger.log(Level.SEVERE, "Error interno al leer el archivo físico", e);
        }
    }

    public void descargarImagenArticulo(String nombreArchivo, String rutaDestinoLocal, DownloadListener listener) {
        File archivoSalida = new File(rutaDestinoLocal, nombreArchivo);
        File directorio = new File(rutaDestinoLocal);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        DescargaRequest request = DescargaRequest.newBuilder()
                .setNombreArchivo(nombreArchivo)
                .build();

        logger.info("[gRPC] Solicitando descarga de: " + nombreArchivo);

        stubAsincrono.descargarImagen(request, new StreamObserver<>() {
            FileOutputStream fos = null;

            @Override
            public void onNext(DescargaResponse response) {
                try {
                    if (fos == null) {
                        fos = new FileOutputStream(archivoSalida);
                    }
                    fos.write(response.getChunk().toByteArray());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error al escribir chunk en disco local", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "[gRPC] Falló al descargar la imagen", t);
                cerrarStreamLocal();
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onError(t));
                }
                apagarCanal();
            }

            @Override
            public void onCompleted() {
                cerrarStreamLocal();
                logger.info("[gRPC] Imagen descargada exitosamente en: " + archivoSalida.getAbsolutePath());
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onSuccess(archivoSalida));
                }
                apagarCanal();
            }

            private void cerrarStreamLocal() {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error cerrando FileOutputStream local", e);
                }
            }
        });
    }

    public void apagarCanal() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}