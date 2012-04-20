package me.kevinwells.darxen.db;

import me.kevinwells.darxen.model.ShapefileId;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ShapefileStatusAdapter extends TableAdapter {

	public static final String TABLE_NAME = "ShapefileStatus";
	
	public static final String KEY_SHAPEFILE = "Shapefile";
	public static final String KEY_CACHED = "Cached";
	
	public ShapefileStatusAdapter(SQLiteDatabase db) {
		super(db);
	}
	
	public boolean hasCache(ShapefileId shapefile) {
		Cursor cursor = getCached(shapefile);
		if (cursor.getCount() == 0) {
			cursor.close();
			return false;
		}
		cursor.moveToFirst();
		boolean res = getBoolean(cursor, cursor.getColumnIndex(KEY_CACHED));
		cursor.close();
		return res;
	}
	
	public void setIsCached(ShapefileId shapefile, boolean cached) {
		Cursor cursor = getCached(shapefile);
		boolean insert = cursor.getCount() == 0;
		cursor.close();
		
		if (insert) {
			ContentValues values = new ContentValues();
			values.put(KEY_SHAPEFILE, shapefile.ordinal());
			values.put(KEY_CACHED, cached);
			db.insertOrThrow(TABLE_NAME, null, values);	
			
		} else {
			ContentValues values = new ContentValues();
			values.put(KEY_CACHED, cached);
			String[] args = new String[] {String.valueOf(shapefile.ordinal())};
			db.update(TABLE_NAME, values, KEY_SHAPEFILE+"=?", args);
		}
	}
	
	private Cursor getCached(ShapefileId shapefile) {
		String projection[] = {KEY_CACHED};
		String args[] = {String.valueOf(shapefile.ordinal())};
		return db.query(TABLE_NAME, projection, KEY_SHAPEFILE+"=?", args, null, null, null);
	}
}
