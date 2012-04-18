package me.kevinwells.darxen;

import java.nio.FloatBuffer;

import me.kevinwells.darxen.model.Buffers;
import android.os.Parcel;
import android.os.Parcelable;

public class ShapefileObjectPartRenderData implements Parcelable {

	public int mCount;

	private FloatBuffer mBuffer;
	
	public ShapefileObjectPartRenderData(int count, float[] array) {
		mCount = count;
		
		generateBuffer(array);
	}
	
	private void generateBuffer(float[] array) {
		if (mBuffer != null) {
			throw new IllegalStateException("Buffer already populated");
		}

		mBuffer = Buffers.allocateFloat(array.length);
		
		mBuffer.put(array);
		mBuffer.position(0);
	}

	public FloatBuffer getBuffer() {
		if (mBuffer == null)
			throw new IllegalStateException("Buffer not populated");
		return mBuffer;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mCount);
		
		Buffers.parcel(mBuffer, dest);
	}

	public ShapefileObjectPartRenderData(Parcel in) {
		mCount = in.readInt();
		
		mBuffer = Buffers.unparcel(in);
	}

	public static final Parcelable.Creator<ShapefileObjectPartRenderData> CREATOR =
			new Parcelable.Creator<ShapefileObjectPartRenderData>() {

		@Override
		public ShapefileObjectPartRenderData createFromParcel(Parcel source) {
			return new ShapefileObjectPartRenderData(source);
		}

		@Override
		public ShapefileObjectPartRenderData[] newArray(int size) {
			return new ShapefileObjectPartRenderData[size];
		}
	};
}
