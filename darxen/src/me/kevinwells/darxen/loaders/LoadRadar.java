package me.kevinwells.darxen.loaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;

import me.kevinwells.darxen.C;
import me.kevinwells.darxen.RadarSite;
import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.Level3Parser;
import me.kevinwells.darxen.data.ParseException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class LoadRadar extends CachedAsyncLoader<DataFile> {
	
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
