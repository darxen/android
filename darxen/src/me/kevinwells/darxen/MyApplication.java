package me.kevinwells.darxen;

import me.kevinwells.darxen.db.DatabaseManager;
import android.app.Application;
import android.os.StrictMode;

public class MyApplication extends Application {
	
	private static MyApplication instance = null;
	
	public static MyApplication getInstance() {
		return instance;
	}
	
	private static final boolean DEBUG = false;

	@Override
	public void onCreate() {
	     if (DEBUG) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .penaltyDeath()
	                 .build());
	     }
		
		
		instance = this;
		DatabaseManager.createInstance(getApplicationContext());
		super.onCreate();
	}
}
