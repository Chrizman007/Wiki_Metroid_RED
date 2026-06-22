package com.metroidwiki.view;

import com.metroidwiki.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.metroidwiki.exception.ErrorConsumoAPIException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

public class DevConsoleFrame extends JFrame {

    private static final String FONT_MONOSPACED = "Monospaced";
    private static final String FONT_SEGOE = "Segoe UI";

    private final Color fondoPrincipal = new Color(20, 20, 22);
    private final Color panelSecundario = new Color(30, 30, 35);
    private final Color acentoNaranja = new Color(255, 110, 64);
    private final Color textoClaro = new Color(0, 255, 100);

    private JTextArea txtJsonOutput;
    private JTextField txtEndpoint;

    private transient String tokenJwt;

    public DevConsoleFrame(String token) {
        this.tokenJwt = token;

        setTitle("Terminal de Diagnóstico de Red - Protocolo gRPC/REST");
        setSize(750, 550);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(fondoPrincipal);
        setLayout(new BorderLayout(15, 15));

        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));
        panelSuperior.setBackground(fondoPrincipal);
        panelSuperior.setBorder(new EmptyBorder(15, 15, 10, 15));

        JLabel lblToken = new JLabel("TOKEN JWT ACTIVO (CREDENCIALES DE DESARROLLADOR):");
        lblToken.setFont(new Font(FONT_SEGOE, Font.BOLD, 11));
        lblToken.setForeground(Color.WHITE);

        JTextField txtToken = new JTextField(tokenJwt);
        txtToken.setEditable(false);
        txtToken.setBackground(panelSecundario);
        txtToken.setForeground(Color.GRAY);
        txtToken.setFont(new Font(FONT_MONOSPACED, Font.PLAIN, 11));

        JPanel panelUrl = new JPanel(new BorderLayout(10, 0));
        panelUrl.setBackground(fondoPrincipal);
        panelUrl.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lblUrlBase = new JLabel(RetrofitClient.BASE_URL);
        lblUrlBase.setFont(new Font(FONT_MONOSPACED, Font.BOLD, 13));
        lblUrlBase.setForeground(acentoNaranja);

        txtEndpoint = new JTextField("articulos");
        txtEndpoint.setBackground(panelSecundario);
        txtEndpoint.setForeground(Color.WHITE);
        txtEndpoint.setFont(new Font(FONT_MONOSPACED, Font.PLAIN, 13));

        JButton btnEnviar = new JButton("CONSUMIR API");
        btnEnviar.setBackground(acentoNaranja);
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFont(new Font(FONT_SEGOE, Font.BOLD, 12));
        btnEnviar.setFocusPainted(false);
        btnEnviar.setBorderPainted(false);
        btnEnviar.setOpaque(true);
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnviar.setBorder(new EmptyBorder(5, 15, 5, 15));
        btnEnviar.addActionListener(e -> ejecutarPeticionCruda());

        panelUrl.add(lblUrlBase, BorderLayout.WEST);
        panelUrl.add(txtEndpoint, BorderLayout.CENTER);
        panelUrl.add(btnEnviar, BorderLayout.EAST);

        panelSuperior.add(lblToken);
        panelSuperior.add(Box.createRigidArea(new Dimension(0, 5)));
        panelSuperior.add(txtToken);
        panelSuperior.add(panelUrl);

        add(panelSuperior, BorderLayout.NORTH);

        txtJsonOutput = new JTextArea("// Presiona 'CONSUMIR API' para interceptar la respuesta REST de Node.js...");
        txtJsonOutput.setBackground(Color.BLACK);
        txtJsonOutput.setForeground(textoClaro);
        txtJsonOutput.setFont(new Font(FONT_MONOSPACED, Font.PLAIN, 13)); // 🛠️ Uso de Constante
        txtJsonOutput.setEditable(false);
        txtJsonOutput.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollOutput = new JScrollPane(txtJsonOutput);
        scrollOutput.setBorder(BorderFactory.createLineBorder(panelSecundario, 1));

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBackground(fondoPrincipal);
        panelCentro.setBorder(new EmptyBorder(0, 15, 15, 15));
        panelCentro.add(scrollOutput, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.CENTER);
    }

    @SuppressWarnings("squid:S2095")
    private void ejecutarPeticionCruda() {
        txtJsonOutput.setText("📡 Conectando con " + RetrofitClient.BASE_URL + txtEndpoint.getText() + "...\nInyectando cabecera Authorization...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws ErrorConsumoAPIException {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    String urlDestino = RetrofitClient.BASE_URL + txtEndpoint.getText();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(urlDestino))
                            .GET()
                            .header("Authorization", "Bearer " + tokenJwt)
                            .header("Accept", "application/json")
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                        String cuerpoError = response.body();
                        String errorBackend = "HttpError_" + response.statusCode();

                        try {
                            JsonObject jsonObject = new Gson().fromJson(cuerpoError, JsonObject.class);
                            if (jsonObject != null && jsonObject.has("error")) {
                                errorBackend = jsonObject.get("error").getAsString();
                            }
                        } catch (com.google.gson.JsonSyntaxException e) {
                            System.err.println("🚨 [DevConsole] Error de sintaxis al parsear JSON. La respuesta no es un JSON válido.");
                            System.err.println("Cuerpo recibido: " + cuerpoError);
                            errorBackend = "NonJsonResponseBody";
                        } catch (Exception e) {
                            System.err.println("🚨 [DevConsole] Error inesperado en el procesamiento de la respuesta fallida: " + e.getMessage());
                            errorBackend = "UnparseableResponseBody";
                        }

                        throw new ErrorConsumoAPIException("El servidor respondió con código de error", response.statusCode(), errorBackend);
                    }

                    return response.body();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ErrorConsumoAPIException("Conexión interrumpida por el sistema", 500, "SystemInterrupted");
                } catch (java.io.IOException ex) {
                    throw new ErrorConsumoAPIException("Fallo crítico de comunicación física con el API Gateway", 500, "GatewayUnreachable");
                }
            }

            @Override
            protected void done() {
                try {
                    String jsonPuro = get();
                    txtJsonOutput.setText(jsonPuro.replace("{", "{\n  ").replace(",", ",\n  ").replace("}", "\n}"));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    txtJsonOutput.setText("OPERACIÓN CANCELADA / INTERRUMPIDA.");
                } catch (ExecutionException ee) {
                    if (ee.getCause() instanceof ErrorConsumoAPIException) {
                        ErrorConsumoAPIException exEspecifica = (ErrorConsumoAPIException) ee.getCause();
                        txtJsonOutput.setText("🚨 RUPTURA DE PROTOCOLO HTTP 🚨"
                                + "\n\nMensaje: " + exEspecifica.getMessage()
                                + "\nIdentificador (error): " + exEspecifica.getErrorBackend()
                                + "\nCódigo Estado HTTP: " + exEspecifica.getCodigoRespuesta());
                    } else {
                        txtJsonOutput.setText("❌ Error desconocido en la consola interna.");
                    }
                }
            }
        };
        worker.execute();
    }
}