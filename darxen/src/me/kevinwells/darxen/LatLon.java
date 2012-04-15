package me.kevinwells.darxen;

import android.os.Parcel;
import android.os.Parcelable;

public class LatLon implements Parcelable {
	
	public double lat;
	public double lon;

	public LatLon() {
	}
	
	public LatLon(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public double distanceTo(LatLon o) {
		final double R = 6371.0; //km
		double dLat = Math.toRadians(o.lat - lat);
		double dLon = Math.toRadians(o.lon - lon);
		double lat1 = Math.toRadians(lat);
		double lat2 = Math.toRadians(o.lat);
		
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return R * c;
	}
	
	public Point2D project(LatLon center, Point2D res) {
		if (res == null)
			res = new Point2D();
		
		Point3D geoCenter = geodeticToGeocentric(center);
		Point3D geoOffset = geodeticToGeocentric(this);
		
		Point3D local = projectToLocal(center, geoCenter, geoOffset);
		
		res.x = local.x;
		res.y = local.y;
		return res;
	}
	
	public static LatLon unproject(Point2D pt, LatLon center, LatLon res) {
		if (res == null)
			res = new LatLon();
		
		Point3D local = new Point3D(pt.x, pt.y, 0.0);
		
		Point3D geoCenter = geodeticToGeocentric(center);
		Point3D geocentric = projectFromLocal(local, center, geoCenter);
		Point3D geodetic = geocentricToGeodetic(geocentric);
		
		res.lat = geodetic.x;
		res.lon = geodetic.y;
		return res;
	}
	
	private static Point3D geodeticToGeocentric(LatLon pt) {
		final double a = 6378137.0;
		final double b = 6356752.314140;
		
		double lat = Math.toRadians(pt.lat);
		double lon = Math.toRadians(pt.lon);

		double ae = Math.acos(b / a);

		double height = 0.0;

		double inner = Math.sin(lat) * Math.sin(ae);

		double N = a / Math.sqrt(1.0 - inner * inner);

		double x = (N + height) * Math.cos(lat) * Math.cos(lon);
		double y = (N + height) * Math.cos(lat) * Math.sin(lon);
		double z = (Math.pow(Math.cos(ae), 2.0) * N + height) * Math.sin(lat);

		//to km
		x /= 1000.0;
		y /= 1000.0;
		z /= 1000.0;

		return new Point3D(x, y, z);
	}

	private static Point3D geocentricToGeodetic(Point3D geocentric) {
		double x = geocentric.x * 1000.0;
		double y = geocentric.y * 1000.0;
		double z = geocentric.z * 1000.0;

		double lat;
		double lon;
		double ht;

		//WGS84
		double equatorial_radius = 6378137.0;
		double eccentricity_squared = 0.0066943799901413800;

		double rho, c, s, ct2, e1, e2a;

		e1 = 1.0 - eccentricity_squared;
		e2a = eccentricity_squared * equatorial_radius;

		rho = Math.sqrt(x * x + y * y);

		if (z == 0.0) {
			if (rho < e2a) {
				ct2 = rho * rho * e1 / (e2a * e2a - rho * rho);
				c = Math.sqrt(ct2 / (1.0 + ct2));
				s = Math.sqrt(1.0 / (1.0 + ct2));
			} else {
				c = 1.0;
				s = 0.0;
			}

			lat = 0.0;
		} else {
			double ct, new_ct, zabs;
			double f, new_f, df_dct, e2;

			zabs = Math.abs(z);

			new_ct = rho / zabs;
			new_f = Double.MAX_VALUE;

			do {
				ct = new_ct;
				f = new_f;

				e2 = Math.sqrt(e1 + ct * ct);

				new_f = rho - zabs * ct - e2a * ct / e2;

				if (new_f == 0.0)
					break;

				df_dct = -zabs - (e2a * e1) / (e2 * e2 * e2);

				new_ct = ct - new_f / df_dct;

				if (new_ct < 0.0)
					new_ct = 0.0;
			} while (Math.abs(new_f) < Math.abs(f));

			s = 1.0 / Math.sqrt(1.0 + ct * ct);
			c = ct * s;

			if (z < 0.0) {
				s = -s;
				lat = -Math.atan(1.0 / ct);
			} else {
				lat = Math.atan(1.0 / ct);
			}
		}

		lon = Math.atan2(y, x);

		ht = rho * c + z * s - equatorial_radius
				* Math.sqrt(1.0 - eccentricity_squared * s * s);
		
		if (lon > Math.PI)
			lon -= Math.PI*2;

		if (lon < -Math.PI)
			lon += Math.PI*2;
		
		return new Point3D(Math.toDegrees(lat), Math.toDegrees(lon), ht);

	}

	private static Point3D projectToLocal(LatLon center, Point3D geoCenter, Point3D geoOffset) {
		double offsetX = geoOffset.x - geoCenter.x;
		double offsetY = geoOffset.y - geoCenter.y;
		double offsetZ = geoOffset.z - geoCenter.z;

		double geodeticCenterLat = Math.toRadians(center.lat);
		double geodeticCenterLon = Math.toRadians(center.lon);
		
		double resX = 	offsetX * -Math.sin(geodeticCenterLon) +
						offsetY * Math.cos(geodeticCenterLon);
		double resY = 	offsetX * -Math.sin(geodeticCenterLat) * Math.cos(geodeticCenterLon) +
						offsetY * -Math.sin(geodeticCenterLat) * Math.sin(geodeticCenterLon) +
						offsetZ * Math.cos(geodeticCenterLat);
		double resZ = 	offsetX * Math.cos(geodeticCenterLat) * Math.cos(geodeticCenterLon) +
						offsetY * Math.cos(geodeticCenterLat) * Math.sin(geodeticCenterLon) +
						offsetZ * Math.sin(geodeticCenterLat);
		
		return new Point3D(resX, resY, resZ);
	}
	
	private static Point3D projectFromLocal(Point3D local, LatLon center, Point3D geoCenter) {
		double offsetX = geoCenter.x;
		double offsetY = geoCenter.y;
		double offsetZ = geoCenter.z;

		double geodeticCenterLat = Math.toRadians(center.lat);
		double geodeticCenterLon = Math.toRadians(center.lon);
		
		//latitude = theta
		//longitude = lambda
		
		double resX =	local.x * -Math.sin(geodeticCenterLon) +
						local.y * -Math.sin(geodeticCenterLat) * Math.cos(geodeticCenterLon) +
						local.z * Math.cos(geodeticCenterLat) * Math.cos(geodeticCenterLon);
		double resY =	local.x * Math.cos(geodeticCenterLon) +
						local.y * -Math.sin(geodeticCenterLat) * Math.sin(geodeticCenterLon) +
						local.z * Math.cos(geodeticCenterLat) * Math.sin(geodeticCenterLon);
		double resZ =	local.y * Math.cos(geodeticCenterLat) +
						local.z * Math.sin(geodeticCenterLat);
		
		return new Point3D(resX + offsetX, resY + offsetY, resZ + offsetZ);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeDouble(lat);
		parcel.writeDouble(lon);
	}

	public LatLon(Parcel in) {
		lat = in.readDouble();
		lon = in.readDouble();
	}
	
	public static final Parcelable.Creator<LatLon> CREATOR =
			new Parcelable.Creator<LatLon>() {
		@Override
		public LatLon createFromParcel(Parcel source) {
			return new LatLon(source);
		}

		@Override
		public LatLon[] newArray(int size) {
			return new LatLon[size];
		}
	};
}
