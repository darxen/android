package me.kevinwells.darxen.model;

import java.util.Map.Entry;
import java.util.TreeMap;

import me.kevinwells.darxen.data.DataFile;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

public class RadarDataModel implements Parcelable {
	
	public interface RadarDataModelListener {
		public void onUpdated();
	}

	private Handler mHandler = new Handler();
	
	private TreeMap<Long, DataFile> mFiles;
	private RadarDataModelListener mCallbacks;
	
	private int mDataLimit;
	
	public RadarDataModel() {
		mFiles = new TreeMap<Long, DataFile>();
		mDataLimit = 15;
	}
	
	public void setCallbacks(RadarDataModelListener callbacks) {
		mCallbacks = callbacks;
		
		if (mFiles.size() > 0)
			mCallbacks.onUpdated();
	}
	
	public int getDataLimit() {
		return mDataLimit;
	}
	
	public synchronized void addDataFile(long time, DataFile data) {
		//add the record
		mFiles.put(time, data);
		
		//limit number of entries
		while (mFiles.size() > mDataLimit) {
			mFiles.remove(mFiles.firstKey());
		}
		
		//notify UI of updates
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mCallbacks != null) {
					mCallbacks.onUpdated();	
				}
			}
		});
	}
	
	public boolean isFull() {
		return mFiles.size() >= mDataLimit;
	}
	
	public synchronized DataFile getLatestData() {
		return mFiles.get(mFiles.lastKey());
	}
	
	public synchronized boolean hasDataFile(long time) {
		return mFiles.containsKey(time);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mDataLimit);
		
		int size = mFiles.size();
		dest.writeInt(size);
		for (Entry<Long, DataFile> entry : mFiles.entrySet()) {
			dest.writeLong(entry.getKey());
			dest.writeSerializable(entry.getValue());
		}
	}
	
	public RadarDataModel(Parcel in) {
		mDataLimit = in.readInt();
		
		int size = in.readInt();
		mFiles = new TreeMap<Long, DataFile>();
		for (int i = 0; i < size; i++) {
			long key = in.readLong();
			DataFile value = (DataFile)in.readSerializable();
			mFiles.put(key, value);
		}
	}
	
	public static final Parcelable.Creator<RadarDataModel> CREATOR = new Parcelable.Creator<RadarDataModel>() {
		
		@Override
		public RadarDataModel[] newArray(int size) {
			return new RadarDataModel[size];
		}
		
		@Override
		public RadarDataModel createFromParcel(Parcel source) {
			return new RadarDataModel(source);
		}
	};
}
