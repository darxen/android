package me.kevinwells.darxen;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	
	public static final String PREFS_NAME = "prefs";
	
	private static final String PREF_UPDATE_TIME = "UpdateTime";
	private static final String PREF_INITIAL_FRAMES = "InitialFrames";
	private static final String PREF_MAXIMUM_FRAMES = "MaximumFrames";
	private static final String PREF_FRAME_DELAY = "FrameDelay";
	
	public static int getInitialFrames() {
		return Integer.valueOf(getPrefs().getString(PREF_INITIAL_FRAMES, "5"));
	}
	
	public static int getMaximumFrames() {
		return Integer.valueOf(getPrefs().getString(PREF_MAXIMUM_FRAMES, "15"));
	}
	
	public static int getFrameDelay() {
		return Integer.valueOf(getPrefs().getString(PREF_FRAME_DELAY, "250"));
	}
	
	public static void unsetLastUpdateTime() {
		getPrefs().edit()
		.remove(PREF_UPDATE_TIME)
		.commit();
	}
	
	public static void setLastUpdateTime(Date time) {
		getPrefs().edit()
		.putLong(PREF_UPDATE_TIME, time.getTime())
		.commit();
	}
	
	public static Date getLastUpdateTime() {
		SharedPreferences prefs = getPrefs();
		if (!prefs.contains(PREF_UPDATE_TIME))
			return null;
		
		return new Date(getPrefs().getLong(PREF_UPDATE_TIME, 0));
	}
	
	private static SharedPreferences getPrefs() {
		return MyApplication.getInstance().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

}
