package com.example.ilazar.mykeep.content.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.example.ilazar.mykeep.content.User;

public class KeepDatabase extends SQLiteOpenHelper {

  public static final String TABLE_USERS = "users";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_USERNAME = "username";
  public static final String COLUMN_PASSWORD = "password";
  public static final String COLUMN_TOKEN = "token";

  private static final String DATABASE_NAME = "keep.db";
  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_CREATE = "create table "
      + TABLE_USERS + "( " + COLUMN_ID
      + " integer primary key autoincrement, "
      + COLUMN_TOKEN + " text not null);";

  public KeepDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
    onCreate(db);
  }

  public void saveUser(User user) {
    ContentValues cv = new ContentValues();
    cv.put(COLUMN_TOKEN, user.getToken());
    SQLiteDatabase db = getWritableDatabase();
    db.insert(TABLE_USERS, null, cv);
    db.close();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  public User getCurrentUser() {
    Cursor c = getReadableDatabase().rawQuery("select token from " + TABLE_USERS, null, null);
    if (c.moveToFirst()) {
      return new User(null, null, c.getString(0));
    } else {
      return null;
    }
  }
}
