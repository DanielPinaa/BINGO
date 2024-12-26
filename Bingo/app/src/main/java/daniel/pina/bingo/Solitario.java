package daniel.pina.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Solitario extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.solitario_layout);

        Button volverButton = findViewById(R.id.volver_button_solit);
        volverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irPlay();
            }
        });
    }

    private void irPlay() {
        startActivity(new Intent(this, Play.class));
        finish();
    }
}
