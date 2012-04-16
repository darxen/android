package me.kevinwells.darxen.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import me.kevinwells.darxen.LatLon;
import me.kevinwells.darxen.R;
import me.kevinwells.darxen.RadarSite;
import me.kevinwells.darxen.shp.DbfFile;
import me.kevinwells.darxen.shp.DbfFile.DbfRecord;
import android.content.Context;

public class LoadSites extends CachedAsyncLoader<ArrayList<RadarSite>> {
	
	public static LoadSites createInstance(Context context) {
		return new LoadSites(context);
	}
	
	private LoadSites(Context context) {
		super(context);
	}

	@Override
	public ArrayList<RadarSite> doInBackground() {
		
		ArrayList<RadarSite> radarSites = new ArrayList<RadarSite>();
		
		InputStream fin = getContext().getResources().openRawResource(R.raw.radars_dbf);
		DbfFile sites = new DbfFile(fin);
		for (DbfRecord site : sites) {
			String name = site.getString(0).toUpperCase();
			double lat = site.getDouble(1);
			double lon = site.getDouble(2);
			
			radarSites.add(new RadarSite(name, new LatLon(lat, lon)));
		}
		try {
			sites.close();
		} catch (Exception e) {}
		
		try {
			fin.close();
		} catch (IOException e) {}
		
		return radarSites;
	}
	
	@Override
	protected boolean shouldUpdate() {
		//radar sites never change
		return false;
	}
}
