package me.kevinwells.darxen.model;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RenderModel {
	
	private RenderData mReadable;
	private RenderData mWritable;
	
	private Queue<RenderData> mReadables;
	private Queue<RenderData> mWritables;
	private Set<RenderData> mModels;
	
	public RenderModel() {
		mReadable = null;
		mWritable = new RenderData();
		
		mReadables = new ConcurrentLinkedQueue<RenderData>();
		mWritables = new ConcurrentLinkedQueue<RenderData>();
		mModels = new HashSet<RenderData>();
	}
	
	public RenderData getReadable() {
		RenderData readable;
		while ((readable = mReadables.poll()) != null) {
			
			//keep track of the number of models, freeing them as necessary
			mModels.add(readable);
			if (mReadable != null) {
				if (mModels.size() >= 6) {
					mModels.remove(mReadable);
				} else {
					mWritables.add(mReadable);
				}
			}
			
			mReadable = readable;
		}
		
		return mReadable;
	}
	
	public RenderData getWritable() {
		return mWritable;
	}
	
	public void clear() {
		mWritable = new RenderData();
		commit();
	}
	
	public void commit() {
		//get or create a data model
		RenderData writable = mWritables.poll();
		if (writable == null)
			writable = new RenderData();
		
		//copy readable into writable
		writable.copy(mWritable);
		
		//enqueue the model
		mReadables.add(writable);
	}
}
