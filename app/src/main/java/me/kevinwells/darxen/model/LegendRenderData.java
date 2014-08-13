package me.kevinwells.darxen.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.FloatBuffer;

public class LegendRenderData implements Parcelable {
	
	private Palette mPalette;
	private FloatBuffer mVertexBuffer;
	private FloatBuffer mColorBuffer;
	private int mCount;
	
	public LegendRenderData() {
		
	}

	public Palette getPalette() {
		return mPalette;
	}
	
	public void setPalette(Palette palette) {
		mPalette = palette;
	}
	
	public FloatBuffer getVertexBuffer() {
		return mVertexBuffer;
	}
	
	public FloatBuffer getColorBuffer() {
		return mColorBuffer;
	}
	
	public int getCount() {
		return mCount;
	}

	public synchronized void setBuffers(FloatBuffer vertexBuffer, FloatBuffer colorBuffer, int count) {
		mVertexBuffer = vertexBuffer;
		mColorBuffer = colorBuffer;
		mCount = count;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(mPalette, flags);
		
		dest.writeInt(mCount);
		Buffers.parcel(mVertexBuffer, dest);
		Buffers.parcel(mColorBuffer, dest);
	}
	
	public LegendRenderData(Parcel in) {
		mPalette = in.readParcelable(Palette.class.getClassLoader());
		
		mCount = in.readInt();
		mVertexBuffer = Buffers.unparcel(in);
		mColorBuffer = Buffers.unparcel(in);
	}
	
	public static final Parcelable.Creator<LegendRenderData> CREATOR = 
			new Parcelable.Creator<LegendRenderData>() {
		@Override
		public LegendRenderData createFromParcel(Parcel source) {
			return new LegendRenderData(source);
		}
		@Override
		public LegendRenderData[] newArray(int size) {
			return new LegendRenderData[size];
		}
	};
}
