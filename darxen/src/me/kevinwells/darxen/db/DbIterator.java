package me.kevinwells.darxen.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.database.Cursor;

/**
 * An abstract iterator over database model objects
 *
 * @param <T> The model object represented by this iterator
 */
public abstract class DbIterator<T> implements Iterable<T>, Iterator<T> {

	private Cursor cursor;
	
	/**
	 * Create a new DbIterator
	 * 
	 * @param cursor The cursor to draw data from, initialized to the first record
	 */
	public DbIterator(Cursor cursor) {
		this.cursor = cursor;
	}
	
	/**
	 * Convert the selected row in the cursor to type T
	 * 
	 * @param cursor The cursor, moved to the appropriate row
	 * 
	 * @return An object that represents the data in the row
	 */
	protected abstract T convertRow(Cursor cursor);
	
	/**
	 * Determine if another object is contained in the cursor
	 * 
	 * @return True if another object is available
	 */
	@Override
	public boolean hasNext() {
		if (cursor == null)
			return false;
		
		return cursor.getPosition() < cursor.getCount() - 1;
	}

	@Override
	public T next() {
		cursor.moveToNext();
		T res = convertRow(cursor);
		return res;
	}
	
	public void close() {
		if (cursor == null) {
			return;
		}
		
		cursor.close();
		cursor = null;
	}
	
	/**
	 * Determines the number or records in the iterator
	 * 
	 * @return The number of objects that will be returned
	 */
	public int getCount() {
		return cursor.getCount();
	}
	
	/**
	 * Read the remaining items into a list
	 * 
	 * @return the list of all of the remaining objects
	 */
	public List<T> toList() {
		List<T> res = new ArrayList<T>();
		while (hasNext()) {
			res.add(next());
		}
		return res;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		//reset and use our existing cursor
		cursor.moveToPosition(-1);
		return this;
	}
}
