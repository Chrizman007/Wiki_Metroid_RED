package com.metroidwiki.view;

import com.metroidwiki.network.RetrofitClient;
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

    // CONSTANTES DE CLEAN CODE
    private static final String FONT_MONOSPACED = "Monospaced";
    private static final String FONT_SEGOE = "Segoe UI";

    private final Color fondoPrincipal = new Color(20, 20, 22);
    private final Color panelSecundario = new Color(30, 30, 35);
    private final Color acentoNaranja = new Color(255, 110, 64);
    private final Color textoClaro = new Color(0, 255, 100);

    private JTextArea txtJsonOutput;
    private JTextField txtEndpoint;

    //HECHO TRANSIENT POR BUENAS PRÁCTICAS
    private transient String tokenJwt;

    public DevConsoleFrame(String token) {
        this.tokenJwt = token;

        setTitle("Terminal de Diagnóstico de Red - Protocolo gRPC/REST");
        setSize(750, 550);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // 🛠️ Corregido a WindowConstants
        setLocationRelativeTo(null);
        getContentPane().setBackground(fondoPrincipal);
        setLayout(new BorderLayout(15, 15));

        // --- Panel Superior: Token e Input ---
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
        txtToken.setFont(new Font(FONT_MONOSPACED, Font.PLAIN, 11)); // 🛠️ Uso de Constante

        JPanel panelUrl = new JPanel(new BorderLayout(10, 0));
        panelUrl.setBackground(fondoPrincipal);
        panelUrl.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lblUrlBase = new JLabel(RetrofitClient.BASE_URL);
        lblUrlBase.setFont(new Font(FONT_MONOSPACED, Font.BOLD, 13)); // 🛠️ Uso de Constante
        lblUrlBase.setForeground(acentoNaranja);

        txtEndpoint = new JTextField("articulos");
        txtEndpoint.setBackground(panelSecundario);
        txtEndpoint.setForeground(Color.WHITE);
        txtEndpoint.setFont(new Font(FONT_MONOSPACED, Font.PLAIN, 13)); // 🛠️ Uso de Constante

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

        // --- Panel Central: Output JSON Estilo Terminal ---
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

    @SuppressWarnings("squid:S2095") // 🛡️ Escudo protector: Evita advertencia de AutoCloseable para mantener compatibilidad con Java 11/17
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
                        throw new ErrorConsumoAPIException("El servidor respondió con código de error", response.statusCode());
                    }

                    return response.body();
                } catch (InterruptedException ex) {
                    //Hilos: Notificamos al sistema que la operación fue interrumpida
                    Thread.currentThread().interrupt();
                    throw new ErrorConsumoAPIException("Conexión interrumpida por el sistema", 500);
                } catch (java.io.IOException ex) {
                    throw new ErrorConsumoAPIException("Fallo crítico de comunicación física con el API Gateway", 500);
                }
            }

            @Override
            protected void done() {
                try {
                    String jsonPuro = get();
                    txtJsonOutput.setText(jsonPuro.replace("{", "{\n  ").replace(",", ",\n  ").replace("}", "\n}"));
                } catch (InterruptedException ie) {
                    //Hilos: Restauramos la interrupción aquí también
                    Thread.currentThread().interrupt();
                    txtJsonOutput.setText("❌ OPERACIÓN CANCELADA / INTERRUMPIDA.");
                } catch (ExecutionException ee) {
                    if (ee.getCause() instanceof ErrorConsumoAPIException) {
                        ErrorConsumoAPIException exEspecifica = (ErrorConsumoAPIException) ee.getCause();
                        txtJsonOutput.setText("❌ RUPTURA DE PROTOCOLO HTTP:\n" + exEspecifica.getMessage() + "\nCódigo HTTP recibido: " + exEspecifica.getCodigoRespuesta());
                    } else {
                        txtJsonOutput.setText("❌ Error desconocido en la consola interna.");
                    }
                }
            }
        };
        worker.execute();
    }
}