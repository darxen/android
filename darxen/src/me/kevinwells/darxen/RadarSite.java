package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class RadarSite implements Parcelable {

	public String mName;
	public LatLon mCenter;
	public String mState;
	public String mCity;
	
	public RadarSite(String name, LatLon center, String state, String city) {
		mName = name;
		mCenter = center;
		mState = state;
		mCity = city;
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s, %s)", mName, mCity, mState);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mName);
		parcel.writeParcelable(mCenter, flags);
		parcel.writeString(mState);
		parcel.writeString(mCity);
	}

	public RadarSite(Parcel in) {
		mName = in.readString();
		mCenter = in.readParcelable(LatLon.class.getClassLoader());
		mState = in.readString();
		mCity = in.readString();
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
