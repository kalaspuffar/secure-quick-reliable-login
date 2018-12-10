package org.ea.sqrl.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.ea.sqrl.database.IdentityContract.IdentityEntry;

import java.util.HashMap;
import java.util.Map;

/**
 * This little database handle makes it possible to create a new data store and save identities with
 * a specific name and update the current data as well as the name for the currently selected
 * identity.
 *
 * @author Daniel Persson
 */
public class IdentityDBHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + IdentityEntry.TABLE_NAME + " (" +
                    IdentityEntry._ID + " INTEGER PRIMARY KEY," +
                    IdentityEntry.COLUMN_NAME_NAME + " TEXT," +
                    IdentityEntry.COLUMN_NAME_DATA + " BLOB)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + IdentityEntry.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SQRLIdentities.db";

    public IdentityDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
         * Might need to look into upgrade and downgrade strategies if we change
         * this but probably not.
         */
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long newIdentity(byte[] data) {
        ContentValues values = new ContentValues();
        values.put(IdentityContract.IdentityEntry.COLUMN_NAME_DATA, data);
        return this.getWritableDatabase().insert(
                    IdentityEntry.TABLE_NAME,
                    null,
                    values
                );
    }

    public byte[] getIdentityData(long id) {
        Cursor cursor = this.getWritableDatabase().query(
                IdentityEntry.TABLE_NAME,
                new String[] {IdentityEntry.COLUMN_NAME_DATA},
                IdentityEntry._ID + " = " + id,
                null,
                null,
                null,
                null
        );

        byte[] returnVal = new byte[] {};
        if (cursor.moveToFirst()) {
            returnVal = cursor.getBlob(0);
        }
        cursor.close();
        return returnVal;
    }

    public Map<Long, String> getIdentitys() {
        Cursor cursor = this.getWritableDatabase().query(
                IdentityEntry.TABLE_NAME,
                new String[] {
                        IdentityEntry._ID,
                        IdentityEntry.COLUMN_NAME_NAME
                },
                null,
                null,
                null,
                null,
                null
        );

        @SuppressLint("UseSparseArrays") Map<Long, String> identities = new HashMap<>();
        while(cursor.moveToNext()) {
            Long id = cursor.getLong(0);
            String name = cursor.getString(1);
            identities.put(id, name != null ? name : "ID " + id);
        }
        cursor.close();
        return identities;
    }

    public void deleteIdentity(long id) {
        this.getWritableDatabase().delete(
                IdentityEntry.TABLE_NAME,
                IdentityEntry._ID + " = " + id,
                null
        );
    }

    public boolean checkUnique(String name) {
        for(Map.Entry<Long, String> entry : getIdentitys().entrySet()) {
            if(entry.getValue().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public void updateIdentityName(long id, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        String newName = name;
        int i = 1;
        while(!checkUnique(newName)) {
            newName = name + " (" + i + ")";
            i++;
        }

        ContentValues values = new ContentValues();
        values.put(IdentityEntry.COLUMN_NAME_NAME, newName);

        db.update(
                IdentityEntry.TABLE_NAME,
                values,
                IdentityEntry._ID + " = " + id,
                null);
    }

    public void updateIdentityData(long id, byte[] data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(IdentityEntry.COLUMN_NAME_DATA, data);

        db.update(
                IdentityEntry.TABLE_NAME,
                values,
                IdentityEntry._ID + " = " + id,
                null);
    }

    public boolean hasIdentities() {
        Map<Long, String> identities = getIdentitys();
        return identities.size() > 0;
    }

    public String getIdentityName(long currentId) {
        Map<Long, String> identities = this.getIdentitys();
        return identities.get(currentId);
    }
}
