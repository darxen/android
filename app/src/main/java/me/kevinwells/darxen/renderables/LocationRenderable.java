package me.kevinwells.darxen.renderables;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.MapMarkerCallbacks;
import me.kevinwells.darxen.MapMarkerRenderable;
import me.kevinwells.darxen.Point2D;
import me.kevinwells.darxen.model.LocationRenderData;

public class LocationRenderable implements MapMarkerRenderable {

	private LocationRenderData mData;
	
	public LocationRenderable(LocationRenderData data) {
		mData = data;
	}
	
	public void setData(LocationRenderData data) {
		mData = data;
	}
	
	public LocationRenderData getData() {
		return mData;
	}
	
	@Override
	public void render(GL10 gl, MapMarkerCallbacks state) {
		staticRender(gl, state, mData);
	}
		
	public static void staticRender(GL10 gl, MapMarkerCallbacks state, LocationRenderData data) {
		if (data == null)
			return;
		
		synchronized (data) {
			FloatBuffer buf;
			
			Point2D position = data.getPosition();
			if (position == null) {
				return;
			}
			
			Point2D offset = state.transform(position, null);
			gl.glPushMatrix();
			gl.glTranslatef((float)offset.x, (float)offset.y, 0.0f);

			buf = data.getCrosshairBuffer();
			if (buf == null)
				return;
			gl.glLineWidth(1.0f);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buf);
			gl.glDrawArrays(GL10.GL_LINES, 0, 4);

			buf = data.getCircleBuffer();
			if (buf == null)
				return;
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buf);
			gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, data.getCircleCount());
			
			gl.glPopMatrix();
		}
	}

}
