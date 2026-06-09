package com.metroidwiki.network;

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
import java.util.concurrent.atomic.AtomicReference;

public class GrpcMediaClient {

    public interface UploadListener {
        void onSuccess(String urlImagen);
        void onError(Throwable t);
    }

    private final ManagedChannel channel;
    // 🛠️ CAMBIO CLAVE: Usamos el Stub Asíncrono para no congelar la pantalla de Java
    private final MediaServiceGrpc.MediaServiceStub stubAsincrono;

    public GrpcMediaClient() {
        channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Red local
                .build();
        stubAsincrono = MediaServiceGrpc.newStub(channel);
    }

    // 🛠️ CAMBIO CLAVE: Método que pica la imagen en pedazos de 4KB (Stream)
    public void subirImagenArticulo(String idArticulo, File archivoImagen) {
        subirImagenArticulo(idArticulo, archivoImagen, null);
    }

    public void subirImagenArticulo(String idArticulo, File archivoImagen, UploadListener listener) {
        AtomicReference<String> urlImagenEnServidor = new AtomicReference<>(null);

        // Preparamos el "oído" para escuchar lo que nos responda Node.js
        StreamObserver<ImagenResponse> responseObserver = new StreamObserver<ImagenResponse>() {
            @Override
            public void onNext(ImagenResponse response) {
                urlImagenEnServidor.set(response.getUrlImagen());
                System.out.println("✅ [gRPC] Node.js dice: " + response.getMensaje());
                System.out.println("🔗 URL en el servidor: " + response.getUrlImagen());
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onSuccess(response.getUrlImagen()));
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ [gRPC] Falló al enviar la imagen: " + t.getMessage());
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onError(t));
                }
                apagarCanal();
            }

            @Override
            public void onCompleted() {
                System.out.println("🏁 [gRPC] Imagen subida exitosamente.");
                apagarCanal();
            }
        };

        // Abrimos el tubo de envío hacia Node.js
        StreamObserver<ImagenRequest> requestObserver = stubAsincrono.subirImagen(responseObserver);

        // Leemos el archivo físico de tu computadora y lo mandamos en pedacitos
        try (FileInputStream fis = new FileInputStream(archivoImagen)) {
            byte[] buffer = new byte[4096]; // Pedacitos de 4 KB
            int bytesLeidos;

            System.out.println("🚀 [gRPC] Enviando archivo: " + archivoImagen.getName());

            while ((bytesLeidos = fis.read(buffer)) != -1) {
                ImagenRequest request = ImagenRequest.newBuilder()
                        .setArticuloId(idArticulo)
                        .setNombreArchivo(archivoImagen.getName())
                        .setChunk(ByteString.copyFrom(buffer, 0, bytesLeidos))
                        .build();

                requestObserver.onNext(request); // Disparamos el pedacito
            }

            requestObserver.onCompleted(); // Le decimos a Node.js "Ya terminé"

        } catch (Exception e) {
            requestObserver.onError(e);
            System.err.println("Error interno al leer el archivo: " + e.getMessage());
        }
    }

    // Cerramos el canal cuando apagamos el cliente
    public void apagarCanal() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}