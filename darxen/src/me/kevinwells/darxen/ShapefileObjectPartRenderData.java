package me.kevinwells.darxen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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

		ByteBuffer vbb = ByteBuffer.allocateDirect(array.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mBuffer = vbb.asFloatBuffer();
		
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
		
		int size = mBuffer.capacity();
		dest.writeInt(size);
		for (int i = 0; i < size; i++) {
			dest.writeFloat(mBuffer.get(i));
		}
	}

	public ShapefileObjectPartRenderData(Parcel in) {
		mCount = in.readInt();
		
		int size = in.readInt();
		ByteBuffer vbb = ByteBuffer.allocateDirect(size);
		vbb.order(ByteOrder.nativeOrder());
		mBuffer = vbb.asFloatBuffer();
		for (int i = 0; i < size; i++) {
			mBuffer.put(in.readFloat());
		}
		mBuffer.position(0);
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
