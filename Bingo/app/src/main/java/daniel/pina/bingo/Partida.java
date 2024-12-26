package daniel.pina.bingo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Partida extends AppCompatActivity {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partida_layout);

        // Obtener los datos del intent
        String serverIp = getIntent().getStringExtra("SERVER_IP");
        int serverPort = getIntent().getIntExtra("SERVER_PORT", 12345);

        // Crear la conexión con el servidor
        new Thread(() -> connectToServer(serverIp, serverPort)).start();
    }

    private void connectToServer(String serverIp, int serverPort) {
        try {
            // Conectar al servidor
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviar el mensaje al servidor
            out.println("PARTIDA");

            // Escuchar mensajes del servidor
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        Log.d("Partida", "Mensaje recibido del servidor: " + message);
                        // Aquí puedes manejar los mensajes recibidos del servidor
                    }
                } catch (IOException e) {
                    Log.e("Partida", "Error al leer del servidor", e);
                }
            }).start();

        } catch (IOException e) {
            Log.e("Partida", "Error al conectar con el servidor", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            Log.e("Partida", "Error al cerrar la conexión", e);
        }
    }
}

