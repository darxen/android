package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class Color implements Parcelable {
	
	public float r;
	public float g;
	public float b;
	public float a;
	
	public Color(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 1.0f;
	}
	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloat(r);
		dest.writeFloat(g);
		dest.writeFloat(b);
		dest.writeFloat(a);
	}
	
	public Color(Parcel in) {
		r = in.readFloat();
		g = in.readFloat();
		b = in.readFloat();
		a = in.readFloat();
	}
	
	public static final Parcelable.Creator<Color> CREATOR = 
			new Parcelable.Creator<Color>() {
		@Override
		public Color createFromParcel(Parcel source) {
			return new Color(source);
		}
		@Override
		public Color[] newArray(int size) {
			return new Color[size];
		}
	};
}
