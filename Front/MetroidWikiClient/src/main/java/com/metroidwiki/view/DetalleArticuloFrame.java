package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;
import com.metroidwiki.model.ComentarioDTO;
import com.metroidwiki.model.ComentarioRequest;
import com.metroidwiki.model.ComentariosListResponse;
import com.metroidwiki.network.ComentarioClient;
import com.metroidwiki.network.RetrofitClient;
import com.metroidwiki.network.GrpcMediaClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class DetalleArticuloFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(DetalleArticuloFrame.class.getName());

    private static final String FONT_SEGOE = "Segoe UI";
    private static final String TITULO_ERROR = "Error";
    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String TXT_PLACEHOLDER_COMENTARIO = "Escribe una transmisión pública...";

    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color fondoFicha = new Color(18, 18, 20);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color acentoRojo = new Color(244, 67, 54);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private transient ArticuloDTO articulo;
    private transient String tokenUsuario;
    private transient String nombreUsuario;
    private transient String usuarioIdActual;
    private transient String rolUsuarioActual;
    private transient JPanel panelComentariosContenedor;

    public DetalleArticuloFrame(ArticuloDTO articulo, String token, String nombreUsuario) {
        this.articulo = articulo;
        this.tokenUsuario = token;
        this.nombreUsuario = nombreUsuario != null ? nombreUsuario : "Usuario";
        this.usuarioIdActual = extraerDatoDesdeToken(token, "\"id\"");
        this.rolUsuarioActual = extraerDatoDesdeToken(token, "\"rol\"");

        setTitle("Federación Galáctica - " + articulo.getTitulo());
        setSize(850, 750);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelContenedorAbsoluto = new JPanel(new BorderLayout());
        panelContenedorAbsoluto.setBackground(fondoPrincipal);

        JScrollPane scrollGeneral = new JScrollPane(panelContenedorAbsoluto);
        scrollGeneral.setBorder(null);
        scrollGeneral.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollGeneral, BorderLayout.CENTER);

        panelContenedorAbsoluto.add(crearHeaderArticulo(), BorderLayout.NORTH);

        JPanel panelCuerpoCentral = new JPanel(new BorderLayout(25, 0));
        panelCuerpoCentral.setBackground(fondoPrincipal);
        panelCuerpoCentral.setBorder(new EmptyBorder(10, 30, 20, 30));

        JTextArea txtLore = new JTextArea(articulo.getContenido());
        txtLore.setFont(new Font(FONT_SEGOE, Font.PLAIN, 15));
        txtLore.setForeground(textoClaro);
        txtLore.setBackground(fondoPrincipal);
        txtLore.setEditable(false);
        txtLore.setLineWrap(true);
        txtLore.setWrapStyleWord(true);
        panelCuerpoCentral.add(txtLore, BorderLayout.CENTER);

        panelCuerpoCentral.add(crearFichaTecnica(), BorderLayout.EAST);
        panelContenedorAbsoluto.add(panelCuerpoCentral, BorderLayout.CENTER);
        panelContenedorAbsoluto.add(crearSeccionComentarios(), BorderLayout.SOUTH);

        cargarComentarios();
    }

    private Icon cargarIcono(String ruta, int width, int height) {
        try {
            String rutaLimpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
            return new FlatSVGIcon(rutaLimpia, width, height);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error cargando SVG " + ruta, e);
            return null;
        }
    }

    private JPanel crearHeaderArticulo() {
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(fondoPrincipal);
        panelHeader.setBorder(new EmptyBorder(25, 30, 15, 30));

        JPanel panelIzquierdo = new JPanel();
        panelIzquierdo.setLayout(new BoxLayout(panelIzquierdo, BoxLayout.Y_AXIS));
        panelIzquierdo.setBackground(fondoPrincipal);

        JLabel lblTitulo = new JLabel(articulo.getTitulo().toUpperCase());
        lblTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 26));
        lblTitulo.setForeground(acentoVerde);

        String fechaLimpia = articulo.getFechaCreacion() != null ? articulo.getFechaCreacion().split("T")[0] : "Fecha estelar desconocida";
        String realAutor = (articulo.getAutor() != null && !articulo.getAutor().isEmpty()) ? articulo.getAutor() : "Operador de la Federación";

        JLabel lblMeta = new JLabel("Registrado por: " + realAutor + "  |  Fecha: " + fechaLimpia);
        lblMeta.setFont(new Font(FONT_SEGOE, Font.ITALIC, 12));
        lblMeta.setForeground(textoGris);

        panelIzquierdo.add(lblTitulo);
        panelIzquierdo.add(Box.createRigidArea(new Dimension(0, 5)));
        panelIzquierdo.add(lblMeta);

        JPanel panelDerecho = new JPanel(new GridBagLayout());
        panelDerecho.setBackground(fondoPrincipal);

        JLabel lblVistas = new JLabel(" " + articulo.getVistas() + " Vistas");
        lblVistas.setFont(new Font(FONT_SEGOE, Font.BOLD, 15));
        lblVistas.setForeground(acentoVerde);
        Icon vistaIcon = cargarIcono("/icons/vista.svg", 22, 22);
        if (vistaIcon != null) {
            lblVistas.setIcon(vistaIcon);
        }
        panelDerecho.add(lblVistas);

        JSeparator lineaDivisoria = new JSeparator();
        lineaDivisoria.setForeground(panelSecundario);
        lineaDivisoria.setBackground(panelSecundario);

        JPanel panelCompleto = new JPanel(new BorderLayout());
        panelCompleto.setBackground(fondoPrincipal);
        panelCompleto.add(panelHeader, BorderLayout.CENTER);
        panelCompleto.add(lineaDivisoria, BorderLayout.SOUTH);

        panelHeader.add(panelIzquierdo, BorderLayout.CENTER);
        panelHeader.add(panelDerecho, BorderLayout.EAST);

        return panelCompleto;
    }

    private JPanel crearFichaTecnica() {
        JPanel ficha = new JPanel();
        ficha.setLayout(new BoxLayout(ficha, BoxLayout.Y_AXIS));
        ficha.setBackground(fondoFicha);
        ficha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelSecundario, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblFichaTitulo = new JLabel("DATOS CLASIFICADOS", SwingConstants.CENTER);
        lblFichaTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 13));
        lblFichaTitulo.setForeground(Color.WHITE);
        lblFichaTitulo.setBackground(panelSecundario);
        lblFichaTitulo.setOpaque(true);
        lblFichaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblFichaTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel lblImagenFicha = new JLabel("Cargando transmisión...", SwingConstants.CENTER);
        lblImagenFicha.setPreferredSize(new Dimension(220, 140));
        lblImagenFicha.setMaximumSize(new Dimension(220, 140));
        lblImagenFicha.setBackground(new Color(12, 12, 14));
        lblImagenFicha.setOpaque(true);
        lblImagenFicha.setForeground(textoGris);
        lblImagenFicha.setFont(new Font(FONT_SEGOE, Font.ITALIC, 11));
        lblImagenFicha.setAlignmentX(Component.CENTER_ALIGNMENT);

        cargarImagenFichaAsincrona(articulo.getImagen(), lblImagenFicha);

        JLabel lblCatTitulo = new JLabel("CATEGORÍA:");
        lblCatTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 11));
        lblCatTitulo.setForeground(acentoVerde);
        lblCatTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCatValor = new JLabel(articulo.getCategoria(), SwingConstants.CENTER);
        lblCatValor.setFont(new Font(FONT_SEGOE, Font.PLAIN, 14));
        lblCatValor.setForeground(textoClaro);
        lblCatValor.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCatValor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel lblDescTitulo = new JLabel("DESCRIPCIÓN BREVE:");
        lblDescTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 11));
        lblDescTitulo.setForeground(acentoVerde);
        lblDescTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea txtDescValor = new JTextArea(articulo.getDescripcion());
        txtDescValor.setFont(new Font(FONT_SEGOE, Font.PLAIN, 13));
        txtDescValor.setForeground(textoClaro);
        txtDescValor.setBackground(fondoFicha);
        txtDescValor.setEditable(false);
        txtDescValor.setLineWrap(true);
        txtDescValor.setWrapStyleWord(true);
        txtDescValor.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtDescValor.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));

        ficha.add(lblFichaTitulo);
        ficha.add(Box.createRigidArea(new Dimension(0, 10)));
        ficha.add(lblImagenFicha);
        ficha.add(Box.createRigidArea(new Dimension(0, 15)));
        ficha.add(lblCatTitulo);
        ficha.add(Box.createRigidArea(new Dimension(0, 3)));
        ficha.add(lblCatValor);
        ficha.add(Box.createRigidArea(new Dimension(0, 15)));
        ficha.add(lblDescTitulo);
        ficha.add(Box.createRigidArea(new Dimension(0, 3)));
        ficha.add(txtDescValor);

        JPanel contenedorConAnchoFijo = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(250, super.getPreferredSize().height);
            }
        };
        contenedorConAnchoFijo.setBackground(fondoPrincipal);
        contenedorConAnchoFijo.add(ficha, BorderLayout.NORTH);

        return contenedorConAnchoFijo;
    }

    private JPanel crearSeccionComentarios() {
        JPanel panelComentarios = new JPanel();
        panelComentarios.setLayout(new BoxLayout(panelComentarios, BoxLayout.Y_AXIS));
        panelComentarios.setBackground(fondoPrincipal);
        panelComentarios.setBorder(new EmptyBorder(20, 30, 40, 30));

        JSeparator lineaSuperior = new JSeparator();
        lineaSuperior.setForeground(panelSecundario);
        lineaSuperior.setBackground(panelSecundario);
        panelComentarios.add(lineaSuperior);
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel lblSeccionTitulo = new JLabel("TRANSMISIONES DE LA COMUNIDAD (COMENTARIOS)");
        lblSeccionTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 14));
        lblSeccionTitulo.setForeground(acentoVerde);
        lblSeccionTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelComentarios.add(lblSeccionTitulo);
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel panelEscribir = new JPanel(new BorderLayout(10, 0));
        panelEscribir.setBackground(fondoPrincipal);
        panelEscribir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panelEscribir.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea txtNuevoComentario = new JTextArea(TXT_PLACEHOLDER_COMENTARIO);
        txtNuevoComentario.setBackground(panelSecundario);
        txtNuevoComentario.setForeground(textoGris);
        txtNuevoComentario.setFont(new Font(FONT_SEGOE, Font.PLAIN, 13));
        txtNuevoComentario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelSecundario),
                new EmptyBorder(5, 10, 5, 10)
        ));
        txtNuevoComentario.setLineWrap(true);
        txtNuevoComentario.setWrapStyleWord(true);
        txtNuevoComentario.setRows(3);

        txtNuevoComentario.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtNuevoComentario.getText().equals(TXT_PLACEHOLDER_COMENTARIO)) {
                    txtNuevoComentario.setText("");
                    txtNuevoComentario.setForeground(textoClaro);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtNuevoComentario.getText().isEmpty()) {
                    txtNuevoComentario.setText(TXT_PLACEHOLDER_COMENTARIO);
                    txtNuevoComentario.setForeground(textoGris);
                }
            }
        });

        JPanel panelDerecha = new JPanel();
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));
        panelDerecha.setBackground(fondoPrincipal);

        JLabel lblContador = new JLabel("0/512");
        lblContador.setFont(new Font(FONT_SEGOE, Font.PLAIN, 11));
        lblContador.setForeground(textoGris);
        lblContador.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton btnComentar = new JButton("");
        btnComentar.setIcon(cargarIcono("/icons/enviar.svg", 22, 22));
        btnComentar.setToolTipText("Enviar transmisión");
        btnComentar.setBackground(acentoVerde);
        btnComentar.setFocusPainted(false);
        btnComentar.setBorderPainted(false);
        btnComentar.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnComentar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnComentar.setBorder(new EmptyBorder(8, 15, 8, 15));

        txtNuevoComentario.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                int len = txtNuevoComentario.getText().length();
                if (len > 512) {
                    txtNuevoComentario.setText(txtNuevoComentario.getText().substring(0, 512));
                    lblContador.setText("512/512 (LÍMITE ALCANZADO)");
                    lblContador.setForeground(acentoRojo);
                } else {
                    lblContador.setText(len + "/512");
                    if (len > 450) {
                        lblContador.setForeground(new Color(255, 200, 0));
                    } else {
                        lblContador.setForeground(textoGris);
                    }
                }
            }
        });

        btnComentar.addActionListener(e -> enviarComentario(txtNuevoComentario, btnComentar));

        panelDerecha.add(lblContador);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 3)));
        panelDerecha.add(btnComentar);

        JScrollPane scrollTexto = new JScrollPane(txtNuevoComentario);
        scrollTexto.setBorder(null);
        panelEscribir.add(scrollTexto, BorderLayout.CENTER);
        panelEscribir.add(panelDerecha, BorderLayout.EAST);
        panelComentarios.add(panelEscribir);
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 20)));

        panelComentariosContenedor = new JPanel();
        panelComentariosContenedor.setLayout(new BoxLayout(panelComentariosContenedor, BoxLayout.Y_AXIS));
        panelComentariosContenedor.setBackground(fondoPrincipal);
        panelComentariosContenedor.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollComentarios = new JScrollPane(panelComentariosContenedor);
        scrollComentarios.setBorder(null);
        scrollComentarios.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollComentarios.setPreferredSize(new Dimension(0, 250));
        scrollComentarios.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        panelComentarios.add(scrollComentarios);

        return panelComentarios;
    }

    private void cargarComentarios() {
        SwingWorker<List<ComentarioDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ComentarioDTO> doInBackground() throws Exception {
                ComentarioClient client = RetrofitClient.getClient().create(ComentarioClient.class);
                Call<ComentariosListResponse> call = client.obtenerComentarios(articulo.getId(), 100, 0);
                Response<ComentariosListResponse> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    return response.body().getComentarios();
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<ComentarioDTO> comentarios = get();
                    mostrarComentarios(comentarios);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Hilo de carga de comentarios interrumpido", ie);
                } catch (ExecutionException ee) {
                    logger.log(Level.SEVERE, "Error ejecutando la carga de comentarios", ee);
                } catch (RuntimeException re) {
                    logger.log(Level.SEVERE, "Error interno en los comentarios", re);
                }
            }
        };
        worker.execute();
    }

    private void mostrarComentarios(List<ComentarioDTO> comentarios) {
        panelComentariosContenedor.removeAll();

        if (comentarios == null || comentarios.isEmpty()) {
            JLabel lblSinComentarios = new JLabel("No hay comentarios aún. ¡Sé el primero en comentar!");
            lblSinComentarios.setFont(new Font(FONT_SEGOE, Font.ITALIC, 13));
            lblSinComentarios.setForeground(textoGris);
            lblSinComentarios.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelComentariosContenedor.add(lblSinComentarios);
        } else {
            for (ComentarioDTO comentario : comentarios) {
                panelComentariosContenedor.add(crearGloboComentario(comentario, obtenerNombreAutor(comentario)));
                panelComentariosContenedor.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        panelComentariosContenedor.revalidate();
        panelComentariosContenedor.add(Box.createRigidArea(new Dimension(0, 0)));
        panelComentariosContenedor.repaint();
    }

    private JPanel crearGloboComentario(ComentarioDTO comentario, String usuario) {
        JPanel globo = new JPanel();
        globo.setLayout(new BoxLayout(globo, BoxLayout.Y_AXIS));
        globo.setBackground(panelSecundario);
        globo.setBorder(new EmptyBorder(10, 15, 10, 15));
        globo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelHeaderComentario = new JPanel(new BorderLayout());
        panelHeaderComentario.setBackground(panelSecundario);
        panelHeaderComentario.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelHeaderComentario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel lblUser = new JLabel(usuario);
        lblUser.setFont(new Font(FONT_SEGOE, Font.BOLD, 12));
        lblUser.setForeground(acentoVerde);
        panelHeaderComentario.add(lblUser, BorderLayout.WEST);

        if ("administrador".equalsIgnoreCase(rolUsuarioActual)) {
            JButton btnBorrar = new JButton("X");
            btnBorrar.setFont(new Font(FONT_SEGOE, Font.BOLD, 12));
            btnBorrar.setBackground(acentoRojo);
            btnBorrar.setForeground(Color.WHITE);

            btnBorrar.setFocusPainted(false);
            btnBorrar.setBorderPainted(false);
            btnBorrar.setOpaque(true);

            btnBorrar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            btnBorrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnBorrar.setToolTipText("Eliminar transmisión (Moderar)");

            btnBorrar.addActionListener(e -> borrarComentarioREST(comentario.getId()));

            panelHeaderComentario.add(btnBorrar, BorderLayout.EAST);
        }

        JTextArea txtMensaje = new JTextArea(comentario.getContenido());
        txtMensaje.setFont(new Font(FONT_SEGOE, Font.PLAIN, 13));
        txtMensaje.setForeground(textoClaro);
        txtMensaje.setBackground(panelSecundario);
        txtMensaje.setEditable(false);
        txtMensaje.setLineWrap(true);
        txtMensaje.setWrapStyleWord(false);
        txtMensaje.setBorder(null);
        txtMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);

        globo.add(panelHeaderComentario);
        globo.add(Box.createRigidArea(new Dimension(0, 5)));
        globo.add(txtMensaje);

        return globo;
    }

    private void borrarComentarioREST(String idComentario) {
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que deseas eliminar este comentario por violar los protocolos de la Federación?",
                "Moderar Transmisión", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmar == JOptionPane.YES_OPTION) {
            try {
                ComentarioClient client = RetrofitClient.getClient().create(ComentarioClient.class);
                String bearerToken = PREFIJO_BEARER + tokenUsuario;

                client.eliminarComentario(idComentario, bearerToken).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            cargarComentarios();
                        } else {
                            JOptionPane.showMessageDialog(DetalleArticuloFrame.this, "Error del servidor al borrar.", TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        JOptionPane.showMessageDialog(DetalleArticuloFrame.this, "Fallo de red: " + t.getMessage(), TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Fallo interno al intentar borrar el comentario", e);
                JOptionPane.showMessageDialog(this, "Error interno de la aplicación.", TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void enviarComentario(JTextArea txtComentario, JButton btnEnviar) {
        String contenido = txtComentario.getText().trim();

        if (contenido.isEmpty() || contenido.equals(TXT_PLACEHOLDER_COMENTARIO)) {
            JOptionPane.showMessageDialog(this, "El comentario no puede estar vacío", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (contenido.length() > 512) {
            JOptionPane.showMessageDialog(this, "El comentario excede el límite de 512 caracteres", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnEnviar.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ComentarioClient client = RetrofitClient.getClient().create(ComentarioClient.class);
                ComentarioRequest request = new ComentarioRequest(contenido);
                String bearerToken = PREFIJO_BEARER + tokenUsuario;

                Call<com.metroidwiki.model.ComentarioResponse> call = client.crearComentario(
                        articulo.getId(),
                        bearerToken,
                        request
                );

                Response<com.metroidwiki.model.ComentarioResponse> response = call.execute();

                if (!response.isSuccessful()) {
                    throw new RuntimeException("Error al crear comentario: " + response.code());
                }

                return null;
            }

            @Override
            protected void done() {
                btnEnviar.setEnabled(true);

                try {
                    get();
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this,
                            "¡Comentario enviado con éxito!",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    txtComentario.setText(TXT_PLACEHOLDER_COMENTARIO);
                    txtComentario.setForeground(textoGris);
                    cargarComentarios();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this, "Operación interrumpida.", TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException ee) {
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this, "Error al enviar comentario: " + ee.getCause().getMessage(), TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                } catch (RuntimeException re) {
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this, "Fallo interno al procesar el comentario.", TITULO_ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String obtenerNombreAutor(ComentarioDTO comentario) {
        if (comentario == null) return "Anónimo";
        if (usuarioIdActual != null && usuarioIdActual.equals(comentario.getAutorId())) return nombreUsuario;
        if (comentario.getAutorNombre() != null && !comentario.getAutorNombre().isEmpty()) return comentario.getAutorNombre();
        if (comentario.getAutorId() != null) return comentario.getAutorId();
        return "Anónimo";
    }

    private String extraerDatoDesdeToken(String token, String keyBuscada) {
        if (token == null || token.isEmpty()) return null;
        try {
            String jwt = token.startsWith(PREFIJO_BEARER) ? token.substring(7) : token;
            String[] partes = jwt.split("\\.");
            if (partes.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(partes[1]));
            int idx = payload.indexOf(keyBuscada);
            if (idx == -1) return null;

            int colon = payload.indexOf(':', idx);
            if (colon == -1) return null;

            int start = colon + 1;
            while (start < payload.length() && (Character.isWhitespace(payload.charAt(start)) || payload.charAt(start) == '"')) {
                start++;
            }

            int end = start;
            while (end < payload.length() && (Character.isLetterOrDigit(payload.charAt(end)) || payload.charAt(end) == '-' || payload.charAt(end) == '_')) {
                end++;
            }
            return payload.substring(start, end);
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "Error decodificando token Base64", e);
            return null;
        }
    }

    private void cargarImagenFichaAsincrona(String nombreImagen, JLabel lblIcono) {
        if (nombreImagen == null || nombreImagen.isEmpty() || nombreImagen.equals("default.png")) {
            lblIcono.setText("SIN IMAGEN");
            return;
        }

        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "metroidwiki_cache";

        GrpcMediaClient grpcClient = new GrpcMediaClient();

        grpcClient.descargarImagenArticulo(nombreImagen, tempDir, new GrpcMediaClient.DownloadListener() {
            @Override
            public void onSuccess(File archivoDescargado) {
                try {
                    Image img = ImageIO.read(archivoDescargado);
                    if (img != null) {
                        Image scaledImg = img.getScaledInstance(220, 140, Image.SCALE_SMOOTH);
                        lblIcono.setText("");
                        lblIcono.setIcon(new ImageIcon(scaledImg));
                    } else {
                        lblIcono.setText("IMAGEN CORRUPTA");
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error al leer imagen temporal", e);
                    lblIcono.setText("ERROR DE LECTURA");
                }
            }

            @Override
            public void onError(Throwable t) {
                lblIcono.setText("ERROR gRPC");
                logger.log(Level.WARNING, "El servidor gRPC no pudo enviar la imagen: " + nombreImagen);
            }
        });
    }
}