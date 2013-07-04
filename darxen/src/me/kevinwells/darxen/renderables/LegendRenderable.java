package me.kevinwells.darxen.renderables;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.Renderable;
import me.kevinwells.darxen.model.LegendRenderData;

public class LegendRenderable implements Renderable {
	
	private LegendRenderData mData;
	
	public LegendRenderable(LegendRenderData data) {
		mData = data;
	}
	
	public void setData(LegendRenderData data) {
		mData = data;
	}

	@Override
	public void render(GL10 gl) {
		staticRender(gl, mData);
	}
		
	private static void staticRender(GL10 gl, LegendRenderData data) {
		if (data == null)
			return;
			
		synchronized (data) {
			if (data.getVertexBuffer() == null)
				return;
			
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, data.getVertexBuffer());
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, data.getColorBuffer());
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, data.getCount());
			
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}
	}

}
