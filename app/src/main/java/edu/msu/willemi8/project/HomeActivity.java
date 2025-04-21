package edu.msu.willemi8.project;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Main screen shown after login.  Lets the user add new items, update existing
 * ones via barcode scan, and log out.  Two‑finger upward swipe also opens the
 * "Add New Item" dialog.
 */
public class HomeActivity extends AppCompatActivity {

    /** Logged‑in user's e‑mail (passed from MainActivity) */
    private String user;

    // Notification channel ID
    private static final String NOTIFICATION_CHANNEL_ID = "pantry_notifications";

    /* ──────────────────────────  swipe helpers ────────────────────────── */
    private float startY1 = -1, startY2 = -1;
    private boolean isTwoFingerSwipe = false;

    /* ──────────────────────────  barcode helpers ──────────────────────── */
    /** Reference to UPC/ID EditText in "New Item" dialog (so a scan can fill it) */
    private EditText currentIdField;
    /** true when scanner was opened to update an existing item */
    private boolean scanForExisting = false;
    /** Activity‑result launcher for ScannerActivity */
    private ActivityResultLauncher<Intent> barcodeLauncher;

    // --- global product catalog (UPC ➜ name) ---
    private final DatabaseReference catalogRef =
            FirebaseDatabase.getInstance().getReference("products");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /* ------------------------------------------------------------- */
        user = getIntent().getStringExtra("email");

        TextView banner = findViewById(R.id.displayUserName);
        banner.setText("Hi " + user.split("@")[0] + " 👋\nHere's what's in your pantry:");

        /* ---------------------- buttons ------------------------------ */
        findViewById(R.id.newItemButton).setOnClickListener(v -> showAddItemDialog());


