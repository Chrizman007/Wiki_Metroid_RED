package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.metroidwiki.network.ArticuloClient;
import com.metroidwiki.network.RetrofitClient;

public class AdminDashboardFrame extends JFrame {

    // Paleta de colores (Modo Oscuro Metroid Oficial)
    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color fondoFicha = new Color(18, 18, 20);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color acentoAmarillo = new Color(255, 193, 7);
    private final Color acentoRojo = new Color(244, 67, 54);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private String tokenUsuarioActual;
    private String nombreUsuarioActual;

    private JLabel lblTotalGlobal;
    private JLabel lblTotalPropios;
    private JLabel lblTotalBorradores;

    private JTable tablaArticulos;
    private DefaultTableModel modeloTabla;
    private JTextField txtBuscador;

    private List<ArticuloDTO> listaArticulosCache = new ArrayList<>();

    public AdminDashboardFrame(String token, String nombreUsuario, List<ArticuloDTO> articulosIniciales) {
        this.tokenUsuarioActual = token;
        this.nombreUsuarioActual = nombreUsuario;
        this.listaArticulosCache = articulosIniciales != null ? articulosIniciales : new ArrayList<>();

        setTitle("Federación Galáctica - Terminal de Administración");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(fondoPrincipal);
        setLayout(new BorderLayout(15, 15));

        add(crearCabeceraAdmin(), BorderLayout.NORTH);

        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setBackground(fondoPrincipal);
        panelCentral.setBorder(new EmptyBorder(0, 25, 25, 25));

        panelCentral.add(crearPanelKPIs());
        panelCentral.add(Box.createRigidArea(new Dimension(0, 20)));
        panelCentral.add(crearBarraBusqueda());
        panelCentral.add(Box.createRigidArea(new Dimension(0, 15)));
        panelCentral.add(crearSeccionTabla());

        add(panelCentral, BorderLayout.CENTER);

        actualizarContadoresKPI();
        renderizarTabla(listaArticulosCache);
    }

    private JPanel crearCabeceraAdmin() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(fondoPrincipal);
        cabecera.setBorder(new EmptyBorder(25, 25, 10, 25));

        JLabel lblTitulo = new JLabel("PANEL DE CONTROL DE CONTENIDO");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(acentoAmarillo);

        JLabel lblRol = new JLabel("Administrador: " + nombreUsuarioActual);
        lblRol.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRol.setForeground(textoGris);

        cabecera.add(lblTitulo, BorderLayout.WEST);
        cabecera.add(lblRol, BorderLayout.EAST);

