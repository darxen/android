package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class RadarSite implements Parcelable {

	public String name;
	public LatLon center;
	
	public RadarSite(String name, LatLon center) {
		this.name = name;
		this.center = center;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(name);
		parcel.writeParcelable(center, flags);
	}

	public RadarSite(Parcel in) {
		name = in.readString();
		center = in.readParcelable(LatLon.class.getClassLoader());
	}
	
	public static final Parcelable.Creator<RadarSite> CREATOR =
			new Parcelable.Creator<RadarSite>() {
		@Override
		public RadarSite createFromParcel(Parcel source) {
			return new RadarSite(source);
		}

		@Override
		public RadarSite[] newArray(int size) {
			return new RadarSite[size];
		}
	};	
}
