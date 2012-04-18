package me.kevinwells.darxen;

import me.kevinwells.darxen.model.Color;

public class ShapefileRenderConfig {

	public Color mColor;
	public float mLineWidth;
	public int mRenderType;

	public ShapefileRenderConfig(Color color, float lineWidth, int renderType) {
		mColor = color;
		mLineWidth = lineWidth;
		mRenderType = renderType;
	}
}