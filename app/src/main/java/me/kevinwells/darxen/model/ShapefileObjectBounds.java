package me.kevinwells.darxen.model;

public class ShapefileObjectBounds {
	
	public int xMin;
	public int xMax;
	public int yMin;
	public int yMax;
	
	public ShapefileObjectBounds(double xMin, double xMax, double yMin, double yMax) {
		assert(xMin < xMax);
		assert(yMin < yMax);
		this.xMin = (int)(xMin * 1E6);
		this.xMax = (int)(xMax * 1E6);
		this.yMin = (int)(yMin * 1E6);
		this.yMax = (int)(yMax * 1E6);
	}
}
