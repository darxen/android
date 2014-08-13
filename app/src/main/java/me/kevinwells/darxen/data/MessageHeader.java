package me.kevinwells.darxen.data;

import java.io.IOException;
import java.io.Serializable;

public class MessageHeader implements Serializable {

	private static final long serialVersionUID = 4479128695856022253L;

	@SuppressWarnings("unused")
	public static MessageHeader parse(DataFileStream stream) throws ParseException, IOException {
		MessageHeader res = new MessageHeader();
		stream.skip(3);
		
		short messageCode = stream.readShort();
		short date = stream.readShort();
		int time = stream.readInt();
		int length = stream.readInt();
		short source = stream.readShort();
		short dest = stream.readShort();
		short numBlocks = stream.readShort();
		
		return res;
	}

}
