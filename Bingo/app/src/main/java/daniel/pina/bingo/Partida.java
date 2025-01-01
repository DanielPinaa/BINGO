package daniel.pina.bingo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class Partida extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private GridLayout bingoGrid;
    private List<Integer> bingoNumbers;
    private boolean[] markedNumbers;

    private Map<Integer,Boolean> listedNumbers;

    private boolean lineaCantada = false;

    private TextToSpeech textToSpeech;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String NOMBRE;

    private MediaPlayer mediaPlayer;

    private Map<Integer,Boolean>linea1 = new HashMap<>();
    private Map<Integer,Boolean>linea2 = new HashMap<>();
    private Map<Integer,Boolean>linea3 = new HashMap<>();
    private Map<Integer,Boolean>linea4 = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partida_layout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
        listedNumbers = new HashMap<>();
        List<List<Integer>> transposedNumbers = new ArrayList<>();
        for (int columna = 0; columna < 8; columna++) {
            List<Integer> currentColumn = new ArrayList<>();
            for (int fila = 0; fila < 4; fila++) {
                if (columna < orderedNumbers.get(fila).size()) {
                    currentColumn.add(orderedNumbers.get(fila).get(columna));
                } else {
                    currentColumn.add(null);
                }
            }
            transposedNumbers.add(currentColumn);
        }

        for (int columna = 0; columna < 8; columna++) {
            List<Integer> currentColumn = transposedNumbers.get(columna);
            for (int fila = 0; fila < 4; fila++) {
                TextView numberView = new TextView(this);
                numberView.setTextSize(32f);
                numberView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                numberView.setPadding(16, 16, 16, 16);
                numberView.setGravity(Gravity.CENTER);

                if (currentColumn.get(fila) != null) {
                    int number = currentColumn.get(fila);
                    numberView.setText(String.valueOf(number));

                    final int columnIndex = columna;
                    final int rowIndex = fila;
                    numberView.setOnClickListener(v -> markNumber(columnIndex, rowIndex, numberView));
                } else {
                    numberView.setText("");
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(fila);
                params.columnSpec = GridLayout.spec(columna);
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

        boolean lineaCompleta = true;

        for(Map.Entry<Integer,Boolean> entry : linea1.entrySet()){
            if(!entry.getValue()){
                lineaCompleta = false;
                break;
            }
        }
        if(!lineaCompleta) {
            lineaCompleta = true;
            for (Map.Entry<Integer, Boolean> entry : linea2.entrySet()) {
                if (!entry.getValue()) {
                    lineaCompleta = false;
                    break;
                }
            }
            if(!lineaCompleta) {
                lineaCompleta = true;
                for (Map.Entry<Integer, Boolean> entry : linea3.entrySet()) {
                    if (!entry.getValue()) {
                        lineaCompleta = false;
                        break;
                    }
                }
                if(!lineaCompleta) {
                    lineaCompleta = true;
                    for (Map.Entry<Integer, Boolean> entry : linea4.entrySet()) {
                        if (!entry.getValue()) {
                            lineaCompleta = false;
                            break;
                        }
                    }
                }
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
                            listedNumbers.put(Integer.parseInt(numero),true);
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
        List<List<Integer>> matrix = new ArrayList<>();

        Random random = new Random();

        Map<Integer,Boolean> numeros = new HashMap<>();


        for(int fila = 0; fila<4; fila++) {
            List<Integer> filaActual = new ArrayList<>();

            for(int col = 0; col<8; col++){


                int min = col * 10 + 1;
                int max = (col + 1) * 10;
                int numero =0;
                do {
                    numero = min + random.nextInt(max - min + 1);
                }while(numeros.containsKey(numero));
                numeros.put(numero,true);
                filaActual.add(col,numero);
                switch (fila){
                    case 0:
                        linea1.put(numero,false);
                        break;
                    case 1:
                        linea2.put(numero,false);
                        break;
                    case 2:
                        linea3.put(numero,false);
                        break;
                    case 3:
                        linea4.put(numero,false);
                }
            }
            matrix.add(fila,filaActual);
        }


        List<int[]>nulls = obtenerPosNull();
        for(int[]pos : nulls){
            int row = pos[0];
            int col = pos[1];
            switch (row){
                case 0:
                    linea1.remove(matrix.get(row).get(col));
                    break;
                case 1:
                    linea2.remove(matrix.get(row).get(col));
                    break;
                case 2:
                    linea3.remove(matrix.get(row).get(col));
                    break;
                case 3:
                    linea4.remove(matrix.get(row).get(col));
            }
            matrix.get(row).set(col,null);
        };

        return matrix;

    }

    public List<int[]> obtenerPosNull(){
        int rows = 4;
        int cols = 8;
        int[][] matrix = new int[rows][cols];

        int[] columnCount = new int[cols];

        List<int[]> indices = new ArrayList<>();

        Random random = new Random();

        for (int row = 0; row < rows; row++) {
            Set<Integer> selectedColumns = new HashSet<>();

            while (selectedColumns.size() < 3) {
                int col = random.nextInt(cols);
                if (selectedColumns.contains(col)) continue;

                if (columnCount[col] < 2) {
                    selectedColumns.add(col);
                    columnCount[col]++;
                    indices.add(new int[]{row, col});
                }
            }
        }
        return indices;
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
                boolean marcado = false;
                if(linea1.containsKey(number)){
                    marcado = true;
                    linea1.put(number,true);
                }
                if(!marcado && linea2.containsKey(number)){
                    marcado = true;
                    linea2.put(number,true);
                }
                if(!marcado && linea3.containsKey(number)){
                    marcado = true;
                    linea3.put(number,true);
                }
                if(!marcado && linea4.containsKey(number)){
                    marcado = true;
                    linea4.put(number,true);
                }
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
}
