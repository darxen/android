package me.kevinwells.darxen;

import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ArrayUtil {
	
	public static <T> T[] cast(Parcelable[] arr, Creator<T> CREATOR) {
		return cast(arr, CREATOR.newArray(arr.length));
	}
	
	@SuppressWarnings("unchecked")
	public static <T, V> T[] cast(V[] arr, T[] out) {
		for (int i = 0; i < arr.length; i++) {
			out[i] = (T) arr[i];
		}
		return out;
	}
}
