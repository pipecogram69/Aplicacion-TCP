package Cliente;

import Utils.Config;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClienteGUI extends JFrame {

    private JTextArea logArea;
    private JLabel estadoLabel, intentosLabel;
    private JTextField mensajeField;
    private JButton enviarBtn;

    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;
    private volatile boolean conectado = false;

    public ClienteGUI() {
        setTitle("Cliente TCP - Panel");
        setSize(750, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color bg       = new Color(10, 40, 90);
        Color panel    = new Color(20, 60, 120);
        Color logBg    = new Color(5, 25, 60);

        setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        JLabel titulo = new JLabel("CLIENTE TCP - CONEXION", JLabel.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);
        titulo.setBorder(new EmptyBorder(10, 0, 10, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(logBg);
        logArea.setForeground(Color.WHITE);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "Logs de conexion", 0, 0,
                new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        estadoLabel = new JLabel("Estado: DESCONECTADO", JLabel.CENTER);
        estadoLabel.setOpaque(true);
        estadoLabel.setBackground(new Color(120, 0, 0));
        estadoLabel.setForeground(Color.WHITE);
        estadoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        intentosLabel = new JLabel("Intentos: 0", JLabel.CENTER);
        intentosLabel.setOpaque(true);
        intentosLabel.setBackground(new Color(50, 80, 140));
        intentosLabel.setForeground(Color.WHITE);
        intentosLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel panelEstado = new JPanel(new GridLayout(2, 1, 5, 5));
        panelEstado.setBackground(panel);
        panelEstado.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelEstado.add(estadoLabel);
        panelEstado.add(intentosLabel);

        mensajeField = new JTextField();
        mensajeField.setFont(new Font("Consolas", Font.PLAIN, 14));
        enviarBtn = new JButton("Enviar");
        enviarBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel panelEnviar = new JPanel(new BorderLayout(5, 5));
        panelEnviar.setBorder(new EmptyBorder(5, 5, 5, 5));
        panelEnviar.add(mensajeField, BorderLayout.CENTER);
        panelEnviar.add(enviarBtn, BorderLayout.EAST);
        panelEnviar.setBackground(panel);

        add(titulo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(panelEstado, BorderLayout.SOUTH);
        add(panelEnviar, BorderLayout.PAGE_END);

        enviarBtn.addActionListener(e -> enviarMensaje());
        new Thread(this::conectar).start();
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void conectar() {
        int intentos = 0;
        while (true) {
            if (conectado) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                continue;
            }
            try {
                log("Intentando conectar con el balanceador...");
                socket = new Socket(Config.HOST, Config.BALANCEADOR_PORT);
                pw = new PrintWriter(socket.getOutputStream(), true);
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                conectado = true;
                intentos = 0;

                SwingUtilities.invokeLater(() -> {
                    estadoLabel.setText("Estado: CONECTADO");
                    estadoLabel.setBackground(new Color(0, 120, 0));
                    intentosLabel.setText("Intentos: 0");
                });
                log("Conectado al balanceador");

                new Thread(() -> {
                    try {
                        String msg;
                        while ((msg = br.readLine()) != null) {
                            log("Servidor: " + msg);
                        }
                        throw new IOException("Servidor cerro la conexion");
                    } catch (IOException e) {
                        if (conectado) {
                            log("Conexion perdida. Reconectando...");
                        }
                        cerrarConexion();
                    }
                }).start();

            } catch (IOException e) {
                conectado = false;
                intentos++;
                final int i = intentos;
                SwingUtilities.invokeLater(() -> {
                    estadoLabel.setText("Estado: DESCONECTADO");
                    estadoLabel.setBackground(new Color(120, 0, 0));
                    intentosLabel.setText("Intentos: " + i);
                });
                log("Conexion fallida. Reintentando... (" + intentos + ")");
                if (intentos >= 5) { intentos = 0; log("Reiniciando contador..."); }
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void enviarMensaje() {
        if (!conectado || pw == null) {
            log("Sin conexion. Espera a reconectar...");
            return;
        }
        String msg = mensajeField.getText().trim();
        if (msg.isEmpty()) return;

        pw.println(msg);
        if (pw.checkError()) {
            log("Error al enviar. Servidor desconectado.");
            cerrarConexion();
            return;
        }
        log("Tu: " + msg);
        mensajeField.setText("");
    }

    private void cerrarConexion() {
        conectado = false;
        pw = null;
        br = null;
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        SwingUtilities.invokeLater(() -> {
            estadoLabel.setText("Estado: DESCONECTADO");
            estadoLabel.setBackground(new Color(120, 0, 0));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
    }
}