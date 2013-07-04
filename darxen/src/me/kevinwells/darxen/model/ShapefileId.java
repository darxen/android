package me.kevinwells.darxen.model;

import org.holoeverywhere.util.SparseArray;

public enum ShapefileId {
	RADAR_SITES,
	STATE_LINES,
	COUNTY_LINES;
	
	private static SparseArray<ShapefileId> types;
	
	static {
		types = new SparseArray<ShapefileId>();
		for (ShapefileId type : ShapefileId.values()) {
			types.put(type.ordinal(), type);
		}
	}
	
	public static ShapefileId fromOrdinal(int ordinal) {
		return types.get(ordinal);
	}
}
