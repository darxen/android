package me.kevinwells.darxen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.Level3Parser;
import me.kevinwells.darxen.data.ParseException;
import me.kevinwells.darxen.shp.DbfFile;
import me.kevinwells.darxen.shp.DbfFile.DbfRecord;
import me.kevinwells.darxen.shp.Shapefile;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class MapActivity extends SherlockFragmentActivity {
	
	private TextView mTitle;
	private RadarView mRadarView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    
    private LatLon mPosition;
    private ArrayList<RadarSite> mRadarSites;
	
    private boolean mLayersLoaded;
    
    private RadarSite mRadarSite;
    
    private static final int TASK_LOAD_SITES = 0;
    private static final int TASK_FIND_SITE = 1;
    private static final int TASK_LOAD_RADAR = 2;
    private static final int TASK_LOAD_SHAPEFILES = 3;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        
        mRadarView = new RadarView(this);
        ((FrameLayout)findViewById(R.id.container)).addView(mRadarView);
        
        mTitle = (TextView)findViewById(R.id.title);
        
        Prefs.unsetLastUpdateTime();

        //TODO load cached site from shared prefs
        loadSites();
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			update();
			break;
		default:
			return false;
		}
		return true;
	}
	
	private void update() {
		if (mRadarSite == null)
			return;
		
		if (!DataPolicy.shouldUpdate(Prefs.getLastUpdateTime(), new Date())) {
			Toast.makeText(this, "Patience, my young Padawan", Toast.LENGTH_SHORT).show();
			return;
		}
		
		setSupportProgressBarIndeterminateVisibility(true);
		
		loadRadar();
	}
	
	private void updateLocation(Location location) {
		if (location == null)
			return;
		
		boolean initSite = mPosition == null;
		Log.v(C.TAG, location.toString());
		mPosition = new LatLon(location.getLatitude(), location.getLongitude());
		mRadarView.setLocation(mPosition);
		
		if (initSite)
			initSite();
	}

    protected void onResume() {
    	super.onResume();
    	
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				updateLocation(location);
			}

			@Override
			public void onProviderDisabled(String provider) {
				new AlertDialog.Builder(MapActivity.this)
	        	.setTitle(R.string.location_services_title)
	        	.setMessage(R.string.location_services_message)
	        	.setCancelable(true)
	        	.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				})
	        	.setPositiveButton(R.string.do_it, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(intent);
					}
				})
				.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).create().show();
			}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (status != LocationProvider.AVAILABLE) {
					//TEMPORARILY_UNAVAILABLE
					//AVAILABLE
					//mRadarView.setLocation(null);
				}
			}
		};

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        updateLocation(location);
		
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	
    	mRadarView.onResume();
    }
    
    protected void onPause() {
    	super.onPause();
    	
    	locationManager.removeUpdates(locationListener);
    	mRadarView.setLocation(null);
    	
    	mRadarView.onPause();
    }
    
    private void initSite() {
    	if (mRadarSites == null || mPosition == null)
    		return;
    	
    	findSite();
    }
    
    private void loadSites() {
    	getSupportLoaderManager().initLoader(TASK_LOAD_SITES, null, mTaskLoadSitesCallbacks);
    }
    
    private static class LoadSites extends AsyncLoader<ArrayList<RadarSite>> {
    	
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

    private LoaderManager.LoaderCallbacks<ArrayList<RadarSite>> mTaskLoadSitesCallbacks =
    		new LoaderManager.LoaderCallbacks<ArrayList<RadarSite>>() {
		@Override
		public Loader<ArrayList<RadarSite>> onCreateLoader(int id, Bundle args) {
			return LoadSites.createInstance(MapActivity.this);
		}
		@Override
		public void onLoadFinished(Loader<ArrayList<RadarSite>> loader, ArrayList<RadarSite> radarSites) {
			MapActivity.this.mRadarSites = radarSites;
			initSite();
		}
		@Override
		public void onLoaderReset(Loader<ArrayList<RadarSite>> loader) {
			MapActivity.this.mRadarSites = null;
		}
    };
    
	private void findSite() {
		Bundle args = FindSite.bundleArgs(mRadarSites, mPosition);
		getSupportLoaderManager().initLoader(TASK_FIND_SITE, args, mTaskFindSiteCallbacks);
	}
	
    private static class FindSite extends AsyncLoader<RadarSite> {
    	
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

		@Override
		protected RadarSite doInBackground() {
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
    
    private LoaderManager.LoaderCallbacks<RadarSite> mTaskFindSiteCallbacks =
    		new LoaderManager.LoaderCallbacks<RadarSite>() {
		@Override
		public Loader<RadarSite> onCreateLoader(int id, Bundle args) {
			return FindSite.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<RadarSite> loader, RadarSite radarSite) {
			mRadarSite = radarSite;
			update();
		}
		@Override
		public void onLoaderReset(Loader<RadarSite> loader) {
			mRadarSite = null;
		}
    };

	private void loadRadar() {
		Bundle args = LoadRadar.bundleArgs(mRadarSite);
		getSupportLoaderManager().initLoader(TASK_LOAD_RADAR, args, mTaskLoadRadarCallbacks);
	}
	
    private static class LoadRadar extends AsyncLoader<DataFile> {
    	
    	private static final String ARG_RADAR_SITE = "RadarSite";
    	
    	public static Bundle bundleArgs(RadarSite radarSite) {
    		Bundle args = new Bundle();
    		args.putParcelable(ARG_RADAR_SITE, radarSite);
    		return args;
    	}
    	
    	public static LoadRadar createInstance(Context context, Bundle args) {
    		RadarSite radarSite = args.getParcelable(ARG_RADAR_SITE);
			return new LoadRadar(context, radarSite);
    	}
    	
    	private RadarSite mRadarSite;
    	
    	private LoadRadar(Context context, RadarSite radarSite) {
    		super(context);
    		mRadarSite = radarSite;
    	}

		@Override
		protected DataFile doInBackground() {
			byte[] data = null;
			do {
		        try {
		        	data = getData(mRadarSite);
				} catch (IOException e) {
					Log.e(C.TAG, "Failed to download radar imagery", e);
				}
			} while (data == null);
	        
	        Level3Parser parser = new Level3Parser();
	        DataFile file;
	        try {
				file = parser.parse(new ByteArrayInputStream(data));
			} catch (ParseException e) {
				Log.e(C.TAG, "Failed to parse radar imagery", e);
				return null;
			} catch (IOException e) {
				Log.e(C.TAG, "Failed to parse radar imagery", e);
				return null;
			}
	        
	        return file;
		}

		private byte[] getData(RadarSite radarSite) throws SocketException, IOException {
	    	ByteArrayOutputStream fout = new ByteArrayOutputStream();
	        
	    	FTPClient ftpClient = new FTPClient();
			ftpClient.connect("tgftp.nws.noaa.gov", 21);
			if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
				throw new IOException("Failed to connect");
			ftpClient.login("anonymous", "darxen");
			
			ftpClient.changeWorkingDirectory("SL.us008001/DF.of/DC.radar/DS.p19r0/SI." + radarSite.name.toLowerCase());
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();
			ftpClient.retrieveFile("sn.last", fout);
			fout.close();
			ftpClient.disconnect();
			
	        return fout.toByteArray();
	    }
    }
    private LoaderManager.LoaderCallbacks<DataFile> mTaskLoadRadarCallbacks =
    		new LoaderManager.LoaderCallbacks<DataFile>() {
		@Override
		public Loader<DataFile> onCreateLoader(int id, Bundle args) {
			return LoadRadar.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<DataFile> loader, DataFile data) {
			if (data == null) {
				finish();
				return;
			}

	        mTitle.setText(new String(data.header).replace("\n", ""));
			
	        if (!mLayersLoaded) {
	        	loadShapefiles(new LatLon(data.description.lat, data.description.lon));
	        }
			
			mRadarView.setData(data);
			
			if (mLayersLoaded) {
				setSupportProgressBarIndeterminateVisibility(false);
			}

			Prefs.setLastUpdateTime(new Date());
		}
		@Override
		public void onLoaderReset(Loader<DataFile> loader) {
			mRadarView.setData(null);
		}
    };
    
    private void loadShapefiles(LatLon center) {
    	Bundle args = LoadShapefiles.bundleArgs(center);
    	getSupportLoaderManager().initLoader(TASK_LOAD_SHAPEFILES, args, mTaskLoadShapefilesCallbacks);
    }
	private static class LoadShapefiles extends AsyncLoader<Void> {
		
		private LatLon mCenter;
		
    	private static final String ARG_CENTER = "Center";
    	
    	public static Bundle bundleArgs(LatLon center) {
    		Bundle args = new Bundle();
        	args.putParcelable(ARG_CENTER, center);
        	return args;
    	}
    	
		public static LoadShapefiles createInstance(Context context, Bundle args) {
			LatLon center = args.getParcelable(ARG_CENTER);
			return new LoadShapefiles(context, center);
		}
	
		private LoadShapefiles(Context context, LatLon center) {
			super(context);
			mCenter = center;
		}

		private ShapefileLayer loadShapefile(ShapefileConfig config) {
			
			Resources resources = getContext().getResources();
			
			InputStream fShp = resources.openRawResource(config.resShp);
			InputStream fShx = resources.openRawResource(config.resShx);
			
			Shapefile shapefile = new Shapefile(fShp, fShx);
			try {
				return new ShapefileLayer(mCenter, shapefile, config);
			} finally {
				shapefile.close();
				
				try {
					fShp.close();
				} catch (IOException e) {}

				try {
					fShx.close();
				} catch (IOException e) {}
			}
		}
		
		@Override
		protected Void doInBackground() {
			ShapefileLayer layer;

			List<ShapefileConfig> configs = new ArrayList<ShapefileConfig>();
			
			configs.add(new ShapefileConfig(R.raw.states_shp, R.raw.states_dbf, R.raw.states_shx,
							3.0f, new Color(1.0f, 1.0f, 1.0f)));
			
			//froyo can't read resources >1MB, like county lines
			if (Build.VERSION.SDK_INT > 8) {
				configs.add(new ShapefileConfig(R.raw.counties_shp, R.raw.counties_dbf, R.raw.counties_shx,
							1.0f, new Color(0.75f, 0.75f, 0.75f)));
			}
			
			for (ShapefileConfig config : configs) {
				layer = loadShapefile(config);
				if (layer == null)
					continue;
				
				mRadarView.addLayer(layer);
			}
			
			return null;
		}
		//FIXME REMOVE
		private static RadarView mRadarView;
	}
	private LoaderManager.LoaderCallbacks<Void> mTaskLoadShapefilesCallbacks =
    		new LoaderManager.LoaderCallbacks<Void>() {
		@Override
		public Loader<Void> onCreateLoader(int id, Bundle args) {
			//FIXME REMOVE
			LoadShapefiles.mRadarView = mRadarView;
			return LoadShapefiles.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<Void> loader, Void data) {
			mLayersLoaded = true;

			setSupportProgressBarIndeterminateVisibility(false);
		}
		@Override
		public void onLoaderReset(Loader<Void> loader) {
			//TODO
		}
    };
}