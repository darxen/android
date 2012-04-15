package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class ShapefileConfig implements Parcelable {
	public int resShp;
	public int resShx;
	public int resDbf;
	
	public float lineWidth;
	public Color color;
	
	public ShapefileConfig(int resShp, int resDbf, int resShx, float lineWidth,
			Color color) {
		this.resShp = resShp;
		this.resDbf = resDbf;
		this.resShx = resShx;
		this.lineWidth = lineWidth;
		this.color = color;
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
		
		dest.writeFloat(lineWidth);
		dest.writeParcelable(color, flags);
		
	}
	
}
