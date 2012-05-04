package me.kevinwells.darxen.model;

import me.kevinwells.darxen.data.DataFile;
import android.os.Parcel;
import android.os.Parcelable;

public class RadarData implements Parcelable {
	
	private DataFile mDataFile;
	private RadarRenderData mRadarRenderData;
	private LegendRenderData mLegendRenderData;
	
	public RadarData(DataFile dataFile) {
		mDataFile = dataFile;
	}
	
	public DataFile getDataFile() {
		return mDataFile;
	}
	
	public RadarRenderData getRadarRenderData() {
		return mRadarRenderData;
	}
	
	public void setRadarRenderData(RadarRenderData renderData) {
		mRadarRenderData = renderData;
	}
	
	public LegendRenderData getLegendRenderData() {
		return mLegendRenderData;
	}
	
	public void setLegendRenderData(LegendRenderData renderData) {
		mLegendRenderData = renderData;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeSerializable(mDataFile);
		out.writeParcelable(mRadarRenderData, flags);
		out.writeParcelable(mLegendRenderData, flags);
	}

	public RadarData(Parcel in) {
		mDataFile = (DataFile)in.readSerializable();
		mRadarRenderData = in.readParcelable(RadarRenderData.class.getClassLoader());
		mLegendRenderData = in.readParcelable(LegendRenderData.class.getClassLoader());
	}
	
	public static final Parcelable.Creator<RadarData> CREATOR = 
			new Parcelable.Creator<RadarData>() {
		@Override
		public RadarData createFromParcel(Parcel source) {
			return new RadarData(source);
		}
		@Override
		public RadarData[] newArray(int size) {
			return new RadarData[size];
		}
	};
}
