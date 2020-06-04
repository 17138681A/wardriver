package com.example.wardriver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Pop extends Activity {
    TextView display;
    Button close;
    String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pop_window);

        //Configure size of pop up window
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = (int)(dm.widthPixels*0.8);
        int height = (int)(dm.heightPixels*0.7);

        getWindow().setLayout(width,height);

        display = findViewById(R.id.displayData);
        close = findViewById(R.id.close);

        //Show data on tv
        Intent intent = getIntent();
        data = intent.getStringExtra("data");
        display.setText(data);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
