package utilities;

import java.util.Comparator;

public class Point3d implements Comparable<Point3d> {

	private float x;
	private float y;
	private float z;

	public Point3d(float x, float y) {
		this.x = x;
		this.y = y;
		this.z = 0;
	}

	public Point3d(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point3d(Point3d xyzPoint) {
		this.x = xyzPoint.getX();
		this.y = xyzPoint.getY();
		this.z = xyzPoint.getZ();
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	/**
	 * Computes the Euclidean distance from this point to the other.
	 * 
	 * @param o1
	 *            other point.
	 * @return euclidean distance.
	 */
	public float euclideanDistance(Point3d o1) {
		return euclideanDistance(o1, this);
	}

	/**
	 * Computes the Euclidean distance from one point to the other.
	 * 
	 * @param o1
	 *            first point.
	 * @param o2
	 *            second point.
	 * @return euclidean distance.
	 */
	private static final float euclideanDistance(Point3d o1, Point3d o2) {
		return (float) Math.sqrt(Math.pow((o1.x - o2.x), 2) + Math.pow((o1.y - o2.y), 2) + Math.pow((o1.z - o2.z), 2));
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Point3d))
			return false;

		Point3d xyzPoint = (Point3d) obj;
		return compareTo(xyzPoint) == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Point3d o) {
		int xComp = X_COMPARATOR.compare(this, o);
		if (xComp != 0)
			return xComp;
		int yComp = Y_COMPARATOR.compare(this, o);
		if (yComp != 0)
			return yComp;
		int zComp = Z_COMPARATOR.compare(this, o);
		return zComp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(Constants.decimalFormat.format(x)).append(", ");
		builder.append(Constants.decimalFormat.format(y)).append(", ");
		builder.append(Constants.decimalFormat.format(z));
		builder.append(")");
		return builder.toString();
	}

	static final Comparator<Point3d> X_COMPARATOR = new Comparator<Point3d>() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Point3d o1, Point3d o2) {
			if (o1.x < o2.x)
				return -1;
			if (o1.x > o2.x)
				return 1;
			return 0;
		}
	};

	static final Comparator<Point3d> Y_COMPARATOR = new Comparator<Point3d>() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Point3d o1, Point3d o2) {
			if (o1.y < o2.y)
				return -1;
			if (o1.y > o2.y)
				return 1;
			return 0;
		}
	};

	static final Comparator<Point3d> Z_COMPARATOR = new Comparator<Point3d>() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Point3d o1, Point3d o2) {
			if (o1.z < o2.z)
				return -1;
			if (o1.z > o2.z)
				return 1;
			return 0;
		}
	};
}