package me.kevinwells.darxen.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Provide generic functionality to all database adapters */
public abstract class TableAdapter {

	private SQLiteOpenHelper mDbManager;
	
	private static Object mRefCountSync = new Object();
	private static RefCount mRefCount;

	/** Shared db connection */
	protected SQLiteDatabase db;

	/**
	 * Creates a new TableAdapter
	 */
	public TableAdapter() {
		mDbManager = DatabaseManager.getInstance();
		
		synchronized (mRefCountSync) {
			if (mRefCount == null) {
				mRefCount = new RefCount(mDbManager);
			}
		}
	}

	/**
	 * Creates a new TableAdapter with an existing database connection
	 * 
	 * @param db The database connection to use
	 */
	public TableAdapter(SQLiteDatabase db) {
		this.db = db;
		mRefCount.ref();
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (db != null)
			throw new RuntimeException("TableAdapter was not closed");
		super.finalize();
	}

	/**
	 * Open a writable version of the database
	 */
	public void open() {
		assert(db == null);
		db = mRefCount.ref();
	}

	/**
	 * Close an open database connection.
	 * 
	 * TableAdapters created with an existing connection should not close the
	 * connection to the database.
	 */
	public void close() {
		assert(db != null);
		db = null;
		
		mRefCount.deref();
	}
	
	public void startTransaction() {
		db.beginTransaction();
	}
	
	public void commitTransaction() {
		db.setTransactionSuccessful();
	}
	
	public void finishTransaction() {
		db.endTransaction();
	}

	/**
	 * Convert the result of a single cell into a String
	 * 
	 * @param cursor An active cursor, with a single column and row
	 * @return the String contained in the only cell
	 */
	protected String getString(Cursor cursor) {
		assert (cursor.getColumnCount() == 1);
		assert (cursor.getCount() == 1);
		cursor.moveToFirst();
		return cursor.getString(0);
	}

	/**
	 * Retrieve a boolean value from the database
	 * 
	 * @param cursor The cursor to read from, moved to the appropriate row
	 * @param column The index of the column to read
	 * @return True if the value is 1, False if 0
	 */
	protected boolean getBoolean(Cursor cursor, int column) {
		int val = cursor.getInt(column);
		assert (val == 0 || val == 1);
		return val == 1;
	}
	
	private static class RefCount {
		
		private int mCount;
		private SQLiteOpenHelper mDbManager;
		
		public RefCount(SQLiteOpenHelper dbManager) {
			mCount = 0;
			mDbManager = dbManager;
		}
		
		public synchronized SQLiteDatabase ref() {
			mCount++;
			//Log.d(C.TAG, "Increment ref count to " + mCount);
			return mDbManager.getWritableDatabase();
		}
		
		public synchronized SQLiteDatabase deref() {
			mCount--;
			//Log.d(C.TAG, "Decrement ref count to " + mCount);
			assert (mCount >= 0);
			
			if (mCount == 0) {
				mDbManager.close();
			}
			return null;
		}
	}
}
