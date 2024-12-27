package daniel.pina.bingoserver.app;
import java.io.*;
import java.net.*;
import java.util.*;
import daniel.pina.bingoserver.Model.Jugador;

public class BingoServer {

    private static final int PORT = 12345; // Puerto del servidor
    private static final List<ClientHandler> clients = new ArrayList<>(); // Lista de clientes conectados
    private static List<Jugador> jugadores = new ArrayList<>();
    private static boolean partidaComenzada = false; // Variable para controlar si la partida ha comenzado

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de Bingo iniciado en la dirección IP "+obtenerIPWiFi()+" en el puerto " + PORT );

            // Hilo para leer la entrada estándar
            new Thread(BingoServer::escucharComando).start();

            while (true) {
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                // Manejar la conexión en un nuevo hilo
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

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
        private boolean lineaCantada = false;

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
                    System.out.println(message);
                    if(message.equals("PARTIDA")){
                        System.out.println("Partida conectada");
                        
                        
                    }
                    else if(message.startsWith("LINEA") && !lineaCantada){
                        lineaCantada = true;
                        String [] aux  = message.split(",");
                        String nombre = aux[1];
                        System.out.println(nombre+" HA CANTADO LÍNEA!");

                        BingoServer.broadcast("LINEA,"+nombre);
                        
                    }
                    else if(message.startsWith("BINGO")){
                        String [] aux  = message.split(",");
                        String nombre = aux[1];
                        System.out.println(nombre+" HA CANTADO BINGO!");

                        BingoServer.broadcast("BINGO,"+nombre);
                        
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

    public static String obtenerIPWiFi() {
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

    public static boolean getPartidaComenzada(){
        return partidaComenzada;
    }

    public static void comenzarPartida() {
        partidaComenzada = true;
    
        List<Integer> numerosBingo = generarNumerosBingo();
    
        BingoServer.broadcast("PARTIDA COMENZADA");
    
       new Thread(() -> {
            for (int numero : numerosBingo) {
                try {
                    BingoServer.broadcast("NUMERO," + numero);
    
                    Thread.sleep(5500);
                } catch (InterruptedException e) {
                    System.err.println("Hilo interrumpido durante el envío de números: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
    
            BingoServer.broadcast("NUMEROS_COMPLETADOS");
        }).start();
    }
    
    private static List<Integer> generarNumerosBingo() {
        List<Integer> numeros = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            numeros.add(i);
        }
        Collections.shuffle(numeros); 
        return numeros;
    }
    

    public static void finalizarPartida(){
        partidaComenzada = false;
    }
}
