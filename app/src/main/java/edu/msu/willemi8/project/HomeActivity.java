package edu.msu.willemi8.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button openCameraBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        openCameraBtn = findViewById(R.id.openCameraBtn);

        openCameraBtn.setOnClickListener(v -> {
            // Launch ScannerActivity
            Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
            startActivity(intent);
        });
    }


    protected boolean onAddItemManually(int id, String name, String expirationDate){
        return false;
    }
}
