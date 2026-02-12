package fr.orsay.lri.varna.models.geom;


import java.awt.geom.Point2D;

/**
 * This class implements a cubic Bezier curve
 * with a constant speed parametrization.
 * The Bezier curve is approximated by a sequence of n straight lines,
 * where the n+1 points between the lines are
 * { B(k/n), k=0,1,...,n } where B is the standard
 * parametrization given here:
 * http://en.wikipedia.org/wiki/Bezier_curve#Cubic_B.C3.A9zier_curves
 * You can then use the constant speed parametrization over this sequence
 * of straight lines.
 * 
 * @author Raphael Champeimont
 */
public class CubicBezierCurve {
	
	/**
	 * The four points defining the curve.
	 */
	private Point2D.Double P0, P1, P2, P3;
	

	
	private int n;
	/**
	 * The number of lines approximating the Bezier curve.
	 */
	public int getN() {
		return n;
	}
	
	
	/**
	 * Get the (exact) length of the approximation curve.
	 */
	public double getApproxCurveLength() {
		return lengths[n-1];
	}
	
	
	
	/**
	 * The n+1 points between the n lines.
	 */
	private Point2D.Double[] points;
	
	
	
	/**
	 * Array of length n.
	 * lengths[i] is the sum of lengths of lines up to and including the
	 * line starting at point points[i]. 
	 */
	private double[] lengths;
	
	
	/**
	 * Array of length n.
	 * The vectors along each line, with a norm of 1.
	 */
	private Point2D.Double[] unitVectors; 
	
	
	
	/**
	 * The standard exact cubic Bezier curve parametrization.
	 * Argument t must be in [0,1].
	 */
	public Point2D.Double standardParam(double t) {
		double x = Math.pow(1-t,3) * P0.x
                 + 3 * Math.pow(1-t,2) * t * P1.x
                 + 3 * (1-t) * t * t * P2.x
                 + t * t * t * P3.x;
		double y = Math.pow(1-t,3) * P0.y
                 + 3 * Math.pow(1-t,2) * t * P1.y
                 + 3 * (1-t) * t * t * P2.y
                 + t * t * t * P3.y;
		return new Point2D.Double(x, y);
	}
	
	
	

	
	/**
	 * Uniform approximated parameterization.
	 * A value in t must be in [0, getApproxCurveLength()].
	 * We have built a function f such that f(t) is the position of
	 * the point on the approximation curve (n straight lines).
	 * The interesting property is that the length of the curve
	 * { f(t), t in [0,l] } is exactly l.
	 * The java function is simply the application of f over each element
	 * of a sorted array, ie. uniformParam(t)[k] = f(t[k]).
	 * Computation time is O(n+m) where n is the number of lines in which
	 * the curve is divided and m is the length of the array given as an
	 * argument. The use of a sorted array instead of m calls to the
	 * function enables us to have a complexity of O(n+m) instead of O(n*m)
	 * because we don't need to search in all the n possible lines for
	 * each value in t (as we know their are in increasing order).
	 */
	public Point2D.Double[] uniformParam(double[] t) {
		int m = t.length;
		Point2D.Double[] result = new Point2D.Double[m];
		int line = 0;
		for (int i=0; i<m; i++) {
			while ((line<n) && (lengths[line] < t[i])) {
				line++;
			}
			if (line >= n) {
				// In theory should not happen, but float computation != math.
				line = n-1;
			}
			if (t[i] < 0) {
				throw (new IllegalArgumentException("t[" + i + "] < 0"));
			}
			// So now we know on which line we are
			double lengthOnLine = t[i] - (line != 0 ? lengths[line-1] : 0);
			double x = points[line].x + unitVectors[line].x * lengthOnLine;
			double y = points[line].y + unitVectors[line].y * lengthOnLine;
			result[i] = new Point2D.Double(x, y);
		}
		return result;
	}
	
	
	
	/**
	 * A Bezier curve can be defined by four points,
	 * see http://en.wikipedia.org/wiki/Bezier_curve#Cubic_B.C3.A9zier_curves
	 * Here we give this four points and a integer to say in how many
	 * line segments we want to cut the Bezier curve (if n is bigger 
	 * the computation takes longer but the precision is better).
	 * The number of lines must be at least 1.
	 */
	public CubicBezierCurve(
			Point2D.Double P0,
			Point2D.Double P1,
			Point2D.Double P2,
			Point2D.Double P3,
			int n) {
		this.P0 = P0;
		this.P1 = P1;
		this.P2 = P2;
		this.P3 = P3;
		this.n = n;
		if (n < 1) {
			throw (new IllegalArgumentException("n must be at least 1"));
		}
		computeData();
	}

	
	private void computeData() {
		points = new Point2D.Double[n+1];
		for (int k=0; k<=n; k++) {
			points[k] = standardParam(((double) k) / n);
		}
		
		lengths = new double[n];
		unitVectors = new Point2D.Double[n];
		double sum = 0;
		for (int i=0; i<n; i++) {
			double l = lineLength(points[i], points[i+1]);
			double dx = (points[i+1].x - points[i].x) / l;
			double dy = (points[i+1].y - points[i].y) / l;
			unitVectors[i] = new Point2D.Double(dx, dy);
			sum += l;
			lengths[i] = sum;
		}
		

		
	}
	
	
	private double lineLength(Point2D.Double P1, Point2D.Double P2) {
		return P2.distance(P1);
	}
	
	
	public Point2D.Double getP0() {
		return P0;
	}

	public Point2D.Double getP1() {
		return P1;
	}

	public Point2D.Double getP2() {
		return P2;
	}

	public Point2D.Double getP3() {
		return P3;
	}




	
}
