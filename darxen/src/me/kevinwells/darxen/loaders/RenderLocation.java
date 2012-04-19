package me.kevinwells.darxen.loaders;

import java.nio.FloatBuffer;

import me.kevinwells.darxen.model.Buffers;
import me.kevinwells.darxen.model.LocationRenderData;
import android.content.Context;
import android.os.Bundle;

public class RenderLocation extends CachedAsyncLoader<LocationRenderData> {

	private static final String ARG_DATA = "Data";
	
	public static Bundle bundleArgs(LocationRenderData data) {
		Bundle args = new Bundle();
		args.putParcelable(ARG_DATA, data);
		return args;
	}
	
	public static RenderLocation createInstance(Context context, Bundle args) {
		LocationRenderData data = args.getParcelable(ARG_DATA);
		return new RenderLocation(context, data);
	}
	
	private LocationRenderData mData;
	
	private RenderLocation(Context context, LocationRenderData data) {
		super(context);
		mData = data;
	}
	
	private static float RADIUS = 0.025f;
	private static int VERTICES = 8;

	@Override
	protected LocationRenderData doInBackground() {

		FloatBuffer crosshair = Buffers.allocateFloat(2 * 4);

		crosshair.put(new float[] {
				-RADIUS, 0.0f,
				RADIUS, 0.0f,
				0.0f, -RADIUS,
				0.0f, RADIUS
		});
		crosshair.position(0);
		
		FloatBuffer circle = Buffers.allocateFloat(2 * VERTICES);
		for (int i = 0; i < VERTICES; i++) {
			circle.put(RADIUS * (float)Math.cos(i * Math.PI * 2.0f / (float)VERTICES));
			circle.put(RADIUS * (float)Math.sin(i * Math.PI * 2.0f / (float)VERTICES));
		}
		circle.position(0);
		
		mData.setCrosshairBuffer(crosshair);
		mData.setCircleBuffer(circle, VERTICES);

		return mData;
	}
}
