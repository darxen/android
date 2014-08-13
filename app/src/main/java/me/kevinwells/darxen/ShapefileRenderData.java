package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShapefileRenderData implements Parcelable,
		Iterable<ShapefileObjectRenderData> {

	private Map<Integer, ShapefileObjectRenderData> mData;

	public ShapefileRenderData() {
		mData = new HashMap<Integer, ShapefileObjectRenderData>();
	}

	public synchronized void remove(int id) {
		if (mData.containsKey(id))
			mData.remove(id);
	}

	public boolean contains(int id) {
		return mData.containsKey(id);
	}
	
	public ShapefileObjectRenderData get(int id) {
		return mData.get(id);
	}

	public synchronized void add(int id, ShapefileObjectRenderData object) {
		mData.put(id, object);
	}
	
	@Override
	public ShapefileRenderData clone() {
		Parcel p = Parcel.obtain();
		writeToParcel(p, 0);
		return CREATOR.createFromParcel(p);
	}

	@Override
	public Iterator<ShapefileObjectRenderData> iterator() {
		return mData.values().iterator();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mData.size());
		for (int id : mData.keySet()) {
			dest.writeInt(id);
			dest.writeParcelable(mData.get(id), flags);
		}
	}

	public ShapefileRenderData(Parcel in) {
		int size = in.readInt();
		mData = new HashMap<Integer, ShapefileObjectRenderData>(size);
		for (int i = 0; i < size; i++) {
			int id = in.readInt();
			ShapefileObjectRenderData obj = in
					.readParcelable(ShapefileObjectRenderData.class
							.getClassLoader());
			mData.put(id, obj);
		}
	}

	public static final Parcelable.Creator<ShapefileRenderData> CREATOR =
			new Parcelable.Creator<ShapefileRenderData>() {
		@Override
		public ShapefileRenderData createFromParcel(Parcel source) {
			return new ShapefileRenderData(source);
		}

		@Override
		public ShapefileRenderData[] newArray(int size) {
			return new ShapefileRenderData[size];
		}
	};
}
