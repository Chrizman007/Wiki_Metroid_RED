package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;
import com.metroidwiki.model.ComentarioDTO;
import com.metroidwiki.model.ComentarioRequest;
import com.metroidwiki.model.ComentariosListResponse;
import com.metroidwiki.network.ComentarioClient;
import com.metroidwiki.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.util.List;

public class DetalleArticuloFrame extends JFrame {

    // Paleta de colores (Modo Oscuro Metroid Oficial)
    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color fondoFicha = new Color(18, 18, 20);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private ArticuloDTO articulo;
    private String tokenUsuario;
    private String nombreUsuario;
    private String usuarioIdActual;
    private JPanel panelComentariosContenedor;

    public DetalleArticuloFrame(ArticuloDTO articulo, String token, String nombreUsuario) {
        this.articulo = articulo;
        this.tokenUsuario = token;
        this.nombreUsuario = nombreUsuario != null ? nombreUsuario : "Usuario";
        this.usuarioIdActual = extraerIdUsuarioDesdeToken(token);

        setTitle("Federación Galáctica - " + articulo.getTitulo());
        setSize(850, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Contenedor principal con scroll por si el artículo es muy extenso
        JPanel panelContenedorAbsoluto = new JPanel(new BorderLayout());
        panelContenedorAbsoluto.setBackground(fondoPrincipal);

        JScrollPane scrollGeneral = new JScrollPane(panelContenedorAbsoluto);
        scrollGeneral.setBorder(null);
        scrollGeneral.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollGeneral, BorderLayout.CENTER);

        // --- 1. BLOQUE DE ENCABEZADO ---
        panelContenedorAbsoluto.add(crearHeaderArticulo(), BorderLayout.NORTH);

        // --- 2. CUERPO CENTRAL (Texto + Ficha Técnica a la derecha) ---
        JPanel panelCuerpoCentral = new JPanel(new BorderLayout(25, 0));
        panelCuerpoCentral.setBackground(fondoPrincipal);
        panelCuerpoCentral.setBorder(new EmptyBorder(10, 30, 20, 30));

        // Texto del Lore (Izquierda)
        JTextArea txtLore = new JTextArea(articulo.getContenido());
        txtLore.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtLore.setForeground(textoClaro);
        txtLore.setBackground(fondoPrincipal);
        txtLore.setEditable(false);
        txtLore.setLineWrap(true);
        txtLore.setWrapStyleWord(true);
        panelCuerpoCentral.add(txtLore, BorderLayout.CENTER);

        // Ficha Técnica / Infobox (Derecha)
        panelCuerpoCentral.add(crearFichaTecnica(), BorderLayout.EAST);

        panelContenedorAbsoluto.add(panelCuerpoCentral, BorderLayout.CENTER);

        // --- 3. SECCIÓN DE COMENTARIOS (Hasta abajo de todo) ---
        panelContenedorAbsoluto.add(crearSeccionComentarios(), BorderLayout.SOUTH);

        // Cargar comentarios del artículo de forma asíncrona
        cargarComentarios();
    }

    /**
     * Construye la cabecera con el Título, Autor, Fecha y las Vistas alineadas a la derecha.
     */
    private JPanel crearHeaderArticulo() {
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(fondoPrincipal);
        panelHeader.setBorder(new EmptyBorder(25, 30, 15, 30));

        // Subpanel izquierdo: Título + Datos del Creador
        JPanel panelIzquierdo = new JPanel();
        panelIzquierdo.setLayout(new BoxLayout(panelIzquierdo, BoxLayout.Y_AXIS));
        panelIzquierdo.setBackground(fondoPrincipal);

        JLabel lblTitulo = new JLabel(articulo.getTitulo().toUpperCase());
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(acentoVerde);

        // Limpieza de fecha por si viene con el formato ISO de Mongo (T00:00:00...)
        String fechaLimpia = articulo.getFechaCreacion() != null ? articulo.getFechaCreacion().split("T")[0] : "Fecha estelar desconocida";
        // Nota: Asumiendo que agregaras un campo getCreador() o similar en el futuro, por ahora simulamos con un texto genérico o dinámico.
        JLabel lblMeta = new JLabel("Registrado por: Operador de la Federación  |  Fecha: " + fechaLimpia);
        lblMeta.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblMeta.setForeground(textoGris);

        panelIzquierdo.add(lblTitulo);
        panelIzquierdo.add(Box.createRigidArea(new Dimension(0, 5)));
        panelIzquierdo.add(lblMeta);

        // Subpanel derecho: Contador de Vistas 👁️
        JPanel panelDerecho = new JPanel(new GridBagLayout()); // GridBagLayout para centrar verticalmente
        panelDerecho.setBackground(fondoPrincipal);

        JLabel lblVistas = new JLabel("👁️ " + articulo.getVistas() + " Vistas");
        lblVistas.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblVistas.setForeground(acentoVerde);
        panelDerecho.add(lblVistas);

        // Separador estético inferior
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

    /**
     * Crea la Ficha Técnica (Infobox) flotante de la derecha estilo Wikipedia.
     */
    /**
     * Crea la Ficha Técnica (Infobox) flotante de la derecha estilo Wikipedia.
     * Corregido: Elimina espacios fantasmas izquierdos y su altura ahora es 100% dinámica.
     */
    private JPanel crearFichaTecnica() {
        JPanel ficha = new JPanel();
        ficha.setLayout(new BoxLayout(ficha, BoxLayout.Y_AXIS));
        ficha.setBackground(fondoFicha);
        ficha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelSecundario, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 🛠️ PARCHE 1: ¡ELIMINAMOS EL TAMAÑO FIJO VERTICAL!
        // Dejamos que BoxLayout sume la altura real de todos los componentes internos.

        // Título de la Ficha
        JLabel lblFichaTitulo = new JLabel("DATOS CLASIFICADOS", SwingConstants.CENTER);
        lblFichaTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFichaTitulo.setForeground(Color.WHITE);
        lblFichaTitulo.setBackground(panelSecundario);
        lblFichaTitulo.setOpaque(true);
        lblFichaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblFichaTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25)); // Fuerza a ocupar todo el ancho

