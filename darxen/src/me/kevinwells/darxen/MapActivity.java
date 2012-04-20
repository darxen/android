package me.kevinwells.darxen;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.loaders.FindSite;
import me.kevinwells.darxen.loaders.LoadRadar;
import me.kevinwells.darxen.loaders.LoadShapefile;
import me.kevinwells.darxen.loaders.LoadSites;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.ShapefileConfig;
import me.kevinwells.darxen.model.ShapefileId;
import me.kevinwells.darxen.renderables.LinearShapefileRenderable;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
        
        mRadarView = new RadarView(this, getSupportLoaderManager());
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
		
		reloadRadar();
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
	
    private LoaderManager.LoaderCallbacks<RadarSite> mTaskFindSiteCallbacks =
    		new LoaderManager.LoaderCallbacks<RadarSite>() {
		@Override
		public Loader<RadarSite> onCreateLoader(int id, Bundle args) {
			return FindSite.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<RadarSite> loader, RadarSite radarSite) {
			mRadarSite = radarSite;
			mRadarView.setCenter(mRadarSite.center);
			
			loadRadar();
	        loadShapefiles(radarSite.center);
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
	
	private void reloadRadar() {
		Bundle args = LoadRadar.bundleArgs(mRadarSite);
		getSupportLoaderManager().restartLoader(TASK_LOAD_RADAR, args, mTaskLoadRadarCallbacks);
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
			
			mRadarView.setData(data);
			
			setSupportProgressBarIndeterminateVisibility(false);
			
			Prefs.setLastUpdateTime(new Date());
		}
		@Override
		public void onLoaderReset(Loader<DataFile> loader) {
			mRadarView.setData(null);
		}
    };
    
    private void loadShapefiles(LatLon center) {
    	if (mShapefiles != null) {
    		for (ShapefileInfo info : mShapefiles) {
				mRadarView.removeUnderlay(info.mUnderlay);
				mRadarView.removeOverlay(info.mOverlay);
			}
    	}
	
    	mShapefiles = new ArrayList<ShapefileInfo>();
    	{
        	//states
    		ShapefileConfig config = new ShapefileConfig(R.raw.states_shp, R.raw.states_dbf, R.raw.states_shx, ShapefileId.STATE_LINES);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f), 3.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f, 0.4f), 3.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.add(new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	//froyo can't read resources >1MB, like county lines
    	if (Build.VERSION.SDK_INT > 8)
    	{
        	//counties
    		ShapefileConfig config = new ShapefileConfig(R.raw.counties_shp, R.raw.counties_dbf, R.raw.counties_shx, ShapefileId.COUNTY_LINES);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f), 1.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f, 0.4f), 1.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.add(new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	for (int i = 0; i < mShapefiles.size(); i++) {
    		ShapefileInfo info = mShapefiles.get(i);
    		
    		ShapefileRenderData model = new ShapefileRenderData();
    		
    		LinearShapefileRenderable underlay = new LinearShapefileRenderable(model, info.mUnderlayRenderConfig);
    		LinearShapefileRenderable overlay = new LinearShapefileRenderable(model, info.mOverlayRenderConfig);
    		mRadarView.addUnderlay(underlay);
    		mRadarView.addOverlay(overlay);
    		info.mUnderlay = underlay;
    		info.mOverlay = overlay;
    		
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
    	int id = TASK_LOAD_SHAPEFILES + index;
    	LoadShapefile loader;
    	loader = LoadShapefile.castInstance(getSupportLoaderManager().initLoader(id, args, mTaskLoadShapefilesCallbacks));
    	loader.setUpdateThrottle(100);
    	
    	mRadarView.setViewpointListener(new ViewpointListener() {
			@Override
			public void onViewpointChanged(LatLon viewpoint) {
				reloadShapefiles(viewpoint);
			}
		});    	
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

			if (info.mUnderlay != null) {
				info.mUnderlay.setData(data);
			}
			if (info.mOverlay != null) {
				info.mOverlay.setData(data);
			}
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