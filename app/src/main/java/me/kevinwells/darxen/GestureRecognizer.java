package me.kevinwells.darxen;

import android.util.SparseArray;
import android.view.MotionEvent;

public class GestureRecognizer {
	
	private class PointerState {
		public float x;
		public float y;
		public PointerState(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private GestureSurface mSurface;
	private SparseArray<PointerState> mPointers;
	private Gesture mGesture;
	
	public GestureRecognizer(GestureSurface surface) {
		mSurface = surface;
		mPointers = new SparseArray<PointerState>();
	}

	public boolean onTouchEvent(MotionEvent e) {
		int action = e.getActionMasked();
		int pointer = e.getActionIndex();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mPointers.put(e.getPointerId(pointer), new PointerState(e.getX(pointer), e.getY(pointer)));
			mGesture = new SingleDragGesture();
			mGesture.down(e);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			mPointers.put(e.getPointerId(pointer), new PointerState(e.getX(pointer), e.getY(pointer)));
			if (e.getPointerCount() == 2)
				mGesture = new MultiDragGesture();
			mGesture.down(e);
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mPointers.remove(e.getPointerId(pointer));
			mGesture.up(e);
			if (e.getPointerCount() == 2)
				mGesture = new SingleDragGesture();
			break;
		case MotionEvent.ACTION_UP:
			mGesture.up(e);
			mGesture = null;
			mPointers.clear();
			break;
		case MotionEvent.ACTION_MOVE:
			boolean res = mGesture.move(e);
			for (int i = 0; i < e.getPointerCount(); i++) {
				PointerState state = mPointers.get(e.getPointerId(i));
				state.x = e.getX(i);
				state.y = e.getY(i);
			}
			return res;
		default:
			return false;
		}
		return true;
	}
	
	private abstract class Gesture {
		public abstract boolean move(MotionEvent e);
		public boolean down(MotionEvent e) {
			return false;
		}
		public boolean up(MotionEvent e) {
			return false;
		}
	}
	
	private class SingleDragGesture extends Gesture {
		private boolean mDown;
		private float mDownX;
		private float mDownY;
		
		private float TAP_THRESHOLD = 0.025f;
		
		@Override
		public boolean move(MotionEvent e) {
			PointerState prev = mPointers.get(e.getPointerId(0));
			float scaleFactor = (float)Math.min(mSurface.getWidth(), mSurface.getHeight()) / 2.0f;
			if (mDown) {
	    		float dx = (e.getX() - mDownX) / scaleFactor;
	    		float dy = -(e.getY() - mDownY) / scaleFactor;
	    		if (Math.abs(dx) > TAP_THRESHOLD || Math.abs(dy) > TAP_THRESHOLD) {
	    			mDown = false;
	    			mSurface.translate(dx, dy);
	    			mSurface.onTouchUpdate();
	    			return true;
	    		}
			} else {
	    		float dx = (e.getX() - prev.x) / scaleFactor;
	    		float dy = -(e.getY() - prev.y) / scaleFactor;
	    		mSurface.translate(dx, dy);
	    		mSurface.onTouchUpdate();
			}
    		return true;
		}
		@Override
		public boolean down(MotionEvent e) {
			mDown = true;
			mDownX = e.getX();
			mDownY = e.getY();
			return true;
		}
		@Override
		public boolean up(MotionEvent e) {
			if (!mDown) {
				return false;
			}
			
			float scaleFactor = (float)Math.min(mSurface.getWidth(), mSurface.getHeight()) / 2.0f;
			float dx = Math.abs((e.getX() - mDownX) / scaleFactor);
    		float dy = Math.abs((e.getY() - mDownY) / scaleFactor);
    		if (dx <= TAP_THRESHOLD && dy <= TAP_THRESHOLD) {
    			mSurface.onTap(e.getX(), e.getY());
    		}
			
			mDown = false;
			return true;
		}
	}
	
	private class MultiDragGesture extends Gesture {
		
		private Point2D findPrevCenter() {
			Point2D center = new Point2D();
			for (int i = 0; i < mPointers.size(); i++) {
				PointerState state = mPointers.valueAt(i);
				center.add(state.x, state.y);
			}
			center.divide(mPointers.size());
			return center;
		}
		
		Point2D findCurrentCenter(MotionEvent e) {
			Point2D center = new Point2D();
			for (int i = 0; i < e.getPointerCount(); i++) {
				center.add(e.getX(i), e.getY(i));
			}
			center.divide(e.getPointerCount());
			return center;
		}
		
		double findScale(MotionEvent e, Point2D prevCenter, Point2D currCenter) {
			double prevRadius = 0.0;
			for (int i = 0; i < mPointers.size(); i++) {
				PointerState state = mPointers.valueAt(i);
				prevRadius += prevCenter.distanceTo(state.x, state.y);
			}
			prevRadius /= mPointers.size();
			
			double currRadius = 0.0;
			for (int i = 0; i < e.getPointerCount(); i++) {
				currRadius += currCenter.distanceTo(e.getX(i), e.getY(i));
			}
			currRadius /= e.getPointerCount();
			
			return currRadius / prevRadius;
		}
		
		@Override
		public boolean move(MotionEvent e) {
			Point2D center = new Point2D(mSurface.getWidth()/2.0, mSurface.getHeight()/2.0);
			float scaleFactor = (float)Math.min(center.x, center.y);
			
			Point2D prevCenter = findPrevCenter();
			Point2D currCenter = findCurrentCenter(e);
			Point2D translate = currCenter.subtract(prevCenter, null).divide(scaleFactor);
			double scale = findScale(e, prevCenter, currCenter);
			
			mSurface.translate((float)translate.x, (float)-translate.y);
			
			currCenter.subtract(center).divide(scaleFactor);
			mSurface.translate((float)-currCenter.x, (float)currCenter.y);
			mSurface.scale((float)scale);
			mSurface.translate((float)currCenter.x, (float)-currCenter.y);
			
			mSurface.onTouchUpdate();
			
			return true;
		}
	}
}