        // Espacio para la Imagen
        JLabel lblImagenFicha = new JLabel("Cargando transmisión...", SwingConstants.CENTER);
        lblImagenFicha.setPreferredSize(new Dimension(220, 140));
        lblImagenFicha.setMaximumSize(new Dimension(220, 140));
        lblImagenFicha.setBackground(new Color(12, 12, 14));
        lblImagenFicha.setOpaque(true);
        lblImagenFicha.setForeground(textoGris);
        lblImagenFicha.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblImagenFicha.setAlignmentX(Component.CENTER_ALIGNMENT);

        cargarImagenFichaAsincrona(articulo.getImagen(), lblImagenFicha);

        // Datos del artículo: Categoría
        JLabel lblCatTitulo = new JLabel("CATEGORÍA:");
        lblCatTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCatTitulo.setForeground(acentoVerde);
        lblCatTitulo.setAlignmentX(Component.CENTER_ALIGNMENT); // 🛠️ Sincronizado a CENTER para borrar el espacio fantasma

        JLabel lblCatValor = new JLabel(articulo.getCategoria(), SwingConstants.CENTER);
        lblCatValor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblCatValor.setForeground(textoClaro);
        lblCatValor.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblCatValor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        // Datos del artículo: Descripción Corta
        JLabel lblDescTitulo = new JLabel("DESCRIPCIÓN BREVE:");
        lblDescTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDescTitulo.setForeground(acentoVerde);
        lblDescTitulo.setAlignmentX(Component.CENTER_ALIGNMENT); // 🛠️ Sincronizado a CENTER

        JTextArea txtDescValor = new JTextArea(articulo.getDescripcion());
        txtDescValor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescValor.setForeground(textoClaro);
        txtDescValor.setBackground(fondoFicha);
        txtDescValor.setEditable(false);
        txtDescValor.setLineWrap(true);
        txtDescValor.setWrapStyleWord(true);
        txtDescValor.setAlignmentX(Component.CENTER_ALIGNMENT); // 🛠️ Sincronizado a CENTER

