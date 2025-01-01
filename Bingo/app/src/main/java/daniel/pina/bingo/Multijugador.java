package daniel.pina.bingo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;

public class Multijugador extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static String SERVER_IP;
    private static String NOMBRE;
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private Button conectarButton;

    private TextToSpeech textToSpeech;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multijugador_layout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Button volverButton = findViewById(R.id.volver_button_multi);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMenu();
            }
        });

        conectarButton = findViewById(R.id.connect_button);
        conectarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conectar();
            }
        });

        textToSpeech = new TextToSpeech(this, this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Esperando que la partida comience...");
        progressDialog.setCancelable(false);

    }

    private void conectar() {
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


        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            new Thread(() -> {
                try {
                    String message;
                    while (in!=null && (message = in.readLine()) != null) {
                        Log.d("Multijugador", "Mensaje recibido del servidor: " + message);

                        if(message.equals(NOMBRE + " se ha conectado a la partida.")){

                            runOnUiThread(() -> {
                                Toast.makeText(Multijugador.this, "Te has unido a la partida correctamente", Toast.LENGTH_SHORT).show();
                                conectarButton.setEnabled(false);
                                progressDialog.show();
                            });
                        }
                        else{
                            speakPlayer(message);
                        }
                        if(message.equals("PARTIDA COMENZADA")){
                            runOnUiThread(() -> progressDialog.dismiss());
                            irPartida(socket, out, in);
                        }
                    }
                } catch (IOException e) {
                    Log.e("Multijugador", "Error al leer del servidor", e);
                }
            }).start();


            out.println(NOMBRE);

        } catch (IOException e) {
            Log.e("Multijugador", "Error al conectar con el servidor", e);
        }
    }

    private void irPartida(Socket socket, PrintWriter out, BufferedReader in) {

        Intent intent = new Intent(this, Partida.class);

        intent.putExtra("SERVER_IP", SERVER_IP);
        intent.putExtra("SERVER_PORT", SERVER_PORT);
        intent.putExtra("NOMBRE", NOMBRE);

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

    private void speakPlayer(String player) {
        if (textToSpeech != null) {
            textToSpeech.speak(player, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void irMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("Partida", "Idioma no soportado para TTS.");
            }
        } else {
            Log.e("Partida", "Error al inicializar TTS.");
        }
    }
}
