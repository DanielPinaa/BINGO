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
import java.util.Random;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class Partida extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private GridLayout bingoGrid;
    private List<Integer> bingoNumbers;
    private boolean[] markedNumbers;

    private boolean lineaCantada = false;

    private TextToSpeech textToSpeech;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String NOMBRE;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partida_layout);

        textToSpeech = new TextToSpeech(this, this);

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

        markedNumbers = new boolean[80];

        List<List<Integer>> orderedNumbers = generateBingoNumbers();

        for (int col = 0; col < 8; col++) {
            List<Integer> currentColumn = orderedNumbers.get(col);
            for (int row = 0; row < 8; row++) {
                TextView numberView = new TextView(this);
                numberView.setTextSize(32f);
                numberView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                numberView.setPadding(16, 16, 16, 16);
                numberView.setGravity(Gravity.CENTER);

                if (row < currentColumn.size() && currentColumn.get(row) != null) {
                    int number = currentColumn.get(row);
                    numberView.setText(String.valueOf(number));

                    final int columnIndex = col;
                    final int rowIndex = row;
                    numberView.setOnClickListener(v -> markNumber(columnIndex, rowIndex, numberView));
                } else {
                    numberView.setText("");
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                numberView.setLayoutParams(params);

                bingoGrid.addView(numberView);
            }
        }


        String serverIp = getIntent().getStringExtra("SERVER_IP");
        int serverPort = getIntent().getIntExtra("SERVER_PORT", 12345);
        NOMBRE = getIntent().getStringExtra("NOMBRE");

        new Thread(() -> connectToServer(serverIp, serverPort)).start();

    }

    private void comprobarBingo() {
        boolean bingoCompleto = true;

        for (int i = 0; i < bingoGrid.getChildCount(); i++) {
            TextView cell = (TextView) bingoGrid.getChildAt(i);

            if (cell.getText().toString().isEmpty()) {
                continue;
            }

            int number = Integer.parseInt(cell.getText().toString());
            if (!markedNumbers[number - 1]) {
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

        for (int row = 0; row < 8; row++) {
            boolean lineaCompleta = true;

            for (int col = 0; col < 8; col++) {
                int index = (col * 8) + row;
                TextView cell = (TextView) bingoGrid.getChildAt(index);

                if (cell.getText().toString().isEmpty()) {
                    continue;
                }

                int number = Integer.parseInt(cell.getText().toString());

                if (!markedNumbers[number - 1]) {
                    lineaCompleta = false;
                    break;
                }
            }

            if (lineaCompleta) {
                lineaCantada = true;
                Toast.makeText(this, "¡Línea completada!", Toast.LENGTH_SHORT).show();

                new Thread(() -> {
                    if (out != null) {
                        out.println("LINEA," + NOMBRE);
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


                            lineaCantada = true;
                            String[] aux = message.split(",");
                            String nombre = aux[1];
                            if (nombre.equals(NOMBRE)) {
                                playSound("linea");
                            }
                            else{
                                speakLinea(nombre);
                            }
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
                            speakNumber(numero);
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

    private void speakNumber(String number) {
        if (textToSpeech != null) {

            if(number.length()>1)
                textToSpeech.speak(number + ", "+ number.charAt(0)+ " " + number.charAt(1), TextToSpeech.QUEUE_FLUSH, null, null);
            else
                textToSpeech.speak(number, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void speakLinea(String nombre) {
        textToSpeech.speak(nombre + " ha cantado línea.", TextToSpeech.QUEUE_FLUSH, null, null);
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


    private List<List<Integer>> generateBingoNumbers() {
        List<List<Integer>> columns = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            List<Integer> column = new ArrayList<>();
            for (int j = i * 10 + 1; j <= i * 10 + 10; j++) {
                column.add(j);
            }
            Collections.shuffle(column);
            List<Integer> selected = column.subList(0, 4);
            while (selected.size() < 8) {
                selected.add(null);
            }
            Collections.shuffle(selected);
            columns.add(selected);
        }

        return columns;
    }




    private void markNumber(int columnIndex, int rowIndex, TextView numberView) {
        if (numberView.getText().toString().isEmpty()) {
            Toast.makeText(this, "Esta celda está vacía.", Toast.LENGTH_SHORT).show();
            return;
        }

        String numberText = numberView.getText().toString();
        int number = Integer.parseInt(numberText);

        if (markedNumbers[number - 1]) {
            Toast.makeText(this, "Este número ya ha sido marcado.", Toast.LENGTH_SHORT).show();
        } else {
            markedNumbers[number - 1] = true;
            numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            numberView.setTextColor(getResources().getColor(android.R.color.white));
        }
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
