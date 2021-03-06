package me.kevinwells.darxen.renderables;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.Renderable;
import me.kevinwells.darxen.ShapefileObjectPartRenderData;
import me.kevinwells.darxen.ShapefileObjectRenderData;
import me.kevinwells.darxen.ShapefileRenderConfig;
import me.kevinwells.darxen.ShapefileRenderData;
import me.kevinwells.darxen.model.Color;

public class LinearShapefileRenderable implements Renderable {
	
	private ShapefileRenderData mData;
	private ShapefileRenderConfig mRenderConfig;

	public LinearShapefileRenderable(ShapefileRenderData data, ShapefileRenderConfig renderConfig) {
		mData = data;
		mRenderConfig = renderConfig;
	}
	
	public synchronized void setData(ShapefileRenderData data) {
		mData = data;
	}

	@Override
	public synchronized void render(GL10 gl) {
		Color color = mRenderConfig.mColor;
		
		synchronized (mData) {
			for (ShapefileObjectRenderData object : mData) {
				for (int i = 0; i < object.mParts.length; i++) {
					ShapefileObjectPartRenderData data = object.mParts[i];
					gl.glColor4f(color.r, color.g, color.b, color.a);
					gl.glVertexPointer(2, GL10.GL_FLOAT, 0, data.getBuffer());
					gl.glLineWidth(mRenderConfig.mLineWidth);
					gl.glPointSize(mRenderConfig.mLineWidth);
					gl.glDrawArrays(mRenderConfig.mRenderType, 0, data.mCount);
				}
			}
		}
	}
	
}
