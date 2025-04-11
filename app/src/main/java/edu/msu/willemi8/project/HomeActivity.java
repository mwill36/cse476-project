package edu.msu.willemi8.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeActivity extends AppCompatActivity {
    String user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent intent = getIntent();
        user = intent.getStringExtra("email");
        TextView userView = findViewById(R.id.displayUserName);
        userView.setText(user);

        Button newItemButton = findViewById(R.id.newItemButton);
        newItemButton.setOnClickListener(v -> showAddItemDialog());
    }

    private void showAddItemDialog() {
        // Inflate the custom layout
        @SuppressLint("InflateParams")
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);

        final EditText inputId = dialogView.findViewById(R.id.inputId);
        final EditText inputName = dialogView.findViewById(R.id.inputName);
        final EditText inputExpiration = dialogView.findViewById(R.id.inputExpiration);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add New Item")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String idStr = inputId.getText().toString().trim();
                    String name = inputName.getText().toString().trim();
                    String expiration = inputExpiration.getText().toString().trim();

                    if (idStr.isEmpty() || name.isEmpty() || expiration.isEmpty()) {
                        Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int id;
                    try {
                        id = Integer.parseInt(idStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "ID must be a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean success = onAddItemManually(id, name, expiration);
                    if (success) {
                        Toast.makeText(this, "Item added!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    protected boolean onAddItemManually(int id, String name, String expirationDate) {
        String safeEmail = user.replace(".", "_");
        DatabaseReference userItemsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(safeEmail)
                .child("items");

        // Create a new item
        FridgeItem item = new FridgeItem(id, name, expirationDate);

        // Push item to Firebase under the user's items using the id as key
        userItemsRef.child(String.valueOf(id)).setValue(item)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        return true;
    }

}
