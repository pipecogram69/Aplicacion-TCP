package Servidor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorSimple {

    private int puerto;

    public ServidorSimple(int puerto) {
        this.puerto = puerto;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("[SERVIDOR] Activo en puerto " + puerto);

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("[SERVIDOR " + puerto + "] Cliente conectado");
                new Thread(() -> manejarCliente(cliente)).start();
            }

        } catch (IOException e) {
            System.out.println("[SERVIDOR " + puerto + "] Error o apagado");
        }
    }

    private void manejarCliente(Socket cliente) {
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter pw = new PrintWriter(cliente.getOutputStream(), true)
        ) {
            String mensaje;
            while ((mensaje = br.readLine()) != null) {
                System.out.println("[SERVIDOR " + puerto + "] Recibido: " + mensaje);
                pw.println("Servidor " + puerto + " respondio: " + mensaje);
            }
        } catch (IOException e) {
            System.out.println("[SERVIDOR " + puerto + "] Cliente desconectado");
        } finally {
            try { if (!cliente.isClosed()) cliente.close(); } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java ServidorSimple <puerto>");
            return;
        }
        new ServidorSimple(Integer.parseInt(args[0])).iniciar();
    }
}