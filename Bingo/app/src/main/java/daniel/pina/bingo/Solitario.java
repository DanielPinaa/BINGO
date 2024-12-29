package daniel.pina.bingo;

import static daniel.pina.bingo.R.*;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class Solitario extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private GridLayout bingoGrid;
    private List<Integer> bingoNumbers;
    private boolean[] markedNumbers;

    private Map<Integer,Boolean> listedNumbers;

    private boolean lineaCantada = false;

    private TextToSpeech textToSpeech;

    private PrintWriter out;
    private BufferedReader in;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.solitario_layout);

        textToSpeech = new TextToSpeech(this, this);


        Button volverButton = findViewById(R.id.volver_solitario_button);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMenu();
            }
        });

        Button lineaButton = findViewById(R.id.linea_solitario_button);
        lineaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarLinea();
            }
        });

        Button bingoButton = findViewById(R.id.bingo_solitario_button);
        bingoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarBingo();
            }
        });


        bingoGrid = findViewById(R.id.bingoGrid);

        markedNumbers = new boolean[80];

        List<List<Integer>> orderedNumbers = generateBingoNumbers();
        listedNumbers = new HashMap<>();
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

        comenzarPartida();
    }

    public void comenzarPartida() {

        List<Integer> numerosBingo = generarNumerosBingo();

        new Thread(() -> {
            try {
                TextView numeros = null;
                Thread.sleep(2000);
                for (int numero : numerosBingo) {
                    numeros = findViewById(id.numeros_solitario_textView);
                    TextView finalNumeros = numeros;
                    runOnUiThread(() -> {
                        finalNumeros.setText("Número: " + numero);
                    });
                    speakNumber(""+numero);
                    listedNumbers.put(numero,true);
                    try {
                        Thread.sleep(7500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                numeros.setText("Fin de la partida");
            } catch (InterruptedException e) {
                System.err.println("Hilo interrumpido durante el envío de números: " + e.getMessage());
                Thread.currentThread().interrupt();
            }




        }).start();
    }

    private List<Integer> generarNumerosBingo() {
        List<Integer> numeros = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            numeros.add(i);
        }
        Collections.shuffle(numeros);
        return numeros;
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
                playSound("linea");
                return;
            }
        }

        Toast.makeText(this, "Aún no has completado ninguna línea.", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
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
        if(!listedNumbers.containsKey(number)){
            Toast.makeText(this, "Este número todavía no ha salido.", Toast.LENGTH_SHORT).show();
        }
        else{
            if (markedNumbers[number - 1]) {
                Toast.makeText(this, "Este número ya ha sido marcado.", Toast.LENGTH_SHORT).show();
            } else {
                markedNumbers[number - 1] = true;
                numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                numberView.setTextColor(getResources().getColor(android.R.color.white));
            }
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

    private void irMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
