package me.kevinwells.darxen;

import javax.microedition.khronos.opengles.GL10;

public interface MapMarkerRenderable {

	public void render(GL10 gl, MapMarkerCallbacks state);
	
}
