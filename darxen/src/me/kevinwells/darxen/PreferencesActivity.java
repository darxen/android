package me.kevinwells.darxen;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PreferencesActivity extends SherlockPreferenceActivity {
	
	public static Intent createIntent(Context context) {
		return new Intent(context, PreferencesActivity.class);
	}
	
	private ListPreference prefInitialFrames;
	private ListPreference prefMaximumFrames;

	//private CheckBoxPreference prefShapefileStates;
	private CheckBoxPreference prefShapefileCounties;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName(Prefs.PREFS_NAME);
		getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);
		
		addPreferencesFromResource(R.xml.preferences);
		
		prefInitialFrames = (ListPreference)findPreference("InitialFrames");
		prefMaximumFrames = (ListPreference)findPreference("MaximumFrames");
		
		//prefShapefileStates = (CheckBoxPreference)findPreference("ShapefileStates");
		prefShapefileCounties = (CheckBoxPreference)findPreference("ShapefileCounties");
		
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
		
		prefShapefileCounties.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean state = (Boolean)newValue;
				
				//always allow disabling
				if (!state)
					return true;
				
				//froyo can't read resources >1MB, like county lines
				if (Build.VERSION.SDK_INT <= 8) {
					Toast.makeText(PreferencesActivity.this, R.string.unsupported_shapefile, Toast.LENGTH_SHORT).show();
					return false;
				}

				return true;
			}
		});
		
	}
}
