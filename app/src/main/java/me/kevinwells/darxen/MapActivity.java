package me.kevinwells.darxen;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.fragments.EnableLocationDialog;
import me.kevinwells.darxen.fragments.RequestSiteDialog;
import me.kevinwells.darxen.fragments.RequestSiteDialog.RequestSiteDialogListener;
import me.kevinwells.darxen.fragments.SwitchRadarSiteDialog;
import me.kevinwells.darxen.fragments.SwitchRadarSiteDialog.SwitchRadarSiteDialogListener;
import me.kevinwells.darxen.loaders.LoadRadar;
import me.kevinwells.darxen.loaders.LoadShapefile;
import me.kevinwells.darxen.loaders.LoadSites;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.RadarData;
import me.kevinwells.darxen.model.RadarDataModel;
import me.kevinwells.darxen.model.RenderData;
import me.kevinwells.darxen.model.RenderModel;
import me.kevinwells.darxen.model.ShapefileConfig;
import me.kevinwells.darxen.model.ShapefileId;
import me.kevinwells.darxen.renderables.LinearShapefileRenderable;

public class MapActivity extends SherlockFragmentActivity implements RequestSiteDialogListener, SwitchRadarSiteDialogListener, MapTapListener {
	
	private TextView mTitle;
	private RadarView mRadarView;
	private RenderModel mModel;
    private LocationManager locationManager;
    private LocationListener locationListener;
    
    private ImageView btnFirst;
    private ImageView btnPrevious;
    private ImageView btnPlay;
    private ImageView btnNext;
    private ImageView btnLast;
    
    private LatLon mPosition;
    private ArrayList<RadarSite> mRadarSites;
	
    private RadarSite mRadarSite;
    private RadarDataModel mRadarData;

    private Map<ShapefileId, ShapefileInfo> mShapefiles;
        
    private static final int TASK_LOAD_SITES = 0;
    private static final int TASK_LOAD_RADAR = 1;
    private static final int TASK_LOAD_SHAPEFILES = 2;

    private static final String STATE_RADAR_SITE = "RadarSite";

