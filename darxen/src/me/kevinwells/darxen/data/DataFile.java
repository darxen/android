package me.kevinwells.darxen.data;

import java.io.IOException;
import java.io.Serializable;

public class DataFile implements Serializable {

	private static final long serialVersionUID = -507221418483805389L;

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
}
