package edu.msu.willemi8.project;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PantryAdapter extends BaseAdapter {
    private final Context context;
    private final List<FridgeItem> items;
    private final String userEmail;
    private final Runnable onDataChanged;

    public PantryAdapter(Context context, List<FridgeItem> items, String userEmail, Runnable onDataChanged) {
        this.context = context;
        this.items = items;
        this.userEmail = userEmail;
        this.onDataChanged = onDataChanged;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FridgeItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FridgeItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_pantry, parent, false);
        }

        TextView itemInfo = convertView.findViewById(R.id.itemInfo);
        Button editButton = convertView.findViewById(R.id.editButton);
        ImageButton deleteButton = convertView.findViewById(R.id.deleteButton); // or ImageButton if you're using that


        String displayText = item.name;
        int textColor = android.graphics.Color.BLACK; // default color
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date exp = sdf.parse(item.expirationDate);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long days = TimeUnit.MILLISECONDS.toDays(exp.getTime() - today.getTimeInMillis());

            if (days <= 0) {
                displayText += " — Expired (" + item.expirationDate + ")";
                textColor = android.graphics.Color.RED;
            }
            else {
                displayText += " — Expires in " + days + " day" + (days != 1 ? "s" : "")
                        + " (" + item.expirationDate + ")";
            }
        }
        catch (Exception e) {
            displayText += " (Exp: " + item.expirationDate + ")";
        }

        itemInfo.setText(displayText);
        itemInfo.setTextColor(textColor);

        editButton.setText("Edit");

        editButton.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_item, null);
            EditText nameField = dialogView.findViewById(R.id.inputName);
            EditText expField = dialogView.findViewById(R.id.inputExpiration);
            EditText idField = dialogView.findViewById(R.id.inputId);
            Button scanButton = dialogView.findViewById(R.id.buttonScan);

            idField.setText(item.id);
            idField.setEnabled(false);
            nameField.setText(item.name);
            expField.setText(item.expirationDate);
            scanButton.setVisibility(View.GONE);

            new AlertDialog.Builder(context)
                    .setTitle("Edit " + item.name)
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = nameField.getText().toString().trim();
                        String newExp = expField.getText().toString().trim();

                        if (newName.isEmpty() || newExp.isEmpty()) {
                            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        item.name = newName;
                        item.expirationDate = newExp;

                        String safeEmail = userEmail.replace(".", "_");
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(safeEmail)
                                .child("items")
                                .child(item.id)
                                .setValue(item)
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(context, "Item updated", Toast.LENGTH_SHORT).show();
                                    onDataChanged.run();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Delete", (dialog, which) -> {
                        String safeEmail = userEmail.replace(".", "_");
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(safeEmail)
                                .child("items")
                                .child(item.id)
                                .removeValue()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(context, "Deleted " + item.name, Toast.LENGTH_SHORT).show();
                                    onDataChanged.run();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete " + item.name + "?")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String safeEmail = userEmail.replace(".", "_");
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(safeEmail)
                                .child("items")
                                .child(item.id)
                                .removeValue()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(context, "Deleted " + item.name, Toast.LENGTH_SHORT).show();
                                    onDataChanged.run(); // Refresh list
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });



        return convertView;
    }
}
