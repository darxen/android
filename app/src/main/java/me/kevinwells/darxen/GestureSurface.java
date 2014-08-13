package me.kevinwells.darxen;

public interface GestureSurface {
	
	public void translate(float dx, float dy);
	public void scale(float factor);
	
	public void onTap(float x, float y);
	public void onTouchUpdate();
	
	public int getWidth();
	public int getHeight();

}
