package com.metroidwiki.view;

import com.metroidwiki.model.LoginRequest;
import com.metroidwiki.model.AuthResponse;
import com.metroidwiki.network.AuthClient;
import com.metroidwiki.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.ConnectException;

public class LoginFrame extends JFrame {
    private JTextField txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private static final Logger logger = Logger.getLogger(LoginFrame.class.getName());

    private static final String FONT_SEGOE = "Segoe UI";
    private static final String TITULO_ERROR = "Error";

    private final Color fondoOscuro = new Color(30, 30, 34);
    private final Color panelColor = new Color(45, 45, 50);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color enlaceColor = new Color(100, 200, 105);

    public LoginFrame() {
        setTitle("Metroid Wiki - Portal de Acceso");
        setSize(400, 440);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(fondoOscuro);
        setLayout(new BorderLayout());

        JLabel lblTitulo = new JLabel("METROID WIKI", SwingConstants.CENTER);
        lblTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 26));
        lblTitulo.setForeground(acentoVerde);
        lblTitulo.setBorder(new EmptyBorder(20, 0, 15, 0));
        add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCampos = new JPanel(new GridBagLayout());
        panelCampos.setBackground(panelColor);

        JPanel panelContenedor = new JPanel(new BorderLayout());
        panelContenedor.setBackground(fondoOscuro);
        panelContenedor.setBorder(new EmptyBorder(0, 30, 10, 30));

        panelCampos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(acentoVerde, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        Font fuenteLabel = new Font(FONT_SEGOE, Font.BOLD, 14);
        Font fuenteInput = new Font(FONT_SEGOE, Font.PLAIN, 16);

        JLabel lblCorreo = new JLabel("Correo Electrónico:", SwingConstants.CENTER);
        lblCorreo.setForeground(textoClaro);
        lblCorreo.setFont(fuenteLabel);

        txtCorreo = new JTextField();
        txtCorreo.setFont(fuenteInput);
        txtCorreo.setHorizontalAlignment(SwingConstants.CENTER); // Corregido: SwingConstants
        txtCorreo.setPreferredSize(new Dimension(200, 35));

        JLabel lblPassword = new JLabel("Contraseña:", SwingConstants.CENTER);
        lblPassword.setForeground(textoClaro);
        lblPassword.setFont(fuenteLabel);

        txtPassword = new JPasswordField();
        txtPassword.setFont(fuenteInput);
        txtPassword.setHorizontalAlignment(SwingConstants.CENTER); // Corregido: SwingConstants
        txtPassword.setPreferredSize(new Dimension(200, 35));

        JCheckBox chkMostrar = new JCheckBox("Mostrar contraseña");
        chkMostrar.setBackground(panelColor);
        chkMostrar.setForeground(textoClaro);
        chkMostrar.setFont(new Font(FONT_SEGOE, Font.PLAIN, 12));
        chkMostrar.setFocusPainted(false);
        chkMostrar.setHorizontalAlignment(SwingConstants.CENTER);
        chkMostrar.addActionListener(e -> {
            if (chkMostrar.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });

        gbc.gridy = 0; panelCampos.add(lblCorreo, gbc);
        gbc.gridy = 1; panelCampos.add(txtCorreo, gbc);
        gbc.gridy = 2; panelCampos.add(lblPassword, gbc);
        gbc.gridy = 3; panelCampos.add(txtPassword, gbc);
        gbc.gridy = 4; panelCampos.add(chkMostrar, gbc);

        panelContenedor.add(panelCampos, BorderLayout.CENTER);
        add(panelContenedor, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel();
        panelInferior.setLayout(new BoxLayout(panelInferior, BoxLayout.Y_AXIS));
        panelInferior.setBackground(fondoOscuro);
        panelInferior.setBorder(new EmptyBorder(10, 0, 25, 0));

        btnLogin = new JButton("INICIAR SESIÓN");
        btnLogin.setFont(new Font(FONT_SEGOE, Font.BOLD, 14));
        btnLogin.setBackground(acentoVerde);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setMaximumSize(new Dimension(250, 45));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> intentarLogin());

        JLabel lblCrearCuenta = new JLabel("Crear cuenta nueva");
        lblCrearCuenta.setFont(new Font(FONT_SEGOE, Font.PLAIN, 13));
        lblCrearCuenta.setForeground(enlaceColor);
        lblCrearCuenta.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblCrearCuenta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lblCrearCuenta.setText("<html><u>Crear cuenta nueva</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblCrearCuenta.setText("Crear cuenta nueva");
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                RegistroFrame registro = new RegistroFrame();
                registro.setVisible(true);
            }
        });

        JPanel panelEnlace = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelEnlace.setBackground(fondoOscuro);
        panelEnlace.setMaximumSize(new Dimension(300, 30));
        panelEnlace.add(lblCrearCuenta);

        panelInferior.add(btnLogin);
        panelInferior.add(Box.createRigidArea(new Dimension(0, 15)));
        panelInferior.add(panelEnlace);

        add(panelInferior, BorderLayout.SOUTH);
    }

    @SuppressWarnings("squid:S3776")
    private void intentarLogin() {
        String correo = txtCorreo.getText();
        String password = new String(txtPassword.getPassword());

        if (correo.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, llena todos los campos.", "Campos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("CONECTANDO...");

        try {
            AuthClient client = RetrofitClient.getClient().create(AuthClient.class);
            LoginRequest request = new LoginRequest(correo, password);

            client.iniciarSesion(request).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        String authHeader = response.headers().get("Authorization");

                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            JOptionPane.showMessageDialog(
                                    LoginFrame.this,
                                    "No se recibió un token JWT válido.",
                                    "Error de autenticación",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            return;
                        }

                        String token = authHeader.substring(7);
                        String nombreCazador = response.body().getNombre();
                        String rolCazador = response.body().getRol();

                        logger.info("⚠️ DEBUG ROL DESDE NODE: " + rolCazador);
                        logger.info("TOKEN JWT RECIBIDO: " + token);
                        logger.info("CAZADOR CONECTADO: " + nombreCazador);
                        logger.info("NIVEL DE ACCESO: " + rolCazador);

                        WikiMainFrame ventanaPrincipal = new WikiMainFrame(token, nombreCazador, rolCazador);
                        ventanaPrincipal.setVisible(true);

                        dispose();
                    } else {
                        if (response.code() == 401) {
                            JOptionPane.showMessageDialog(LoginFrame.this, "Credenciales incorrectas. Verifica tu correo y contraseña.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(LoginFrame.this, "Error HTTP: " + response.code(), TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    if (t instanceof ConnectException) {
                        JOptionPane.showMessageDialog(LoginFrame.this, "No se pudo conectar al API Gateway.", "Fallo de Conexión", JOptionPane.ERROR_MESSAGE);
                    } else if (t instanceof IOException) {
                        JOptionPane.showMessageDialog(LoginFrame.this, "Error de red: " + t.getMessage(), TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Error interno de ejecución", ex);
            JOptionPane.showMessageDialog(this, "Error interno de red.", TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
            restaurarBoton();
        }
    }

    private void restaurarBoton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("INICIAR SESIÓN");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            logger.log(Level.WARNING, "No se pudo cargar el estilo visual del sistema", e);
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}