package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class RadarSite implements Parcelable {
	
	private static final String STATIC_SITE = null;

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

	public static RadarSite Find(List<RadarSite> radarSites, LatLon position, double within) {
		if (STATIC_SITE != null) {
			for (int i = 0; i < radarSites.size(); i++)
				if (radarSites.get(i).mName.equals(STATIC_SITE))
					return radarSites.get(i);
		}

		double currentMinDistance = Double.MAX_VALUE;
		RadarSite result = null;

		for (int i = 0; i < radarSites.size(); i++) {
			RadarSite candidate = radarSites.get(i);
			double distance = position.distanceTo(candidate.mCenter);
			if (distance < currentMinDistance) {
				result = candidate;
				currentMinDistance = distance;
			}
		}
		
		if (currentMinDistance <= within) {
			return result;
		} else {
			return null;
		}
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
