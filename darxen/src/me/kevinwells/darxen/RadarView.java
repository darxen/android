package me.kevinwells.darxen;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.RadialDataPacket;
import me.kevinwells.darxen.loaders.RenderLegend;
import me.kevinwells.darxen.loaders.RenderLocation;
import me.kevinwells.darxen.loaders.RenderRadar;
import me.kevinwells.darxen.model.LegendRenderData;
import me.kevinwells.darxen.model.LocationRenderData;
import me.kevinwells.darxen.model.RadarRenderData;
import me.kevinwells.darxen.renderables.LegendRenderable;
import me.kevinwells.darxen.renderables.LocationRenderable;
import me.kevinwells.darxen.renderables.RadarRenderable;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.MotionEvent;

public class RadarView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureSurface, MapMarkerCallbacks {

	private LoaderManager mLoaderManager;
	
	private DataFile mData;
	
	private LatLon mCenter;
	
	private GestureRecognizer mRecognizer = new GestureRecognizer(this);

	private float[] mTransform = new float[48];
	private static final int OFFSET_RENDER = 0;
	private static final int OFFSET_CURRENT = 16;
	private static final int OFFSET_TEMP = 32;
	
	private List<Renderable> mBackground;
	private List<Renderable> mForeground;
	
	private LegendRenderable mLegend;
	private RadarRenderable mRadar;
	private LocationRenderable mLocation;
	
	private ViewpointListener mViewpointListener;
	
	private static final int TASK_RENDER_RADAR = 100;
	private static final int TASK_RENDER_LEGEND = 101;
	private static final int TASK_RENDER_LOCATION = 102;

	public RadarView(Context context, LoaderManager loaderManager) {
		super(context);
		mLoaderManager = loaderManager;
		
		mBackground = new ArrayList<Renderable>();
		mForeground = new ArrayList<Renderable>();
		
		//setEGLContextClientVersion(2);
		setRenderer(this);
		Matrix.setIdentityM(mTransform, OFFSET_CURRENT);
		scale(1.0f/230.0f);
		updateRenderTransform();
	}

	public synchronized void setData(DataFile data) {
		if (mData == null) {
			//set the initial transform
			Matrix.setIdentityM(mTransform, OFFSET_CURRENT);
			RadialDataPacket packet = (RadialDataPacket)data.description.symbologyBlock.packets[0];
			scale(1.0f/packet.rangeBinCount);
		}
		
		if (mData == null) {
			mData = data;
			loadRadar();
			loadLegend();
			
		} else {
			mData = data;
			reloadRadar();
			reloadLegend();
		}
	}
	
	public synchronized void setCenter(LatLon center) {
		mCenter = center;
	}
	
	public synchronized void addUnderlay(Renderable layer) {
		mBackground.add(layer);
	}
	
	public synchronized void removeUnderlay(Renderable layer) {
		mBackground.remove(layer);
	}
	
	public synchronized void addOverlay(Renderable layer) {
		mForeground.add(layer);
	}
	
	public synchronized void removeOverlay(Renderable layer) {
		mForeground.remove(layer);
	}
	
