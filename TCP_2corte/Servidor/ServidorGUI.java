package Servidor;

import Utils.Config;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServidorGUI extends JFrame {

    private JTextArea logArea;
    private JButton btnIniciar, btnAuto, btnApagar;
    private JLabel estadoLabel, titulo;

    private ServerSocket serverSocket;
    private final List<Socket> clientes = new ArrayList<>();

    private volatile boolean servidorActivo = false;
    private boolean modoAutoReinicio = false;
    private boolean apagadoManual = false;

    public ServidorGUI() {
        setTitle("Servidor TCP - Panel de Control");
        setSize(820, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color bg = new Color(10, 40, 90);
        Color panelColor = new Color(20, 60, 120);

        setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        titulo = new JLabel("SERVIDOR TCP - PANEL DE CONTROL", JLabel.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);
        titulo.setBorder(new EmptyBorder(10, 0, 10, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(new Color(5, 25, 60));
        logArea.setForeground(Color.WHITE);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Logs del servidor", 0, 0,
                new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        btnIniciar = crearBoton("Iniciar sin reinicio", new Color(0, 140, 70));
        btnAuto    = crearBoton("Activar auto-reinicio", new Color(0, 90, 180));
        btnApagar  = crearBoton("Apagar servidor", new Color(180, 40, 40));

        estadoLabel = new JLabel("Estado: DETENIDO", JLabel.CENTER);
        estadoLabel.setOpaque(true);
        estadoLabel.setBackground(new Color(120, 0, 0));
        estadoLabel.setForeground(Color.WHITE);
        estadoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel controles = new JPanel(new GridLayout(2, 2, 15, 10));
        controles.setBackground(panelColor);
        controles.setBorder(new EmptyBorder(15, 15, 15, 15));
        controles.add(btnIniciar);
        controles.add(btnAuto);
        controles.add(btnApagar);
        controles.add(estadoLabel);

        btnIniciar.addActionListener(e -> { modoAutoReinicio = false; iniciarServidor(); });
        btnAuto.addActionListener(e -> { modoAutoReinicio = true; iniciarServidor(); });
        btnApagar.addActionListener(e -> apagarServidor());

        add(titulo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(controles, BorderLayout.SOUTH);
    }

    private JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFocusPainted(false);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return btn;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void iniciarServidor() {
        if (servidorActivo) return;
        apagadoManual = false;
        servidorActivo = true;

        new Thread(() -> {
            while (true) {
                try {
                    serverSocket = new ServerSocket(Config.PORT);
                    log("Servidor iniciado en puerto " + Config.PORT);
                    log("Modo: " + (modoAutoReinicio ? "AUTO-REINICIO" : "SIN REINICIO"));

                    SwingUtilities.invokeLater(() -> {
                        estadoLabel.setText("Estado: ACTIVO");
                        estadoLabel.setBackground(new Color(0, 120, 0));
                    });

                    while (servidorActivo) {
                        Socket cliente = serverSocket.accept();
                        clientes.add(cliente);
                        log("Cliente conectado: " + cliente.getPort());
                        // 🆕 hilo por cliente para leer y responder
                        new Thread(() -> manejarCliente(cliente)).start();
                    }

                } catch (IOException e) {
                    log("Servidor detenido");
                }

                if (modoAutoReinicio && !apagadoManual) {
                    log("Reiniciando en 5 segundos...");
                    esperar(5000);
                    servidorActivo = true;
                } else {
                    log("Servidor detenido sin reinicio");
                    SwingUtilities.invokeLater(() -> {
                        estadoLabel.setText("Estado: DETENIDO");
                        estadoLabel.setBackground(new Color(120, 0, 0));
                    });
                    break;
                }
            }
        }).start();
    }

    // 🆕 lee mensajes del cliente y responde
    private void manejarCliente(Socket cliente) {
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter pw = new PrintWriter(cliente.getOutputStream(), true)
        ) {
            String mensaje;
            while ((mensaje = br.readLine()) != null) {
                log("Recibido de " + cliente.getPort() + ": " + mensaje);
                pw.println("Servidor respondio: " + mensaje);
            }
        } catch (IOException e) {
            log("Cliente " + cliente.getPort() + " desconectado");
        } finally {
            clientes.remove(cliente);
            try { if (!cliente.isClosed()) cliente.close(); } catch (IOException ignored) {}
        }
    }

    private void apagarServidor() {
    try {
        log("Apagando servidor...");

        // 🔧 Solo marca manual si NO es auto-reinicio
        if (!modoAutoReinicio) apagadoManual = true;

        servidorActivo = false;

        for (Socket c : clientes) {
            if (!c.isClosed()) c.close();
        }
        clientes.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        SwingUtilities.invokeLater(() -> {
            estadoLabel.setText("Estado: DETENIDO");
            estadoLabel.setBackground(new Color(120, 0, 0));
        });

    } catch (IOException e) {
        log("Error apagando servidor");
    }
}   

    private void esperar(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServidorGUI().setVisible(true));
    }
}