package me.kevinwells.darxen.model;

import java.util.ArrayList;
import java.util.List;

import me.kevinwells.darxen.LatLon;
import me.kevinwells.darxen.Renderable;
import me.kevinwells.darxen.renderables.LegendRenderable;
import me.kevinwells.darxen.renderables.LocationRenderable;
import me.kevinwells.darxen.renderables.RadarRenderable;

public class RenderData {
	
	public float[] mTransform;
	
	public LatLon mCenter;
	
	public boolean mBackgroundModified;
	public boolean mForegroundModified;
	public List<Renderable> mBackground;
	public List<Renderable> mForeground;

	public LegendRenderable mLegend;
	public RadarRenderable mRadar;
	public LocationRenderable mLocation;
	
	public RenderData() {
		mBackground = new ArrayList<Renderable>();
		mForeground = new ArrayList<Renderable>();
	}
	
	public void setTransform(float[] transform) {
		assert(mTransform != transform);
		assert(transform.length == 16);
		mTransform = transform;
	}
	
	public void setCenter(LatLon center) {
		assert(mCenter != center);
		mCenter = center;
	}
	
	public void addUnderlay(Renderable layer) {
		mBackground.add(layer);
		mBackgroundModified = true;
	}
	
	public void removeUnderlay(Renderable layer) {
		mBackground.remove(layer);
		mBackgroundModified = true;
	}
	
	public void addOverlay(Renderable layer) {
		mForeground.add(layer);
		mForegroundModified = true;
	}
	
	public void removeOverlay(Renderable layer) {
		mForeground.remove(layer);
		mForegroundModified = true;
	}
	
	public void setLegend(LegendRenderable legend) {
		assert(mLegend != legend);
		mLegend = legend;
	}

	public void setRadar(RadarRenderable radar) {
		assert(mRadar != radar);
		mRadar = radar;
	}

	public void setLocation(LocationRenderable location) {
		assert(mLocation != location);
		mLocation = location;
	}

	@SuppressWarnings("unchecked")
	private static <T> ArrayList<T> cloneList(List<T> list) {
		return (ArrayList<T>)((ArrayList<T>)list).clone();
	}
	
	/**
	 * Copy references from model into this
	 * @param model The model to copy from
	 */
	public void copy(RenderData model) {
		mTransform = model.mTransform;
		mCenter = model.mCenter;
		mBackground = cloneList(model.mBackground);
		mForeground = cloneList(model.mForeground);
		mLegend = model.mLegend;
		mRadar = model.mRadar;
		mLocation = model.mLocation;
	}
}
