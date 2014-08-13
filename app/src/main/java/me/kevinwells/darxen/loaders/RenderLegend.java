package me.kevinwells.darxen.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.model.Buffers;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.LegendRenderData;
import me.kevinwells.darxen.model.Palette;
import me.kevinwells.darxen.model.PaletteType;
import me.kevinwells.darxen.model.RadarData;

public class RenderLegend extends CachedAsyncLoader<LegendRenderData> {

	private static final String ARG_DATA = "Data";
	
	public static Bundle bundleArgs(RadarData data) {
		Bundle args = new Bundle();
		args.putParcelable(ARG_DATA, data);
		return args;
	}
	
	public static RenderLegend createInstance(Context context, Bundle args) {
		RadarData data = args.getParcelable(ARG_DATA);
		return new RenderLegend(context, data);
	}
	
	public static RenderLegend getInstance(LoaderManager manager, int id) {
		Loader<LegendRenderData> res = manager.getLoader(id);
		return (RenderLegend)res;
	}
	
	private RadarData mData;
	
	private RenderLegend(Context context, RadarData data) {
		super(context);
		mData = data;
	}
	
	public RadarData getData() {
		return mData;
	}

	@Override
	protected LegendRenderData doInBackground() {
		
		if (mData.getLegendRenderData() != null) {
			return mData.getLegendRenderData();
		}
		
		DataFile file = mData.getDataFile();
		LegendRenderData renderData = new LegendRenderData();
		
		//require a file to render
		if (file == null) {
			return renderData;
		}
	
		//set the palette
		Palette palette = getPalette(file);
		renderData.setPalette(palette);
		
		//render the legend
		render(palette, renderData);
		
		//cache the result and return
		mData.setLegendRenderData(renderData);
		return renderData;
	}
	
	private static Palette getPalette(DataFile file) {
		switch (file.description.opmode) {
		case PRECIPITATION:
			return new Palette(PaletteType.REFLECTIVITY_PRECIPITATION);
			
		case CLEAN_AIR:
			return new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
			
		case MAINTENANCE:
			return new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
			
		default:
			return new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);
		}
	}
	
	private static final float BORDER = 0.005f;
	private static final float WIDTH = 0.7f;
	private static final float HEIGHT = 0.05f;
	
	private void render(Palette palette, LegendRenderData renderData) {
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
		renderData.setBuffers(buffer.getVertBuffer(), buffer.getColorBuffer(), buffer.size());
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
