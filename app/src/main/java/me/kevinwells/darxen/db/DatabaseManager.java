package me.kevinwells.darxen.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.kevinwells.darxen.C;

public class DatabaseManager extends SQLiteOpenHelper {
	
	private static DatabaseManager instance = null;
	private static final String DATABASE_NAME = "darxen.db";
	
	/**
	 * Create the single, static instance of DatabaseManager
	 * 
	 * @param context The context to use
	 * @return The newly created DatabaseManager
	 */
	public static DatabaseManager createInstance(Context context) {
		instance = new DatabaseManager(context.getApplicationContext(), DATABASE_NAME);
		return instance;
	}
	
	private Context mContext;
	
	/**
	 * Retrieves the instance of DatabaseManager
	 * 
	 * @return The single instance of DatabaseManager
	 */
	public static DatabaseManager getInstance() {
		assert(instance != null);
		return instance;
	}

	private static final int DATABASE_VERSION = 7;
	
	private static final String TABLE_SHAPEFILE_STATUS = "ShapefileStatus";
	private static final String CREATE_TABLE_SHAPEFILE_STATUS =
			"CREATE TABLE " + TABLE_SHAPEFILE_STATUS + " " +
				"( Shapefile INTEGER PRIMARY KEY" +
				", Cached INTEGER NOT NULL" +
				");";
	
	private static final String TABLE_SHAPEFILE_OBJECTS = "ShapefileObjects";
	private static final String CREATE_TABLE_SHAPEFILE_OBJECTS =
			"CREATE TABLE " + TABLE_SHAPEFILE_OBJECTS + " " +
				"( Shapefile INTEGER NOT NULL" +
				", Id INTEGER NOT NULL" +
				", XMin INTEGER NOT NULL" +
				", XMax INTEGER NOT NULL" +
				", YMin INTEGER NOT NULL" +
				", YMax INTEGER NOT NULL" +
				", PRIMARY KEY (Shapefile, Id)" +
				");";
	
	private DatabaseManager(Context context, String dbName) {
		super(context, dbName, null, DATABASE_VERSION);
		mContext = context;
	}
	
	@SuppressLint("SdCardPath") // Not a path to sdcard
	private static final String DB_PATH = "/data/data/me.kevinwells.darxen/databases/";
	private static final String DB_NAME = "darxen.db";
	
	private boolean dbExists() {
		//ensure that the file exists
		File f = new File(DB_PATH + DB_NAME);
		boolean exists = f.exists() && f.length() > 0;
		if (!exists)
			return false;
		
		//check the database version
		int version;
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
			Cursor cursor = db.rawQuery("PRAGMA user_version", null);
			cursor.moveToFirst();
			version = cursor.getInt(0);
			cursor.close();
			db.close();
		} catch (Exception ex) {
			Log.e(C.TAG, "Unable to query database version", ex);
			return false;
		}
		return version == DATABASE_VERSION;
	}

	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		if (!dbExists()) {
			try {
				Log.i(C.TAG, "Copying prepopulated database");
				File f = new File(DB_PATH);
				if (!f.exists())
					f.mkdir();
				
				InputStream fin = mContext.getAssets().open("darxen.db");
				OutputStream fout = new FileOutputStream(DB_PATH + DB_NAME);
				
				byte[] buffer = new byte[4096];
				for (int count = fin.read(buffer); count != -1; count = fin.read(buffer))
					fout.write(buffer, 0, count);
				
				fout.close();
				fin.close();
				
			} catch (IOException e) {
				Log.e(C.TAG, "Failed to copy prepopulated database", e);
			}
		}
		
		return super.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(C.TAG, "Creating a new database");
		db.execSQL(CREATE_TABLE_SHAPEFILE_STATUS);
		db.execSQL(CREATE_TABLE_SHAPEFILE_OBJECTS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(C.TAG, "Upgrading an existing database");
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHAPEFILE_OBJECTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHAPEFILE_STATUS);
		
		onCreate(db);
	}

}
