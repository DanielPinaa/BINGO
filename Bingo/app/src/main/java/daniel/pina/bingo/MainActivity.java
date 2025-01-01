package daniel.pina.bingo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Button jugarButton = findViewById(R.id.jugar_button);
        jugarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irPlay();
            }
        });

        Button jugarSoloButton = findViewById(R.id.jugar_solitario_button);
        jugarSoloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irSolitario();
            }
        });
    }

    private void irSolitario() {
        startActivity(new Intent(this, Solitario.class));
        finish();
    }

    private void irPlay() {
        startActivity(new Intent(this, Multijugador.class));
        finish();
    }


}
