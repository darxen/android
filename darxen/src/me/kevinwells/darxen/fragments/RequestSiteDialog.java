package me.kevinwells.darxen.fragments;

import java.util.List;

import me.kevinwells.darxen.ArrayUtil;
import me.kevinwells.darxen.R;
import me.kevinwells.darxen.RadarSite;

import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.app.DialogFragment;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class RequestSiteDialog extends DialogFragment {
	
	public static final String TAG = "RequestSite";
	
	private static final String KEY_RADAR_SITES = "RadarSites";
	
	public interface RequestSiteDialogListener {
		public void onRequestSiteCancelled();
		public void onRequestSiteResult(RadarSite site);
	}
	
	private RadarSite[] mSites;
	private String[] mDisplayNames;
	
	public static RequestSiteDialog create(List<RadarSite> sites) {
		RequestSiteDialog res = new RequestSiteDialog();
		Bundle args = new Bundle();
		
		RadarSite siteArray[] = new RadarSite[sites.size()];
		for (int i = 0; i < siteArray.length; i++) {
			siteArray[i] = sites.get(i);
		}

		args.putParcelableArray(KEY_RADAR_SITES, siteArray);
		res.setArguments(args);
		
		return res;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mSites = ArrayUtil.cast(getArguments().getParcelableArray(KEY_RADAR_SITES), RadarSite.CREATOR);
		
		mDisplayNames = new String[mSites.length];
		for (int i = 0; i < mDisplayNames.length; i++) {
			mDisplayNames[i] = mSites[i].toString();
		}
		
		return new AlertDialog.Builder(getActivity())
		.setTitle(R.string.dialog_title_pick_site)
		.setItems(mDisplayNames, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onItemClicked(mSites[which]);
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
		FragmentActivity activity = getActivity();
		if (activity == null)
			return null;
		
		try {
			return (RequestSiteDialogListener)activity;
		} catch (ClassCastException ex) {
			throw new RuntimeException("Activity must implement " + RequestSiteDialogListener.class.getName());
		}
	}
}
