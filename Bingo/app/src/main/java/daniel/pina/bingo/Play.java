package daniel.pina.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Play extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jugar_layout);

        Button solitarioButton = findViewById(R.id.solitario_button);
        solitarioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irSolitario();
            }
        });

        Button multijugadorButton = findViewById(R.id.multijugador_button);
        multijugadorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMultijugador();
            }
        });

        Button volverButton = findViewById(R.id.volver_button_play);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irMenu();
            }
        });

    }

    private void irMultijugador() {
        startActivity(new Intent(this, Multijugador.class));
        finish();
    }

    private void irSolitario() {
        startActivity(new Intent(this, Solitario.class));
        finish();
    }

    private void irMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
