package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;
import com.metroidwiki.model.ArticuloRequest;
import com.metroidwiki.model.ArticuloResponse;
import com.metroidwiki.network.ArticuloClient;
import com.metroidwiki.network.RetrofitClient;
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

public class EditarArticuloFrame extends JFrame {

    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color acentoAmarillo = new Color(255, 193, 7);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private JTextField txtTitulo;
    private JComboBox<String> cmbCategoria;
    private JComboBox<String> cmbEstado; // 🛠️ NUEVO CAMPO DE ESTADO
    private JTextArea txtDescripcion;
    private JTextArea txtContenido;
    private JTextField txtRutaImagen;
    private JButton btnSeleccionarImagen;
    private JButton btnGuardar;

    private String tokenJwt;
    private ArticuloDTO articuloActual;
    private File archivoImagenSeleccionado;
    private AdminDashboardFrame dashboardAdmin;

    public EditarArticuloFrame(String token, ArticuloDTO articuloActual, AdminDashboardFrame dashboardAdmin) {
        this.tokenJwt = token;
        this.articuloActual = articuloActual;
        this.dashboardAdmin = dashboardAdmin;

        setTitle("Modificar Artículo - " + articuloActual.getTitulo());
        setSize(650, 800); // un poco más alto para acomodar el nuevo campo
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(fondoPrincipal);
        setLayout(new BorderLayout(10, 10));

        // --- CABECERA ---
        JLabel lblTituloVentana = new JLabel("EDICIÓN DE REGISTRO CLASIFICADO", SwingConstants.CENTER);
        lblTituloVentana.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTituloVentana.setForeground(acentoAmarillo); // Amarillo para denotar "Edición"
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
        txtTitulo = new JTextField(articuloActual.getTitulo());
        estilizarInput(txtTitulo);

        // Categoría
        JLabel lblCategoria = new JLabel("Categoría:");
        lblCategoria.setForeground(textoClaro);
        lblCategoria.setFont(fuenteLabel);
        String[] categorias = {"Lore", "Items", "Enemigos", "Ubicaciones", "Personajes"};
        cmbCategoria = new JComboBox<>(categorias);
        estilizarComboBox(cmbCategoria);
        cmbCategoria.setSelectedItem(articuloActual.getCategoria());

        // 🛠️ ESTADO (NUEVO)
        JLabel lblEstado = new JLabel("Estado de Publicación:");
        lblEstado.setForeground(textoClaro);
        lblEstado.setFont(fuenteLabel);
        String[] estados = {"EnBorrador", "EnRevision", "Publicado", "Archivado"};
        cmbEstado = new JComboBox<>(estados);
        estilizarComboBox(cmbEstado);
        cmbEstado.setSelectedItem(articuloActual.getEstado() != null ? articuloActual.getEstado() : "EnBorrador");

        // Multimedia
        JLabel lblMultimedia = new JLabel("Actualizar Archivo Multimedia (Opcional):");
        lblMultimedia.setForeground(textoClaro);
        lblMultimedia.setFont(fuenteLabel);

        JPanel panelImagenSelector = new JPanel(new BorderLayout(10, 0));
        panelImagenSelector.setBackground(fondoPrincipal);
        panelImagenSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        txtRutaImagen = new JTextField("Mantener imagen actual...");
        txtRutaImagen.setEditable(false);
        estilizarInput(txtRutaImagen);
        txtRutaImagen.setForeground(textoGris);

        btnSeleccionarImagen = new JButton("Cambiar...");
        btnSeleccionarImagen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSeleccionarImagen.setBackground(panelSecundario);
        btnSeleccionarImagen.setForeground(Color.WHITE);
        btnSeleccionarImagen.setFocusPainted(false);
        btnSeleccionarImagen.setBorderPainted(false);
        btnSeleccionarImagen.addActionListener(e -> abrirSelectorImagen());

        panelImagenSelector.add(txtRutaImagen, BorderLayout.CENTER);
        panelImagenSelector.add(btnSeleccionarImagen, BorderLayout.EAST);

        // Descripción
        JLabel lblDesc = new JLabel("Descripción Breve:");
        lblDesc.setForeground(textoClaro);
        lblDesc.setFont(fuenteLabel);
        txtDescripcion = new JTextArea(articuloActual.getDescripcion(), 3, 20);
        estilizarTextArea(txtDescripcion);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        scrollDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Contenido
        JLabel lblContenido = new JLabel("Contenido / Lore:");
        lblContenido.setForeground(textoClaro);
        lblContenido.setFont(fuenteLabel);
        txtContenido = new JTextArea(articuloActual.getContenido(), 8, 20);
        estilizarTextArea(txtContenido);
        JScrollPane scrollContenido = new JScrollPane(txtContenido);

        // Ensamblado
        panelFormulario.add(lblTitulo); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(txtTitulo); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblCategoria); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(cmbCategoria); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

