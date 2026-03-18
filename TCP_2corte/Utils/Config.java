package Utils;

public class Config {
    public static final int PORT = 12345;
    public static final String HOST = "127.0.0.1";

    public static final int BALANCEADOR_PORT = 4000;
    public static final int[] SERVERS = {5001, 5002, 5003};

    // 🆕 Tiempo máximo esperando conexión a un servidor (ms)
    public static final int TIMEOUT_CONEXION = 2000;

    // 🆕 Cada cuánto el balanceador revisa servidores caídos (ms)
    public static final int INTERVALO_REVIVAL = 10000;
}