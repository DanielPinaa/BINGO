package daniel.pina.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Fin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fin_layout);

        String nombreGanador = getIntent().getStringExtra("NOMBRE");

        TextView textView = findViewById(R.id.ganador_textView);
        textView.setText(String.format("¡%s ha hecho BINGO!", nombreGanador));

        Button volverButton = findViewById(R.id.volver_fin_button);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMultijugador();
            }
        });
    }

    private void irMultijugador() {
        startActivity(new Intent(this, Multijugador.class));
        finish();
    }
}
