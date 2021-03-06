package me.kevinwells.darxen;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.view.MotionEvent;

import me.kevinwells.darxen.data.RadialDataPacket;
import me.kevinwells.darxen.loaders.RenderLegend;
import me.kevinwells.darxen.loaders.RenderLocation;
import me.kevinwells.darxen.loaders.RenderRadar;
import me.kevinwells.darxen.model.LegendRenderData;
import me.kevinwells.darxen.model.LocationRenderData;
import me.kevinwells.darxen.model.RadarData;
import me.kevinwells.darxen.model.RadarDataModel;
import me.kevinwells.darxen.model.RadarRenderData;
import me.kevinwells.darxen.model.RenderData;
import me.kevinwells.darxen.model.RenderModel;
import me.kevinwells.darxen.renderables.LegendRenderable;
import me.kevinwells.darxen.renderables.LocationRenderable;
import me.kevinwells.darxen.renderables.RadarRenderable;

public class RadarView extends GLSurfaceView implements GestureSurface {

	private LoaderManager mLoaderManager;
	private MapTapListener mMapListener;

	private RadarDataModel mRadarData;
	private RadarData mCurrentData;
	
	private GestureRecognizer mRecognizer = new GestureRecognizer(this);

	private RadarRenderer mRenderer;
	private RenderModel mModel;
	
	private boolean mHasTransform = false;
	private float[] mTransform = new float[32];
	private static final int OFFSET_CURRENT = 0;
	private static final int OFFSET_TEMP = 16;
	
	private ViewpointListener mViewpointListener;
	
	private static final int TASK_RENDER_RADAR = 100;
	private static final int TASK_RENDER_LEGEND = 101;
	private static final int TASK_RENDER_LOCATION = 102;
	private static final int TASK_PRERENDER_RADAR_START = 110;
	
	private int mPrerenderCount = 0;

	public RadarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mModel = new RenderModel();
		
		Matrix.setIdentityM(mTransform, OFFSET_CURRENT);
		scale(1.0f/230.0f);
		updateTransform();
		mModel.commit();
		mHasTransform = false;
		
		mRenderer = new RadarRenderer(mModel);
		
