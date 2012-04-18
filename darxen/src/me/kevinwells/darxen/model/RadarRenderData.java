package me.kevinwells.darxen.model;

import java.nio.FloatBuffer;

import android.os.Parcel;
import android.os.Parcelable;

public class RadarRenderData implements Parcelable {
	
	private Palette mPalette;

	private FloatBuffer[] mRadialBuffers;
	private int[] mRadialSize;
	
	public RadarRenderData() {
		mRadialBuffers = new FloatBuffer[16];
		mRadialSize = new int[16];
	}

	public Palette getPalette() {
		return mPalette;
	}
	
	public void setPalette(Palette palette) {
		mPalette = palette;
	}
	
	public FloatBuffer getVertexBuffer(int i) {
		return mRadialBuffers[i];
	}
	
	public int getCount(int i) {
		return mRadialSize[i];
	}

	public synchronized void setBuffers(FloatBuffer[] radialBuffers, int[] radialSizes) {
		assert(radialBuffers.length == 16);
		assert(radialSizes.length == 16);
		
		mRadialBuffers = radialBuffers;
		mRadialSize = radialSizes;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(mPalette, flags);
		
		for (int i = 0; i < 16; i++) {
			Buffers.parcel(mRadialBuffers[i], dest);
		}
		dest.writeIntArray(mRadialSize);
	}
	
	public RadarRenderData(Parcel in) {
		mPalette = in.readParcelable(Palette.class.getClassLoader());
		
		mRadialBuffers = new FloatBuffer[16];
		for (int i = 0; i < 16; i++) {
			mRadialBuffers[i] = Buffers.unparcel(in);
		}
		mRadialSize = in.createIntArray();
	}
	
	public static final Parcelable.Creator<RadarRenderData> CREATOR = 
			new Parcelable.Creator<RadarRenderData>() {
		@Override
		public RadarRenderData createFromParcel(Parcel source) {
			return new RadarRenderData(source);
		}
		@Override
		public RadarRenderData[] newArray(int size) {
			return new RadarRenderData[size];
		}
	};
}
