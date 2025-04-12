package edu.msu.willemi8.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {
    String user;

    private float startY1 = -1;
    private float startY2 = -1;
    private boolean isTwoFingerSwipe = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent intent = getIntent();
        user = intent.getStringExtra("email");
        TextView userView = findViewById(R.id.displayUserName);
        String username = user.split("@")[0];
        String welcomeMessage = "Hi " + username + " 👋\nHere's what’s in your pantry:";
        userView.setText(welcomeMessage);

        Button newItemButton = findViewById(R.id.newItemButton);
        newItemButton.setOnClickListener(v -> showAddItemDialog());

        loadItems();

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
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        sdf.setLenient(false);
                        Date enteredDate = sdf.parse(expiration);

                        Calendar todayCal = Calendar.getInstance();
                        todayCal.set(Calendar.HOUR_OF_DAY, 0);
                        todayCal.set(Calendar.MINUTE, 0);
                        todayCal.set(Calendar.SECOND, 0);
                        todayCal.set(Calendar.MILLISECOND, 0);
                        Date today = todayCal.getTime();

                        if (enteredDate.before(today)) {
                            Toast.makeText(this, "Expiration date cannot be in the past", Toast.LENGTH_SHORT).show();
                            return;
                        }

                    }
                    catch (ParseException e) {
                        Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show();
                        return;
                    }


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

    private void loadItems() {
        String safeEmail = user.replace(".", "_");
        DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(safeEmail)
                .child("items");

        TextView itemsView = findViewById(R.id.itemsView);

        itemsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                StringBuilder builder = new StringBuilder();

                for (DataSnapshot itemSnapshot : task.getResult().getChildren()) {
                    FridgeItem item = itemSnapshot.getValue(FridgeItem.class);
                    if (item != null) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            Date expiration = sdf.parse(item.expirationDate);

                            Calendar todayCal = Calendar.getInstance();
                            todayCal.set(Calendar.HOUR_OF_DAY, 0);
                            todayCal.set(Calendar.MINUTE, 0);
                            todayCal.set(Calendar.SECOND, 0);
                            todayCal.set(Calendar.MILLISECOND, 0);
                            Date today = todayCal.getTime();

                            Calendar expCal = Calendar.getInstance();
                            expCal.setTime(expiration);
                            expCal.set(Calendar.HOUR_OF_DAY, 0);
                            expCal.set(Calendar.MINUTE, 0);
                            expCal.set(Calendar.SECOND, 0);
                            expCal.set(Calendar.MILLISECOND, 0);
                            expiration = expCal.getTime();

                            long diffInMillis = expiration.getTime() - today.getTime();
                            long daysLeft = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                            builder.append("• ")
                                    .append(item.name)
                                    .append(" — Expires in ")
                                    .append(daysLeft)
                                    .append(" day").append(daysLeft != 1 ? "s" : "")
                                    .append(" (").append(item.expirationDate).append(")\n");

                        }
                        catch (Exception e) {
                            builder.append("• ")
                                    .append(item.name)
                                    .append(" (Exp: ")
                                    .append(item.expirationDate)
                                    .append(")\n");
                        }

                    }
                }

                itemsView.setText(builder.toString());
            } else {
                itemsView.setText("Failed to load items.");
            }
        });
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
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show();
                    loadItems(); // 👈 refresh the view
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        return true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 2) {
                    startY1 = event.getY(0);
                    startY2 = event.getY(1);
                    isTwoFingerSwipe = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isTwoFingerSwipe && pointerCount == 2) {
                    float endY1 = event.getY(0);
                    float endY2 = event.getY(1);

                    // Check for upward movement of both fingers
                    if ((startY1 - endY1 > 100) && (startY2 - endY2 > 100)) {
                        isTwoFingerSwipe = false; // prevent triggering multiple times

                        // Simulate button click
                        Button newItemButton = findViewById(R.id.newItemButton);
                        newItemButton.performClick();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                startY1 = -1;
                startY2 = -1;
                isTwoFingerSwipe = false;
                break;
        }

        return super.onTouchEvent(event);
    }


}
