package me.kevinwells.darxen;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.LoaderManager;

import me.kevinwells.darxen.db.DatabaseManager;

public class MyApplication extends Application {
	
	private static MyApplication instance = null;
	
	public static MyApplication getInstance() {
		return instance;
	}
	
	public static final boolean DEBUG = false;
	private static final boolean PURGE_DB = true;

	public void purgeDB() {
		DatabaseManager manager = DatabaseManager.getInstance();
		manager.onUpgrade(manager.getWritableDatabase(), 0, 0);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onCreate() {
		instance = this;
		DatabaseManager.createInstance(getApplicationContext());

	     if (DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectNetwork()
					.detectDiskReads()
					.detectDiskWrites()
					.detectCustomSlowCalls()
					.penaltyLog()
					.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.detectLeakedClosableObjects()
					.build());

	         if (PURGE_DB) {
	             purgeDB();
	         }

	         LoaderManager.enableDebugLogging(true);
	     }

		super.onCreate();
	}
}
