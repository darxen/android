package me.kevinwells.darxen.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ShapefileConfig implements Parcelable {
	public int resShp;
	public int resShx;
	public int resDbf;
	public ShapefileId id;

	public ShapefileConfig(int resShp, int resDbf, int resShx, ShapefileId id) {
		this.resShp = resShp;
		this.resDbf = resDbf;
		this.resShx = resShx;
		this.id = id;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(resShp);
		dest.writeInt(resShx);
		dest.writeInt(resDbf);
		dest.writeInt(id.ordinal());
	}
	
	public ShapefileConfig(Parcel in) {
		resShp = in.readInt();
		resShx = in.readInt();
		resDbf = in.readInt();
		id = ShapefileId.fromOrdinal(in.readInt());
	}

	public static final Parcelable.Creator<ShapefileConfig> CREATOR =
			new Parcelable.Creator<ShapefileConfig>() {

		@Override
		public ShapefileConfig createFromParcel(Parcel source) {
			return new ShapefileConfig(source);
		}

		@Override
		public ShapefileConfig[] newArray(int size) {
			return new ShapefileConfig[size];
		}

	};

}
