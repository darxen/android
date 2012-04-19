package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class Point2D implements Parcelable {

	public double x;
	public double y;

	public Point2D() {
	}

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point2D add(double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public Point2D divide(double value) {
		this.x /= value;
		this.y /= value;
		return this;
	}

	public Point2D subtract(Point2D pt, Point2D res) {
		if (res == null)
			res = new Point2D();
		res.x = x - pt.x;
		res.y = y - pt.y;
		return res;
	}
	public Point2D subtract(Point2D pt) {
		return this.subtract(pt, this);
	}
	
	public double distanceTo(double x, double y) {
		return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeDouble(x);
		parcel.writeDouble(y);
	}

	public Point2D(Parcel in) {
		x = in.readDouble();
		y = in.readDouble();
	}
	
	public static final Parcelable.Creator<Point2D> CREATOR =
			new Parcelable.Creator<Point2D>() {
		@Override
		public Point2D createFromParcel(Parcel source) {
			return new Point2D(source);
		}

		@Override
		public Point2D[] newArray(int size) {
			return new Point2D[size];
		}
	};
}
