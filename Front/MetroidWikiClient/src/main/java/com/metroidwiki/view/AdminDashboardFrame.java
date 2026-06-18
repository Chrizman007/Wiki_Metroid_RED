package com.metroidwiki.view;

import com.metroidwiki.model.ArticuloDTO;
import com.metroidwiki.model.ArticuloRequest;
import com.metroidwiki.model.ArticuloResponse;
import com.metroidwiki.network.ArticuloClient;
import com.metroidwiki.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
// 🛠️ Importar Lector de SVG
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class AdminDashboardFrame extends JFrame {

    // Paleta de colores (Modo Oscuro Metroid Oficial)
    private final Color fondoPrincipal = new Color(25, 25, 28);
    private final Color panelSecundario = new Color(35, 35, 40);
    private final Color fondoFicha = new Color(18, 18, 20);
    private final Color acentoVerde = new Color(76, 175, 80);
    private final Color acentoAmarillo = new Color(255, 193, 7);
    private final Color acentoRojo = new Color(244, 67, 54);
    private final Color acentoGris = new Color(158, 158, 158);
    private final Color textoClaro = new Color(230, 230, 230);
    private final Color textoGris = new Color(150, 150, 150);

    private String tokenUsuarioActual;
    private String nombreUsuarioActual;

    private JLabel lblTotalGlobal;
    private JLabel lblTotalPropios;
    private JLabel lblTotalBorradores;
    private JLabel lblTotalArchivados;

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
        filtrarArticulosActivos();
    }

    // ==========================================
    // 🛠️ HERRAMIENTA: CARGADOR DE ÍCONOS SVG
    // ==========================================
    private Icon cargarIcono(String ruta, int width, int height) {
        try {
            String rutaLimpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
            return new FlatSVGIcon(rutaLimpia, width, height);
        } catch (Exception e) {
            System.err.println("❌ Error cargando SVG " + ruta + ": " + e.getMessage());
            return null;
        }
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
        JPanel panelKPIs = new JPanel(new GridLayout(1, 4, 20, 0));
        panelKPIs.setBackground(fondoPrincipal);
        panelKPIs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JButton cardTotal = crearTarjetaBotonKPI("TOTAL ACTIVOS", "0", acentoVerde);
        lblTotalGlobal = (JLabel) cardTotal.getClientProperty("labelValor");
        cardTotal.addActionListener(e -> filtrarArticulosActivos());

        JButton cardPropios = crearTarjetaBotonKPI("CREADOS POR MÍ", "0", acentoAmarillo);
        lblTotalPropios = (JLabel) cardPropios.getClientProperty("labelValor");
        cardPropios.addActionListener(e -> filtrarArticulosPropios());

        JButton cardBorradores = crearTarjetaBotonKPI("EN REVISIÓN", "0", acentoRojo);
        lblTotalBorradores = (JLabel) cardBorradores.getClientProperty("labelValor");
        cardBorradores.addActionListener(e -> filtrarArticulosBorradores());

        JButton cardArchivados = crearTarjetaBotonKPI("ARCHIVADOS", "0", acentoGris);
        lblTotalArchivados = (JLabel) cardArchivados.getClientProperty("labelValor");
        cardArchivados.addActionListener(e -> filtrarArticulosArchivados());

        panelKPIs.add(cardTotal);
        panelKPIs.add(cardPropios);
        panelKPIs.add(cardBorradores);
        panelKPIs.add(cardArchivados);

        return panelKPIs;
    }

    private JPanel crearBarraBusqueda() {
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.X_AXIS));
        panelBusqueda.setBackground(fondoPrincipal);
        panelBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelBusqueda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblIcono = new JLabel("Buscar: ");
        lblIcono.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblIcono.setForeground(textoClaro);

        txtBuscador = new JTextField();
        txtBuscador.setMaximumSize(new Dimension(400, 35));
        txtBuscador.setPreferredSize(new Dimension(300, 35));
        txtBuscador.setBackground(panelSecundario);
        txtBuscador.setForeground(Color.WHITE);
        txtBuscador.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65)),
                new EmptyBorder(5, 10, 5, 10)
        ));

        // 🛠️ BOTÓN BUSCAR (Texto modificado e Ícono inyectado)
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setIcon(cargarIcono("/icons/buscar.svg", 18, 18));
        btnBuscar.setIconTextGap(8); // Separación entre el icono y el texto
        btnBuscar.setBackground(acentoVerde);
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBorderPainted(false);
        btnBuscar.setMaximumSize(new Dimension(110, 35)); // Ampliado un poquito para el ícono
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.addActionListener(e -> ejecutarBusqueda());

        // 🛠️ BOTÓN REFRESCAR (Texto acortado e Ícono inyectado)
        JButton btnRefrescar = new JButton("Actualizar");
        btnRefrescar.setIcon(cargarIcono("/icons/actualizar.svg", 18, 18));
        btnRefrescar.setIconTextGap(8);
        btnRefrescar.setBackground(new Color(33, 150, 243));
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.setBorderPainted(false);
        btnRefrescar.setMaximumSize(new Dimension(130, 35)); // Ajustado para el ícono
        btnRefrescar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefrescar.addActionListener(e -> refrescarTablaServidor());

        panelBusqueda.add(lblIcono);
        panelBusqueda.add(txtBuscador);
        panelBusqueda.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(Box.createRigidArea(new Dimension(15, 0)));
        panelBusqueda.add(btnRefrescar);

        return panelBusqueda;
    }

    private JPanel crearSeccionTabla() {
        JPanel panelTablaContenedor = new JPanel(new BorderLayout());
        panelTablaContenedor.setBackground(panelSecundario);
        panelTablaContenedor.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));

        String[] columnas = {"ID", "Título", "Categoría", "Autor", "Estado", "Editar", "Archivar"};
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

        JTableHeader header = tablaArticulos.getTableHeader();
        header.setPreferredSize(new Dimension(0, 35));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(fondoFicha);
                c.setForeground(Color.WHITE);
                c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                ((JLabel) c).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(50, 50, 55)));
                return c;
            }
        };

        for (int i = 0; i < tablaArticulos.getColumnModel().getColumnCount(); i++) {
            tablaArticulos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

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

                if (column == 4) {
                    String estado = value != null ? value.toString() : "";
                    if (estado.equalsIgnoreCase("Publicado")) c.setForeground(acentoVerde);
                    else if (estado.equalsIgnoreCase("EnRevision")) c.setForeground(acentoRojo);
                    else if (estado.equalsIgnoreCase("Archivado")) c.setForeground(acentoGris);
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

        tablaArticulos.getColumnModel().getColumn(5).setCellRenderer(new BotonIconoRenderer("Editar"));
        tablaArticulos.getColumnModel().getColumn(6).setCellRenderer(new BotonIconoRenderer("Archivar"));
        tablaArticulos.getColumnModel().getColumn(5).setPreferredWidth(60);
        tablaArticulos.getColumnModel().getColumn(6).setPreferredWidth(60);

        tablaArticulos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = tablaArticulos.columnAtPoint(e.getPoint());
                int row = tablaArticulos.rowAtPoint(e.getPoint());

                if (row >= 0) {
                    String titulo = tablaArticulos.getValueAt(row, 1).toString();

                    ArticuloDTO articuloSeleccionado = listaArticulosCache.stream()
                            .filter(a -> a.getTitulo().equals(titulo))
                            .findFirst()
                            .orElse(null);

                    if (articuloSeleccionado != null) {
                        if (col == 5) {
                            EditarArticuloFrame editor = new EditarArticuloFrame(tokenUsuarioActual, articuloSeleccionado, AdminDashboardFrame.this);
                            editor.setVisible(true);

                        } else if (col == 6) {
                            int confirmar = JOptionPane.showConfirmDialog(AdminDashboardFrame.this,
                                    "¿Estás seguro de que deseas archivar este artículo?\nDejará de ser visible para los usuarios de la Wiki.",
                                    "Confirmar Archivado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if(confirmar == JOptionPane.YES_OPTION) {
                                ejecutarArchivadoREST(articuloSeleccionado);
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

    private void ejecutarArchivadoREST(ArticuloDTO articulo) {
        try {
            ArticuloClient client = RetrofitClient.getClient().create(ArticuloClient.class);

            ArticuloRequest request = new ArticuloRequest(
                    articulo.getTitulo(),
                    articulo.getCategoria(),
                    articulo.getDescripcion(),
                    articulo.getContenido(),
                    null,
                    "Archivado"
            );

            String bearerToken = "Bearer " + tokenUsuarioActual;

            client.actualizarArticulo(bearerToken, articulo.getId(), request).enqueue(new Callback<ArticuloResponse>() {
                @Override
                public void onResponse(Call<ArticuloResponse> call, Response<ArticuloResponse> response) {
                    if (response.isSuccessful()) {
                        JOptionPane.showMessageDialog(AdminDashboardFrame.this, "Artículo archivado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        refrescarTablaServidor();
                    } else {
                        JOptionPane.showMessageDialog(AdminDashboardFrame.this, "Error del servidor: " + response.code(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                @Override
                public void onFailure(Call<ArticuloResponse> call, Throwable t) {
                    JOptionPane.showMessageDialog(AdminDashboardFrame.this, "Fallo de red: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error interno al archivar.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarContadoresKPI() {
        long activos = listaArticulosCache.stream()
                .filter(a -> !"Archivado".equalsIgnoreCase(a.getEstado()))
                .count();

        long propios = listaArticulosCache.stream()
                .filter(a -> a.getAutor() != null && a.getAutor().equalsIgnoreCase(nombreUsuarioActual))
                .count();

        long borradores = listaArticulosCache.stream()
                .filter(a -> "EnRevision".equalsIgnoreCase(a.getEstado()))
                .count();

        long archivados = listaArticulosCache.stream()
                .filter(a -> "Archivado".equalsIgnoreCase(a.getEstado()))
                .count();

        lblTotalGlobal.setText(String.valueOf(activos));
        lblTotalPropios.setText(String.valueOf(propios));
        lblTotalBorradores.setText(String.valueOf(borradores));
        lblTotalArchivados.setText(String.valueOf(archivados));
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
                    "",
                    ""
            });
        }
    }

    private void filtrarArticulosActivos() {
        List<ArticuloDTO> activos = listaArticulosCache.stream()
                .filter(a -> !"Archivado".equalsIgnoreCase(a.getEstado()))
                .collect(Collectors.toList());
        renderizarTabla(activos);
    }

    private void filtrarArticulosArchivados() {
        List<ArticuloDTO> archivados = listaArticulosCache.stream()
                .filter(a -> "Archivado".equalsIgnoreCase(a.getEstado()))
                .collect(Collectors.toList());
        renderizarTabla(archivados);
    }

    private void filtrarArticulosPropios() {
        List<ArticuloDTO> propios = listaArticulosCache.stream()
                .filter(a -> a.getAutor() != null && a.getAutor().equalsIgnoreCase(nombreUsuarioActual))
                .collect(Collectors.toList());
        renderizarTabla(propios);
    }

    private void filtrarArticulosBorradores() {
        List<ArticuloDTO> borradores = listaArticulosCache.stream()
                .filter(a -> "EnRevision".equalsIgnoreCase(a.getEstado()))
                .collect(Collectors.toList());
        renderizarTabla(borradores);
    }

    private void ejecutarBusqueda() {
        String query = txtBuscador.getText().trim().toLowerCase();

        List<ArticuloDTO> filtrados = listaArticulosCache.stream()
                .filter(a -> !"Archivado".equalsIgnoreCase(a.getEstado()))
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

    class BotonIconoRenderer extends DefaultTableCellRenderer {
        private String tipo;
        private Icon iconoEditar;
        private Icon iconoArchivar;

        public BotonIconoRenderer(String tipo) {
            this.tipo = tipo;
            setHorizontalAlignment(SwingConstants.CENTER);
            iconoEditar = cargarIcono("/icons/editar.svg", 24, 24);
            iconoArchivar = cargarIcono("/icons/archivar.svg", 24, 24);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setBorder(noFocusBorder);
            lbl.setText("");

            if (isSelected) {
                lbl.setBackground(new Color(55, 55, 60));
            } else {
                lbl.setBackground(panelSecundario);
            }

            if (tipo.equals("Editar")) {
                lbl.setIcon(iconoEditar);
                lbl.setToolTipText("Editar Artículo");
            } else {
                lbl.setIcon(iconoArchivar);
                lbl.setToolTipText("Archivar Artículo");
            }

            return lbl;
        }
    }

    public void refrescarTablaServidor() {
        try {
            ArticuloClient client = RetrofitClient.getClient().create(ArticuloClient.class);
            client.obtenerArticulos().enqueue(new Callback<com.metroidwiki.model.ArticulosListResponse>() {
                @Override
                public void onResponse(Call<com.metroidwiki.model.ArticulosListResponse> call, Response<com.metroidwiki.model.ArticulosListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listaArticulosCache = response.body().getArticulos();
                        actualizarContadoresKPI();
                        filtrarArticulosActivos();
                    }
                }

                @Override
                public void onFailure(Call<com.metroidwiki.model.ArticulosListResponse> call, Throwable t) {
                    System.err.println("Fallo de red al refrescar: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error en el refresco automático: " + e.getMessage());
        }
    }
}