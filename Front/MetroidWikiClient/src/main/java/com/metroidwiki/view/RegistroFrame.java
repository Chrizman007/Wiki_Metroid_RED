package com.metroidwiki.view;

import com.metroidwiki.model.AuthResponse;
import com.metroidwiki.model.RegistroRequest;
import com.metroidwiki.network.AuthClient;
import com.metroidwiki.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.ConnectException;

public class RegistroFrame extends JFrame {
    // NUEVO: Campo para el nombre
    private JTextField txtNombre;
    private JTextField txtCorreo;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmarPassword;
    private JButton btnRegistrar;
    private JButton btnVolver;

    private final Color fondoOscuro = new Color(30, 30, 34);
    private final Color panelColor = new Color(45, 45, 50);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color botonSecundario = new Color(100, 100, 110);

    public RegistroFrame() {
        setTitle("Metroid Wiki - Nueva Cuenta");
        // Aumentamos un poquito la altura para que quepa el nuevo campo cómodamente
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(fondoOscuro);
        setLayout(new BorderLayout());

        JLabel lblTitulo = new JLabel("NUEVO CAZADOR", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
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

        Font fuenteLabel = new Font("Segoe UI", Font.BOLD, 14);
        Font fuenteInput = new Font("Segoe UI", Font.PLAIN, 16);

        // --- NUEVO: NOMBRE ---
        JLabel lblNombre = new JLabel("Nombre de Cazador:", SwingConstants.CENTER);
        lblNombre.setForeground(textoClaro);
        lblNombre.setFont(fuenteLabel);

        txtNombre = new JTextField();
        txtNombre.setFont(fuenteInput);
        txtNombre.setHorizontalAlignment(JTextField.CENTER);
        txtNombre.setPreferredSize(new Dimension(200, 35));

        // --- CORREO ---
        JLabel lblCorreo = new JLabel("Correo Electrónico:", SwingConstants.CENTER);
        lblCorreo.setForeground(textoClaro);
        lblCorreo.setFont(fuenteLabel);

        txtCorreo = new JTextField();
        txtCorreo.setFont(fuenteInput);
        txtCorreo.setHorizontalAlignment(JTextField.CENTER);
        txtCorreo.setPreferredSize(new Dimension(200, 35));

        // --- CONTRASEÑA ---
        JLabel lblPassword = new JLabel("Contraseña:", SwingConstants.CENTER);
        lblPassword.setForeground(textoClaro);
        lblPassword.setFont(fuenteLabel);

        txtPassword = new JPasswordField();
        txtPassword.setFont(fuenteInput);
        txtPassword.setHorizontalAlignment(JTextField.CENTER);
        txtPassword.setPreferredSize(new Dimension(200, 35));

        // --- CONFIRMAR CONTRASEÑA ---
        JLabel lblConfirmar = new JLabel("Confirmar Contraseña:", SwingConstants.CENTER);
        lblConfirmar.setForeground(textoClaro);
        lblConfirmar.setFont(fuenteLabel);

        txtConfirmarPassword = new JPasswordField();
        txtConfirmarPassword.setFont(fuenteInput);
        txtConfirmarPassword.setHorizontalAlignment(JTextField.CENTER);
        txtConfirmarPassword.setPreferredSize(new Dimension(200, 35));

        // Acomodamos en la cuadrícula
        gbc.gridy = 0; panelCampos.add(lblNombre, gbc);
        gbc.gridy = 1; panelCampos.add(txtNombre, gbc);
        gbc.gridy = 2; panelCampos.add(lblCorreo, gbc);
        gbc.gridy = 3; panelCampos.add(txtCorreo, gbc);
        gbc.gridy = 4; panelCampos.add(lblPassword, gbc);
        gbc.gridy = 5; panelCampos.add(txtPassword, gbc);
        gbc.gridy = 6; panelCampos.add(lblConfirmar, gbc);
        gbc.gridy = 7; panelCampos.add(txtConfirmarPassword, gbc);

        panelContenedor.add(panelCampos, BorderLayout.CENTER);
        add(panelContenedor, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel();
        panelInferior.setLayout(new BoxLayout(panelInferior, BoxLayout.Y_AXIS));
        panelInferior.setBackground(fondoOscuro);
        panelInferior.setBorder(new EmptyBorder(10, 0, 20, 0));

        btnRegistrar = new JButton("CREAR CUENTA");
        btnRegistrar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRegistrar.setBackground(acentoVerde);
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setOpaque(true);
        btnRegistrar.setMaximumSize(new Dimension(250, 45));
        btnRegistrar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegistrar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnVolver = new JButton("Volver al Login");
        btnVolver.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVolver.setBackground(botonSecundario);
        btnVolver.setForeground(Color.WHITE);
        btnVolver.setFocusPainted(false);
        btnVolver.setBorderPainted(false);
        btnVolver.setOpaque(true);
        btnVolver.setMaximumSize(new Dimension(250, 35));
        btnVolver.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnRegistrar.addActionListener(e -> intentarRegistro());
        btnVolver.addActionListener(e -> dispose());

        panelInferior.add(btnRegistrar);
        panelInferior.add(Box.createRigidArea(new Dimension(0, 10)));
        panelInferior.add(btnVolver);

        add(panelInferior, BorderLayout.SOUTH);
    }

    private void intentarRegistro() {
        String nombre = txtNombre.getText(); // Extraemos el nuevo campo
        String correo = txtCorreo.getText();
        String password = new String(txtPassword.getPassword());
        String confirmar = new String(txtConfirmarPassword.getPassword());

        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, llena todos los campos.", "Campos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmar)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error de validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("REGISTRANDO...");

        try {
            AuthClient client = RetrofitClient.getClient().create(AuthClient.class);
            // Ahora le mandamos el nombre también en la petición
            RegistroRequest request = new RegistroRequest(nombre, correo, password);

            client.registrarUsuario(request).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        JOptionPane.showMessageDialog(RegistroFrame.this, "¡Cazador registrado exitosamente!\nYa puedes iniciar sesión.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        if (response.code() == 400) {
                            JOptionPane.showMessageDialog(RegistroFrame.this, "El correo ya está en uso o los datos son inválidos.", "Error de Registro (400)", JOptionPane.ERROR_MESSAGE);
                        } else if (response.code() == 500) {
                            JOptionPane.showMessageDialog(RegistroFrame.this, "Error interno en el servidor de Node.js.", "Error de Servidor (500)", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(RegistroFrame.this, "No se pudo registrar. Código HTTP: " + response.code(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    if (t instanceof ConnectException) {
                        JOptionPane.showMessageDialog(RegistroFrame.this, "Fallo al conectar con el API Gateway.\nVerifica que Node.js esté corriendo.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                    } else if (t instanceof IOException) {
                        JOptionPane.showMessageDialog(RegistroFrame.this, "Error de I/O en la red: " + t.getMessage(), "Error de Red", JOptionPane.ERROR_MESSAGE);
                    } else if (t instanceof IllegalArgumentException) {
                        JOptionPane.showMessageDialog(RegistroFrame.this, "Error procesando la solicitud: " + t.getMessage(), "Error Interno", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(RegistroFrame.this, "Fallo crítico en la comunicación de red.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Error de configuración de red interna.", "Error de Configuración", JOptionPane.ERROR_MESSAGE);
            restaurarBoton();
        }
    }

    private void restaurarBoton() {
        btnRegistrar.setEnabled(true);
        btnRegistrar.setText("CREAR CUENTA");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new RegistroFrame().setVisible(true);
        });
    }
}