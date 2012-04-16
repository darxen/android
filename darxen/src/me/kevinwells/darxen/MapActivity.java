package me.kevinwells.darxen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.Level3Parser;
import me.kevinwells.darxen.data.ParseException;
import me.kevinwells.darxen.shp.DbfFile;
import me.kevinwells.darxen.shp.DbfFile.DbfRecord;
import me.kevinwells.darxen.shp.Shapefile;
import me.kevinwells.darxen.shp.ShapefileObject;
import me.kevinwells.darxen.shp.ShapefilePoint;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.WindowManager;
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
    
    private List<ShapefileInfo> mShapefiles;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	//hide notification area
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
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
			//mRadarSite.center.lat -= 0.6;
			//mRadarSite.center.lon -= 0.6;
			//reloadShapefiles(mRadarSite.center);
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
    
    private static class LoadSites extends CachedAsyncLoader<ArrayList<RadarSite>> {
    	
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
	
    private static class FindSite extends CachedAsyncLoader<RadarSite> {
    	
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
		getSupportLoaderManager().initLoader(TASK_LOAD_RADAR, args, mTaskLoadRadarCallbacks).startLoading();
	}
	
    private static class LoadRadar extends CachedAsyncLoader<DataFile> {
    	
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
    	mLayersLoaded = true;
	
    	mShapefiles = new ArrayList<ShapefileInfo>();
    	{
        	//states
    		ShapefileConfig config = new ShapefileConfig(R.raw.states_shp, R.raw.states_dbf, R.raw.states_shx);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f), 3.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f, 0.4f), 3.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.add(new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	//froyo can't read resources >1MB, like county lines
    	if (Build.VERSION.SDK_INT > 8)
    	{
        	//counties
    		ShapefileConfig config = new ShapefileConfig(R.raw.counties_shp, R.raw.counties_dbf, R.raw.counties_shx);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f), 1.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f, 0.4f), 1.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.add(new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	for (int i = 0; i < mShapefiles.size(); i++) {
    		ShapefileInfo info = mShapefiles.get(i);
    		
    		ShapefileRenderData model = new ShapefileRenderData();
			loadShapefile(i, center, info.mConfig, model);
		}
    }
    
    private void reloadShapefiles(LatLon viewpoint) {
    	for (int i = 0; i < mShapefiles.size(); i++) {
			int index = TASK_LOAD_SHAPEFILES + i;
			LoadShapefile loader = LoadShapefile.getInstance(getSupportLoaderManager(), index);

			loader.setViewPoint(viewpoint);
			loader.startLoading();
		}	
    }
    
    private void loadShapefile(int index, LatLon center, ShapefileConfig config, ShapefileRenderData data) {
    	Bundle args = LoadShapefile.bundleArgs(center, config, data);
    	getSupportLoaderManager().initLoader(TASK_LOAD_SHAPEFILES + index, args, mTaskLoadShapefilesCallbacks);
    }
    
	private static class LoadShapefile extends CachedAsyncLoader<ShapefileRenderData> {
		
		private LatLon mCenter;
		private LatLon mViewpoint;
		
		private ShapefileConfig mConfig;
		private ShapefileRenderData mData;
		
    	private static final String ARG_CENTER = "Center";
    	private static final String ARG_CONFIG = "Config";
    	private static final String ARG_DATA = "Data";
    	
    	public static Bundle bundleArgs(LatLon center, ShapefileConfig config, ShapefileRenderData data) {
    		Bundle args = new Bundle();
        	args.putParcelable(ARG_CENTER, center);
        	args.putParcelable(ARG_CONFIG, config);
        	args.putParcelable(ARG_DATA, data);
        	return args;
    	}
    	
		public static LoadShapefile createInstance(Context context, Bundle args) {
			LatLon center = args.getParcelable(ARG_CENTER);
			ShapefileConfig config = args.getParcelable(ARG_CONFIG);
			ShapefileRenderData data = args.getParcelable(ARG_DATA);
			return new LoadShapefile(context, center, config, data);
		}
		
		public static LoadShapefile getInstance(LoaderManager manager, int id) {
			Loader<ShapefileRenderData> res = manager.getLoader(id);
			return (LoadShapefile)res;
		}
	
		private LoadShapefile(Context context, LatLon center, ShapefileConfig config, ShapefileRenderData data) {
			super(context);
			mCenter = center;
			mConfig = config;
			mData = data;
			
			mViewpoint = mCenter;
		}
		
		private synchronized LatLon getViewpoint() {
			return mViewpoint;
		}
		
		public synchronized void setViewPoint(LatLon point) {
			mViewpoint = point;
		}
		
		@Override
		protected ShapefileRenderData doInBackground() {
			//mData = mData.clone();
			
			LatLon viewpoint = getViewpoint();

			Resources resources = getContext().getResources();
			
			InputStream fShp = resources.openRawResource(mConfig.resShp);
			InputStream fShx = resources.openRawResource(mConfig.resShx);
			
			Shapefile shapefile = new Shapefile(fShp, fShx);
			try {
				
				for (int i = 0; i < shapefile.getShapeCount(); i++) {
					ShapefileObject obj = shapefile.get(i);
					
					if (!obj.isNear(viewpoint.lat, viewpoint.lon)) {
						obj.close();
						mData.remove(i);
						continue;
						
					} else if (mData.contains(i)) {
						obj.close();
						mData.get(i).generateBuffers();
						continue;
					}
					
					obj.load();
					mData.add(i, generateObject(obj));
				}
				
			} finally {
				shapefile.close();
				
				try {
					fShp.close();
				} catch (IOException e) {}

				try {
					fShx.close();
				} catch (IOException e) {}
			}
			
			return mData;
		}
		
		private ShapefileObjectRenderData generateObject(ShapefileObject obj) {
			
			List<ShapefileObjectPartRenderData> parts = new ArrayList<ShapefileObjectPartRenderData>();

			for (int j = 0; j < obj.nParts; j++) {
				int start = obj.panPartStart[j];
				int end = start;
				for (int k = start; k < obj.nVertices; k++) {
					end = k+1;
					if (j < obj.nParts-1 && k+1 == obj.panPartStart[j+1])
						break;
				}
				parts.add(generateObjectPart(obj, start, end));
			}
			
			ShapefileObjectPartRenderData[] array = new ShapefileObjectPartRenderData[parts.size()];
			parts.toArray(array);
			return new ShapefileObjectRenderData(array);
		}
		
		private LatLon latLon = new LatLon();
		private ShapefilePoint shapePt = new ShapefilePoint();
		private Point2D p2 = new Point2D();
		private ShapefileObjectPartRenderData generateObjectPart(ShapefileObject obj, int start, int end) {
			int count = end-start;
			float array[] = new float[count * 2];
			int j = 0;
			
			for (int i = start; i < end; i++) {
				obj.getPoint(i, shapePt);
				latLon.lat = shapePt.y;
				latLon.lon = shapePt.x;
				p2 = latLon.project(mCenter, p2);
				array[j++] = (float)p2.x;
				array[j++] = (float)p2.y;
			}
			
			return new ShapefileObjectPartRenderData(count, array);	
		}
	}
	
	private LoaderManager.LoaderCallbacks<ShapefileRenderData> mTaskLoadShapefilesCallbacks =
    		new LoaderManager.LoaderCallbacks<ShapefileRenderData>() {
		@Override
		public Loader<ShapefileRenderData> onCreateLoader(int id, Bundle args) {
			return LoadShapefile.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<ShapefileRenderData> loader, ShapefileRenderData data) {
			int index = loader.getId() - TASK_LOAD_SHAPEFILES;
			ShapefileInfo info = mShapefiles.get(index);
			
			if (info.mUnderlay == null && info.mOverlay == null) {
				LinearShapefileRenderable underlay = new LinearShapefileRenderable(data, info.mUnderlayRenderConfig);
				LinearShapefileRenderable overlay = new LinearShapefileRenderable(data, info.mOverlayRenderConfig);
				mRadarView.addUnderlay(underlay);
				mRadarView.addOverlay(overlay);
				info.mUnderlay = underlay;
				info.mOverlay = overlay;
			} else {
				if (info.mUnderlay != null) {
					info.mUnderlay.setData(data);
				}
				if (info.mOverlay != null) {
					info.mOverlay.setData(data);
				}
			}
			setSupportProgressBarIndeterminateVisibility(false);
		}
		@Override
		public void onLoaderReset(Loader<ShapefileRenderData> loader) {
			int index = loader.getId() - TASK_LOAD_SHAPEFILES;
			ShapefileInfo info = mShapefiles.get(index);
			
			mRadarView.removeUnderlay(info.mUnderlay);
			mRadarView.removeUnderlay(info.mOverlay);
		}
    };

    private class ShapefileInfo
    {
    	public ShapefileConfig mConfig;
    	public ShapefileRenderConfig mUnderlayRenderConfig;
    	public ShapefileRenderConfig mOverlayRenderConfig;
    	public LinearShapefileRenderable mUnderlay;
    	public LinearShapefileRenderable mOverlay;
		
    	public ShapefileInfo(ShapefileConfig config, ShapefileRenderConfig underRenderConfig, ShapefileRenderConfig overRenderConfig) {
			this.mConfig = config;
			this.mUnderlayRenderConfig = underRenderConfig;
			this.mOverlayRenderConfig = overRenderConfig;
		}
    }
}