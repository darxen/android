package me.kevinwells.darxen.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;

import me.kevinwells.darxen.R;

public class EnableLocationDialog extends DialogFragment {

	public static final String TAG = "EnableLocation";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
		.setTitle(R.string.location_services_title)
		.setMessage(R.string.location_services_message)
		.setPositiveButton(R.string.do_it, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(intent);
			}
		})
		.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getActivity().finish();
				dismiss();
			}
		}).create();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);

		getActivity().finish();
	}
}
