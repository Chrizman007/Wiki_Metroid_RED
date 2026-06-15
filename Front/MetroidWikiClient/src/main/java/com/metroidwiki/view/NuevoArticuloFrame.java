package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloRequest;
import com.metroidwiki.model.ArticuloResponse;
import com.metroidwiki.network.ArticuloClient;
import com.metroidwiki.network.RetrofitClient;
// 🛠️ IMPORTAMOS NUESTRO CLIENTE gRPC
import com.metroidwiki.network.GrpcMediaClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.io.File;
import javax.swing.SwingUtilities;

public class NuevoArticuloFrame extends JFrame {

    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private JTextField txtTitulo;
    private JComboBox<String> cmbCategoria;
    private JTextArea txtDescripcion;
    private JTextArea txtContenido;
    private JTextField txtRutaImagen;
    private JButton btnSeleccionarImagen;
    private JButton btnGuardar;

    private String tokenJwt;
    private File archivoImagenSeleccionado;

    public NuevoArticuloFrame(String token) {
        this.tokenJwt = token;

        setTitle("Redactar Nuevo Artículo");
        setSize(650, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(fondoPrincipal);
        setLayout(new BorderLayout(10, 10));

        // --- CABECERA ---
        JLabel lblTituloVentana = new JLabel("NUEVO REGISTRO EN LA WIKI", SwingConstants.CENTER);
        lblTituloVentana.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTituloVentana.setForeground(acentoVerde);
        lblTituloVentana.setBorder(new EmptyBorder(15, 0, 10, 0));
        add(lblTituloVentana, BorderLayout.NORTH);

        // --- FORMULARIO CENTRAL ---
        JPanel panelFormulario = new JPanel();
        panelFormulario.setLayout(new BoxLayout(panelFormulario, BoxLayout.Y_AXIS));
        panelFormulario.setBackground(fondoPrincipal);
        panelFormulario.setBorder(new EmptyBorder(10, 30, 10, 30));

        Font fuenteLabel = new Font("Segoe UI", Font.BOLD, 14);

        // Título
        JLabel lblTitulo = new JLabel("Título del Artículo:");
        lblTitulo.setForeground(textoClaro);
        lblTitulo.setFont(fuenteLabel);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtTitulo = new JTextField();
        estilizarInput(txtTitulo);
        txtTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Categoría
        JLabel lblCategoria = new JLabel("Categoría:");
        lblCategoria.setForeground(textoClaro);
        lblCategoria.setFont(fuenteLabel);
        lblCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] categorias = {"Lore", "Items", "Enemigos", "Ubicaciones", "Personajes"};
        cmbCategoria = new JComboBox<>(categorias);
        cmbCategoria.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        cmbCategoria.setBackground(panelSecundario);
        cmbCategoria.setForeground(textoClaro);
        cmbCategoria.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmbCategoria.setUI(new BasicComboBoxUI());
        cmbCategoria.setOpaque(true);
        cmbCategoria.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                list.setBackground(panelSecundario);
                list.setSelectionBackground(acentoVerde);
                list.setSelectionForeground(Color.WHITE);

                if (isSelected) {
                    item.setBackground(acentoVerde);
                    item.setForeground(Color.WHITE);
                } else {
                    item.setBackground(panelSecundario);
                    item.setForeground(textoClaro);
                }

                item.setBorder(new EmptyBorder(5, 10, 5, 10));
                return item;
            }
        });

        // Multimedia
        JLabel lblMultimedia = new JLabel("Archivo Multimedia:");
        lblMultimedia.setForeground(textoClaro);
        lblMultimedia.setFont(fuenteLabel);
        lblMultimedia.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelImagenSelector = new JPanel(new BorderLayout(10, 0));
        panelImagenSelector.setBackground(fondoPrincipal);
        panelImagenSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panelImagenSelector.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtRutaImagen = new JTextField("No se ha seleccionado ningún archivo multimedia...");
        txtRutaImagen.setEditable(false);
        estilizarInput(txtRutaImagen);
        txtRutaImagen.setForeground(textoGris);

        btnSeleccionarImagen = new JButton("Buscar...");
        btnSeleccionarImagen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSeleccionarImagen.setBackground(panelSecundario);
        btnSeleccionarImagen.setForeground(Color.WHITE);
        btnSeleccionarImagen.setFocusPainted(false);
        btnSeleccionarImagen.setOpaque(true);
        btnSeleccionarImagen.setBorderPainted(false);
        btnSeleccionarImagen.addActionListener(e -> abrirSelectorImagen());

        panelImagenSelector.add(txtRutaImagen, BorderLayout.CENTER);
        panelImagenSelector.add(btnSeleccionarImagen, BorderLayout.EAST);

        // Descripción Breve
        JLabel lblDesc = new JLabel("Descripción Breve:");
        lblDesc.setForeground(textoClaro);
        lblDesc.setFont(fuenteLabel);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtDescripcion = new JTextArea(3, 20);
        estilizarTextArea(txtDescripcion);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        scrollDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Contenido Completo
        JLabel lblContenido = new JLabel("Contenido / Lore:");
        lblContenido.setForeground(textoClaro);
        lblContenido.setFont(fuenteLabel);
        lblContenido.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtContenido = new JTextArea(8, 20);
        estilizarTextArea(txtContenido);
        JScrollPane scrollContenido = new JScrollPane(txtContenido);
        scrollContenido.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Añadir componentes en orden al contenedor
        panelFormulario.add(lblTitulo); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(txtTitulo); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblCategoria); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(cmbCategoria); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblMultimedia); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(panelImagenSelector); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblDesc); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(scrollDesc); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblContenido); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(scrollContenido);

        add(panelFormulario, BorderLayout.CENTER);

        // --- BOTONES INFERIORES ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBotones.setBackground(fondoPrincipal);
        panelBotones.setBorder(new EmptyBorder(10, 0, 20, 0));

        btnGuardar = new JButton("GUARDAR ARTÍCULO");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGuardar.setBackground(acentoVerde);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setPreferredSize(new Dimension(200, 40));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);
        btnGuardar.addActionListener(e -> enviarArticulo());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelar.setBackground(new Color(100, 100, 100));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setOpaque(true);
        btnCancelar.setBorderPainted(false);
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void estilizarInput(JTextField campo) {
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        campo.setBackground(panelSecundario);
        campo.setForeground(Color.WHITE);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        campo.setOpaque(true);
    }

    private void estilizarTextArea(JTextArea area) {
        area.setBackground(panelSecundario);
        area.setForeground(Color.WHITE);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        area.setOpaque(true);
    }

    private void abrirSelectorImagen() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccionar archivo multimedia de la Wiki");

        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Multimedia (JPG, PNG, MP4, MP3, WAV)", "jpg", "jpeg", "png", "gif", "mp4", "mov", "avi", "mp3", "wav", "ogg", "flac");
        selector.setFileFilter(filtro);
        selector.setAcceptAllFileFilterUsed(true);

        int resultado = selector.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            archivoImagenSeleccionado = selector.getSelectedFile();
            txtRutaImagen.setText(archivoImagenSeleccionado.getName());
            txtRutaImagen.setForeground(Color.WHITE);
        }
    }

    private void enviarArticulo() {
        String titulo = txtTitulo.getText();
        String categoria = cmbCategoria.getSelectedItem().toString();
        String descripcion = txtDescripcion.getText();
        String contenido = txtContenido.getText();

        if (titulo.isEmpty() || descripcion.isEmpty() || contenido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor llena todos los campos.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("GUARDANDO...");

        String nombreArchivoMultimedia = (archivoImagenSeleccionado != null) ? archivoImagenSeleccionado.getName() : null;

        try {
            ArticuloClient client = RetrofitClient.getClient().create(ArticuloClient.class);
            ArticuloRequest request = new ArticuloRequest(titulo, categoria, descripcion, contenido, nombreArchivoMultimedia);

            String bearerToken = "Bearer " + tokenJwt;

            client.crearArticulo(bearerToken, request).enqueue(new Callback<ArticuloResponse>() {
                @Override
                public void onResponse(Call<ArticuloResponse> call, Response<ArticuloResponse> response) {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR ARTÍCULO");

                    if (response.isSuccessful() && response.body() != null && response.body().getArticulo() != null && response.body().getArticulo().getId() != null) {

                        // 🛠️ 1. OBTENEMOS EL ID DEL ARTÍCULO CREADO (Asegúrate de que tu ArticuloResponse tenga getArticulo().getId())
                        // Si tu modelo es distinto, ajústalo aquí para extraer el ID que devuelve Node.js
                        String idArticuloCreado = response.body().getArticulo().getId();

                        // 🛠️ 2. DISPARAMOS gRPC SI HAY IMAGEN
                        if (archivoImagenSeleccionado != null && archivoImagenSeleccionado.exists()) {
                            try {
                                GrpcMediaClient grpcClient = new GrpcMediaClient();
                                grpcClient.subirImagenArticulo(idArticuloCreado, archivoImagenSeleccionado, new GrpcMediaClient.UploadListener() {
                                    @Override
                                    public void onSuccess(String urlImagen) {
                                        System.out.println("gRPC upload completed: " + urlImagen);
                                        SwingUtilities.invokeLater(() -> {
                                            JOptionPane.showMessageDialog(NuevoArticuloFrame.this, "¡Artículo guardado con imagen en la red!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                                            dispose();
                                        });
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        System.err.println("Fallo al conectar con gRPC: " + t.getMessage());
                                        SwingUtilities.invokeLater(() -> {
                                            JOptionPane.showMessageDialog(NuevoArticuloFrame.this,
                                                    "Artículo guardado, pero no se pudo subir la imagen: " + t.getMessage(),
                                                    "Advertencia", JOptionPane.WARNING_MESSAGE);
                                            dispose();
                                        });
                                    }
                                });
                                System.out.println("Enviando foto a la velocidad de la luz...");
                            } catch (Exception ex) {
                                System.err.println("Fallo al conectar con gRPC: " + ex.getMessage());
                                JOptionPane.showMessageDialog(NuevoArticuloFrame.this,
                                        "Artículo guardado, pero error al conectar gRPC: " + ex.getMessage(),
                                        "Advertencia", JOptionPane.WARNING_MESSAGE);
                                dispose();
                            }
                        } else {
                            // Sin imagen, cerramos directamente
                            JOptionPane.showMessageDialog(NuevoArticuloFrame.this, "¡Artículo guardado en la red con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        }

                    } else {
                        String mensajeError = "Error del Servidor: " + response.code();
                        if (response.body() != null && response.body().getMessage() != null) {
                            mensajeError += " - " + response.body().getMessage();
                        }
                        JOptionPane.showMessageDialog(NuevoArticuloFrame.this, mensajeError, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                @Override
                public void onFailure(Call<ArticuloResponse> call, Throwable t) {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR ARTÍCULO");
                    JOptionPane.showMessageDialog(NuevoArticuloFrame.this, "Fallo de red: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error interno en la aplicación.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}