	public synchronized void setViewpointListener(ViewpointListener listener) {
		mViewpointListener = listener;
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
	public void onTouchUpdate() {
		updateViewpoint();
		updateRenderTransform();
	}
	
	private synchronized void updateRenderTransform() {
		for (int i = 0; i < 16; i++)
			mTransform[OFFSET_RENDER+i] = mTransform[OFFSET_CURRENT+i];
	}
	
	private void updateViewpoint() {
		if (!Matrix.invertM(mTransform, OFFSET_TEMP, mTransform, OFFSET_CURRENT))
			return;
		
		if (mCenter == null)
			return;

		float vect[] = new float[4];
		vect[3] = 1.0f;
		Matrix.multiplyMV(vect, 0, mTransform, OFFSET_TEMP, vect, 0);

		LatLon viewpoint = LatLon.unproject(new Point2D(vect[0], vect[1]), mCenter, null);
		
		if (mViewpointListener != null) {
			mViewpointListener.onViewpointChanged(viewpoint);
		}
	}
	
	public synchronized void setLocation(LatLon pos) {
		updateLocation(pos);
	}
	
	private void updateLocation(LatLon pos) {
		if (mCenter == null || pos == null) {
			loadLocation(null);
			
		} else {
			Point2D p = null;
			p = pos.project(mCenter, null);
			loadLocation(p);			
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return mRecognizer.onTouchEvent(e);
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glMultMatrixf(mTransform, OFFSET_RENDER);
		
		for (Renderable renderable : mBackground) {
			renderable.render(gl);
		}
		
		gl.glEnable(GL10.GL_BLEND);
		if (mRadar != null) {
			mRadar.render(gl);
		}
		
		for (Renderable renderable : mForeground) {
			renderable.render(gl);
		}
		
		renderMapLegend(gl);
		
		renderLegend(gl);
	}
	
	private void renderMapLegend(GL10 gl) {
		gl.glLoadIdentity();
		
		if (mLocation != null) {
			mLocation.render(gl, this);
		}
	}
	
	private void renderLegend(GL10 gl) {
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		//GLU.gluOrtho2D(gl, 0, getWidth(), getHeight(), 0);
		{
			float width = getWidth();
			float height = getHeight();
			if (height > width) {
				float aspect = (float)height / width;
				GLU.gluOrtho2D(gl, 0, 1, aspect, 0);
			} else {
				float aspect = (float)width/ height;
				GLU.gluOrtho2D(gl, 0, aspect, 1, 0);
			}
		}
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		if (mLegend != null) {
			mLegend.render(gl);
		}
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		if (height > width) {
			float aspect = (float)height / width;
			GLU.gluOrtho2D(gl, -1, 1, -aspect, aspect);
		} else {
			float aspect = (float)width/ height;
			GLU.gluOrtho2D(gl, -aspect, aspect, -1, 1);
		}
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		
		gl.glDisable(GL10.GL_COLOR_MATERIAL);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
	}
	
	private void loadRadar() {
		RadarRenderData renderData = new RadarRenderData();
		if (mRadar == null) {
			mRadar = new RadarRenderable(renderData);
		}
		Bundle args = RenderRadar.bundleArgs(mData, renderData);
		mLoaderManager.initLoader(TASK_RENDER_RADAR, args, mTaskLoadRadarCallbacks);
	}
	
	private void reloadRadar() {
		RadarRenderData renderData = new RadarRenderData();
		if (mRadar == null) {
			mRadar = new RadarRenderable(renderData);
		}
		Bundle args = RenderRadar.bundleArgs(mData, renderData);
		mLoaderManager.restartLoader(TASK_RENDER_RADAR, args, mTaskLoadRadarCallbacks);
	}
	
    private LoaderManager.LoaderCallbacks<RadarRenderData> mTaskLoadRadarCallbacks =
    		new LoaderManager.LoaderCallbacks<RadarRenderData>() {
		@Override
		public Loader<RadarRenderData> onCreateLoader(int id, Bundle args) {
			return RenderRadar.createInstance(getContext(), args);
		}
		@Override
		public void onLoadFinished(Loader<RadarRenderData> loader, RadarRenderData renderData) {
			mRadar.setData(renderData);
		}
		@Override
		public void onLoaderReset(Loader<RadarRenderData> loader) {
			mRadar.setData(new RadarRenderData());
		}
    };
	
	private void loadLegend() {
		LegendRenderData renderData = new LegendRenderData();
		if (mLegend == null) {
			mLegend = new LegendRenderable(renderData);
		}
		Bundle args = RenderLegend.bundleArgs(mData, renderData);
		mLoaderManager.initLoader(TASK_RENDER_LEGEND, args, mTaskLoadLegendCallbacks);
	}
	
	private void reloadLegend() {
		LegendRenderData renderData = new LegendRenderData();
		if (mLegend == null) {
			mLegend = new LegendRenderable(renderData);
		}
		Bundle args = RenderLegend.bundleArgs(mData, renderData);
		mLoaderManager.restartLoader(TASK_RENDER_LEGEND, args, mTaskLoadLegendCallbacks);
	}
	
    private LoaderManager.LoaderCallbacks<LegendRenderData> mTaskLoadLegendCallbacks =
    		new LoaderManager.LoaderCallbacks<LegendRenderData>() {
		@Override
		public Loader<LegendRenderData> onCreateLoader(int id, Bundle args) {
			return RenderLegend.createInstance(getContext(), args);
		}
		@Override
		public void onLoadFinished(Loader<LegendRenderData> loader, LegendRenderData renderData) {
			mLegend.setData(renderData);
		}
		@Override
		public void onLoaderReset(Loader<LegendRenderData> loader) {
			mLegend.setData(new LegendRenderData());
		}
    };
    
	private void loadLocation(Point2D position) {
		if (mLocation == null) {
			LocationRenderData renderData = new LocationRenderData();
			renderData.setPosition(position);
			mLocation = new LocationRenderable(renderData);
			Bundle args = RenderLocation.bundleArgs(renderData);
			mLoaderManager.initLoader(TASK_RENDER_LOCATION, args, mTaskLoadLocationCallbacks);
			
		} else {
			if (position != null)
				mLocation.getData().setPosition(position);
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
			mLocation.setData(renderData);
		}
		@Override
		public void onLoaderReset(Loader<LocationRenderData> loader) {
			mLocation.getData().setPosition(null);
		}
    };

    private float[] mTransformPt = new float[4];
	@Override
	public Point2D transform(Point2D pos, Point2D res) {
		if (res == null)
			res = new Point2D();
		
		mTransformPt[0] = (float)pos.x;
		mTransformPt[1] = (float)pos.y;
		mTransformPt[2] = 0.0f;
		mTransformPt[3] = 1.0f;
		
		Matrix.multiplyMV(mTransformPt, 0, mTransform, OFFSET_RENDER, mTransformPt, 0);
		
		res.x = mTransformPt[0];
		res.y = mTransformPt[1];
		
		return res;
	}
}
