package me.kevinwells.darxen.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Palette implements Parcelable {

	private static Color[] REFLECTIVITY_PALETTE = new Color[] {
			new Color(0.25f, 0.25f, 0.25f, 200.0f / 255.0f),
			new Color(0.25f, 0.25f, 0.25f, 64.0f / 255.0f),
			new Color(0.25f, 0.25f, 0.25f, 132.0f / 255.0f),
			new Color(40.0f / 255.0f, 126.0f / 255.0f, 40.0f / 255.0f),
			new Color(60.0f / 255.0f, 160.0f / 255.0f, 20.0f / 255.0f),
			new Color(120.0f / 255.0f, 220.0f / 255.0f, 20.0f / 255.0f),
			new Color(250.0f / 255.0f, 250.0f / 255.0f, 20.0f / 255.0f),
			new Color(250.0f / 255.0f, 204.0f / 255.0f, 20.0f / 255.0f),
			new Color(250.0f / 255.0f, 153.0f / 255.0f, 20.0f / 255.0f),
			new Color(250.0f / 255.0f, 79.0f / 255.0f, 20.0f / 255.0f),
			new Color(250.0f / 255.0f, 0.0f / 255.0f, 20.0f / 255.0f),
			new Color(220.0f / 255.0f, 30.0f / 255.0f, 70.0f / 255.0f),
			new Color(200.0f / 255.0f, 30.0f / 255.0f, 100.0f / 255.0f),
			new Color(170.0f / 255.0f, 30.0f / 255.0f, 150.0f / 255.0f),
			new Color(255.0f / 255.0f, 0.0f / 255.0f, 156.0f / 255.0f),
			new Color(255.0f / 255.0f, 255.0f / 255.0f, 255.0f / 255.0f) };

	private static Color[] CLEANAIR_PALETTE = new Color[] {
			new Color(0.25f, 0.25f, 0.25f, 200.0f / 255.0f),
			new Color(0.25f, 0.25f, 0.25f, 120.0f / 255.0f),
			new Color(0.25f, 0.25f, 0.25f, 160.0f / 255.0f),
			new Color(0.25f, 0.25f, 0.25f, 200.0f / 255.0f),
			new Color(60.0f / 255.0f, 60.0f / 255.0f, 60.0f / 255.0f),
			new Color(70.0f / 255.0f, 70.0f / 255.0f, 70.0f / 255.0f),
			new Color(80.0f / 255.0f, 80.0f / 255.0f, 80.0f / 255.0f),
			new Color(90.0f / 255.0f, 90.0f / 255.0f, 90.0f / 255.0f),
			new Color(100.0f / 255.0f, 100.0f / 255.0f, 100.0f / 255.0f),
			new Color(20.0f / 255.0f, 70.0f / 255.0f, 20.0f / 255.0f),
			new Color(30.0f / 255.0f, 120.0f / 255.0f, 20.0f / 255.0f),
			new Color(30.0f / 255.0f, 155.0f / 255.0f, 20.0f / 255.0f),
			new Color(60.0f / 255.0f, 175.0f / 255.0f, 20.0f / 255.0f),
			new Color(80.0f / 255.0f, 200.0f / 255.0f, 20.0f / 255.0f),
			new Color(110.0f / 255.0f, 210.0f / 255.0f, 20.0f / 255.0f),
			new Color(240.0f / 255.0f, 240.0f / 255.0f, 20.0f / 255.0f) };
	
	private Color[] mColors;
	
	public Palette(PaletteType type) {
		switch (type) {
		case REFLECTIVITY_PRECIPITATION:
			mColors = REFLECTIVITY_PALETTE;
			break;
			
		case REFLECTIVITY_CLEAN_AIR:
			mColors = CLEANAIR_PALETTE;
			break;
		}
	}
	
	public Color get(int i) {
		return mColors[i];
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelableArray(mColors, flags);
	}
	
	public Palette(Parcel in) {
		mColors = in.createTypedArray(Color.CREATOR);
	}
	
	public static final Parcelable.Creator<Palette> CREATOR =
			new Parcelable.Creator<Palette>() {

		@Override
		public Palette createFromParcel(Parcel source) {
			return new Palette(source);
		}

		@Override
		public Palette[] newArray(int size) {
			return new Palette[size];
		}
	};
}
