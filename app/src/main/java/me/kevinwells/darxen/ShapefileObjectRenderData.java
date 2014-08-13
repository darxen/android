package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class ShapefileObjectRenderData implements Parcelable {

	public ShapefileObjectPartRenderData[] mParts;
	
	public ShapefileObjectRenderData(ShapefileObjectPartRenderData[] parts) {
		mParts = parts;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelableArray(mParts, flags);
	}

	public ShapefileObjectRenderData(Parcel in) {
		in.readParcelableArray(ShapefileObjectPartRenderData[].class.getClassLoader());
	}

	public static final Parcelable.Creator<ShapefileObjectRenderData> CREATOR =
			new Parcelable.Creator<ShapefileObjectRenderData>() {
		@Override
		public ShapefileObjectRenderData createFromParcel(Parcel source) {
			return new ShapefileObjectRenderData(source);
		}

		@Override
		public ShapefileObjectRenderData[] newArray(int size) {
			return new ShapefileObjectRenderData[size];
		}
	};
}