        findViewById(R.id.existingItemButton).setOnClickListener(v -> {
            scanForExisting = true;
            barcodeLauncher.launch(new Intent(this, ScannerActivity.class));
        });

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            getSharedPreferences("login_prefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        /* ------------------- scanner result -------------------------- */
        barcodeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    String upc = result.getData().getStringExtra("UPC");
                    if (upc == null) return;

                    if (scanForExisting) {
                        scanForExisting = false;
                        handleExistingUpc(upc);
                    } else if (currentIdField != null) {
                        currentIdField.setText(upc);        // populate New‑Item dialog
                    }
                });

        loadItems();
        createNotificationChannel();
    }

    /* ───────────────────────────── dialogs ───────────────────────────── */

    /** Convenience – plain call from the + button (no prefills) */
    private void showAddItemDialog() {
        showAddItemDialog(null, null);
    }

    /**
     * Re‑usable dialog   (A) from + button,   (B) from scanner fallback
     * @param upcPrefill   null or the scanned UPC to lock in
     * @param namePrefill  null or a suggested product name (from catalog)
     */
    private void showAddItemDialog(@Nullable String upcPrefill,
                                   @Nullable String namePrefill) {

        @SuppressLint("InflateParams")
        View dialog = getLayoutInflater().inflate(R.layout.dialog_add_item, null);

        EditText inputId  = dialog.findViewById(R.id.inputId);
        EditText inputNm  = dialog.findViewById(R.id.inputName);
        EditText inputExp = dialog.findViewById(R.id.inputExpiration);

        // If we arrive from a scan, remember this EditText so the Scan button can fill it
        currentIdField = (upcPrefill == null) ? inputId : null;

        // Apply prefills (and lock UPC if present)
        if (upcPrefill != null) {
            inputId.setText(upcPrefill);
            inputId.setEnabled(false);
        }
        if (namePrefill != null) {
            inputNm.setText(namePrefill);
        }

        View scanBtn = dialog.findViewById(R.id.buttonScan);

        if (upcPrefill == null) {           // normal “Add New Item” flow
            scanBtn.setVisibility(View.VISIBLE);
            scanBtn.setOnClickListener(v -> {
                /* remember which EditText should receive the scanned UPC */
                currentIdField = inputId;
                barcodeLauncher.launch(new Intent(this, ScannerActivity.class));
            });
        } else {                            // we already have the UPC → hide button
            scanBtn.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setTitle("Add New Item")
                .setView(dialog)
                .setPositiveButton("Add", (d, w) -> {
                    currentIdField = null;                 // dialog is closing

                    String upc  = inputId.getText().toString().trim();
                    String name = inputNm.getText().toString().trim();
                    String exp  = inputExp.getText().toString().trim();

                    if (upc.isEmpty() || name.isEmpty() || exp.isEmpty()) {
                        Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!upc.matches("\\d+")) {
                        Toast.makeText(this, "UPC/ID must contain only digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!isFutureDate(exp)) return;

                    onAddItemManually(upc, name, exp);     // <‑‑ will also cache name in catalog
                })
                .setNegativeButton("Cancel", (d,w) -> currentIdField = null)
                .show();
    }

    /** Prompts the user for a new expiration date and updates Firebase. */
    private void promptForNewExpiration(DatabaseReference itemRef, FridgeItem item) {
        @SuppressLint("InflateParams")
        View v = getLayoutInflater().inflate(R.layout.dialog_update_exp, null);
        EditText expInput = v.findViewById(R.id.inputNewExpiration);

        new AlertDialog.Builder(this)
                .setTitle("Update \"" + item.name + "\"")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String newExp = expInput.getText().toString().trim();
                    if (!isFutureDate(newExp)) return;

                    itemRef.child("expirationDate").setValue(newExp)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Expiration updated", Toast.LENGTH_SHORT).show();
                                // Update the item object with the new expiration date
                                item.expirationDate = newExp;

                                // Check if notification should be sent
                                scheduleExpirationNotification(item);
                                loadItems();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ───────────────────────── firebase helpers ─────────────────────── */

    /** Adds a brand‑new item to Firebase. */
    private void onAddItemManually(String upc, String name, String exp) {
        String safeEmail = user.replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("items");

        FridgeItem item = new FridgeItem(upc, name, exp);

        ref.child(upc).setValue(item)
                .addOnSuccessListener(v -> {
                    catalogRef.child(upc).setValue(name);   // NEW ➊
                    Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show();
                    loadItems();
                    scheduleExpirationNotification(item);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** After scanning for “existing item”, look it up and prompt for new date. */
    private void handleExistingUpc(String upc) {
        String safeEmail = user.replace(".", "_");
        DatabaseReference itemRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("items").child(upc);

        itemRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                // Look in the global catalog for a remembered name
                catalogRef.child(upc).get().addOnSuccessListener(prodSnap -> {
                    String knownName = prodSnap.getValue(String.class);   // could be null
                    showAddItemDialog(upc, knownName);                    // 👈 opens dialog with prefills
                });
                return;
            }
            FridgeItem item = snap.getValue(FridgeItem.class);
            promptForNewExpiration(itemRef, item);
        });
    }

    /* ───────────────────────── utilities ────────────────────────────── */

    /** Returns true if yyyy‑MM‑dd string is today or future; else shows toast. */
    private boolean isFutureDate(String yyyymmdd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Date entered = sdf.parse(yyyymmdd);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (entered.before(today.getTime())) {
                Toast.makeText(this, "Expiration date cannot be in the past", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /** Pulls all items for this user and displays them. */
    private void loadItems() {
        String safeEmail = user.replace(".", "_");
        DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(safeEmail).child("items");

        ListView listView = findViewById(R.id.itemsList);

        itemsRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful())
            {
                Toast.makeText(this, "Failed to load items", Toast.LENGTH_SHORT).show();
                return;
            }
            List<FridgeItem> fridgeItems = new ArrayList<>();
            for (DataSnapshot snap : task.getResult().getChildren())
            {
                FridgeItem item = snap.getValue(FridgeItem.class);
                if (item != null) fridgeItems.add(item);
            }

            PantryAdapter adapter = new PantryAdapter(this, fridgeItems, user, this::loadItems, this::scheduleExpirationNotification);
            listView.setAdapter(adapter);
        });
    }

    /* ─────────────────────  swipe gesture override ──────────────────── */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int ptrs = event.getPointerCount();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (ptrs == 2) {
                    startY1 = event.getY(0);
                    startY2 = event.getY(1);
                    isTwoFingerSwipe = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTwoFingerSwipe && ptrs == 2) {
                    if (startY1 - event.getY(0) > 100 && startY2 - event.getY(1) > 100) {
                        isTwoFingerSwipe = false;
                        findViewById(R.id.newItemButton).performClick();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                startY1 = startY2 = -1;
                isTwoFingerSwipe = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    /* ────────────────────── notification helpers ───────────────────── */

    /** Creates the notification channel required for Android 8.0 (API level 26) and higher */
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Pantry Notifications";
            String description = "Notifications for expiring pantry items";
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.RED);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /** Checks if notifications should be sent for a newly added or updated item*/
    private void scheduleExpirationNotification(FridgeItem item) {
        try
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setLenient(false);
            Date expirationDate = dateFormat.parse(item.expirationDate);
            if (expirationDate == null)
            {
                return;
            }
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            Date todayDate = today.getTime();
            long daysDiff = TimeUnit.MILLISECONDS.toDays(expirationDate.getTime() - todayDate.getTime());
            if (daysDiff == 0)
            {
                sendExpiresNotification(item, 0);
            }
            else if (daysDiff > 0 && daysDiff <= 3)
            {
                sendExpiresNotification(item, daysDiff);
            }
        }
        catch (ParseException e)
        {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
        }
    }

    /** Actually sends the push notification */
    private void sendExpiresNotification(FridgeItem item, long daysRemaining) {
        try
        {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("email", user);
            int requestCode;
            try
            {
                requestCode = Integer.parseInt(item.id) + (int)(daysRemaining * 1000);
            }
            catch (NumberFormatException e)
            {
                requestCode = item.id.hashCode() + (int)(daysRemaining * 1000);
            }
            if (requestCode < 0)
            {
                requestCode = Math.abs(requestCode);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
            String title;
            String content;
            if (daysRemaining == 0)
            {
                title = "Item Expires Today!";
                content = item.name + " expires today!";
            }
            else
            {
                String daysText = daysRemaining == 1 ? "day" : "days";
                title = "Item Expiring Soon!";
                content = item.name + " will expire in " + daysRemaining + " " + daysText + "!";
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setVibrate(new long[]{0, 300, 100, 300})
                    .setDefaults(NotificationCompat.DEFAULT_ALL);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                    return;
                }
            }
            notificationManager.notify(requestCode, builder.build());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /** Requests notification permissions on device */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101)
        {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
