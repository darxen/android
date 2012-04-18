package me.kevinwells.darxen.renderables;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import me.kevinwells.darxen.Renderable;
import me.kevinwells.darxen.data.DataFile;
import me.kevinwells.darxen.data.Description.OpMode;
import me.kevinwells.darxen.data.RadialDataPacket;
import me.kevinwells.darxen.data.RadialPacket;
import me.kevinwells.darxen.model.Buffers;
import me.kevinwells.darxen.model.Color;
import me.kevinwells.darxen.model.Palette;
import me.kevinwells.darxen.model.PaletteType;

public class RadarRenderable implements Renderable {
	
	private Palette mPalette;
	
	private FloatBuffer[] mRadialBuffers = new FloatBuffer[16];
	private int[] mRadialSize = new int[16];
	
	public RadarRenderable(DataFile data) {
		RadialDataPacket packet = (RadialDataPacket)data.description.symbologyBlock.packets[0];

		if (data.description.opmode == OpMode.PRECIPITATION)
			mPalette = new Palette(PaletteType.REFLECTIVITY_PRECIPITATION);
		else
			mPalette = new Palette(PaletteType.REFLECTIVITY_CLEAN_AIR);

		renderRadialData(packet);
	}
	
	@Override
	public void render(GL10 gl) {
		for (int i = 1; i < 16; i++) {
			Color c = mPalette.get(i);
			gl.glColor4f(c.r, c.g, c.b, c.a);
			FloatBuffer buf = mRadialBuffers[i];
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buf);
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mRadialSize[i]);
		}
	}

	private void renderRadialData(RadialDataPacket packet) {
		float kmPerRangeBin = 1.0f;
		
		for (int i = 1; i < 16; i++) {
			if (mRadialBuffers[i] == null) {
				TriangleBuffer buffer = new TriangleBuffer();
				for (int az = 0; az < packet.radials.length; az++) {
					RadialPacket radial = packet.radials[az];
					
					float start = 90.0f - (radial.start + radial.delta);
					float end = start + radial.delta;
					float cosx1 = (float)Math.cos(Math.toRadians(start));
					float siny1 = (float)Math.sin(Math.toRadians(start));
					float cosx2 = (float)Math.cos(Math.toRadians(end));
					float siny2 = (float)Math.sin(Math.toRadians(end));
					
					int startRange = 0;
					for (int range = 0; range < radial.codes.length; range++) {
						int color = radial.codes[range];
						if (startRange == 0 && color == i)
							startRange = range;
						
						if ((startRange != 0 && color < i) ||
								(startRange != 0 && (range == packet.rangeBinCount-1))) {
							if (color >= i && range == packet.rangeBinCount-1)
								range++;
							Vertex v1 = new Vertex(
									(startRange-1) * kmPerRangeBin * cosx1,
									(startRange-1) * kmPerRangeBin * siny1);
							Vertex v2 = new Vertex(
									(range-1) * kmPerRangeBin * cosx1,
									(range-1) * kmPerRangeBin * siny1);
							Vertex v3 = new Vertex(
									(range-1) * kmPerRangeBin * cosx2,
									(range-1) * kmPerRangeBin * siny2);
							Vertex v4 = new Vertex(
									(startRange-1) * kmPerRangeBin * cosx2,
									(startRange-1) * kmPerRangeBin * siny2);
							
							buffer.addTriangle(v1, v2, v3);
							buffer.addTriangle(v3, v4, v1);
							startRange = 0;
						}
					}
				}
				mRadialBuffers[i] = buffer.getBuffer();
				mRadialSize[i] = buffer.size();
			}
		}
	}
	
	public class Vertex {
		public float x;
		public float y;
		public Vertex(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public class TriangleBuffer {
		
		private class Triangle {
			public Vertex p1;
			public Vertex p2;
			public Vertex p3;
			public Triangle(Vertex p1, Vertex p2, Vertex p3) {
				this.p1 = p1;
				this.p2 = p2;
				this.p3 = p3;
			}
		}
		
		private ArrayList<Triangle> buffer;
		
		public TriangleBuffer() {
			buffer = new ArrayList<Triangle>();
		}
		
		public int size() {
			return buffer.size() * 3;
		}

		public void addTriangle(Vertex p1, Vertex p2, Vertex p3) {
			buffer.add(new Triangle(p1, p2, p3));
		}
		
		public FloatBuffer getBuffer() {
			FloatBuffer buf = Buffers.allocateFloat(buffer.size() * 2 * 3);
			
			for (Triangle t : buffer) {
				buf.put(t.p1.x);
				buf.put(t.p1.y);
				buf.put(t.p2.x);
				buf.put(t.p2.y);
				buf.put(t.p3.x);
				buf.put(t.p3.y);
			}
			buf.position(0);
			return buf;
		}
	}

}
