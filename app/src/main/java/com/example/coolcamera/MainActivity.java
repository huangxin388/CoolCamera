package com.example.coolcamera;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static int CAMERA = 1;
    public static int VARIATION = 2;
    public static int JISAW = 3;
    public static int COLORCHANGE = 4;

    private ImageButton imgCamera;
    private ImageButton imgVariation;
    private ImageButton imgJigsaw;
    private ImageButton btnColorChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imgCamera = (ImageButton)findViewById(R.id.main_carema);
        imgVariation = (ImageButton)findViewById(R.id.main_variation);
        imgJigsaw = (ImageButton)findViewById(R.id.main_jigsaw);
        btnColorChange = (ImageButton)findViewById(R.id.main_color_change);

        imgCamera.setOnClickListener(this);
        imgVariation.setOnClickListener(this);
        imgJigsaw.setOnClickListener(this);
        btnColorChange.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, ContentActivity.class);
        switch (view.getId())
        {
            case R.id.main_carema:
                intent.putExtra("start_view",CAMERA);
                break;
            case R.id.main_variation:
                intent.putExtra("start_view",VARIATION);
                break;
            case R.id.main_jigsaw:
                intent.putExtra("start_view",JISAW);
                break;
            case R.id.main_color_change:
                intent.putExtra("start_view",COLORCHANGE);
                break;
            default:
        }
        startActivity(intent);
    }
}