    public static Intent createIntent(Context context, RadarSite site) {
    	Intent intent = new Intent(context, MapActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    	intent.putExtra(STATE_RADAR_SITE, site);
    	return intent;
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	//hide notification area
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.main);
        
        mPosition = null;
        mRadarSites = null;
        mRadarSite = null;
        mRadarData = null;
        mShapefiles = null;
        
        mRadarView = (RadarView)findViewById(R.id.radarview);
        mRadarView.setLoaderManager(getSupportLoaderManager());
        mRadarView.setMapTapCallbacks(this);
        mModel = mRadarView.getModel();
        
        mTitle = (TextView)findViewById(R.id.title);
        
        btnFirst = (ImageView)findViewById(R.id.btnFirst);
        btnFirst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnFirst_clicked();
			}
		});
        
        btnPrevious = (ImageView)findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnPrevious_clicked();
			}
		});
        
        btnPlay = (ImageView)findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnPlay_clicked();
			}
		});
        
        btnNext = (ImageView)findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnNext_clicked();
			}
		});
        
        btnLast = (ImageView)findViewById(R.id.btnLast);
        btnLast.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnLast_clicked();
			}
		});
        
        Prefs.unsetLastUpdateTime();
        
        Intent intent = getIntent();
        
        if (icicle != null) {
        	mRadarSite = icicle.getParcelable(STATE_RADAR_SITE);
        } else if (intent.hasExtra(STATE_RADAR_SITE)) {
        	mRadarSite = intent.getParcelableExtra(STATE_RADAR_SITE);
        }
        
        loadSites();
    }

    @Override
	protected void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		
		icicle.putParcelable(STATE_RADAR_SITE, mRadarSite);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		boolean visible = mRadarData != null && mRadarData.getCount() > 1;
		
		if (visible) {
			boolean hasPrevious = mRadarData.hasPrevious();
			btnPrevious.setEnabled(hasPrevious);
			btnFirst.setEnabled(hasPrevious);
			
			boolean hasNext = mRadarData.hasNext();
			btnNext.setEnabled(hasNext);
			btnLast.setEnabled(hasNext);
			
			btnPlay.setImageResource(mRadarData.isAnimating() ? R.drawable.action_pause : R.drawable.action_play);
		}
		
		findViewById(R.id.animation).setVisibility(visible ? View.VISIBLE : View.GONE);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			update();
			break;
		case R.id.preferences:
			startActivity(PreferencesActivity.createIntent(this));
			break;
		default:
			return false;
		}
		return true;
	}
	
	private void btnFirst_clicked() {
		mRadarData.moveFirst();
	}
	
	private void btnPrevious_clicked() {
		mRadarData.movePrevious();
	}
	
	private void btnPlay_clicked() {
		if (mRadarData.isAnimating()) {
			mRadarData.stopAnimation();
		} else {
			mRadarData.startAnimation();
		}
		supportInvalidateOptionsMenu();
	}
	
	private void btnNext_clicked() {
		mRadarData.moveNext();
	}
	
	private void btnLast_clicked() {
		mRadarData.moveLast();
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
	
	private boolean updateLocation(Location location) {
		if (location == null)
			return false;

		unsetStatus();

		Log.v(C.TAG, location.toString());
		mPosition = new LatLon(location.getLatitude(), location.getLongitude());
		
		initSite();
		
		mRadarView.setLocation(mPosition);
		
		return true;
	}

	@Override
    protected void onResume() {
    	super.onResume();
    	
    	if (mRadarSite != null)
    		foundSite(mRadarSite);
    	
    	final Handler timer = new Handler();
    	final Runnable timeout = new Runnable() {
			@Override
			@SuppressWarnings("deprecation")
			public void run() {
				if (locationListener != null) {
					locationManager.removeUpdates(locationListener);
					locationListener = null;

					setStatus(R.string.status_wait_site);
					if (getSupportFragmentManager().findFragmentByTag(RequestSiteDialog.TAG) == null) {
						RequestSiteDialog.create(mRadarSites).show(getSupportFragmentManager(), RequestSiteDialog.TAG);
					}
				}
			}
		};
    	
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				timer.removeCallbacks(timeout);
				
				updateLocation(location);
			}

			@Override
			@SuppressWarnings("deprecation")
			public void onProviderDisabled(String provider) {
				if (getSupportFragmentManager().findFragmentByTag(EnableLocationDialog.TAG) == null) {
					new EnableLocationDialog().show(getSupportFragmentManager(), EnableLocationDialog.TAG);
				}
			}

			@Override
			public void onProviderEnabled(String provider) {
		        if (mRadarSite == null) {
		        	//only wait for so long
		        	timer.removeCallbacks(timeout);
		        	timer.postDelayed(timeout, 15000);
		        }
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		//determine which provider is available
		String provider = LocationManager.NETWORK_PROVIDER;
		if (locationManager.getProvider(provider) == null) {
			provider = LocationManager.GPS_PROVIDER;
			if (locationManager.getProvider(provider) == null) {
				Toast.makeText(this, R.string.location_required, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}

		if (mRadarSite == null) {
			setStatus(R.string.status_wait_location);
		}
		
        Location location = locationManager.getLastKnownLocation(provider);
        if (!updateLocation(location) && mRadarSite == null && locationManager.isProviderEnabled(provider)) {
        	//only wait for so long
        	timer.postDelayed(timeout, 15000);
        }
		
        locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
    	
    	mRadarView.onResume();
    	
    	loadSites();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (locationListener != null) {
    		locationManager.removeUpdates(locationListener);
    		locationListener = null;
    	}
    	mRadarView.setLocation(null);
    	
    	mRadarView.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	if (mRadarData != null) {
    		mRadarData.removeCallbacks(mRadarModelListener);
    		mRadarView.setRadarModel(null);
    	}
    }

	@Override
	public void onRequestSiteCancelled() {
		Toast.makeText(this, R.string.location_required, Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	public void onRequestSiteResult(RadarSite site) {
		unsetStatus();
		foundSite(site);
	}
	
	@Override
	public void onMapTap(LatLon latlon) {
		// Switch radar sites
		if (mRadarSites == null) {
			return;
		}
		
		findNewSite(latlon);
	}
    
    private void setStatus(final int res) {
    	this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getSupportActionBar().setSubtitle(res);
			}
		});
    }
    
    private void unsetStatus() {
    	this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getSupportActionBar().setSubtitle(null);
			}
		});
    }
    
    private void initSite() {
    	//need both the list of radar sites and a position to find a site
    	if (mRadarSites == null || mPosition == null)
    		return;
    	
    	//don't override the previously selected radar site
    	if (mRadarSite != null)
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

	private static final double SITE_DISTANCE_GPS = 300; //km
	private static final double SITE_DISTANCE_TAP = 20; //km
	
	private void findSite() {
		RadarSite result = RadarSite.Find(mRadarSites, mPosition, SITE_DISTANCE_GPS);
		if (result == null) {
			//TODO no close radar sites, ask the user to select one
			return;
		}
		foundSite(result);
	}
	
	@SuppressWarnings("deprecation")
	private void findNewSite(LatLon position) {
		if (mRadarSites == null) {
			return;
		}
		
		RadarSite radarSite = RadarSite.Find(mRadarSites, position, SITE_DISTANCE_TAP);
		
		if (radarSite == null) {
			return;
		}
		
		new SwitchRadarSiteDialog()
			.applyArguments(radarSite)
			.show(getSupportFragmentManager(), SwitchRadarSiteDialog.TAG);
		
	}

	@Override
	public void onSwitchRadarSite(RadarSite site) {
		//restart the activity with the selected radar site
		Intent intent = createIntent(this, site);
		overridePendingTransition(0, 0);
		getSupportLoaderManager().destroyLoader(TASK_LOAD_SHAPEFILES);
		getSupportLoaderManager().destroyLoader(TASK_LOAD_RADAR);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
	}
	
	private void foundSite(RadarSite radarSite) {
		mRadarSite = radarSite;
		RenderData data = mModel.getWritable();
		data.setCenter(mRadarSite.mCenter);
		mModel.commit();
		
		loadRadar();
        loadShapefiles(radarSite.mCenter);
        
        if (mPosition != null)
        	mRadarView.setLocation(mPosition);
	}

	private void loadRadar() {
		if (mRadarData == null) {
			mRadarData = new RadarDataModel();
			mRadarData.addCallbacks(mRadarModelListener);
			mRadarView.setRadarModel(mRadarData);
		}
		
		Bundle args = LoadRadar.bundleArgs(mRadarSite, mRadarData);
		getSupportLoaderManager().initLoader(TASK_LOAD_RADAR, args, mTaskLoadRadarCallbacks);
	}
	
	private void reloadRadar() {
		if (mRadarData == null) {
			mRadarData = new RadarDataModel();
			mRadarData.addCallbacks(mRadarModelListener);
			mRadarView.setRadarModel(mRadarData);
		}
		
		Bundle args = LoadRadar.bundleArgs(mRadarSite, mRadarData);
		getSupportLoaderManager().restartLoader(TASK_LOAD_RADAR, args, mTaskLoadRadarCallbacks);
		
		Prefs.setLastUpdateTime(new Date());
	}
	
    private LoaderManager.LoaderCallbacks<RadarDataModel> mTaskLoadRadarCallbacks =
    		new LoaderManager.LoaderCallbacks<RadarDataModel>() {
		@Override
		public Loader<RadarDataModel> onCreateLoader(int id, Bundle args) {
			return LoadRadar.createInstance(MapActivity.this, args);
		}
		@Override
		public void onLoadFinished(Loader<RadarDataModel> loader, RadarDataModel data) {
			if (data == null) {
				finish();
				return;
			}
			
			if (data != mRadarData) {
				mRadarData.removeCallbacks(mRadarModelListener);
				mRadarData = data;
				data.addCallbacks(mRadarModelListener);
				mRadarView.setRadarModel(mRadarData);
			}
			
			updateCurrentFrame();
			setSupportProgressBarIndeterminateVisibility(false);
		}
		@Override
		public void onLoaderReset(Loader<RadarDataModel> loader) {
		}
    };
    
    private void updateCurrentFrame() {
		RadarData data = mRadarData.getCurrentData();
		
		if (data == null) {
			return;
		}
		
		mTitle.setText(new String(data.getDataFile().header).replace("\n", ""));
		
		supportInvalidateOptionsMenu();
    }
    
    private RadarDataModel.RadarDataModelListener mRadarModelListener = new RadarDataModel.RadarDataModelListener() {
		@Override
		public void onUpdated() {
			supportInvalidateOptionsMenu();
		}
		@Override
		public void onCurrentChanged(long time) {
			updateCurrentFrame();
		}
		@Override
		public void onPastFrameAdded(long time) {
		}
	};
    
    private void loadShapefiles(LatLon center) {
    	//cleanup old shapefiles
    	if (mShapefiles != null) {
    		for (ShapefileInfo info : mShapefiles.values()) {
    			RenderData data = mModel.getWritable();
    			data.removeUnderlay(info.mUnderlay);
    			data.removeOverlay(info.mOverlay);
    			mModel.commit();
			}
    	}
	
    	//build current shapefiles
    	mShapefiles = new HashMap<ShapefileId, ShapefileInfo>();
    	
    	if (Prefs.isShapefileEnabled(ShapefileId.RADAR_SITES))
    	{
    		ShapefileConfig config = new ShapefileConfig(R.raw.radars_shp, R.raw.radars_dbf, R.raw.radars_shx, ShapefileId.RADAR_SITES);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(0.0f, 0.0f, 1.0f), 10.0f, GL10.GL_POINTS);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(0.0f, 0.0f, 1.0f, 0.4f), 10.0f, GL10.GL_POINTS);
    		mShapefiles.put(ShapefileId.RADAR_SITES, new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	if (Prefs.isShapefileEnabled(ShapefileId.STATE_LINES))
    	{
    		ShapefileConfig config = new ShapefileConfig(R.raw.states_shp, R.raw.states_dbf, R.raw.states_shx, ShapefileId.STATE_LINES);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f), 3.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(1.0f, 1.0f, 1.0f, 0.4f), 3.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.put(ShapefileId.STATE_LINES, new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	if (Prefs.isShapefileEnabled(ShapefileId.COUNTY_LINES))
    	{
    		ShapefileConfig config = new ShapefileConfig(R.raw.counties_shp, R.raw.counties_dbf, R.raw.counties_shx, ShapefileId.COUNTY_LINES);
    		ShapefileRenderConfig underRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f), 1.0f, GL10.GL_LINE_STRIP);
    		ShapefileRenderConfig overRenderConfig = new ShapefileRenderConfig(new Color(0.75f, 0.75f, 0.75f, 0.4f), 1.0f, GL10.GL_LINE_STRIP);
    		mShapefiles.put(ShapefileId.COUNTY_LINES, new ShapefileInfo(config, underRenderConfig, overRenderConfig));
    	}
    	
    	//remove old loaders
    	LoaderManager loaderManager = getSupportLoaderManager();
    	for (ShapefileId id : ShapefileId.values()) {
    		if (mShapefiles.containsKey(id))
    			continue;
    		
			int index = TASK_LOAD_SHAPEFILES + id.ordinal();
			if (loaderManager.getLoader(index) != null)
				loaderManager.destroyLoader(index);
		}
    	
    	//init new loaders
    	for (ShapefileInfo info : mShapefiles.values()) {
    		
    		ShapefileRenderData model = new ShapefileRenderData();
    		
    		LinearShapefileRenderable underlay = new LinearShapefileRenderable(model, info.mUnderlayRenderConfig);
    		LinearShapefileRenderable overlay = new LinearShapefileRenderable(model, info.mOverlayRenderConfig);

			RenderData data = mModel.getWritable();
			data.addUnderlay(underlay);
			data.addOverlay(overlay);
			mModel.commit();
    		info.mUnderlay = underlay;
    		info.mOverlay = overlay;
    		
			loadShapefile(center, info.mConfig, model);
		}
    }
    
    private void reloadShapefiles(LatLon viewpoint) {
    	for (ShapefileId id : mShapefiles.keySet()) {
    		int index = TASK_LOAD_SHAPEFILES + id.ordinal();
			LoadShapefile loader = LoadShapefile.getInstance(getSupportLoaderManager(), index);

			loader.setViewPoint(viewpoint);
			loader.startLoading();
		}
    }
    
    private void loadShapefile(LatLon center, ShapefileConfig config, ShapefileRenderData data) {
    	Bundle args = LoadShapefile.bundleArgs(center, config, data);
    	int id = TASK_LOAD_SHAPEFILES + config.id.ordinal();
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
			ShapefileId id = ShapefileId.fromOrdinal(loader.getId() - TASK_LOAD_SHAPEFILES);
			ShapefileInfo info = mShapefiles.get(id);

			if (info.mUnderlay != null) {
				info.mUnderlay.setData(data);
			}
			if (info.mOverlay != null) {
				info.mOverlay.setData(data);
			}
		}
		@Override
		public void onLoaderReset(Loader<ShapefileRenderData> loader) {
			ShapefileId id = ShapefileId.fromOrdinal(loader.getId() - TASK_LOAD_SHAPEFILES);
			if (!mShapefiles.containsKey(id))
				return;
			
			ShapefileInfo info = mShapefiles.get(id);
			
			RenderData data = mModel.getWritable();
			data.removeUnderlay(info.mUnderlay);
			data.removeOverlay(info.mOverlay);
			mModel.commit();
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
