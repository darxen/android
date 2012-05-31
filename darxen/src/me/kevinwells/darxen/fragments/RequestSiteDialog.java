package me.kevinwells.darxen.fragments;

import java.util.List;

import me.kevinwells.darxen.R;
import me.kevinwells.darxen.RadarSite;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class RequestSiteDialog extends DialogFragment {
	
	public static final String TAG = "RequestSite";
	
	public interface RequestSiteDialogListener {
		public void onRequestSiteCancelled();
		public void onRequestSiteResult(RadarSite site);
	}
	
	private List<RadarSite> mSites;
	private String[] mDisplayNames;
	
	public RequestSiteDialog(List<RadarSite> sites) {
		mSites = sites;
		
		mDisplayNames = new String[sites.size()];
		for (int i = 0; i < mDisplayNames.length; i++) {
			mDisplayNames[i] = sites.get(i).toString();
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
		.setTitle(R.string.dialog_title_pick_site)
		.setItems(mDisplayNames, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onItemClicked(mSites.get(which));
			}
		})
		.create();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		
		RequestSiteDialogListener callbacks = getCallbacks();
		if (callbacks == null)
			return;
		
		callbacks.onRequestSiteCancelled();
	}

	private void onItemClicked(RadarSite site) {
		RequestSiteDialogListener callbacks = getCallbacks();
		if (callbacks == null)
			return;
		
		callbacks.onRequestSiteResult(site);
	}

	private RequestSiteDialogListener getCallbacks() {
		Activity activity = getActivity();
		if (activity == null)
			return null;
		
		try {
			return (RequestSiteDialogListener)activity;
		} catch (ClassCastException ex) {
			throw new RuntimeException("Activity must implement " + RequestSiteDialogListener.class.getName());
		}
	}
}
