package daniel.pina.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Partida extends AppCompatActivity {

    private GridLayout bingoGrid;
    private List<Integer> bingoNumbers;
    private boolean[] markedNumbers;

    private boolean lineaCantada = false;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String NOMBRE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partida_layout);

        Button volverButton = findViewById(R.id.volver_multijugador_button);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMultijugador();
            }
        });

        Button lineaButton = findViewById(R.id.linea_button);
        lineaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarLinea();
            }
        });

        Button bingoButton = findViewById(R.id.bingo_button);
        bingoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarBingo();
            }
        });

        bingoGrid = findViewById(R.id.bingoGrid);

        bingoNumbers = generateBingoNumbers();
        markedNumbers = new boolean[bingoNumbers.size()];

        for (int i = 0; i < bingoNumbers.size(); i++) {
            final TextView numberView = new TextView(this);
            numberView.setText(String.valueOf(bingoNumbers.get(i)));
            numberView.setTextSize(32f); // Tamaño del texto más grande
            numberView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            numberView.setPadding(25, 25, 25, 25);
            numberView.setGravity(Gravity.CENTER);

            final int index = i;
            numberView.setOnClickListener(v -> markNumber(index, numberView));

            bingoGrid.addView(numberView);
        }


        String serverIp = getIntent().getStringExtra("SERVER_IP");
        int serverPort = getIntent().getIntExtra("SERVER_PORT", 12345);
        NOMBRE = getIntent().getStringExtra("NOMBRE");

        new Thread(() -> connectToServer(serverIp, serverPort)).start();

        // Escuchar mensajes del servidor
        new Thread(() -> {
            try {
                String message;
                while (in!=null && (message = in.readLine()) != null) {
                    if(message.startsWith("LINEA")){
                        String [] aux  = message.split(",");
                        String nombre = aux[1];
                        if(!nombre.equals(NOMBRE))
                            Toast.makeText(this, nombre+" HA CANTADO LÍNEA!", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                Log.e("Multijugador", "Error al leer del servidor", e);
            }
        }).start();
    }

    private void comprobarBingo() {

    }

    private void comprobarLinea() {
        if (lineaCantada) {
            Toast.makeText(this, "¡Ya has cantado línea!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Número de columnas en la cuadrícula
        int columns = 5;

        // Comprobar cada fila
        for (int row = 0; row < columns; row++) {
            boolean lineaCompleta = true;

            // Comprobar cada número en la fila
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (!markedNumbers[index]) {
                    lineaCompleta = false;
                    break;
                }
            }

            if (lineaCompleta) {
                lineaCantada = true; // Marcar que se ha cantado línea
                Toast.makeText(this, "¡Línea completada!", Toast.LENGTH_SHORT).show();

                // Enviar mensaje al servidor en un hilo secundario
                new Thread(() -> {
                    if (out != null) {
                        out.println("LINEA,"+NOMBRE);
                    }
                }).start();

                return;
            }
        }

        // Si no se encontró ninguna línea completa
        Toast.makeText(this, "Aún no has completado ninguna línea.", Toast.LENGTH_SHORT).show();
    }


    private void irMultijugador() {
        startActivity(new Intent(this, Multijugador.class));
        finish();
    }

    private void connectToServer(String serverIp, int serverPort) {
        try {
            // Conectar al servidor
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviar un mensaje indicando que la partida ha comenzado
            out.println("PARTIDA");

            // Escuchar mensajes del servidor
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        Log.d("Partida", "Mensaje recibido del servidor: " + message);

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

    // Método para generar números de bingo aleatorios (1-75)
    private List<Integer> generateBingoNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers); // Mezclar los números
        return numbers.subList(0, 25); // Seleccionar los primeros 25 números
    }

    // Método para marcar un número cuando el usuario haga clic
    private void markNumber(int index, TextView numberView) {
        if (markedNumbers[index]) {
            Toast.makeText(this, "Este número ya ha sido marcado.", Toast.LENGTH_SHORT).show();
        } else {
            // Marcar el número como seleccionado
            markedNumbers[index] = true;
            numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light)); // Cambiar color a verde
            numberView.setTextColor(getResources().getColor(android.R.color.white)); // Cambiar color del texto
            Toast.makeText(this, "Número marcado: " + numberView.getText(), Toast.LENGTH_SHORT).show();


        }
    }
}