        return cabecera;
    }

    private JPanel crearPanelKPIs() {
        JPanel panelKPIs = new JPanel(new GridLayout(1, 3, 20, 0));
        panelKPIs.setBackground(fondoPrincipal);
        panelKPIs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JButton cardTotal = crearTarjetaBotonKPI("TOTAL ARTÍCULOS", "0", acentoVerde);
        lblTotalGlobal = (JLabel) cardTotal.getClientProperty("labelValor");
        cardTotal.addActionListener(e -> renderizarTabla(listaArticulosCache));

        JButton cardPropios = crearTarjetaBotonKPI("CREADOS POR MÍ", "0", acentoAmarillo);
        lblTotalPropios = (JLabel) cardPropios.getClientProperty("labelValor");
        cardPropios.addActionListener(e -> filtrarArticulosPropios());

        JButton cardBorradores = crearTarjetaBotonKPI("EN REVISIÓN", "0", acentoRojo);
        lblTotalBorradores = (JLabel) cardBorradores.getClientProperty("labelValor");
        cardBorradores.addActionListener(e -> filtrarArticulosBorradores());

        panelKPIs.add(cardTotal);
        panelKPIs.add(cardPropios);
        panelKPIs.add(cardBorradores);

        return panelKPIs;
    }

    private JPanel crearBarraBusqueda() {
        // 🛠️ CAMBIO ESTRUCTURAL: Usamos un BoxLayout Horizontal estricto
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.X_AXIS));
        panelBusqueda.setBackground(fondoPrincipal);
        panelBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 🛠️ Blindamos el alto del contenedor para que no se deforme
        panelBusqueda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblIcono = new JLabel("Buscar: ");
        lblIcono.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblIcono.setForeground(textoClaro);

        txtBuscador = new JTextField();
        // Para este Layout estricto, es vital definir el tamaño máximo
        txtBuscador.setMaximumSize(new Dimension(400, 35));
        txtBuscador.setPreferredSize(new Dimension(300, 35));
        txtBuscador.setBackground(panelSecundario);
        txtBuscador.setForeground(Color.WHITE);
        txtBuscador.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65)),
                new EmptyBorder(5, 10, 5, 10)
        ));

        JButton btnBuscar = new JButton("Filtrar");
        btnBuscar.setBackground(acentoVerde);
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBorderPainted(false);
        btnBuscar.setMaximumSize(new Dimension(100, 35)); // 🛠️ Tamaño estricto
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> ejecutarBusqueda());

        JButton btnRefrescar = new JButton("Actualizar Tabla");
        btnRefrescar.setBackground(new Color(33, 150, 243));
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.setBorderPainted(false);
        btnRefrescar.setMaximumSize(new Dimension(150, 35)); // 🛠️ Tamaño estricto
        btnRefrescar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefrescar.addActionListener(e -> refrescarTablaServidor());

        // 🛠️ ENSAMBLAJE ESTRICTO (Componente -> Espacio -> Componente)
        panelBusqueda.add(lblIcono);
        panelBusqueda.add(txtBuscador);
        panelBusqueda.add(Box.createRigidArea(new Dimension(10, 0))); // 10px de separación
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(Box.createRigidArea(new Dimension(15, 0))); // 15px de separación
        panelBusqueda.add(btnRefrescar);

        return panelBusqueda;
    }

    private JPanel crearSeccionTabla() {
        JPanel panelTablaContenedor = new JPanel(new BorderLayout());
        panelTablaContenedor.setBackground(panelSecundario);
        panelTablaContenedor.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));

        String[] columnas = {"ID", "Título", "Categoría", "Autor", "Estado", "Editar", "Borrar"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tablaArticulos = new JTable(modeloTabla);
        tablaArticulos.setBackground(panelSecundario);
        tablaArticulos.setForeground(textoClaro);
        tablaArticulos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tablaArticulos.setRowHeight(40);
        tablaArticulos.setShowGrid(false);

        // 🛠️ RENDERER PERSONALIZADO PARA ENCABEZADOS (Forza el fondo oscuro)
        JTableHeader header = tablaArticulos.getTableHeader();
        header.setPreferredSize(new Dimension(0, 35));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(fondoFicha); // Forzamos el color oscuro
                c.setForeground(Color.WHITE); // Texto blanco brillante
                c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                // Le ponemos un pequeño borde sutil para separar columnas
                ((JLabel) c).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(50, 50, 55)));
                return c;
            }
        };

        // Aplicamos el nuevo renderizador a todas las columnas del encabezado
        for (int i = 0; i < tablaArticulos.getColumnModel().getColumnCount(); i++) {
            tablaArticulos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // 🛠️ RENDERER PARA LAS CELDAS NORMALES (Mantiene tu diseño actual)
        DefaultTableCellRenderer renderCelda = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(noFocusBorder);

                if (isSelected) {
                    c.setBackground(new Color(55, 55, 60));
                } else {
                    c.setBackground(panelSecundario);
                }

                if (column == 4) { // Columna Estado
                    String estado = value != null ? value.toString() : "";
                    if (estado.equalsIgnoreCase("Publicado")) c.setForeground(acentoVerde);
                    else c.setForeground(acentoAmarillo);
                } else {
                    c.setForeground(textoClaro);
                }
                return c;
            }
        };
        renderCelda.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < 5; i++) {
            tablaArticulos.getColumnModel().getColumn(i).setCellRenderer(renderCelda);
        }

        tablaArticulos.getColumnModel().getColumn(5).setCellRenderer(new BotonSimpleRenderer("Editar"));
        tablaArticulos.getColumnModel().getColumn(6).setCellRenderer(new BotonSimpleRenderer("Borrar"));
        tablaArticulos.getColumnModel().getColumn(5).setPreferredWidth(80);
        tablaArticulos.getColumnModel().getColumn(6).setPreferredWidth(80);

        tablaArticulos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = tablaArticulos.columnAtPoint(e.getPoint());
                int row = tablaArticulos.rowAtPoint(e.getPoint());

                if (row >= 0) {
                    // 1. Obtenemos el título del artículo al que le dimos clic
                    String titulo = tablaArticulos.getValueAt(row, 1).toString();

                    // 2. Buscamos el objeto DTO completo en nuestra lista de caché usando el título
                    // (Sabemos que es seguro porque en MongoDB configuramos el título como "unique: true")
                    ArticuloDTO articuloSeleccionado = listaArticulosCache.stream()
                            .filter(a -> a.getTitulo().equals(titulo))
                            .findFirst()
                            .orElse(null);

                    if (articuloSeleccionado != null) {
                        if (col == 5) { // Clic en Editar

                            // 🛠️ ¡MAGIA! Abrimos la ventana pasándole el Token y el Artículo completo
                            EditarArticuloFrame editor = new EditarArticuloFrame(tokenUsuarioActual, articuloSeleccionado, AdminDashboardFrame.this);
                            editor.setVisible(true);

                        } else if (col == 6) { // Clic en Borrar
                            int confirmar = JOptionPane.showConfirmDialog(AdminDashboardFrame.this,
                                    "¿Estás seguro de que deseas destruir los datos de este artículo?",
                                    "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if(confirmar == JOptionPane.YES_OPTION) {
                                // Dejamos el aviso de que falta programar el backend para esta función
                                JOptionPane.showMessageDialog(AdminDashboardFrame.this,
                                        "El frontend detectó la orden, pero necesitamos programar la ruta DELETE en Node.js.");
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaArticulos);
        scrollTabla.getViewport().setBackground(panelSecundario);
        scrollTabla.setBorder(null);

        panelTablaContenedor.add(scrollTabla, BorderLayout.CENTER);
        return panelTablaContenedor;
    }

    private void actualizarContadoresKPI() {
        int total = listaArticulosCache.size();

        long propios = listaArticulosCache.stream()
                .filter(a -> a.getAutor() != null && a.getAutor().equalsIgnoreCase(nombreUsuarioActual))
                .count();

        long borradores = listaArticulosCache.stream()
                .filter(a -> a.getEstado() != null && a.getEstado().equalsIgnoreCase("EnRevision"))
                .count();

        lblTotalGlobal.setText(String.valueOf(total));
        lblTotalPropios.setText(String.valueOf(propios));
        lblTotalBorradores.setText(String.valueOf(borradores));
    }

    private void renderizarTabla(List<ArticuloDTO> listaFiltrada) {
        modeloTabla.setRowCount(0);
        for (ArticuloDTO art : listaFiltrada) {
            modeloTabla.addRow(new Object[]{
                    art.getId() != null ? art.getId().substring(Math.max(art.getId().length() - 6, 0)) : "N/A",
                    art.getTitulo(),
                    art.getCategoria(),
                    art.getAutor() != null ? art.getAutor() : "Desconocido",
                    art.getEstado(),
                    "[ EDITAR ]",
                    "[ BORRAR ]"
            });
        }
    }

    private void filtrarArticulosPropios() {
        List<ArticuloDTO> propios = listaArticulosCache.stream()
                .filter(a -> a.getAutor() != null && a.getAutor().equalsIgnoreCase(nombreUsuarioActual))
                .collect(Collectors.toList());
        renderizarTabla(propios);
    }

    private void filtrarArticulosBorradores() {
        List<ArticuloDTO> borradores = listaArticulosCache.stream()
                .filter(a -> a.getEstado() != null && a.getEstado().equalsIgnoreCase("EnRevision"))
                .collect(Collectors.toList());
        renderizarTabla(borradores);
    }

    private void ejecutarBusqueda() {
        String query = txtBuscador.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderizarTabla(listaArticulosCache);
            return;
        }
        List<ArticuloDTO> filtrados = listaArticulosCache.stream()
                .filter(a -> a.getTitulo().toLowerCase().contains(query))
                .collect(Collectors.toList());
        renderizarTabla(filtrados);
    }

    private JButton crearTarjetaBotonKPI(String titulo, String valorInicial, Color colorAcento) {
        JButton botonCard = new JButton();
        botonCard.setLayout(new BorderLayout());
        botonCard.setBackground(fondoFicha);
        botonCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(panelSecundario, 1), new EmptyBorder(12, 15, 12, 15)));
        botonCard.setFocusPainted(false);
        botonCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botonCard.setOpaque(true);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitulo.setForeground(textoGris);

        JLabel lblValor = new JLabel(valorInicial);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValor.setForeground(colorAcento);

        botonCard.add(lblTitulo, BorderLayout.NORTH);
        botonCard.add(lblValor, BorderLayout.CENTER);
        botonCard.putClientProperty("labelValor", lblValor);

        return botonCard;
    }

    // 🛠️ CLASE PARA DIBUJAR LOS BOTONES INDEPENDIENTES
    class BotonSimpleRenderer extends DefaultTableCellRenderer {
        private String tipo;
        public BotonSimpleRenderer(String tipo) {
            this.tipo = tipo;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(noFocusBorder);
            if (isSelected) c.setBackground(new Color(55, 55, 60)); else c.setBackground(panelSecundario);

            setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (tipo.equals("Editar")) {
                c.setForeground(new Color(33, 150, 243)); // Azul
                setText("[ EDITAR ]");
            } else {
                c.setForeground(new Color(244, 67, 54)); // Rojo
                setText("[ BORRAR ]");
            }
            return c;
        }
    }

    // 🛠️ MÉTODO DE REFRESCO AUTOMÁTICO Y MANUAL
    public void refrescarTablaServidor() {
        try {
            ArticuloClient client = RetrofitClient.getClient().create(ArticuloClient.class);

            // Llamamos al endpoint GET /articulos para bajarnos lo último de MongoDB
            client.obtenerArticulos().enqueue(new retrofit2.Callback<com.metroidwiki.model.ArticulosListResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.metroidwiki.model.ArticulosListResponse> call, retrofit2.Response<com.metroidwiki.model.ArticulosListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Actualizamos nuestra memoria caché con los nuevos datos
                        listaArticulosCache = response.body().getArticulos();

                        // Volvemos a computar las tarjetas KPI de arriba
                        actualizarContadoresKPI();

                        // Volvemos a dibujar las filas con el nuevo estado
                        renderizarTabla(listaArticulosCache);

                        System.out.println("🔄 [Dashboard] Sincronización con MongoDB Atlas completada con éxito.");
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.metroidwiki.model.ArticulosListResponse> call, Throwable t) {
                    System.err.println("Fallo de red al refrescar: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error en el refresco automático: " + e.getMessage());
        }
    }

}