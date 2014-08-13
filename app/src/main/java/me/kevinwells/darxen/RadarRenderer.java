package me.kevinwells.darxen;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.model.RenderData;
import me.kevinwells.darxen.model.RenderModel;

public class RadarRenderer implements GLSurfaceView.Renderer, MapMarkerCallbacks {

	private RenderModel mModel;
	private RenderData mData;
	
	private float mWidth;
	private float mHeight;
	
	public RadarRenderer(RenderModel model) {
		mModel = model;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		mData = mModel.getReadable();
		if (mData == null)
			return;
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glMultMatrixf(mData.mTransform, 0);
		
		for (Renderable renderable : mData.mBackground) {
			renderable.render(gl);
		}
		
		gl.glEnable(GL10.GL_BLEND);
		if (mData.mRadar != null) {
			mData.mRadar.render(gl);
		}
		
		for (Renderable renderable : mData.mForeground) {
			renderable.render(gl);
		}
		
		renderMapLegend(gl);
		
		renderLegend(gl);
		mData = null;
	}
	
	private void renderMapLegend(GL10 gl) {
		gl.glLoadIdentity();
		
		if (mData.mLocation != null) {
			mData.mLocation.render(gl, this);
		}
	}
	
	private void renderLegend(GL10 gl) {
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		//GLU.gluOrtho2D(gl, 0, getWidth(), getHeight(), 0);
		{
			float width = mWidth;
			float height = mHeight;
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
		
		if (mData.mLegend != null) {
			mData.mLegend.render(gl);
		}
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		
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
	
    private float[] mTransformPt = new float[4];
	@Override
	public Point2D transform(Point2D pos, Point2D res) {
		if (res == null)
			res = new Point2D();
		
		mTransformPt[0] = (float)pos.x;
		mTransformPt[1] = (float)pos.y;
		mTransformPt[2] = 0.0f;
		mTransformPt[3] = 1.0f;
		
		Matrix.multiplyMV(mTransformPt, 0, mData.mTransform, 0, mTransformPt, 0);
		
		res.x = mTransformPt[0];
		res.y = mTransformPt[1];
		
		return res;
	}
}
