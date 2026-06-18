package com.metroidwiki.view;

import com.metroidwiki.network.RetrofitClient;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ExecutionException;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class WikiMainFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(WikiMainFrame.class.getName());

    //CONSTANTES DE CLEAN CODE
    private static final String FONT_SEGOE = "Segoe UI";
    private static final String TXT_BUSCAR_WIKI = "Buscar en la Wiki...";

    // Paleta de colores (Modo Oscuro Metroid)
    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color fondoLateral = new Color(18, 18, 20);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    //VARIABLES TRANSIENT PARA OBJETOS NO SERIALIZABLES
    private transient String tokenUsuarioActual;
    private transient String nombreUsuario;
    private transient String rolUsuario;

    private transient JPanel gridTarjetas;
    private transient JPanel panelCategorias;
    private transient JPanel panelLateral;

    // Memoria caché para no pedir a la BD cada vez que filtramos
    private transient List<com.metroidwiki.model.ArticuloDTO> listaArticulosCache = new ArrayList<>();

    public WikiMainFrame(String token, String nombreUsuario, String rolUsuario) {
        this.tokenUsuarioActual = token;
        this.nombreUsuario = nombreUsuario != null ? nombreUsuario : "Desconocido";
        this.rolUsuario = rolUsuario != null ? rolUsuario.toLowerCase() : "lector";

        setTitle("Metroid Wiki - Archivos Centrales");
        setSize(1100, 750);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 🛠️ Corregido WindowConstants
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Barra Lateral
        add(crearBarraLateral(), BorderLayout.WEST);

        // 2. Área Central
        JPanel panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(fondoPrincipal);

        panelContenido.add(crearCabecera(), BorderLayout.NORTH);
        panelContenido.add(crearAreaTarjetas(), BorderLayout.CENTER);

        add(panelContenido, BorderLayout.CENTER);
    }

    private Icon cargarIcono(String ruta, int width, int height) {
        try {
            String rutaLimpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
            return new FlatSVGIcon(rutaLimpia, width, height);
        } catch (RuntimeException e) { // 🛠️ Excepción específica
            logger.log(Level.SEVERE, "❌ Error cargando SVG " + ruta, e);
            return null;
        }
    }

    // ==========================================
    // 1. BARRA LATERAL (Con filtros y permisos)
    // ==========================================

    private JPanel crearBarraLateral() {
        panelLateral = new JPanel();
        panelLateral.setLayout(new BoxLayout(panelLateral, BoxLayout.Y_AXIS));
        panelLateral.setBackground(fondoLateral);

        panelLateral.setPreferredSize(new Dimension(240, 0));
        panelLateral.setBorder(new EmptyBorder(25, 10, 10, 10));

        String textoLogoHtml = "<html><div style='text-align: center;'>" +
                "<span style='color: #4CAF50; font-family: Impact, sans-serif; font-size: 26px; font-style: italic;'>METROID</span><br>" +
                "<span style='color: #CCCCCC; font-family: \"Segoe UI\", sans-serif; font-size: 14px; letter-spacing: 4px;'>W I K I</span>" +
                "</div></html>";
        JLabel lblLogo = new JLabel(textoLogoHtml, SwingConstants.CENTER);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separadorPrincipal = new JSeparator();
        separadorPrincipal.setMaximumSize(new Dimension(200, 1));
        separadorPrincipal.setForeground(acentoVerde);
        separadorPrincipal.setBackground(acentoVerde);

        JButton btnInicio = crearBotonMenu("Inicio (Todos)", true);
        btnInicio.addActionListener(e -> renderizarGrid(listaArticulosCache));

        JButton btnDestacados = crearBotonMenu("Destacados", false);
        btnDestacados.setIcon(cargarIcono("/icons/destaca.svg", 24, 24));
        btnDestacados.setIconTextGap(12);
        btnDestacados.addActionListener(e -> filtrarDestacados());

        JButton btnNuevasEntradas = crearBotonMenu("Nuevas Entradas", false);
        btnNuevasEntradas.setIcon(cargarIcono("/icons/nuevos.svg", 24, 24));
        btnNuevasEntradas.setIconTextGap(12);
        btnNuevasEntradas.addActionListener(e -> filtrarNuevasEntradas());

        JButton btnMenuCategorias = crearBotonMenu("Categorías ▼", false);
        btnMenuCategorias.setIcon(cargarIcono("/icons/categorias.svg", 24, 24));
        btnMenuCategorias.setIconTextGap(12);
        crearPanelCategoriasColapsable();

        btnMenuCategorias.addActionListener(e -> {
            boolean isVisible = panelCategorias.isVisible();
            panelCategorias.setVisible(!isVisible);
            btnMenuCategorias.setText(isVisible ? "Categorías ▼" : "Categorías ▲");
            panelLateral.revalidate();
            panelLateral.repaint();
        });

        JButton btnAdministracion = crearBotonMenu("Administración", false);
        btnAdministracion.setIcon(cargarIcono("/icons/admin.svg", 24, 24));
        btnAdministracion.setIconTextGap(12);
        btnAdministracion.setForeground(new Color(255, 193, 7));
        btnAdministracion.addActionListener(e -> {
            AdminDashboardFrame dashboard = new AdminDashboardFrame(tokenUsuarioActual, nombreUsuario, listaArticulosCache);
            dashboard.setVisible(true);
        });

        JButton btnConsolaDev = crearBotonMenu("Terminal API", false);
        btnConsolaDev.setIcon(cargarIcono("/icons/dev.svg", 24, 24));
        btnConsolaDev.setIconTextGap(12);
        btnConsolaDev.setForeground(new Color(0, 255, 150));
        btnConsolaDev.addActionListener(e -> {
            DevConsoleFrame consola = new DevConsoleFrame(tokenUsuarioActual);
            consola.setVisible(true);
        });

        JButton btnCerrarSesion = crearBotonMenu("Cerrar Sesión", false);
        btnCerrarSesion.setIcon(cargarIcono("/icons/salir.svg", 24, 24));
        btnCerrarSesion.setIconTextGap(12);
        btnCerrarSesion.setForeground(new Color(255, 100, 100));
        btnCerrarSesion.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        panelLateral.add(Box.createRigidArea(new Dimension(0, 10)));
        panelLateral.add(lblLogo);
        panelLateral.add(Box.createRigidArea(new Dimension(0, 20)));
        panelLateral.add(separadorPrincipal);
        panelLateral.add(Box.createRigidArea(new Dimension(0, 15)));

        boolean tienePrivilegios = false;

        if (rolUsuario.equals("desarrollador")) {
            panelLateral.add(btnConsolaDev);
            panelLateral.add(Box.createRigidArea(new Dimension(0, 5)));
            tienePrivilegios = true;
        }

        if (rolUsuario.equals("administrador")) {
            panelLateral.add(btnAdministracion);
            panelLateral.add(Box.createRigidArea(new Dimension(0, 5)));
            tienePrivilegios = true;
        }

        if (tienePrivilegios) {
            JSeparator separadorSecundario = new JSeparator();
            separadorSecundario.setMaximumSize(new Dimension(180, 1));
            separadorSecundario.setForeground(panelSecundario);
            separadorSecundario.setBackground(panelSecundario);

            panelLateral.add(Box.createRigidArea(new Dimension(0, 10)));
            panelLateral.add(separadorSecundario);
            panelLateral.add(Box.createRigidArea(new Dimension(0, 25)));
        } else {
            panelLateral.add(Box.createRigidArea(new Dimension(0, 25)));
        }

        panelLateral.add(btnInicio);
        panelLateral.add(Box.createRigidArea(new Dimension(0, 10)));
        panelLateral.add(btnDestacados);
        panelLateral.add(Box.createRigidArea(new Dimension(0, 10)));
        panelLateral.add(btnNuevasEntradas);
        panelLateral.add(Box.createRigidArea(new Dimension(0, 10)));
        panelLateral.add(btnMenuCategorias);
        panelLateral.add(panelCategorias);

        panelLateral.add(Box.createVerticalGlue());
        panelLateral.add(btnCerrarSesion);

        return panelLateral;
    }

    private void crearPanelCategoriasColapsable() {
        panelCategorias = new JPanel();
        panelCategorias.setLayout(new BoxLayout(panelCategorias, BoxLayout.Y_AXIS));
        panelCategorias.setBackground(fondoLateral);
        panelCategorias.setVisible(false);
        panelCategorias.setBorder(new EmptyBorder(5, 15, 5, 0));

        String[] categorias = {"Lore", "Items", "Enemigos", "Ubicaciones", "Personajes"};
        for (String cat : categorias) {
            JButton btnCat = crearBotonMenu("• " + cat, false);
            btnCat.setFont(new Font(FONT_SEGOE, Font.PLAIN, 14));
            btnCat.setPreferredSize(new Dimension(200, 35));
            btnCat.addActionListener(e -> filtrarPorCategoria(cat));
            panelCategorias.add(btnCat);
            panelCategorias.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(fondoPrincipal);
        cabecera.setBorder(new EmptyBorder(20, 30, 10, 30));

        JLabel lblTitulo = new JLabel("ARCHIVOS DE LA FEDERACIÓN");
        lblTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 22));
        lblTitulo.setForeground(acentoVerde);

        JLabel lblCazador = new JLabel("Cazador: " + nombreUsuario + " [" + rolUsuario.toUpperCase() + "]");
        lblCazador.setFont(new Font(FONT_SEGOE, Font.BOLD, 14));
        lblCazador.setForeground(textoClaro);

        cabecera.add(lblTitulo, BorderLayout.WEST);
        cabecera.add(lblCazador, BorderLayout.EAST);

        return cabecera;
    }

    private JPanel crearAreaTarjetas() {
        JPanel panelDatos = new JPanel(new BorderLayout(0, 15));
        panelDatos.setBackground(fondoPrincipal);
        panelDatos.setBorder(new EmptyBorder(10, 30, 30, 30));

        JPanel panelHerramientas = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelHerramientas.setBackground(fondoPrincipal);

        JTextField txtBuscador = new JTextField(TXT_BUSCAR_WIKI);
        txtBuscador.setPreferredSize(new Dimension(250, 35));
        txtBuscador.setBackground(panelSecundario);
        txtBuscador.setForeground(textoGris);
        txtBuscador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65)),
                new EmptyBorder(5, 10, 5, 10)
        ));

        txtBuscador.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtBuscador.getText().equals(TXT_BUSCAR_WIKI)) {
                    txtBuscador.setText("");
                    txtBuscador.setForeground(Color.WHITE);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtBuscador.getText().isEmpty()) {
                    txtBuscador.setText(TXT_BUSCAR_WIKI);
                    txtBuscador.setForeground(textoGris);
                }
            }
        });

        JButton btnBuscar = crearBotonAccion("Buscar", acentoVerde);
        btnBuscar.setIcon(cargarIcono("/icons/buscar.svg",22,22));

        btnBuscar.addActionListener(e -> {
            String query = txtBuscador.getText().trim().toLowerCase();
            if (query.isEmpty() || query.equals(TXT_BUSCAR_WIKI.toLowerCase())) {
                renderizarGrid(listaArticulosCache);
            } else {
                List<com.metroidwiki.model.ArticuloDTO> resultados = listaArticulosCache.stream()
                        .filter(a -> a.getTitulo().toLowerCase().contains(query) ||
                                a.getCategoria().toLowerCase().contains(query))
                        .toList(); // 🛠️ Refactorizado a .toList()
                renderizarGrid(resultados);
            }
        });

        JButton btnRefrescar = crearBotonAccion("", panelSecundario);
        btnRefrescar.setIcon(cargarIcono("/icons/actualizar.svg", 22, 22));
        btnRefrescar.setPreferredSize(new Dimension(45, 35));
        btnRefrescar.setToolTipText("Refrescar Bóveda de Artículos");
        btnRefrescar.addActionListener(e -> cargarArticulosDesdeRed());

        panelHerramientas.add(txtBuscador);
        panelHerramientas.add(btnBuscar);
        panelHerramientas.add(btnRefrescar);

        if (rolUsuario.equals("administrador")) {
            JButton btnNuevoArticulo = crearBotonAccion("", new Color(0, 123, 255));
            btnNuevoArticulo.setIcon(cargarIcono("/icons/crear.svg", 22, 22));
            btnNuevoArticulo.setPreferredSize(new Dimension(45, 35));
            btnNuevoArticulo.setToolTipText("Redactar Nuevo Artículo");

            btnNuevoArticulo.addActionListener(e -> {
                NuevoArticuloFrame nuevoArticulo = new NuevoArticuloFrame(tokenUsuarioActual);
                nuevoArticulo.setVisible(true);
            });
            panelHerramientas.add(btnNuevoArticulo);
        }

        gridTarjetas = new JPanel(new GridLayout(0, 3, 25, 25));
        gridTarjetas.setBackground(fondoPrincipal);

        JPanel contenedorGrid = new JPanel(new BorderLayout());
        contenedorGrid.setBackground(fondoPrincipal);
        contenedorGrid.add(gridTarjetas, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(contenedorGrid);
        scrollPane.getViewport().setBackground(fondoPrincipal);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panelDatos.add(panelHerramientas, BorderLayout.NORTH);
        panelDatos.add(scrollPane, BorderLayout.CENTER);

        cargarArticulosDesdeRed();

        return panelDatos;
    }

    private void cargarArticulosDesdeRed() {
        gridTarjetas.removeAll();
        gridTarjetas.add(new JLabel("Descargando archivos clasificados..."));
        gridTarjetas.revalidate();
        gridTarjetas.repaint();

        try {
            com.metroidwiki.network.ArticuloClient client = com.metroidwiki.network.RetrofitClient.getClient().create(com.metroidwiki.network.ArticuloClient.class);

            client.obtenerArticulos().enqueue(new retrofit2.Callback<com.metroidwiki.model.ArticulosListResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.metroidwiki.model.ArticulosListResponse> call, retrofit2.Response<com.metroidwiki.model.ArticulosListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listaArticulosCache = response.body().getArticulos();
                        renderizarGrid(listaArticulosCache);
                    } else {
                        mostrarErrorGrid("Error al leer la base de datos.");
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.metroidwiki.model.ArticulosListResponse> call, Throwable t) {
                    mostrarErrorGrid("Fallo de red al intentar contactar al servidor.");
                }
            });
        } catch (RuntimeException ex) { // 🛠️ Excepción específica de Red
            logger.log(Level.SEVERE, "Fallo interno al ejecutar la petición de red", ex);
            mostrarErrorGrid("Error interno del sistema.");
        }
    }

    private void renderizarGrid(List<com.metroidwiki.model.ArticuloDTO> listaAMostrar) {
        gridTarjetas.removeAll();

        List<com.metroidwiki.model.ArticuloDTO> publicados = listaAMostrar.stream()
                .filter(a -> "Publicado".equalsIgnoreCase(a.getEstado()))
                .toList();

        if (publicados.isEmpty()) {
            JLabel lblVacio = new JLabel("No se encontraron artículos publicados con estos filtros.");
            lblVacio.setForeground(textoGris);
            gridTarjetas.add(lblVacio);
        } else {
            for (com.metroidwiki.model.ArticuloDTO articulo : publicados) {
                gridTarjetas.add(crearTarjetaArticulo(articulo));
            }
        }
        gridTarjetas.revalidate();
        gridTarjetas.repaint();
    }

    private void mostrarErrorGrid(String mensaje) {
        gridTarjetas.removeAll();
        JLabel lblError = new JLabel(mensaje);
        lblError.setForeground(Color.RED);
        gridTarjetas.add(lblError);
        gridTarjetas.revalidate();
        gridTarjetas.repaint();
    }

    private void filtrarDestacados() {
        List<com.metroidwiki.model.ArticuloDTO> destacados = listaArticulosCache.stream()
                .sorted((a1, a2) -> Integer.compare(a2.getVistas(), a1.getVistas()))
                .toList();
        renderizarGrid(destacados);
    }

    private void filtrarNuevasEntradas() {
        List<com.metroidwiki.model.ArticuloDTO> recientes = listaArticulosCache.stream()
                .sorted((a1, a2) -> {
                    if (a1.getFechaCreacion() == null || a2.getFechaCreacion() == null) return 0;
                    return a2.getFechaCreacion().compareTo(a1.getFechaCreacion());
                })
                .toList();
        renderizarGrid(recientes);
    }

    private void filtrarPorCategoria(String categoria) {
        List<com.metroidwiki.model.ArticuloDTO> filtrados = listaArticulosCache.stream()
                .filter(a -> a.getCategoria().equalsIgnoreCase(categoria))
                .toList();
        renderizarGrid(filtrados);
    }

    @SuppressWarnings("squid:S3776")
    private JPanel crearTarjetaArticulo(com.metroidwiki.model.ArticuloDTO articulo) {
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(panelSecundario);
        tarjeta.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
        tarjeta.setPreferredSize(new Dimension(240, 310));

        JPanel panelImagen = new JPanel(new BorderLayout());
        panelImagen.setBackground(new Color(15, 15, 18));
        panelImagen.setPreferredSize(new Dimension(240, 150));

        JLabel lblIcono = new JLabel("Cargando imagen...", SwingConstants.CENTER);
        lblIcono.setFont(new Font(FONT_SEGOE, Font.ITALIC, 12));
        lblIcono.setForeground(textoGris);
        panelImagen.add(lblIcono, BorderLayout.CENTER);

        cargarImagenAsincrona(articulo.getImagen(), lblIcono);

        JPanel panelTextos = new JPanel();
        panelTextos.setLayout(new BoxLayout(panelTextos, BoxLayout.Y_AXIS));
        panelTextos.setBackground(panelSecundario);
        panelTextos.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTitulo = new JLabel(articulo.getTitulo());
        lblTitulo.setFont(new Font(FONT_SEGOE, Font.BOLD, 16));
        lblTitulo.setForeground(textoClaro);

        JLabel lblCategoria = new JLabel("Categoría: " + articulo.getCategoria());
        lblCategoria.setFont(new Font(FONT_SEGOE, Font.PLAIN, 12));
        lblCategoria.setForeground(textoGris);

        JLabel lblVistas = new JLabel(" Vistas: " + articulo.getVistas());
        lblVistas.setFont(new Font(FONT_SEGOE, Font.PLAIN, 12));
        lblVistas.setForeground(acentoVerde);
        Icon vistaIcon = cargarIcono("/icons/vista.svg", 16, 16);
        if (vistaIcon != null) {
            lblVistas.setIcon(vistaIcon);
        }

        panelTextos.add(lblTitulo);
        panelTextos.add(Box.createRigidArea(new Dimension(0, 5)));
        panelTextos.add(lblCategoria);
        panelTextos.add(Box.createRigidArea(new Dimension(0, 2)));
        panelTextos.add(lblVistas);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panelBotones.setBackground(panelSecundario);

        JButton btnLeer = crearBotonAccion("Ver", acentoVerde);
        btnLeer.setPreferredSize(new Dimension(110, 30));

        btnLeer.addActionListener(e -> {
            try {
                com.metroidwiki.network.ArticuloClient client = com.metroidwiki.network.RetrofitClient.getClient().create(com.metroidwiki.network.ArticuloClient.class);
                retrofit2.Call<com.metroidwiki.model.ArticuloResponse> call = client.obtenerArticulo(articulo.getId(), tokenUsuarioActual != null ? "Bearer " + tokenUsuarioActual : null);
                call.enqueue(new retrofit2.Callback<com.metroidwiki.model.ArticuloResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.metroidwiki.model.ArticuloResponse> call, retrofit2.Response<com.metroidwiki.model.ArticuloResponse> response) {
                        com.metroidwiki.model.ArticuloDTO artToOpen = articulo;
                        if (response.isSuccessful() && response.body() != null && response.body().getArticulo() != null) {
                            artToOpen = response.body().getArticulo();
                            lblVistas.setText(" Vistas: " + artToOpen.getVistas());
                            for (int i = 0; i < listaArticulosCache.size(); i++) {
                                if (listaArticulosCache.get(i).getId().equals(artToOpen.getId())) {
                                    listaArticulosCache.set(i, artToOpen);
                                    break;
                                }
                            }
                        }
                        DetalleArticuloFrame vistaDetalle = new DetalleArticuloFrame(artToOpen, tokenUsuarioActual, nombreUsuario);
                        vistaDetalle.setVisible(true);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.metroidwiki.model.ArticuloResponse> call, Throwable t) {
                        DetalleArticuloFrame vistaDetalle = new DetalleArticuloFrame(articulo, tokenUsuarioActual, nombreUsuario);
                        vistaDetalle.setVisible(true);
                    }
                });
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Error ejecutando petición de vista", ex);
                DetalleArticuloFrame vistaDetalle = new DetalleArticuloFrame(articulo, tokenUsuarioActual, nombreUsuario);
                vistaDetalle.setVisible(true);
            }
        });

        panelBotones.add(btnLeer);

        tarjeta.add(panelImagen, BorderLayout.NORTH);
        tarjeta.add(panelTextos, BorderLayout.CENTER);
        tarjeta.add(panelBotones, BorderLayout.SOUTH);

        return tarjeta;
    }

    private void cargarImagenAsincrona(String nombreImagen, JLabel lblIcono) {
        if (nombreImagen == null || nombreImagen.isEmpty() || nombreImagen.equals("default.png")) {
            lblIcono.setText("SIN IMAGEN");
            return;
        }

        String nombreCodificado = nombreImagen.replace(" ", "%20");
        String urlCompleta;
        if (nombreImagen.startsWith("http://") || nombreImagen.startsWith("https://")) {
            urlCompleta = nombreCodificado;
        } else {
            urlCompleta = RetrofitClient.BASE_URL + "articulos/public/imagenes/" + nombreCodificado;
        }

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                URL url = new URL(urlCompleta);
                java.awt.Image img = javax.imageio.ImageIO.read(url);
                if (img != null) {
                    java.awt.Image scaledImg = img.getScaledInstance(240, 150, java.awt.Image.SCALE_SMOOTH);
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
                        lblIcono.setText("IMAGEN CORRUPTA");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    lblIcono.setText("ERROR DE RED");
                    logger.log(Level.SEVERE, "Hilo de imagen interrumpido", ie);
                } catch (ExecutionException ee) {
                    lblIcono.setText("ERROR DE RED");
                    logger.log(Level.SEVERE, "Error ejecutando la descarga de imagen", ee);
                } catch (RuntimeException re) {
                    lblIcono.setText("ERROR DE RED");
                    logger.log(Level.SEVERE, "Fallo interno en el renderizado de la imagen", re);
                }
            }
        };
        worker.execute();
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    private JButton crearBotonMenu(String texto, boolean activo) {
        JButton boton = new JButton(texto);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        boton.setFont(new Font(FONT_SEGOE, activo ? Font.BOLD : Font.PLAIN, 15));
        boton.setForeground(activo ? Color.WHITE : textoGris);
        boton.setBackground(activo ? acentoVerde : fondoLateral);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setOpaque(true);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setBorder(new EmptyBorder(0, 15, 0, 0));
        return boton;
    }

    private JButton crearBotonAccion(String texto, Color colorFondo) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font(FONT_SEGOE, Font.BOLD, 13));
        boton.setForeground(Color.WHITE);
        boton.setBackground(colorFondo);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(130, 35));
        return boton;
    }
}