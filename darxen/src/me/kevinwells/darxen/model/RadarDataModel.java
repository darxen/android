package me.kevinwells.darxen.model;

import java.util.Map.Entry;
import java.util.TreeMap;

import me.kevinwells.darxen.compat.CompatTreeMap;
import me.kevinwells.darxen.data.DataFile;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

public class RadarDataModel implements Parcelable {
	
	public interface RadarDataModelListener {
		public void onUpdated();
		public void onCurrentChanged(long time);
	}

	private Handler mHandler = new Handler();
	
	private TreeMap<Long, DataFile> mFiles;
	private RadarDataModelListener mCallbacks;
	
	private long mCurrent;
	
	private int mDataLimit;
	
	public RadarDataModel() {
		mFiles = new CompatTreeMap<Long, DataFile>();
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
		
		//set the current frame
		if (mCurrent == 0) {
			mCurrent = time;
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mCallbacks != null) {
						mCallbacks.onCurrentChanged(mCurrent);	
					}
				}
			});
		}
	}
	
	public synchronized int getCount() {
		return mFiles.size();
	}
	
	public boolean isFull() {
		return mFiles.size() >= mDataLimit;
	}
	
	public synchronized DataFile getLatestData() {
		return mFiles.get(mFiles.lastKey());
	}
	
	public synchronized DataFile getData(long time) {
		return mFiles.get(time);
	}
	
	public synchronized boolean hasDataFile(long time) {
		return mFiles.containsKey(time);
	}

	private long getPrevious(long time) {
		Long key = mFiles.lowerKey(time);
		
		if (key == null)
			return mFiles.firstKey();
		
		return key;
	}

	private long getNext(long time) {
		Long key = mFiles.higherKey(time);
		
		if (key == null)
			return mFiles.lastKey();
		
		return key;
	}
	
	public synchronized boolean hasPrevious() {
		return getPrevious(mCurrent) != mCurrent;
	}
	
	public synchronized boolean hasNext() {
		return getNext(mCurrent) != mCurrent;
	}
	
	private void postOnCurrentChanged() {
		if (mCallbacks != null) {
			mCallbacks.onCurrentChanged(mCurrent);
		}
	}
	
	private void setCurrent(Long time) {
		if (time == null)
			return;
		
		if (time != mCurrent) {
			mCurrent = time;
			postOnCurrentChanged();
		}
	}

	public synchronized void moveFirst() {
		setCurrent(mFiles.firstKey());
	}
	
	public synchronized void moveLast() {
		setCurrent(mFiles.lastKey());
	}
	
	public synchronized void moveNext() {
		setCurrent(getNext(mCurrent));
	}
	
	public synchronized void movePrevious() {
		setCurrent(getPrevious(mCurrent));
	}
	
	private Runnable mAnimateTask = new Runnable() {
		
		@Override
		public void run() {
			if (hasNext()) {
				moveNext();
			} else {
				moveFirst();
			}
			
			if (hasNext()) {
				mHandler.postDelayed(this, 1000);
			} else {
				mHandler.postDelayed(this, 2000);
			}
		}
	};
	private boolean mAnimating;
	
	public synchronized void startAnimation() {
		mHandler.post(mAnimateTask);
		mAnimating = true;
	}
	
	public synchronized void stopAnimation() {
		mHandler.removeCallbacks(mAnimateTask);
		mAnimating = false;
	}
	
	public synchronized boolean isAnimating() {
		return mAnimating;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mCurrent);
		dest.writeInt(mDataLimit);
		
		int size = mFiles.size();
		dest.writeInt(size);
		for (Entry<Long, DataFile> entry : mFiles.entrySet()) {
			dest.writeLong(entry.getKey());
			dest.writeSerializable(entry.getValue());
		}
	}
	
	public RadarDataModel(Parcel in) {
		mCurrent = in.readLong();
		mDataLimit = in.readInt();
		
		int size = in.readInt();
		mFiles = new CompatTreeMap<Long, DataFile>();
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
