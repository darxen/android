package me.kevinwells.darxen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.os.Parcel;
import android.os.Parcelable;

public class ShapefileObjectPartRenderData implements Parcelable {

	//public int id;
	public int mCount;

	public float[] mArray;
	private FloatBuffer mBuffer;
	
	public ShapefileObjectPartRenderData(int count, float[] array) {
		mCount = count;
		mArray = array;
		
		generateBuffer();
	}
	
	public void generateBuffer() {
		if (mBuffer != null) {
			return;
			//throw new IllegalStateException("Buffer already populated");
		}

		ByteBuffer vbb = ByteBuffer.allocateDirect(mArray.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mBuffer = vbb.asFloatBuffer();
		
		for (int i = 0; i < mArray.length; i++) {
			mBuffer.put(mArray[i]);
		}
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
		//dest.writeInt(id);
		dest.writeInt(mCount);
		dest.writeFloatArray(mArray);
		// can't parcel buffer
	}

	public ShapefileObjectPartRenderData(Parcel in) {
		//id = in.readInt();
		mCount = in.readInt();
		mArray = in.createFloatArray();
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
