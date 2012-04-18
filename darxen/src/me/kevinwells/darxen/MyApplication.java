package me.kevinwells.darxen;

import me.kevinwells.darxen.db.DatabaseManager;
import android.app.Application;

public class MyApplication extends Application {
	
	private static MyApplication instance = null;
	
	public static MyApplication getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		instance = this;
		DatabaseManager.createInstance(getApplicationContext());
	}
}
