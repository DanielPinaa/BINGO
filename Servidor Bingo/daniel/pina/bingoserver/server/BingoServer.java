import java.io.*;
import java.net.*;
import java.util.*;
import daniel.pina.bingoserver.Model.Jugador;

public class BingoServer {

    private static final int PORT = 12345; // Puerto del servidor
    private static final List<ClientHandler> clients = new ArrayList<>(); // Lista de clientes conectados
    private static List<Jugador> jugadores = new ArrayList<>();
    private static boolean partidaComenzada = false; // Variable para controlar si la partida ha comenzado

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de Bingo iniciado en la dirección IP "+obtenerIPWiFi()+" en el puerto " + PORT );

            // Hilo para leer la entrada estándar
            new Thread(BingoServer::escucharComando).start();

            while (true) {
                // Aceptar conexiones de clientes si la partida no ha comenzado
                if (!partidaComenzada) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                    // Manejar la conexión en un nuevo hilo
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Enviar mensaje a todos los clientes
    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Método para escuchar los comandos de la entrada estándar
    private static void escucharComando() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while ((command = reader.readLine()) != null) {
                if (command.equalsIgnoreCase("EMPEZAR PARTIDA")) {
                    // Detener la aceptación de nuevas conexiones
                    partidaComenzada = true;

                    // Informar a todos los clientes que la partida ha comenzado
                    BingoServer.broadcast("PARTIDA COMENZADA");

                    // Salir del bucle de lectura
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer la entrada estándar: " + e.getMessage());
        }
    }

    // Clase para manejar la conexión con cada cliente
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Configurar streams de entrada y salida
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Leer mensajes del cliente
                String message;
                while ((message = in.readLine()) != null) {
                    if(message.equals("PARTIDA")){
                        System.out.println("Partida conectada");
                        
                        //ENVIAR EL CARTÓN A CADA JUGADOR
                    }
                    else{
                        System.out.println("Jugador conectado: " + message);
                        jugadores.add(new Jugador(message));
                        System.out.println("Número de jugadores: " + jugadores.size());
                        // Enviar mensaje a todos los clientes
                        BingoServer.broadcast(message + " se ha conectado a la partida.");
                    }
                    
                }

            } catch (IOException e) {
                System.err.println("Error con el cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar el socket: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    private static String obtenerIPWiFi() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface interfaz = interfaces.nextElement();
                // Filtrar solo interfaces activas y no loopback
                if (interfaz.isUp() && !interfaz.isLoopback()) {
                    // Obtener direcciones asociadas a la interfaz
                    Enumeration<InetAddress> direcciones = interfaz.getInetAddresses();
                    while (direcciones.hasMoreElements()) {
                        InetAddress direccion = direcciones.nextElement();
                        // Filtrar IPv4 y excluir direcciones de enlace local
                        if (!direccion.isLoopbackAddress() && direccion.getHostAddress().indexOf(':') == -1) {
                            // Validar si es la interfaz Wi-Fi (opcional)
                            if (interfaz.getName().contains("wlan") || interfaz.getDisplayName().toLowerCase().contains("wi-fi")) {
                                return direccion.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Error al obtener la dirección IP de la interfaz Wi-Fi.");
            e.printStackTrace();
        }
        return null;
    }
}
