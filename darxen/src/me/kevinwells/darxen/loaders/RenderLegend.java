package me.kevinwells.darxen.loaders;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.model.Buffers;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.LegendRenderData;
import me.kevinwells.darxen.model.Palette;
import me.kevinwells.darxen.model.PaletteType;
import android.content.Context;
import android.os.Bundle;

public class RenderLegend extends CachedAsyncLoader<LegendRenderData> {

	private static final String ARG_DATA_FILE = "DataFile";
	private static final String ARG_DATA = "Data";
	
	public static Bundle bundleArgs(DataFile file, LegendRenderData data) {
		Bundle args = new Bundle();
		args.putSerializable(ARG_DATA_FILE, file);
		args.putParcelable(ARG_DATA, data);
		return args;
	}
	
	public static RenderLegend createInstance(Context context, Bundle args) {
		DataFile file = (DataFile)args.getSerializable(ARG_DATA_FILE);
		LegendRenderData data = args.getParcelable(ARG_DATA);
		return new RenderLegend(context, file, data);
	}
	
	private DataFile mFile;
	private LegendRenderData mData;
	
	private RenderLegend(Context context, DataFile file, LegendRenderData data) {
		super(context);
		mFile = file;
		mData = data;
	}

	@Override
	protected LegendRenderData doInBackground() {
		
		if (mData.getPalette() == null) {
			Palette palette;
			
			switch (mFile.description.opmode) {
			case PRECIPITATION:
				palette = new Palette(PaletteType.REFLECTIVITY_PRECIPITATION);
				break;
				
			case CLEAN_AIR:
				palette = new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
				break;
				
			case MAINTENANCE:
				palette = new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
				break;
				
			default:
				palette = new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
				break;
			}
			
			mData.setPalette(palette);
		}
		
		render(mData.getPalette());
		
		return mData;
	}
	
	private static final float BORDER = 0.005f;
	private static final float WIDTH = 0.7f;
	private static final float HEIGHT = 0.05f;
	
	private void render(Palette palette) {
		RectangleBuffer buffer = new RectangleBuffer();
		
		buffer.addRectangle(new Vertex(0.0f, 0.0f), new Vertex(WIDTH, HEIGHT), new Color(0.0f, 0.0f, 0.0f, 1.0f));
		
		float borderTotal = 16 * BORDER;
		float boxWidth = WIDTH - borderTotal;
		
		for (int i = 0; i < 15; i++) {
			Color c = palette.get(i+1);
			
			float xStart = (i / 15.0f) * boxWidth + (i+1) * BORDER;
			float xEnd = ((i+1) / 15.0f) * boxWidth + (i+1) * BORDER;
			
			Vertex v1 = new Vertex(xStart, BORDER);
			Vertex v2 = new Vertex(xEnd, HEIGHT-BORDER);
			buffer.addRectangle(v1, v2, c);
		}
		mData.setBuffers(buffer.getVertBuffer(), buffer.getColorBuffer(), buffer.size());
	}
	
	public class Vertex {
		public float x;
		public float y;
		public Vertex(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public class RectangleBuffer {
		
		private class Rectangle {
			public Vertex p1;
			public Vertex p2;
			public Color c;
			public Rectangle(Vertex p1, Vertex p2, Color c) {
				this.p1 = p1;
				this.p2 = p2;
				this.c = c;
			}
		}
		
		private ArrayList<Rectangle> buffer;
		
		public RectangleBuffer() {
			buffer = new ArrayList<Rectangle>();
		}
		
		public int size() {
			return buffer.size() * 6;
		}

		public void addRectangle(Vertex p1, Vertex p2, Color c) {
			buffer.add(new Rectangle(p1, p2, c));
		}
		
		public FloatBuffer getVertBuffer() {
			FloatBuffer buf = Buffers.allocateFloat(buffer.size() * 2 * 6);
			
			for (Rectangle t : buffer) {
				buf.put(t.p1.x);
				buf.put(t.p1.y);
				
				buf.put(t.p2.x);
				buf.put(t.p1.y);
				
				buf.put(t.p2.x);
				buf.put(t.p2.y);
				
				buf.put(t.p2.x);
				buf.put(t.p2.y);
				
				buf.put(t.p1.x);
				buf.put(t.p2.y);
				
				buf.put(t.p1.x);
				buf.put(t.p1.y);
			}
			buf.position(0);
			return buf;
		}
		
		public FloatBuffer getColorBuffer() {
			FloatBuffer colorBuf = Buffers.allocateFloat(buffer.size() * 4 * 6);
			
			for (Rectangle t : buffer) {
				for (int i = 0; i < 6; i++) {
					colorBuf.put(t.c.r);
					colorBuf.put(t.c.g);
					colorBuf.put(t.c.b);
					colorBuf.put(t.c.a);
				}
			}
			colorBuf.position(0);
			return colorBuf;
		}
	}

}
