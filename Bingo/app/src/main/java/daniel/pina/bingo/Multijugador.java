package daniel.pina.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Multijugador extends AppCompatActivity {

    private static String SERVER_IP; // IP del servidor
    private static String NOMBRE; // IP del servidor
    private static final int SERVER_PORT = 12345; // Puerto del servidor
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Button conectarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multijugador_layout);

        Button volverButton = findViewById(R.id.volver_button_multi);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irPlay();
            }
        });

        conectarButton = findViewById(R.id.connect_button);
        conectarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conectar();
            }
        });


    }

    private void conectar() {
        // Obtener el valor ingresado en el campo de texto
        EditText nameField = findViewById(R.id.nombre_TextText);
        String nombrePartida = nameField.getText().toString();

        if (nombrePartida.isEmpty()) {
            Log.e("Multijugador", "El nombre no puede estar vacío");
            return;
        }
        NOMBRE = nombrePartida;

        EditText ipField = findViewById(R.id.ip_partida_TextText);
        String direccionIP = ipField.getText().toString();

        if (direccionIP.isEmpty()) {
            Log.e("Multijugador", "La dirección IP no puede estar vacía");
            return;
        }

        SERVER_IP = direccionIP;

        // Iniciar la conexión en un hilo separado
        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Escuchar mensajes del servidor
            new Thread(() -> {
                try {
                    String message;
                    while (in!=null && (message = in.readLine()) != null) {
                        Log.d("Multijugador", "Mensaje recibido del servidor: " + message);
                        // Aquí puedes actualizar la UI o manejar los datos recibidos
                        System.out.println(message);
                        if(message.equals(NOMBRE + " se ha conectado a la partida.")){
                            runOnUiThread(() -> {
                                // Mostrar el aviso de conexión exitosa en la UI
                                Toast.makeText(Multijugador.this, "Te has unido a la partida correctamente", Toast.LENGTH_SHORT).show();
                                conectarButton.setEnabled(false);
                            });
                        }
                        else if(message.equals("PARTIDA COMENZADA")){
                            irPartida(socket, out, in);
                        }
                    }
                } catch (IOException e) {
                    Log.e("Multijugador", "Error al leer del servidor", e);
                }
            }).start();

            // Enviar un mensaje al servidor (por ejemplo, para unirte al juego)
            out.println(NOMBRE);

        } catch (IOException e) {
            Log.e("Multijugador", "Error al conectar con el servidor", e);
        }
    }

    private void irPartida(Socket socket, PrintWriter out, BufferedReader in) {
        // Crear el intent para la actividad Partida
        Intent intent = new Intent(this, Partida.class);

        // Pasar los datos necesarios a la nueva actividad
        intent.putExtra("SERVER_IP", SERVER_IP);
        intent.putExtra("SERVER_PORT", SERVER_PORT);
        intent.putExtra("NOMBRE", NOMBRE);

        // Iniciar la actividad
        startActivity(intent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            Log.e("Multijugador", "Error al cerrar la conexión", e);
        }
    }

    private void irPlay() {
        startActivity(new Intent(this, Play.class));
        finish();
    }
}
