package Balanceador;

import Utils.Config;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Balanceador {

    private static int index = 0;
    private static final Set<Integer> servidoresCaidos = new HashSet<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        new Thread(Balanceador::revivirServidores).start();

        try (ServerSocket serverSocket = new ServerSocket(Config.BALANCEADOR_PORT)) {
            System.out.println("[BALANCEADOR] Activo en puerto " + Config.BALANCEADOR_PORT);

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("[BALANCEADOR] Cliente conectado: " + cliente.getPort());
                new Thread(() -> manejarCliente(cliente)).start();
            }

        } catch (IOException e) {
            System.out.println("[BALANCEADOR] Error fatal");
            e.printStackTrace();
        }
    }

    private static Socket conectarServidorDisponible() throws IOException {
        synchronized (lock) {
            int intentos = 0;
            int total = Config.SERVERS.length;

            while (intentos < total) {
                int puerto = Config.SERVERS[index];
                index = (index + 1) % total;

                if (servidoresCaidos.contains(puerto)) {
                    System.out.println("[BALANCEADOR] Saltando servidor caido: " + puerto);
                    intentos++;
                    continue;
                }

                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(Config.HOST, puerto), Config.TIMEOUT_CONEXION);
                    System.out.println("[BALANCEADOR] Conectado a servidor: " + puerto);
                    return s;
                } catch (IOException e) {
                    System.out.println("[BALANCEADOR] Servidor " + puerto + " no responde, marcando caido");
                    servidoresCaidos.add(puerto);
                    intentos++;
                }
            }
            throw new IOException("Sin servidores disponibles");
        }
    }

    private static void revivirServidores() {
        while (true) {
            try { Thread.sleep(Config.INTERVALO_REVIVAL); } catch (InterruptedException ignored) {}

            synchronized (lock) {
                Set<Integer> recuperados = new HashSet<>();
                for (int puerto : servidoresCaidos) {
                    try {
                        Socket test = new Socket();
                        test.connect(new InetSocketAddress(Config.HOST, puerto), Config.TIMEOUT_CONEXION);
                        test.close();
                        recuperados.add(puerto);
                        System.out.println("[BALANCEADOR] Servidor recuperado: " + puerto);
                    } catch (IOException ignored) {
                        System.out.println("[BALANCEADOR] Servidor " + puerto + " sigue caido");
                    }
                }
                servidoresCaidos.removeAll(recuperados);
            }
        }
    }

    private static void manejarCliente(Socket cliente) {
        Socket servidor = null;
        try {
            servidor = conectarServidorDisponible();

            BufferedReader brCliente  = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter    pwServidor = new PrintWriter(servidor.getOutputStream(), true);
            BufferedReader brServidor = new BufferedReader(new InputStreamReader(servidor.getInputStream()));
            PrintWriter    pwCliente  = new PrintWriter(cliente.getOutputStream(), true);

            final Socket servidorRef = servidor;

            Thread clienteAServidor = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = brCliente.readLine()) != null) {
                        pwServidor.println(linea);
                    }
                } catch (IOException e) {
                    System.out.println("[BALANCEADOR] Cliente desconectado");
                } finally {
                    cerrar(cliente);
                    cerrar(servidorRef);
                }
            });

            Thread servidorACliente = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = brServidor.readLine()) != null) {
                        pwCliente.println(linea);
                    }
                    // readLine() = null significa que el servidor cerro
                    pwCliente.println("[AVISO] El servidor se desconecto. Reconectando...");
                } catch (IOException e) {
                    try {
                        pwCliente.println("[AVISO] El servidor se desconecto. Reconectando...");
                    } catch (Exception ignored) {}
                } finally {
                    cerrar(servidorRef);
                    cerrar(cliente);
                }
            });

            clienteAServidor.start();
            servidorACliente.start();
            clienteAServidor.join();
            servidorACliente.join();

        } catch (IOException e) {
            System.out.println("[BALANCEADOR] Sin servidores para cliente " + cliente.getPort());
            try {
                PrintWriter pw = new PrintWriter(cliente.getOutputStream(), true);
                pw.println("[ERROR] No hay servidores disponibles. Intenta mas tarde.");
            } catch (IOException ignored) {}
            cerrar(cliente);
            cerrar(servidor);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void cerrar(Socket s) {
        try { if (s != null && !s.isClosed()) s.close(); } catch (IOException ignored) {}
    }
}