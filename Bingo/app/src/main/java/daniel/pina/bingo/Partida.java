package daniel.pina.bingo;

import android.content.Intent;
import android.media.MediaPlayer;
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

    private MediaPlayer mediaPlayer;

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

    }

    private void comprobarBingo() {
        boolean bingoCompleto = true;
        for (boolean marcado : markedNumbers) {
            if (!marcado) {
                bingoCompleto = false;
                break;
            }
        }

        if (bingoCompleto) {
            Toast.makeText(this, "¡Bingo completado!", Toast.LENGTH_SHORT).show();


            new Thread(() -> {
                if (out != null) {
                    out.println("BINGO," + NOMBRE);
                }
            }).start();
        } else {
            Toast.makeText(this, "Aún no has completado el bingo.", Toast.LENGTH_SHORT).show();
        }
    }


    private void comprobarLinea() {
        if (lineaCantada) {
            Toast.makeText(this, "¡Ya has cantado línea!", Toast.LENGTH_SHORT).show();
            return;
        }

        int columns = 5;
        for (int row = 0; row < columns; row++) {
            boolean lineaCompleta = true;

            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (!markedNumbers[index]) {
                    lineaCompleta = false;
                    break;
                }
            }

            if (lineaCompleta) {
                System.out.println(NOMBRE +" Va a cantar línea");
                lineaCantada = true;
                Toast.makeText(this, "¡Línea completada!", Toast.LENGTH_SHORT).show();


                new Thread(() -> {
                    if (out != null) {
                        System.out.println("out no es null");
                        out.println("LINEA,"+NOMBRE);
                    }
                }).start();

                return;
            }
        }


        Toast.makeText(this, "Aún no has completado ninguna línea.", Toast.LENGTH_SHORT).show();
    }


    private void irMultijugador() {
        startActivity(new Intent(this, Multijugador.class));
        finish();
    }

    private void connectToServer(String serverIp, int serverPort) {
        try {

            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("PARTIDA");


            new Thread(() -> {
                try {
                    String message;
                    while (in!=null && (message = in.readLine()) != null) {
                        Log.d("Partida", "Mensaje recibido del servidor: " + message);

                        if (message.startsWith("LINEA")) {
                            playSound("linea");

                            lineaCantada = true;
                            String[] aux = message.split(",");
                            String nombre = aux[1];
                            System.out.println(nombre);


                            runOnUiThread(() -> {
                                if (!nombre.equals(NOMBRE)) {
                                    Toast.makeText(this, nombre + " HA CANTADO LÍNEA!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else if (message.startsWith("BINGO")) {

                            playSound("bingo");
                            String[] aux = message.split(",");
                            String nombre = aux[1];


                            runOnUiThread(() -> {
                                if (!nombre.equals(NOMBRE)) {
                                    Toast.makeText(this, nombre + " HA CANTADO BINGO!", Toast.LENGTH_SHORT).show();
                                }
                            });
                            irPantallaFin(nombre);
                        }
                        else if (message.startsWith("NUMERO")) {

                            String[] aux = message.split(",");
                            String numero = aux[1];

                            runOnUiThread(() -> {
                                TextView numeroTextView = findViewById(R.id.numeros_textView);
                                numeroTextView.setText(String.format("Número: %s", numero));
                            });
                        } else if (message.equals("NUMEROS_COMPLETADOS")) {
                            irPantallaFin("NADIE");
                        }

                    }
                } catch (IOException e) {
                    Log.e("Partida", "Error al leer del servidor", e);
                }
            }).start();

        } catch (IOException e) {
            Log.e("Partida", "Error al conectar con el servidor", e);
        }
    }

    private void playSound(String sound) {
        if (sound.equals("linea")) {
            mediaPlayer = MediaPlayer.create(this, R.raw.linea_sound_effect);
        }
        else{
            mediaPlayer = MediaPlayer.create(this, R.raw.victory_sound_effect);
        }


        mediaPlayer.start();
    }

    private void irPantallaFin(String nombre) {
        Intent intent = new Intent(this, Fin.class);

        intent.putExtra("NOMBRE", nombre);

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
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        } catch (IOException e) {
            Log.e("Partida", "Error al cerrar la conexión", e);
        }
    }


    private List<Integer> generateBingoNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers);
        return numbers.subList(0, 25);
    }


    private void markNumber(int index, TextView numberView) {
        if (markedNumbers[index]) {
            Toast.makeText(this, "Este número ya ha sido marcado.", Toast.LENGTH_SHORT).show();
        } else {

            markedNumbers[index] = true;
            numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            numberView.setTextColor(getResources().getColor(android.R.color.white));
        }
    }
}
