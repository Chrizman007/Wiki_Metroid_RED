package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import javax.imageio.ImageIO;

public class DetalleArticuloFrame extends JFrame {

    // Paleta de colores (Modo Oscuro Metroid Oficial)
    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color fondoFicha = new Color(18, 18, 20);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private ArticuloDTO articulo;

    public DetalleArticuloFrame(ArticuloDTO articulo) {
        this.articulo = articulo;

        setTitle("Federación Galáctica - " + articulo.getTitulo());
        setSize(850, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Contenedor principal con scroll por si el artículo es muy extenso
        JPanel panelContenedorAbsoluto = new JPanel(new BorderLayout());
        panelContenedorAbsoluto.setBackground(fondoPrincipal);

        JScrollPane scrollGeneral = new JScrollPane(panelContenedorAbsoluto);
        scrollGeneral.setBorder(null);
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
        panelEscribir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panelEscribir.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtNuevoComentario = new JTextField("Escribe una transmisión pública...");
        txtNuevoComentario.setBackground(panelSecundario);
        txtNuevoComentario.setForeground(textoGris);
        txtNuevoComentario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNuevoComentario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelSecundario),
                new EmptyBorder(5, 10, 5, 10)
        ));

        JButton btnComentar = new JButton("ENVIAR");
        btnComentar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnComentar.setBackground(acentoVerde);
        btnComentar.setForeground(Color.WHITE);
        btnComentar.setFocusPainted(false);
        btnComentar.setBorderPainted(false);

        panelEscribir.add(txtNuevoComentario, BorderLayout.CENTER);
        panelEscribir.add(btnComentar, BorderLayout.EAST);
        panelComentarios.add(panelEscribir);
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 20)));

        // Un par de comentarios estáticos como plantilla visual para el profesor
        panelComentarios.add(crearGloboComentario("Adam_Malkovich", "Excelente informe estelar. Los datos sobre el planeta ZEBES concuerdan."));
        panelComentarios.add(Box.createRigidArea(new Dimension(0, 10)));
        panelComentarios.add(crearGloboComentario("Anthony_Higgs", "¡Esa fotografía quedó genial! Buen trabajo documentando la fauna Chozo."));

        return panelComentarios;
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

        JLabel lblText = new JLabel(mensaje);
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblText.setForeground(textoClaro);

        globo.add(lblUser);
        globo.add(Box.createRigidArea(new Dimension(0, 3)));
        globo.add(lblText);

        return globo;
    }

    private void cargarImagenFichaAsincrona(String nombreImagen, JLabel lblIcono) {
        if (nombreImagen == null || nombreImagen.isEmpty() || nombreImagen.equals("default.png")) {
            lblIcono.setText("SIN IMAGEN");
            return;
        }

        String nombreCodificado = nombreImagen.replace(" ", "%20");
        String urlCompleta = "http://localhost:3001/articulos/public/imagenes/" + nombreCodificado;

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