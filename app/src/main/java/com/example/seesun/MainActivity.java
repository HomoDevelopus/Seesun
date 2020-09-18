package com.example.seesun;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button detectButton;
    private Bitmap croopBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detectButton = findViewById(R.id.btn_detect);

        detectButton.setOnClickListener(v->{
            final Handler handler = new Handler();

            new Thread(() -> {
                final List<Classifier.ReCognition> results = detector.recognizeImage(cropBitmap);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleResult(croopBitmap, results);     // ImageView에 영상 출력하기
                    }
                });
            }).start();

        });
    }
}