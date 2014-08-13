package me.kevinwells.darxen.model;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Buffers {
	
	public static FloatBuffer allocateFloat(int count) {
		ByteBuffer vbb = ByteBuffer.allocateDirect(count * 4);
		vbb.order(ByteOrder.nativeOrder());
		return vbb.asFloatBuffer();
	}
	
	public static IntBuffer allocateInt(int count) {
		ByteBuffer vbb = ByteBuffer.allocateDirect(count * 4);
		vbb.order(ByteOrder.nativeOrder());
		return vbb.asIntBuffer();
	}
	
	public static void parcel(FloatBuffer buf, Parcel dest) {
		int size = buf.capacity();
		dest.writeInt(size);
		for (int i = 0; i < size; i++) {
			dest.writeFloat(buf.get(i));
		}
	}
	
	public static FloatBuffer unparcel(Parcel in) {
		int size = in.readInt();
		FloatBuffer buf = allocateFloat(size);
		for (int i = 0; i < size; i++) {
			buf.put(in.readFloat());
		}
		buf.position(0);
		return buf;
	}

}
