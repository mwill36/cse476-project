package edu.msu.willemi8.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

//public class DatabaseHelper extends SQLiteOpenHelper {
//
//    // Database Name and Version
//    private static final String DATABASE_NAME = "FridgeDatabase.db";
//    private static final int DATABASE_VERSION = 1;
//
//    // Table Name
//    private static final String TABLE_ITEMS = "fridge_items";
//
//    // Column Names
//    private static final String COLUMN_ID = "id";
//    private static final String COLUMN_NAME = "name";
//    private static final String COLUMN_EXPIRATION = "expirationDate";
//
//    // Create Table Query
//    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_ITEMS + " (" +
//            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//            COLUMN_NAME + " TEXT, " +
//            COLUMN_EXPIRATION + " TEXT);";
//
//    public DatabaseHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(CREATE_TABLE);
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
//        onCreate(db);
//    }
//
//    // Insert an Item
//    public long insertItem(String name, String expirationDate) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(COLUMN_NAME, name);
//        values.put(COLUMN_EXPIRATION, expirationDate);
//
//        long id = db.insert(TABLE_ITEMS, null, values);
//        db.close();
//        return id;
//    }
//
//    // Get All Items
//    public List<FridgeItem> getAllItems() {
//        List<FridgeItem> itemList = new ArrayList<>();
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ITEMS + " ORDER BY " + COLUMN_EXPIRATION + " ASC", null);
//
//        if (cursor.moveToFirst()) {
//            do {
//                FridgeItem item = new FridgeItem(
//                        cursor.getInt(0),
//                        cursor.getString(1),
//                        cursor.getString(2)
//                );
//                itemList.add(item);
//            } while (cursor.moveToNext());
//        }
//
//        cursor.close();
//        db.close();
//        return itemList;
//    }
//
//    // Delete an Item
//    public void deleteItem(int id) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_ITEMS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
//        db.close();
//    }
//}


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "user_db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_USERS = "users";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_EMAIL + " TEXT PRIMARY KEY," +
                COL_PASSWORD + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean userExists(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                null,
                COL_EMAIL + "=? AND " + COL_PASSWORD + "=?",
                new String[]{email, password},
                null, null, null);

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean insertUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                null,
                COL_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
}
