package me.kevinwells.darxen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.RadialDataPacket;
import me.kevinwells.darxen.renderables.RadarRenderable;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.view.MotionEvent;

public class RadarView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureSurface {

	private DataFile mData;
	private LatLon mPos;
	private FloatBuffer mPosBuf;
	
	private GestureRecognizer mRecognizer = new GestureRecognizer(this);

	private float[] mTransform = new float[32];
	
	private List<Renderable> mBackground;
	private List<Renderable> mForeground;
	
	private RadarRenderable mRadar;
	
	private ViewpointListener mViewpointListener;

	public RadarView(Context context) {
		super(context);
		mBackground = new ArrayList<Renderable>();
		mForeground = new ArrayList<Renderable>();
		
		//setEGLContextClientVersion(2);
		setRenderer(this);
		Matrix.setIdentityM(mTransform, 0);
	}

	public synchronized void setData(DataFile file) {
		if (mData == null) {
			//set the initial transform
			Matrix.setIdentityM(mTransform, 0);
			RadialDataPacket packet = (RadialDataPacket)file.description.symbologyBlock.packets[0];
			scale(1.0f/packet.rangeBinCount);
		}
		
		mData = file;
		mRadar = null;
		
		if (file != null) {
			mRadar = new RadarRenderable(file);
		}
		
		updateLocation();
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
		Matrix.setIdentityM(mTransform, 16);
		Matrix.scaleM(mTransform, 16, factor, factor, 1.0f);
		Matrix.multiplyMM(mTransform, 0, mTransform, 16, mTransform, 0);
	}
	
	@Override
	public void translate(float dx, float dy) {
		Matrix.setIdentityM(mTransform, 16);
		Matrix.translateM(mTransform, 16, dx, dy, 0.0f);
		Matrix.multiplyMM(mTransform, 0, mTransform, 16, mTransform, 0);
	}
	
	@Override
	public void onTouchUpdate() {
		updateViewpoint();
	}
	
	private void updateViewpoint() {
		if (!Matrix.invertM(mTransform, 16, mTransform, 0))
			return;

		float vect[] = new float[4];
		vect[3] = 1.0f;
		Matrix.multiplyMV(vect, 0, mTransform, 16, vect, 0);

		LatLon center = new LatLon(mData.description.lat, mData.description.lon);
		LatLon viewpoint = LatLon.unproject(new Point2D(vect[0], vect[1]), center, null);
		
		if (mViewpointListener != null) {
			mViewpointListener.onViewpointChanged(viewpoint);
		}
	}
	
	public synchronized void setLocation(LatLon pos) {
		mPos = pos;
		updateLocation();
	}
	
	private void updateLocation() {
		if (mData == null || mPos == null) {
			mPosBuf = null;
			return;
		}
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(2 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mPosBuf = vbb.asFloatBuffer();

		Point2D p = mPos.project(new LatLon(mData.description.lat, mData.description.lon), null);
		mPosBuf.put((float)p.x);
		mPosBuf.put((float)p.y);
		mPosBuf.position(0);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return mRecognizer.onTouchEvent(e);
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glMultMatrixf(mTransform, 0);
		
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
		
		//TODO render legend
		
		if (mPosBuf != null) {
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mPosBuf);
			gl.glPointSize(10.0f);
			gl.glDrawArrays(GL10.GL_POINTS, 0, 1);
		}
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
}
