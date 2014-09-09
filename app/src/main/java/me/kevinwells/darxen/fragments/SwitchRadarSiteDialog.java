package me.kevinwells.darxen.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import me.kevinwells.darxen.R;
import me.kevinwells.darxen.RadarSite;

public class SwitchRadarSiteDialog extends DialogFragment {

	public static final String TAG = "SwitchRadarSite";
	
	public interface SwitchRadarSiteDialogListener {
		public void onSwitchRadarSite(RadarSite site);
	}
	
	public static final String EXTRA_RADAR_SITE = "RadarSite";
	
	private RadarSite mRadarSite;

	public SwitchRadarSiteDialog applyArguments(RadarSite site) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(EXTRA_RADAR_SITE, site);
		setArguments(bundle);
		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mRadarSite = getArguments().getParcelable(EXTRA_RADAR_SITE);

		return new AlertDialog.Builder(getActivity())
		.setTitle(R.string.switch_radarsite_title)
		.setMessage(String.format(getString(R.string.switch_radarsite_message),
			mRadarSite.mName, mRadarSite.mCity, mRadarSite.mState))
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SwitchRadarSiteDialogListener callbacks = getCallbacks();
				if (callbacks != null) {
					callbacks.onSwitchRadarSite(mRadarSite);
				}
				dismiss();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		}).create();
	}

	private SwitchRadarSiteDialogListener getCallbacks() {
		FragmentActivity activity = getActivity();
		if (activity == null)
			return null;
		
		try {
			return (SwitchRadarSiteDialogListener)activity;
		} catch (ClassCastException ex) {
			throw new RuntimeException("Activity must implement " + SwitchRadarSiteDialogListener.class.getName());
		}
	}
}
