package me.kevinwells.darxen.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import me.kevinwells.darxen.C;
import me.kevinwells.darxen.LatLon;
import me.kevinwells.darxen.Point2D;
import me.kevinwells.darxen.ShapefileObjectPartRenderData;
import me.kevinwells.darxen.ShapefileObjectRenderData;
import me.kevinwells.darxen.ShapefileRenderData;
import me.kevinwells.darxen.db.DbIterator;
import me.kevinwells.darxen.db.ShapefileObjectsAdapter;
import me.kevinwells.darxen.model.ShapefileConfig;
import me.kevinwells.darxen.model.ShapefileObjectBounds;
import me.kevinwells.darxen.shp.Shapefile;
import me.kevinwells.darxen.shp.ShapefileObject;
import me.kevinwells.darxen.shp.ShapefilePoint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

public class LoadShapefile extends CachedAsyncLoader<ShapefileRenderData> {
	
	private LatLon mCenter;
	private LatLon mViewpoint;
	
	private ShapefileConfig mConfig;
	private ShapefileRenderData mData;
	
	private ShapefileObjectBounds mPrevBounds;
	
	private static final String ARG_CENTER = "Center";
	private static final String ARG_CONFIG = "Config";
	private static final String ARG_DATA = "Data";
	
	private static final double DISPLAY_RADIUS = 4.0;
	
	public static Bundle bundleArgs(LatLon center, ShapefileConfig config, ShapefileRenderData data) {
		Bundle args = new Bundle();
    	args.putParcelable(ARG_CENTER, center);
    	args.putParcelable(ARG_CONFIG, config);
    	args.putParcelable(ARG_DATA, data);
    	return args;
	}
	
	public static LoadShapefile createInstance(Context context, Bundle args) {
		LatLon center = args.getParcelable(ARG_CENTER);
		ShapefileConfig config = args.getParcelable(ARG_CONFIG);
		ShapefileRenderData data = args.getParcelable(ARG_DATA);
		return new LoadShapefile(context, center, config, data);
	}
	
	public static LoadShapefile getInstance(LoaderManager manager, int id) {
		Loader<ShapefileRenderData> res = manager.getLoader(id);
		return (LoadShapefile)res;
	}
	
	public static LoadShapefile castInstance(Loader<ShapefileRenderData> loader) {
		return (LoadShapefile)loader;
	}

	private LoadShapefile(Context context, LatLon center, ShapefileConfig config, ShapefileRenderData data) {
		super(context);
		mCenter = center;
		mConfig = config;
		mData = data;
		
		mViewpoint = mCenter;
	}
	
	private synchronized LatLon getViewpoint() {
		return mViewpoint;
	}
	
	public synchronized void setViewPoint(LatLon point) {
		mViewpoint = point;
	}
	
	@Override
	protected ShapefileRenderData doInBackground() {
		//mData = mData.clone();
		
		//ensure we have cached bounding information for this shapefile
		buildCache();

		LatLon viewpoint = getViewpoint();

		ShapefileObjectBounds bounds;
		bounds = new ShapefileObjectBounds(
				viewpoint.lon - DISPLAY_RADIUS, viewpoint.lon + DISPLAY_RADIUS,
				viewpoint.lat - DISPLAY_RADIUS, viewpoint.lat + DISPLAY_RADIUS);
		
		Resources resources = getContext().getResources();
		
		InputStream fShp = resources.openRawResource(mConfig.resShp);
		InputStream fShx = resources.openRawResource(mConfig.resShx);
		
		Shapefile shapefile = new Shapefile(fShp, fShx);
		ShapefileObjectsAdapter adapter = new ShapefileObjectsAdapter();
		adapter.open();
		try {
			DbIterator<Integer> ids;
			if (mPrevBounds == null) {
				ids = adapter.getBoundedObjects(mConfig.id, bounds);
			} else {
				DbIterator<Integer> excludedIds = adapter.getExcludedObjects(mConfig.id, mPrevBounds, bounds);
				
				for (int id : excludedIds) {
					mData.remove(id);
				}
				excludedIds.close();
				
				ids = adapter.getIncludedObjects(mConfig.id, mPrevBounds, bounds);
			}
			
			for (int id : ids) {
				if (mData.contains(id))
					continue;
				
				ShapefileObject obj = shapefile.get(id);
				obj.load();
				mData.add(id, generateObject(obj));
			}
			ids.close();
			
			mPrevBounds = bounds;
			
		} finally {
			adapter.close();
			shapefile.close();
			
			try {
				fShp.close();
			} catch (IOException e) {}

			try {
				fShx.close();
			} catch (IOException e) {}
		}
		
		return mData;
	}
	
	private void buildCache() {
		ShapefileObjectsAdapter adapter = new ShapefileObjectsAdapter();
		adapter.open();
		try {
		
			if (adapter.hasCache(mConfig.id)) {
				return;
			}
			
			Log.i(C.TAG, "Building cache for shapefile " + mConfig.id);
			adapter.purgeCache(mConfig.id);
			
			Resources resources = getContext().getResources();
			
			InputStream fShp = resources.openRawResource(mConfig.resShp);
			InputStream fShx = resources.openRawResource(mConfig.resShx);
			
			Shapefile shapefile = new Shapefile(fShp, fShx);
			
			adapter.startTransaction();
			try {
				for (int i = 0; i < shapefile.getShapeCount(); i++) {
					ShapefileObject obj = shapefile.get(i);
					
					ShapefileObjectBounds bounds = new ShapefileObjectBounds(obj.dfXMin, obj.dfXMax, obj.dfYMin, obj.dfYMax);
					adapter.addBounds(mConfig.id, i, bounds);
					obj.close();
				}
				
				adapter.setIsCached(mConfig.id);
			
				adapter.commitTransaction();
			} finally {
				shapefile.close();
				
				try {
					fShp.close();
				} catch (IOException e) {}
	
				try {
					fShx.close();
				} catch (IOException e) {}
				
				adapter.finishTransaction();
			}
		} finally {
			adapter.close();
		}
	}
	
	private ShapefileObjectRenderData generateObject(ShapefileObject obj) {
		
		List<ShapefileObjectPartRenderData> parts = new ArrayList<ShapefileObjectPartRenderData>();

		if (obj.nParts == 0) {
			// single point
			parts.add(generateObjectPart(obj, 0, 1));
		} else {
			for (int j = 0; j < obj.nParts; j++) {
				int start = obj.panPartStart[j];
				int end = start;
				for (int k = start; k < obj.nVertices; k++) {
					end = k+1;
					if (j < obj.nParts-1 && k+1 == obj.panPartStart[j+1])
						break;
				}
				parts.add(generateObjectPart(obj, start, end));
			}
		}
		
		ShapefileObjectPartRenderData[] array = new ShapefileObjectPartRenderData[parts.size()];
		parts.toArray(array);
		return new ShapefileObjectRenderData(array);
	}
	
	private LatLon latLon = new LatLon();
	private ShapefilePoint shapePt = new ShapefilePoint();
	private Point2D p2 = new Point2D();
	private ShapefileObjectPartRenderData generateObjectPart(ShapefileObject obj, int start, int end) {
		int count = end-start;
		float array[] = new float[count * 2];
		int j = 0;
		
		for (int i = start; i < end; i++) {
			obj.getPoint(i, shapePt);
			latLon.lat = shapePt.y;
			latLon.lon = shapePt.x;
			p2 = latLon.project(mCenter, p2);
			array[j++] = (float)p2.x;
			array[j++] = (float)p2.y;
		}
		
		return new ShapefileObjectPartRenderData(count, array);	
	}
}
