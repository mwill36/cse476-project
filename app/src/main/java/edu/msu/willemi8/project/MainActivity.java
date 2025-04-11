package edu.msu.willemi8.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        EditText emailInput = findViewById(R.id.editTextEmail);
        EditText passwordInput = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.userExists(email, password)) {
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                goToHomeActivity();
            } else if (dbHelper.checkEmailExists(email)) {
                Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show();
            } else {
                if (dbHelper.insertUser(email, password)) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    goToHomeActivity();
                } else {
                    Toast.makeText(this, "Failed to create account.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void goToHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
    }


}