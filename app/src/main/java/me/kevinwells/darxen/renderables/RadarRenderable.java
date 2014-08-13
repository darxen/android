package me.kevinwells.darxen.renderables;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.Renderable;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.Palette;
import me.kevinwells.darxen.model.RadarRenderData;

public class RadarRenderable implements Renderable {
	
	private RadarRenderData mData;
	
	public RadarRenderable(RadarRenderData data) {
		mData = data;
	}
	
	public void setData(RadarRenderData data) {
		mData = data;
	}
	
	@Override
	public void render(GL10 gl) {
		staticRender(gl, mData);
	}
		
	private static void staticRender(GL10 gl, RadarRenderData data) {
		if (data == null)
			return;
		
		synchronized (data) {
			Palette palette = data.getPalette();
			for (int i = 1; i < 16; i++) {
				FloatBuffer vertexBuffer = data.getVertexBuffer(i);
				if (vertexBuffer == null)
					continue;
				
				Color c = palette.get(i);
				gl.glColor4f(c.r, c.g, c.b, c.a);
				gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, data.getCount(i));
			}
		}
	}


}
