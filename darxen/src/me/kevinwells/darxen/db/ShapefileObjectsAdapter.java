package me.kevinwells.darxen.db;

import me.kevinwells.darxen.model.ShapefileId;
import me.kevinwells.darxen.model.ShapefileObjectBounds;
import android.content.ContentValues;
import android.database.Cursor;

public class ShapefileObjectsAdapter extends TableAdapter {

	public static final String TABLE_NAME = "ShapefileObjects";
	
	public static final String KEY_SHAPEFILE = "Shapefile";
	public static final String KEY_ID = "Id";
	
	public static final String KEY_X_MIN = "XMin";
	public static final String KEY_X_MAX = "XMax";
	public static final String KEY_Y_MIN = "YMin";
	public static final String KEY_Y_MAX = "YMax";
	
	public boolean hasCache(ShapefileId shapefile) {
		String projection[] = {KEY_ID};
		String args[] = {String.valueOf(shapefile.ordinal())};
		Cursor cursor;
		cursor = db.query(TABLE_NAME, projection, KEY_SHAPEFILE+"=?", args, null, null, null);
		return cursor.getCount() >= 1;
	}
	
	public void purgeCache(ShapefileId shapefile) {
		String args[] = {String.valueOf(shapefile.ordinal())};
		db.delete(TABLE_NAME, KEY_SHAPEFILE+"=?", args);
	}
	
	public void addBounds(ShapefileId shapefile, int index, ShapefileObjectBounds bounds) {
		ContentValues values = new ContentValues();
		values.put(KEY_SHAPEFILE, shapefile.ordinal());
		values.put(KEY_ID, index);
		
		values.put(KEY_X_MIN, bounds.xMin);
		values.put(KEY_X_MAX, bounds.xMax);
		values.put(KEY_Y_MIN, bounds.yMin);
		values.put(KEY_Y_MAX, bounds.yMax);
		
		db.insertOrThrow(TABLE_NAME, null, values);
	}
	
	public String s(ShapefileId shapefile) {
		return String.valueOf(shapefile.ordinal());
	}
	
	public String s(double f) {
		return String.valueOf(f);
	}
	
	public DbIterator<Integer> getBoundedObjects(ShapefileId shapefile, ShapefileObjectBounds bounds) {
		String projection[] = {KEY_ID};
		String where = KEY_SHAPEFILE+"=? AND " +
				"((XMin>=? AND XMin<=?) OR " +
				" (XMax>=? AND XMax<=?) OR " +
				" (?>=XMin AND ?<=XMax) OR " +
				" (?>=XMin AND ?<=XMax)) AND " +
				"((YMin>=? AND YMin<=?) OR " +
				" (YMax>=? AND YMax<=?) OR " +
				" (?>=YMin AND ?<=YMax) OR " +
				" (?>=YMin AND ?<=YMax)) ";
		String args[] = {s(shapefile),
				s(bounds.xMin), s(bounds.xMax),
				s(bounds.xMin), s(bounds.xMax),
				s(bounds.xMin), s(bounds.xMin),
				s(bounds.xMax), s(bounds.xMax),
				s(bounds.yMin), s(bounds.yMax),
				s(bounds.yMin), s(bounds.yMax),
				s(bounds.yMin), s(bounds.yMin),
				s(bounds.yMax), s(bounds.yMax)
				};
		
		Cursor cursor;
		cursor = db.query(TABLE_NAME, projection, where, args, null, null, KEY_ID);
		
		return new IntegerIterator(cursor);
	}
	
	public DbIterator<Integer> getExcludedObjects(ShapefileId shapefile, ShapefileObjectBounds oldBounds, ShapefileObjectBounds newBounds) {
		return findDifference(shapefile, oldBounds, newBounds);
	}
	
	public DbIterator<Integer> getIncludedObjects(ShapefileId shapefile, ShapefileObjectBounds oldBounds, ShapefileObjectBounds newBounds) {
		return findDifference(shapefile, newBounds, oldBounds);
	}

	private DbIterator<Integer> findDifference(ShapefileId shapefile, ShapefileObjectBounds bounds, ShapefileObjectBounds remove) {

		String query = 
				"SELECT "+KEY_ID+" FROM "+TABLE_NAME +
				" WHERE "+KEY_SHAPEFILE+"=? AND " +
				"((XMin>=? AND XMin<=?) OR " +
				" (XMax>=? AND XMax<=?) OR " +
				" (?>=XMin AND ?<=XMax) OR " +
				" (?>=XMin AND ?<=XMax)) AND " +
				"((YMin>=? AND YMin<=?) OR " +
				" (YMax>=? AND YMax<=?) OR " +
				" (?>=YMin AND ?<=YMax) OR " +
				" (?>=YMin AND ?<=YMax)) AND NOT (" +
				
				"((XMin>=? AND XMin<=?) OR " +
				" (XMax>=? AND XMax<=?) OR " +
				" (?>=XMin AND ?<=XMax) OR " +
				" (?>=XMin AND ?<=XMax)) AND " +
				"((YMin>=? AND YMin<=?) OR " +
				" (YMax>=? AND YMax<=?) OR " +
				" (?>=YMin AND ?<=YMax) OR " +
				" (?>=YMin AND ?<=YMax))) " +
				
				" ORDER BY " + KEY_ID;

		String args[] = {s(shapefile),
				s(bounds.xMin), s(bounds.xMax),
				s(bounds.xMin), s(bounds.xMax),
				s(bounds.xMin), s(bounds.xMin),
				s(bounds.xMax), s(bounds.xMax),
				s(bounds.yMin), s(bounds.yMax),
				s(bounds.yMin), s(bounds.yMax),
				s(bounds.yMin), s(bounds.yMin),
				s(bounds.yMax), s(bounds.yMax),
				
				s(remove.xMin), s(remove.xMax),
				s(remove.xMin), s(remove.xMax),
				s(remove.xMin), s(remove.xMin),
				s(remove.xMax), s(remove.xMax),
				s(remove.yMin), s(remove.yMax),
				s(remove.yMin), s(remove.yMax),
				s(remove.yMin), s(remove.yMin),
				s(remove.yMax), s(remove.yMax)
				};
	
		Cursor cursor;
		cursor = db.rawQuery(query, args);
		
		return new IntegerIterator(cursor);
	}
	
	private class IntegerIterator extends DbIterator<Integer> {
		public IntegerIterator(Cursor cursor) {
			super(cursor);
		}
		@Override
		protected Integer convertRow(Cursor cursor) {
			return cursor.getInt(0);
		}
	}
	
}
