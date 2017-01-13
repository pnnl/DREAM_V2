package utilities;

import java.util.Comparator;

/**
 * 2-dimensional double object
 * @author port091
 */

public class Point2d implements Comparable<Point2d> {

	private double x;
	private double y;

	public Point2d(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point2d(Point2d xyzPoint) {
		this.x = xyzPoint.getX();
		this.y = xyzPoint.getY();
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	/**
	 * Computes the Euclidean distance from this point to the other.
	 * 
	 * @param o1
	 *            other point.
	 * @return euclidean distance.
	 */
	public double euclideanDistance(Point2d o1) {
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
	private static final double euclideanDistance(Point2d o1, Point2d o2) {
		return Math.sqrt(Math.pow((o1.x - o2.x), 2) + Math.pow((o1.y - o2.y), 2));
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Point2d))
			return false;

		Point2d xyzPoint = (Point2d) obj;
		return compareTo(xyzPoint) == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Point2d o) {
		int xComp = X_COMPARATOR.compare(this, o);
		if (xComp != 0)
			return xComp;
		int yComp = Y_COMPARATOR.compare(this, o);
		return yComp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(Constants.decimalFormat.format(x)).append(", ");
		builder.append(Constants.decimalFormat.format(y));
		builder.append(")");
		return builder.toString();
	}

	static final Comparator<Point2d> X_COMPARATOR = new Comparator<Point2d>() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Point2d o1, Point2d o2) {
			if (o1.x < o2.x)
				return -1;
			if (o1.x > o2.x)
				return 1;
			return 0;
		}
	};

	static final Comparator<Point2d> Y_COMPARATOR = new Comparator<Point2d>() {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Point2d o1, Point2d o2) {
			if (o1.y < o2.y)
				return -1;
			if (o1.y > o2.y)
				return 1;
			return 0;
		}
	};
}