        // 🛠️ PARCHE 2: Le permitimos un crecimiento vertical ilimitado (Integer.MAX_VALUE)
        // pero limitamos su ancho a 220 para que use el espacio total interno de la ficha técnica de 250.
        txtDescValor.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));

        // Ensamblar componentes de forma secuencial en el BoxLayout
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

        // 🛠️ PARCHE 3: CONTROLADOR INTELIGENTE DE MEDIDAS
        // Para que BorderLayout.EAST respete un ancho fijo de 250 pero mantenga una altura dinámica,
        // creamos este panel contenedor y sobreescribimos su método getPreferredSize en caliente.
        JPanel contenedorConAnchoFijo = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                // Forzamos el ancho a 250, pero dejamos que la altura total se adapte al tamaño real calculado de la ficha
                return new Dimension(250, super.getPreferredSize().height);
            }
        };
        contenedorConAnchoFijo.setBackground(fondoPrincipal);

        // Metemos la ficha acoplada al NORTE del contenedor. Así, si el contenedor es muy alto por el frame,
        // la ficha no se estira hacia abajo deformándose, sino que conserva su altura orgánica ideal.
        contenedorConAnchoFijo.add(ficha, BorderLayout.NORTH);

        return contenedorConAnchoFijo;
    }

    /**
     * Genera la sección inferior dedicada a la caja de comentarios de la Federación.
     */
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
        lblSeccionTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSeccionTitulo.setForeground(acentoVerde);
        lblSeccionTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelComentarios.add(lblSeccionTitulo);
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 15)));

        // Cuadro para escribir un nuevo comentario
        JPanel panelEscribir = new JPanel(new BorderLayout(10, 0));
        panelEscribir.setBackground(fondoPrincipal);
        panelEscribir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panelEscribir.setAlignmentX(Component.LEFT_ALIGNMENT);

        // TextArea en lugar de TextField para permitir multi-línea
        JTextArea txtNuevoComentario = new JTextArea("Escribe una transmisión pública...");
        txtNuevoComentario.setBackground(panelSecundario);
        txtNuevoComentario.setForeground(textoGris);
        txtNuevoComentario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNuevoComentario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelSecundario),
                new EmptyBorder(5, 10, 5, 10)
        ));
        txtNuevoComentario.setLineWrap(true);
        txtNuevoComentario.setWrapStyleWord(true);
        txtNuevoComentario.setRows(3);

        // Limpieza del placeholder al enfocarse
        txtNuevoComentario.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtNuevoComentario.getText().equals("Escribe una transmisión pública...")) {
                    txtNuevoComentario.setText("");
                    txtNuevoComentario.setForeground(textoClaro);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtNuevoComentario.getText().isEmpty()) {
                    txtNuevoComentario.setText("Escribe una transmisión pública...");
                    txtNuevoComentario.setForeground(textoGris);
                }
            }
        });

        // Panel derecho con botón y contador de caracteres
        JPanel panelDerecha = new JPanel();
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));
        panelDerecha.setBackground(fondoPrincipal);

        JLabel lblContador = new JLabel("0/512");
        lblContador.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblContador.setForeground(textoGris);
        lblContador.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton btnComentar = new JButton("ENVIAR");
        btnComentar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnComentar.setBackground(acentoVerde);
        btnComentar.setForeground(Color.WHITE);
        btnComentar.setFocusPainted(false);
        btnComentar.setBorderPainted(false);
        btnComentar.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Actualizar contador de caracteres en tiempo real
        txtNuevoComentario.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                int len = txtNuevoComentario.getText().length();
                if (len > 512) {
                    txtNuevoComentario.setText(txtNuevoComentario.getText().substring(0, 512));
                    lblContador.setText("512/512 (LÍMITE ALCANZADO)");
                    lblContador.setForeground(new Color(255, 100, 100));
                } else {
                    lblContador.setText(len + "/512");
                    if (len > 450) {
                        lblContador.setForeground(new Color(255, 200, 0)); // Amarillo advertencia
                    } else {
                        lblContador.setForeground(textoGris);
                    }
                }
            }
        });

        // Click en el botón ENVIAR
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

        // Panel dinámico para mostrar comentarios cargados
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

    /**
     * Carga los comentarios del artículo de forma asíncrona
     */
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
                return List.of(); // Retornar lista vacía si falla
            }

            @Override
            protected void done() {
                try {
                    List<ComentarioDTO> comentarios = get();
                    mostrarComentarios(comentarios);
                } catch (Exception e) {
                    System.err.println("Error cargando comentarios: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Muestra los comentarios en el panel dinámico
     */
    private void mostrarComentarios(List<ComentarioDTO> comentarios) {
        panelComentariosContenedor.removeAll();

        if (comentarios == null || comentarios.isEmpty()) {
            JLabel lblSinComentarios = new JLabel("No hay comentarios aún. ¡Sé el primero en comentar!");
            lblSinComentarios.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lblSinComentarios.setForeground(textoGris);
            lblSinComentarios.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelComentariosContenedor.add(lblSinComentarios);
        } else {
            for (ComentarioDTO comentario : comentarios) {
                panelComentariosContenedor.add(crearGloboComentario(
                        obtenerNombreAutor(comentario),
                        comentario.getContenido()
                ));
                panelComentariosContenedor.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        panelComentariosContenedor.revalidate();
        panelComentariosContenedor.repaint();
    }

    /**
     * Envía un nuevo comentario al servidor
     */
    private void enviarComentario(JTextArea txtComentario, JButton btnEnviar) {
        String contenido = txtComentario.getText().trim();

        // Validaciones
        if (contenido.isEmpty() || contenido.equals("Escribe una transmisión pública...")) {
            JOptionPane.showMessageDialog(this, "El comentario no puede estar vacío", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (contenido.length() > 512) {
            JOptionPane.showMessageDialog(this, "El comentario excede el límite de 512 caracteres", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Desabilitar botón mientras se envía
        btnEnviar.setEnabled(false);
        btnEnviar.setText("ENVIANDO...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ComentarioClient client = RetrofitClient.getClient().create(ComentarioClient.class);
                ComentarioRequest request = new ComentarioRequest(contenido);
                String bearerToken = "Bearer " + tokenUsuario;

                Call<com.metroidwiki.model.ComentarioResponse> call = client.crearComentario(
                        articulo.getId(),
                        bearerToken,
                        request
                );

                Response<com.metroidwiki.model.ComentarioResponse> response = call.execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Error al crear comentario: " + response.code());
                }

                return null;
            }

            @Override
            protected void done() {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("ENVIAR");

                try {
                    get();
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this,
                            "¡Comentario enviado con éxito!",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    txtComentario.setText("Escribe una transmisión pública...");
                    txtComentario.setForeground(textoGris);
                    cargarComentarios(); // Recargar comentarios
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DetalleArticuloFrame.this,
                            "Error al enviar comentario: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private JPanel crearGloboComentario(String usuario, String mensaje) {
        JPanel globo = new JPanel();
        globo.setLayout(new BoxLayout(globo, BoxLayout.Y_AXIS));
        globo.setBackground(panelSecundario);
        globo.setBorder(new EmptyBorder(10, 15, 10, 15));
        globo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUser = new JLabel(usuario);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(acentoVerde);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea txtMensaje = new JTextArea(mensaje);
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMensaje.setForeground(textoClaro);
        txtMensaje.setBackground(panelSecundario);
        txtMensaje.setEditable(false);
        txtMensaje.setLineWrap(true);
        txtMensaje.setWrapStyleWord(false);
        txtMensaje.setOpaque(false);
        txtMensaje.setBorder(null);
        txtMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtMensaje.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
        txtMensaje.setMinimumSize(new Dimension(100, 40));

        globo.add(lblUser);
        globo.add(Box.createRigidArea(new Dimension(0, 3)));
        globo.add(txtMensaje);

        return globo;
    }

    private String obtenerNombreAutor(ComentarioDTO comentario) {
        if (comentario == null) {
            return "Anónimo";
        }
        if (usuarioIdActual != null && usuarioIdActual.equals(comentario.getAutorId())) {
            return nombreUsuario;
        }
        if (comentario.getAutorNombre() != null && !comentario.getAutorNombre().isEmpty()) {
            return comentario.getAutorNombre();
        }
        if (comentario.getAutorId() != null) {
            return comentario.getAutorId();
        }
        return "Anónimo";
    }

    private String extraerIdUsuarioDesdeToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] partes = jwt.split("\\.");
            if (partes.length < 2) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(partes[1]));
            int idx = payload.indexOf("\"id\"");
            if (idx == -1) {
                return null;
            }
            int colon = payload.indexOf(':', idx);
            if (colon == -1) {
                return null;
            }
            int start = colon + 1;
            while (start < payload.length() && Character.isWhitespace(payload.charAt(start))) {
                start++;
            }
            if (start >= payload.length()) {
                return null;
            }
            if (payload.charAt(start) == '"') {
                int end = payload.indexOf('"', start + 1);
                if (end == -1) {
                    return null;
                }
                return payload.substring(start + 1, end);
            }
            int end = start;
            while (end < payload.length() && (Character.isLetterOrDigit(payload.charAt(end)) || payload.charAt(end) == '-' || payload.charAt(end) == '_')) {
                end++;
            }
            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private void cargarImagenFichaAsincrona(String nombreImagen, JLabel lblIcono) {
        if (nombreImagen == null || nombreImagen.isEmpty() || nombreImagen.equals("default.png")) {
            lblIcono.setText("SIN IMAGEN");
            return;
        }

        String nombreCodificado = nombreImagen.replace(" ", "%20");
        String urlCompleta = RetrofitClient.BASE_URL + "articulos/public/imagenes/" + nombreCodificado;

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL(urlCompleta);
                Image img = ImageIO.read(url);
                if (img != null) {
                    Image scaledImg = img.getScaledInstance(220, 140, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImg);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icono = get();
                    if (icono != null) {
                        lblIcono.setText("");
                        lblIcono.setIcon(icono);
                    } else {
                        lblIcono.setText("IMAGEN NO ENCONTRADA");
                    }
                } catch (Exception e) {
                    lblIcono.setText("ERROR DE TRANSMISIÓN");
                }
            }
        };
        worker.execute();
    }
}