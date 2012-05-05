package me.kevinwells.darxen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PreferencesActivity extends SherlockPreferenceActivity {
	
	public static Intent createIntent(Context context) {
		return new Intent(context, PreferencesActivity.class);
	}
	
	private ListPreference prefInitialFrames;
	private ListPreference prefMaximumFrames;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName(Prefs.PREFS_NAME);
		getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);
		
		addPreferencesFromResource(R.xml.preferences);
		
		prefInitialFrames = (ListPreference)findPreference("InitialFrames");
		prefMaximumFrames = (ListPreference)findPreference("MaximumFrames");
		
		prefInitialFrames.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int val = Integer.valueOf((String)newValue);
				
				return val <= Prefs.getMaximumFrames();
			}
		});
		
		prefMaximumFrames.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int val = Integer.valueOf((String)newValue);
				
				return val >= Prefs.getInitialFrames();
			}
		});
		
	}
}
