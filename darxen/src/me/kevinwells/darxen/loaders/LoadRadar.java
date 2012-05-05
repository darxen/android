package me.kevinwells.darxen.loaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.kevinwells.darxen.C;
import me.kevinwells.darxen.Prefs;
import me.kevinwells.darxen.RadarSite;
import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.Level3Parser;
import me.kevinwells.darxen.data.ParseException;
import me.kevinwells.darxen.model.RadarData;
import me.kevinwells.darxen.model.RadarDataModel;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

public class LoadRadar extends CachedAsyncLoader<RadarDataModel> {
	
	private static final String ARG_RADAR_SITE = "RadarSite";
	private static final String ARG_RADAR_DATA_MODEL = "RadarDataModel";
	
	public static Bundle bundleArgs(RadarSite radarSite, RadarDataModel model) {
		Bundle args = new Bundle();
		args.putParcelable(ARG_RADAR_SITE, radarSite);
		args.putParcelable(ARG_RADAR_DATA_MODEL, model);
		return args;
	}
	
	public static LoadRadar createInstance(Context context, Bundle args) {
		RadarSite radarSite = args.getParcelable(ARG_RADAR_SITE);
		RadarDataModel model = args.getParcelable(ARG_RADAR_DATA_MODEL);
		return new LoadRadar(context, radarSite, model);
	}
	
	public static LoadRadar getInstance(LoaderManager manager, int id) {
		Loader<RadarDataModel> res = manager.getLoader(id);
		return (LoadRadar)res;
	}
	
	private RadarSite mRadarSite;
	private RadarDataModel mModel;
	
	private LoadRadar(Context context, RadarSite radarSite, RadarDataModel model) {
		super(context);
		mRadarSite = radarSite;
		mModel = model;
	}

	@Override
	protected RadarDataModel doInBackground() {
		
		//initialize the FTP connection
		FTPClient ftpClient = null;
		do {
			try {
		    	ftpClient = openClient();
			} catch (IOException ex) {
				Log.e(C.TAG, "Failed to initialize FTP client", ex);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					return null;
				}
			}
		} while (ftpClient == null);  
		
		try {
			//get a listing of files
			List<FTPFile> files = null;
			do {
				try {
					files = getFileList(ftpClient);
				} catch (IOException ex) {
					Log.e(C.TAG, "Failed to download file list", ex);
					Thread.sleep(2000);
				}
			} while (files == null);
			
			//Download the most recent files
			int minIndex = files.size() - Math.min(Prefs.getInitialFrames(), mModel.getDataLimit()) - 1;
			for (int i = files.size() - 1; i > minIndex; i--) {
				if (Thread.interrupted())
					return null;
				
				FTPFile ftpFile = files.get(i);
				long time = ftpFile.getTimestamp().getTime().getTime();
				if (mModel.hasDataFile(time)) {
					//already downloaded this data, skip it
					continue;
				}
				
				//get the data
				byte[] data = null;
				do {
					try {
			        	data = getData(ftpClient, ftpFile.getName());
					} catch (IOException ex) {
						Log.e(C.TAG, "Failed to download radar imagery", ex);
						Thread.sleep(2000);
					}
				} while (data == null);
				
				//parse the data
				Level3Parser parser = new Level3Parser();
		        DataFile file;
		        try {
					file = parser.parse(new ByteArrayInputStream(data));
				} catch (ParseException e) {
					Log.e(C.TAG, "Failed to parse radar imagery", e);
					continue;
				} catch (IOException e) {
					Log.e(C.TAG, "Failed to parse radar imagery", e);
					continue;
				}
		        
		        //store the data
		        mModel.addDataFile(time, new RadarData(file));
			}
	        
		} catch (InterruptedException ex) {
			//we were cancelled, abort the load
			return null;
			
		} finally {
			//cleanup the client
			try {
		    	ftpClient.disconnect();
			} catch (IOException ex) {
				Log.e(C.TAG, "Failed to disconnect FTP client", ex);
			}
		}
        
        return mModel;
	}
		
	private FTPClient openClient() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect("tgftp.nws.noaa.gov", 21);
		if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode()))
			throw new IOException("Failed to connect");
		ftpClient.login("anonymous", "darxen");
		
		ftpClient.changeWorkingDirectory("SL.us008001/DF.of/DC.radar/DS.p19r0/SI." + mRadarSite.name.toLowerCase());
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.enterLocalPassiveMode();
		
		return ftpClient;
	}

	private List<FTPFile> getFileList(FTPClient ftpClient) throws IOException {
		//retrieve the file list
		List<FTPFile> files = new ArrayList<FTPFile>(Arrays.asList(ftpClient.listFiles()));
		
		//sort the file list by time
		Collections.sort(files, new Comparator<FTPFile>() {
			@Override
			public int compare(FTPFile lhs, FTPFile rhs) {
				long lhsTime = lhs.getTimestamp().getTime().getTime();
				long rhsTime = rhs.getTimestamp().getTime().getTime();
				
				if (lhsTime < rhsTime)
					return -1;
				if (lhsTime > rhsTime)
					return 1;
				return 0;
			}
		});
		
		//remove null entries and the 'latest' entry
		for (Iterator<FTPFile> it = files.iterator(); it.hasNext();) {
			FTPFile ftpFile = (FTPFile) it.next();
			if (ftpFile == null)
				it.remove();
			else if (ftpFile.getName() == null || "sn.last".equals(ftpFile.getName()))
				it.remove();
		}
		
		return files;
	}
	
	private byte[] getData(FTPClient ftpClient, String name) throws IOException {
    	ByteArrayOutputStream fout = new ByteArrayOutputStream();
        
		Log.d(C.TAG, "Downloading " + name);
		ftpClient.retrieveFile(name, fout);
		fout.close();
		
        return fout.toByteArray();
    }
}
