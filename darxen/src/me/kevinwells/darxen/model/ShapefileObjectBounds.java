package me.kevinwells.darxen.model;

public class ShapefileObjectBounds {
	
	public double xMin;
	public double xMax;
	public double yMin;
	public double yMax;
	
	public ShapefileObjectBounds(double xMin, double xMax, double yMin, double yMax) {
		assert(xMin < xMax);
		assert(yMin < yMax);
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
	}
}
