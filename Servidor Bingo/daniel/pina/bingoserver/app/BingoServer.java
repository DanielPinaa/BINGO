package daniel.pina.bingoserver.app;
import java.io.*;
import java.net.*;
import java.util.*;
import daniel.pina.bingoserver.Model.Jugador;

public class BingoServer {

    private static final int PORT = 12345; 
    private static final List<ClientHandler> clients = new ArrayList<>(); 
    private static List<Jugador> jugadores = new ArrayList<>();
    private static boolean partidaComenzada = false; 

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de Bingo iniciado en la dirección IP "+obtenerIPWiFi()+" en el puerto " + PORT );

            new Thread(BingoServer::escucharComando).start();

            while (true) {
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private static void escucharComando() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while ((command = reader.readLine()) != null) {
                if (command.equalsIgnoreCase("EMPEZAR PARTIDA")) {

                    BingoServer.broadcast("PARTIDA COMENZADA");

                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer la entrada estándar: " + e.getMessage());
        }
    }

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
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

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
                if (interfaz.isUp() && !interfaz.isLoopback()) {
                    Enumeration<InetAddress> direcciones = interfaz.getInetAddresses();
                    while (direcciones.hasMoreElements()) {
                        InetAddress direccion = direcciones.nextElement();
                        if (!direccion.isLoopbackAddress() && direccion.getHostAddress().indexOf(':') == -1) {
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
        
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                for (int numero : numerosBingo) {
                    try {
                        BingoServer.broadcast("NUMERO," + numero);
                        System.out.println("NÚMERO: "+numero);
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Hilo interrumpido durante el envío de números: " + e.getMessage());
                Thread.currentThread().interrupt();
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
