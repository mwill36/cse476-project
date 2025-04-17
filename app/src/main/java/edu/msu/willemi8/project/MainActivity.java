package edu.msu.willemi8.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Login / Register screen.
 * Persists successful logins in SharedPreferences so the user
 * is taken straight to HomeActivity on the next app launch.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS  = "login_prefs";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_PASSWORD = "password";

    private EditText emailInput, pwdInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1.  If we already have stored credentials, skip login UI
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedEmail = prefs.getString(KEY_EMAIL, null);
        String savedPass  = prefs.getString(KEY_PASSWORD, null);

        if (savedEmail != null && savedPass != null) {
            autoLogin(savedEmail, savedPass);
            return;           // don’t inflate the login layout
        }

        // 2.  Show login screen
        setContentView(R.layout.activity_main);

        emailInput = findViewById(R.id.editTextEmail);
        pwdInput   = findViewById(R.id.editTextPassword);
        Button loginBtn = findViewById(R.id.buttonLogin);

        loginBtn.setOnClickListener(v -> handleLogin());
    }

    /** Auto‑navigates to HomeActivity with stored credentials. */
    private void autoLogin(String email, String password) {
        Toast.makeText(this, "Welcome back, " + email, Toast.LENGTH_SHORT).show();
        goToHome(email);
    }

    /** Handles the login / registration button. */
    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String pass  = pwdInput.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter both e‑mail and password", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        String safeEmail = email.replace(".", "_");

        ref.child(safeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                if (snap.exists()) {
                    String storedPass = snap.child("password").getValue(String.class);
                    if (pass.equals(storedPass)) {
                        saveCredentials(email, pass);
                        Toast.makeText(MainActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        goToHome(email);
                    } else {
                        Toast.makeText(MainActivity.this, "Incorrect password.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Register new user
                    snap.getRef().child("password").setValue(pass)
                            .addOnSuccessListener(a -> {
                                saveCredentials(email, pass);
                                Toast.makeText(MainActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                                goToHome(email);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
            @Override public void onCancelled(DatabaseError err) {
                Toast.makeText(MainActivity.this, "DB error: " + err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Persist e‑mail & password locally (simple demo; encrypt in production). */
    private void saveCredentials(String email, String pass) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, pass)
                .apply();
    }

    private void goToHome(String email) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();   // prevent returning to login with back button
    }
}