        panelFormulario.add(lblEstado); panelFormulario.add(Box.createRigidArea(new Dimension(0, 5)));
        panelFormulario.add(cmbEstado); panelFormulario.add(Box.createRigidArea(new Dimension(0, 15)));

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

        btnGuardar = new JButton("ACTUALIZAR ARTÍCULO");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGuardar.setBackground(acentoAmarillo);
        btnGuardar.setForeground(fondoPrincipal); // Letra oscura para contrastar con amarillo
        btnGuardar.setPreferredSize(new Dimension(220, 40));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.addActionListener(e -> enviarActualizacion());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelar.setBackground(new Color(100, 100, 100));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setPreferredSize(new Dimension(120, 40));
        btnCancelar.setFocusPainted(false);
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
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarTextArea(JTextArea area) {
        area.setBackground(panelSecundario);
        area.setForeground(Color.WHITE);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarComboBox(JComboBox<String> combo) {
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        combo.setBackground(panelSecundario);
        combo.setForeground(textoClaro);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setUI(new BasicComboBoxUI());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
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
    }

    private void abrirSelectorImagen() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccionar nueva imagen (Opcional)");
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Multimedia", "jpg", "jpeg", "png", "gif");
        selector.setFileFilter(filtro);

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoImagenSeleccionado = selector.getSelectedFile();
            txtRutaImagen.setText(archivoImagenSeleccionado.getName());
            txtRutaImagen.setForeground(Color.WHITE);
        }
    }

    private void enviarActualizacion() {
        String titulo = txtTitulo.getText();
        String categoria = cmbCategoria.getSelectedItem().toString();
        String estado = cmbEstado.getSelectedItem().toString(); // 🛠️ CAPTURAMOS ESTADO
        String descripcion = txtDescripcion.getText();
        String contenido = txtContenido.getText();

        if (titulo.isEmpty() || descripcion.isEmpty() || contenido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor llena todos los campos.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("ACTUALIZANDO...");

        try {
            ArticuloClient client = RetrofitClient.getClient().create(ArticuloClient.class);

            // 🛠️ USAMOS EL NUEVO CONSTRUCTOR DE 6 PARÁMETROS QUE CREAMOS
            ArticuloRequest request = new ArticuloRequest(titulo, categoria, descripcion, contenido, null, estado);

            String bearerToken = "Bearer " + tokenJwt;

            client.actualizarArticulo(bearerToken, articuloActual.getId(), request).enqueue(new Callback<ArticuloResponse>() {
                @Override
                public void onResponse(Call<ArticuloResponse> call, Response<ArticuloResponse> response) {
                    if (response.isSuccessful()) {

                        // 🛠️ ¡LA MAGIA DE SINCRONIZACIÓN!
                        // Le decimos a la ventana de administración (si existe) que se refresque sola
                        if (dashboardAdmin != null) {
                            dashboardAdmin.refrescarTablaServidor();
                        }

                        // Si eligió una imagen NUEVA, disparamos gRPC
                        if (archivoImagenSeleccionado != null && archivoImagenSeleccionado.exists()) {
                            subirNuevaImagenGrpc(articuloActual.getId());
                        } else {
                            JOptionPane.showMessageDialog(EditarArticuloFrame.this, "¡Artículo actualizado con éxito!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            dispose(); // Cerramos la ventana de edición
                        }
                    } else {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("ACTUALIZAR ARTÍCULO");
                        JOptionPane.showMessageDialog(EditarArticuloFrame.this, "Error del servidor: " + response.code(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                @Override
                public void onFailure(Call<ArticuloResponse> call, Throwable t) {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("ACTUALIZAR ARTÍCULO");
                    JOptionPane.showMessageDialog(EditarArticuloFrame.this, "Fallo de red: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error interno en la aplicación.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void subirNuevaImagenGrpc(String articuloId) {
        try {
            GrpcMediaClient grpcClient = new GrpcMediaClient();
            grpcClient.subirImagenArticulo(articuloId, archivoImagenSeleccionado, new GrpcMediaClient.UploadListener() {
                @Override
                public void onSuccess(String urlImagen) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(EditarArticuloFrame.this, "Artículo e imagen actualizados.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    });
                }
                @Override
                public void onError(Throwable t) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(EditarArticuloFrame.this, "Artículo actualizado, pero falló la imagen: " + t.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
                        dispose();
                    });
                }
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al conectar gRPC.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }
}
