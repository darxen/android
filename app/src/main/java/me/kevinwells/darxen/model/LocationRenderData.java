package me.kevinwells.darxen.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.FloatBuffer;

import me.kevinwells.darxen.LatLon;
import me.kevinwells.darxen.Point2D;

public class LocationRenderData implements Parcelable {
	
	private Point2D mPosition;
	
	private FloatBuffer mCrosshairBuffer;
	private FloatBuffer mCircleBuffer;
	private int mCircleCount;

	public LocationRenderData() {
	}
	
	public synchronized void setPosition(Point2D position) {
		mPosition = position;
	}
	
	public Point2D getPosition() {
		return mPosition;
	}
	
	public FloatBuffer getCrosshairBuffer() {
		return mCrosshairBuffer;
	}

	public synchronized void setCrosshairBuffer(FloatBuffer crosshairBuffer) {
		mCrosshairBuffer = crosshairBuffer;
	}

	public FloatBuffer getCircleBuffer() {
		return mCircleBuffer;
	}

	public int getCircleCount() {
		return mCircleCount;
	}

	public synchronized void setCircleBuffer(FloatBuffer circleBuffer, int circleCount) {
		mCircleBuffer = circleBuffer;
		mCircleCount = circleCount;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(mPosition, flags);
		Buffers.parcel(mCrosshairBuffer, dest);
		Buffers.parcel(mCircleBuffer, dest);
		dest.writeInt(mCircleCount);
	}
	
	public LocationRenderData(Parcel in) {
		mPosition = in.readParcelable(LatLon.class.getClassLoader());
		mCrosshairBuffer = Buffers.unparcel(in);
		mCircleBuffer = Buffers.unparcel(in);
		mCircleCount = in.readInt();
	}

	public static final Parcelable.Creator<LocationRenderData> CREATOR = 
			new Parcelable.Creator<LocationRenderData>() {
		@Override
		public LocationRenderData createFromParcel(Parcel source) {
			return new LocationRenderData(source);
		}
		@Override
		public LocationRenderData[] newArray(int size) {
			return new LocationRenderData[size];
		}
	};
}
