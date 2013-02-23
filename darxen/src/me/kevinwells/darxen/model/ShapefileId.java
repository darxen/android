package me.kevinwells.darxen.model;

import java.util.HashMap;

public enum ShapefileId {
	RADAR_SITES,
	STATE_LINES,
	COUNTY_LINES;
	
	private static HashMap<Integer, ShapefileId> types;
	
	static {
		types = new HashMap<Integer, ShapefileId>();
		for (ShapefileId type : ShapefileId.values()) {
			types.put(type.ordinal(), type);
		}
	}
	
	public static ShapefileId fromOrdinal(int ordinal) {
		return types.get(ordinal);
	}
}
