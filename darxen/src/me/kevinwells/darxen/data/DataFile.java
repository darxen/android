package me.kevinwells.darxen.data;

import java.io.IOException;

import android.os.Parcel;
import android.os.Parcelable;

public class DataFile implements Parcelable {
	
	public byte[] header;
	
	public MessageHeader messageHeader;
	
	public Description description;

	public DataFile() {
		
	}
	
	public static DataFile parse(DataFileStream stream) throws ParseException, IOException {
		DataFile res = new DataFile();
		
		res.header = new byte[27];
		stream.read(res.header);
		
		res.messageHeader = MessageHeader.parse(stream);
		res.description = Description.parse(stream);
		
		return res;
	}
	
	public boolean equals(DataFile other) {
		return header == other.header;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DataFile)
			return equals((DataFile)other);
		
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByteArray(header);
		//MessageHeader
		dest.writeParcelable(description, flags);
	}
	
	public DataFile(Parcel in) {
		header = in.createByteArray();
		description = in.readParcelable(Description.class.getClassLoader());
	}

	public static final Parcelable.Creator<DataFile> CREATOR = 
			new Parcelable.Creator<DataFile>() {
		@Override
		public DataFile createFromParcel(Parcel source) {
			return new DataFile(source);
		}
		@Override
		public DataFile[] newArray(int size) {
			return new DataFile[size];
		}
	};
}