		//setEGLContextClientVersion(2);
		setRenderer(mRenderer);
	}
	
	public void setLoaderManager(LoaderManager loaderManager) {
		mLoaderManager = loaderManager;
	}
	
	public void setMapTapCallbacks(MapTapListener listener) {
		mMapListener = listener;
	}
	
	public RenderModel getModel() {
		return mModel;
	}
	
	private RenderData getData() {
		return mModel.getWritable();
	}

	public void setRadarModel(RadarDataModel radarData) {
		if (mRadarData != null) {
			mRadarData.removeCallbacks(mRadarModelListener);
		}
		
		mRadarData = radarData;
		
		if (mRadarData != null) {
			mRadarData.addCallbacks(mRadarModelListener);
		}
		updateCurrentFrame();
	}

	public void setLocation(LatLon pos) {
		updateLocation(pos);
	}
	
	private void updateLocation(LatLon pos) {
		if (getData().mCenter == null || pos == null) {
			loadLocation(null);
			
		} else {
			Point2D p = null;
			p = pos.project(getData().mCenter, null);
			loadLocation(p);
		}
	}

	public void setViewpointListener(ViewpointListener listener) {
		mViewpointListener = listener;
	}

	private static final String STATE_TRANSFORM = "Transform";
	private static final String STATE_PARENT = "Parent";
	
	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle icicle = new Bundle();
		
		icicle.putFloatArray(STATE_TRANSFORM, mTransform);
		icicle.putParcelable(STATE_PARENT, super.onSaveInstanceState());
		
		return icicle;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle icicle = (Bundle)state;
		
		mTransform = icicle.getFloatArray(STATE_TRANSFORM);
		updateTransform();
		
		super.onRestoreInstanceState(icicle.getParcelable(STATE_PARENT));
	}
	
	@Override
	public void scale(float factor) {
		Matrix.setIdentityM(mTransform, OFFSET_TEMP);
		Matrix.scaleM(mTransform, OFFSET_TEMP, factor, factor, 1.0f);
		Matrix.multiplyMM(mTransform, OFFSET_CURRENT, mTransform, OFFSET_TEMP, mTransform, OFFSET_CURRENT);
	}
	
	@Override
	public void translate(float dx, float dy) {
		Matrix.setIdentityM(mTransform, OFFSET_TEMP);
		Matrix.translateM(mTransform, OFFSET_TEMP, dx, dy, 0.0f);
		Matrix.multiplyMM(mTransform, OFFSET_CURRENT, mTransform, OFFSET_TEMP, mTransform, OFFSET_CURRENT);
	}
	
	@Override
	public void onTap(float x, float y) {
		if (mMapListener == null) {
			return;
		}
		
		float width = getWidth();
		float height = getHeight();
		
		if (!Matrix.invertM(mTransform, OFFSET_TEMP, mTransform, OFFSET_CURRENT))
			return;
		
		if (getData().mCenter == null)
			return;

		float vect[] = new float[4];
		vect[0] = x / (width / 2.0f) - 1.0f;
		vect[1] = -(y / (height / 2.0f) - 1.0f);
		vect[3] = 1.0f;
		if (height > width) {
			float aspect = height / width;
			vect[1] *= aspect;
		} else {
			float aspect = width / height;
			vect[0] *= aspect;
		}
		Matrix.multiplyMV(vect, 0, mTransform, OFFSET_TEMP, vect, 0);

		LatLon latlon = LatLon.unproject(new Point2D(vect[0], vect[1]), getData().mCenter, null);

		mMapListener.onMapTap(latlon);
	}
	
	@Override
	public void onTouchUpdate() {
		updateTransform();
		updateViewpoint();
	}
	
	private void updateTransform() {
		float[] transform = new float[16];
		for (int i = 0; i < transform.length; i++) {
			transform[i] = mTransform[OFFSET_CURRENT + i];
		}
		
		mModel.getWritable().setTransform(transform);
		mModel.commit();
		
		mHasTransform = true;
	}
	
	private void updateViewpoint() {
		if (!Matrix.invertM(mTransform, OFFSET_TEMP, mTransform, OFFSET_CURRENT))
			return;
		
		if (getData().mCenter == null)
			return;

		float vect[] = new float[4];
		vect[3] = 1.0f;
		Matrix.multiplyMV(vect, 0, mTransform, OFFSET_TEMP, vect, 0);

		LatLon viewpoint = LatLon.unproject(new Point2D(vect[0], vect[1]), getData().mCenter, null);
		
		if (mViewpointListener != null) {
			mViewpointListener.onViewpointChanged(viewpoint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return mRecognizer.onTouchEvent(e);
	}
	
	private void updateCurrentFrame() {
		RadarData data = mRadarData != null ? mRadarData.getCurrentData() : null;
		if (mCurrentData == data) {
			return;
		}
		
		if (!mHasTransform && data != null) {
			//set the initial transform
			Matrix.setIdentityM(mTransform, OFFSET_CURRENT);
			RadialDataPacket packet = (RadialDataPacket)data.getDataFile().description.symbologyBlock.packets[0];
			scale(1.0f/packet.rangeBinCount);
			updateTransform();
		}
		
		//FIXME detect if preloading data and handle appropriately
		mCurrentData = data;
		loadRadar();
		loadLegend();
	}
	
    private RadarDataModel.RadarDataModelListener mRadarModelListener = new RadarDataModel.RadarDataModelListener() {
		@Override
		public void onUpdated() {
		}
		@Override
		public void onCurrentChanged(long time) {
			updateCurrentFrame();
		}
		@Override
		public void onPastFrameAdded(long time) {
			preloadRadar(time);
		}
	};
	
	private void preloadRadar(long time) {
		RadarData data = mRadarData.getData(time);
		
		if (data.getRadarRenderData() == null) {
			int taskId = TASK_PRERENDER_RADAR_START + mPrerenderCount++;

			Bundle args = RenderRadar.bundleArgs(data);
			mLoaderManager.initLoader(taskId, args, 
					new LoaderManager.LoaderCallbacks<RadarRenderData>() {
				@Override
				public Loader<RadarRenderData> onCreateLoader(int id, Bundle args) {
					return RenderRadar.createInstance(getContext(), args);
				}
				@Override
				public void onLoadFinished(Loader<RadarRenderData> loader, RadarRenderData renderData) {
				}
				@Override
				public void onLoaderReset(Loader<RadarRenderData> loader) {
				}
		    });
		}
		
		if (data.getLegendRenderData() == null) {
			int taskId = TASK_PRERENDER_RADAR_START + mPrerenderCount++;
			
			Bundle args = RenderLegend.bundleArgs(data);
			mLoaderManager.initLoader(taskId, args, 
					new LoaderManager.LoaderCallbacks<LegendRenderData>() {
				@Override
				public Loader<LegendRenderData> onCreateLoader(int id, Bundle args) {
					return RenderLegend.createInstance(getContext(), args);
				}
				@Override
				public void onLoadFinished(Loader<LegendRenderData> loader, LegendRenderData renderData) {
				}
				@Override
				public void onLoaderReset(Loader<LegendRenderData> loader) {
				}
		    });
		}
	}

	private void loadRadar() {
		if (mCurrentData == null && getData().mRadar != null) {
			getData().mRadar.setData(null);
			return;
		}
		
		Bundle args = RenderRadar.bundleArgs(mCurrentData);
		mLoaderManager.initLoader(TASK_RENDER_RADAR, args, mTaskLoadRadarCallbacks);
		
		if (RenderRadar.getInstance(mLoaderManager, TASK_RENDER_RADAR).getData() != mCurrentData) {
			mLoaderManager.restartLoader(TASK_RENDER_RADAR, args, mTaskLoadRadarCallbacks);
		}
	}
	
    private LoaderManager.LoaderCallbacks<RadarRenderData> mTaskLoadRadarCallbacks =
    		new LoaderManager.LoaderCallbacks<RadarRenderData>() {
		@Override
		public Loader<RadarRenderData> onCreateLoader(int id, Bundle args) {
			return RenderRadar.createInstance(getContext(), args);
		}
		@Override
		public void onLoadFinished(Loader<RadarRenderData> loader, RadarRenderData renderData) {
			RenderData data = getData();

			if (data.mRadar == null) {
				data.setRadar(new RadarRenderable(renderData));
				mModel.commit();
			} else {
				data.mRadar.setData(renderData);
			}
		}
		@Override
		public void onLoaderReset(Loader<RadarRenderData> loader) {
			getData().mRadar.setData(new RadarRenderData());
		}
    };
	
	private void loadLegend() {
		if (mCurrentData == null && getData().mLegend != null) {
			getData().mLegend.setData(null);
			return;
		}
		
		Bundle args = RenderLegend.bundleArgs(mCurrentData);
		mLoaderManager.initLoader(TASK_RENDER_LEGEND, args, mTaskLoadLegendCallbacks);
		
		if (RenderLegend.getInstance(mLoaderManager, TASK_RENDER_LEGEND).getData() != mCurrentData) {
			mLoaderManager.restartLoader(TASK_RENDER_LEGEND, args, mTaskLoadLegendCallbacks);	
		}
	}
	
    private LoaderManager.LoaderCallbacks<LegendRenderData> mTaskLoadLegendCallbacks =
    		new LoaderManager.LoaderCallbacks<LegendRenderData>() {
		@Override
		public Loader<LegendRenderData> onCreateLoader(int id, Bundle args) {
			return RenderLegend.createInstance(getContext(), args);
		}
		@Override
		public void onLoadFinished(Loader<LegendRenderData> loader, LegendRenderData renderData) {
			RenderData data = getData();
			
			if (data.mLegend == null) {
				data.setLegend(new LegendRenderable(renderData));
				mModel.commit();
			} else {
				data.mLegend.setData(renderData);	
			}
		}
		@Override
		public void onLoaderReset(Loader<LegendRenderData> loader) {
			getData().mLegend.setData(new LegendRenderData());
		}
    };
    
	private void loadLocation(Point2D position) {
		RenderData data = getData();
		
		if (data.mLocation == null) {
			LocationRenderData renderData = new LocationRenderData();
			renderData.setPosition(position);
			data.setLocation(new LocationRenderable(renderData));
			mModel.commit();
			Bundle args = RenderLocation.bundleArgs(renderData);
			mLoaderManager.initLoader(TASK_RENDER_LOCATION, args, mTaskLoadLocationCallbacks);
			
		} else {
			if (position != null)
				data.mLocation.getData().setPosition(position);
		}
	}
	
    private LoaderManager.LoaderCallbacks<LocationRenderData> mTaskLoadLocationCallbacks =
    		new LoaderManager.LoaderCallbacks<LocationRenderData>() {
		@Override
		public Loader<LocationRenderData> onCreateLoader(int id, Bundle args) {
			return RenderLocation.createInstance(getContext(), args);
		}
		@Override
		public void onLoadFinished(Loader<LocationRenderData> loader, LocationRenderData renderData) {
			getData().mLocation.setData(renderData);
		}
		@Override
		public void onLoaderReset(Loader<LocationRenderData> loader) {
			getData().mLocation.getData().setPosition(null);
		}
    };

}
