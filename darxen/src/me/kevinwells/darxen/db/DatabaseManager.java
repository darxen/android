package me.kevinwells.darxen.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
	
	/**
	 * Retrieves the instance of DatabaseManager
	 * 
	 * @return The single instance of DatabaseManager
	 */
	public static DatabaseManager getInstance() {
		assert(instance != null);
		return instance;
	}

	private static final int DATABASE_VERSION = 5;
	
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
				", XMin REAL NOT NULL" +
				", XMax REAL NOT NULL" +
				", YMin REAL NOT NULL" +
				", YMax REAL NOT NULL" +
				", PRIMARY KEY (Shapefile, Id)" +
				");";
	
	private DatabaseManager(Context context, String dbName) {
		super(context, dbName, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_SHAPEFILE_STATUS);
		db.execSQL(CREATE_TABLE_SHAPEFILE_OBJECTS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHAPEFILE_OBJECTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHAPEFILE_STATUS);
		
		onCreate(db);
	}

}
