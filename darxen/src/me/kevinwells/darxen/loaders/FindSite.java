package me.kevinwells.darxen.loaders;

import java.util.ArrayList;
import java.util.List;

import me.kevinwells.darxen.LatLon;
import me.kevinwells.darxen.RadarSite;
import android.content.Context;
import android.os.Bundle;

public class FindSite extends CachedAsyncLoader<RadarSite> {
	
	private static final String ARG_RADAR_SITES = "RadarSites";
	private static final String ARG_POSITION = "Position";
	
	public static Bundle bundleArgs(ArrayList<RadarSite> radarSites, LatLon position) {
		Bundle args = new Bundle();
    	args.putParcelableArrayList(ARG_RADAR_SITES, radarSites);
    	args.putParcelable(ARG_POSITION, position);
    	return args;
	}
	
	public static FindSite createInstance(Context context, Bundle args) {
		ArrayList<RadarSite> radarSites = args.getParcelableArrayList(ARG_RADAR_SITES);
		LatLon position = args.getParcelable(ARG_POSITION);
		return new FindSite(context, radarSites, position);
	}
	
	private List<RadarSite> mRadarSites;
	private LatLon mPosition;
	
	private FindSite(Context context, List<RadarSite> radarSites, LatLon position) {
		super(context);
		mRadarSites = radarSites;
		mPosition = position;
	}
	
	private static final String STATIC_SITE = null;

	@Override
	protected RadarSite doInBackground() {
		if (STATIC_SITE != null) {
			for (int i = 0; i < mRadarSites.size(); i++)
				if (mRadarSites.get(i).name.equals(STATIC_SITE))
					return mRadarSites.get(i);
		}
		
		double[] distances = new double[mRadarSites.size()];
		
		for (int i = 0; i < mRadarSites.size(); i++)
			distances[i] = mPosition.distanceTo(mRadarSites.get(i).center);
		
		double minValue = distances[0];
		int minIndex = 0;
		for (int i = 1; i < distances.length; i++) {
			if (distances[i] < minValue) {
				minValue = distances[i];
				minIndex = i;
			}
		}
		
		return mRadarSites.get(minIndex);
	}
	
	@Override
	protected boolean shouldUpdate() {
		//shouldn't ever wander to far
		return false;
	}
}
