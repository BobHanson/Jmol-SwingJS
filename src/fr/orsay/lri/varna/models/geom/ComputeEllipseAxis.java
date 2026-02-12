/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.geom;

public class ComputeEllipseAxis {
	public static boolean debug = false;
	
	/**
	 * Given one axis half-length b and the circumference l,
	 * find the other axis half-length a.
	 * Returns 0 if there is no solution.
	 * Implemented with Newton's method.
	 */
	public static double computeEllipseAxis(double b, double l) {
		if (l/4 <= b || b <= 0 || l <= 0) {
			// No such ellipse can exist.
			return 0;
		} else {
			int steps = 0;
			double x_n = 10;
			double x_n_plus_1, f_x_n, f_x_n_plus_1;
			f_x_n = f(x_n,b) - l;
			while (true) {
				//System.out.println("x_n = " + x_n + "  f(x_n)=" + f_x_n);
				x_n_plus_1 = x_n - f_x_n/fprime(x_n,b);
				f_x_n_plus_1 = f(x_n_plus_1,b) - l;
				if (x_n_plus_1 < 0) {
					System.out.println("ComputeEllipseAxis: x_n < 0 => returning 0");
					return 0;
				}
				// We want a precision of 0.01 on arc length
				if (Math.abs(f_x_n_plus_1 - f_x_n) < 0.01) {
					if (debug) System.out.println("#steps = " + steps);
					return x_n_plus_1;
				}
				x_n = x_n_plus_1;
				f_x_n = f_x_n_plus_1;
				steps++;
			}
		}
	}
	
	
	
	private static double f(double a, double b) {
		// This is Ramanujan's approximation of an ellipse circumference
		// Nice because it is fast to compute (no need to compute an integral)
		// and the derivative is also simple (and fast to compute).
		return Math.PI*(3*(a+b) - Math.sqrt(10*a*b + 3*(a*a + b*b)));
	}
	
	private static double fprime(double a, double b) {
		return Math.PI*(3 - (5*b + 3*a)/Math.sqrt(10*a*b + 3*(a*a + b*b)));
	}
	
	
	/*
	private static void test(double a, double b, double l) {
		double a2 = computeEllipseAxis(b, l);
		System.out.println("true a=" + a + " l=" + l + "   estimated a=" + a2 + " l(from true a)=" + f(a,b));
	}
	
	private static void test(double b, double l) {
		double a2 = computeEllipseAxis(b, l);
		System.out.println("true l=" + l + "   estimated a=" + a2);
	}
	
	
	public static void main(String[] args) {
		double b = 4;
		test(100, b, 401.3143);
		test(7, b, 35.20316);
		test(4, b, 25.13274);
		test(1, b, 17.15684);
		test(0.5, b, 16.37248);
		test(0.25, b, 16.11448);
		test(0.1, b, 16.02288);
		test(0.01, b, 16.00034);
		test(0, b, 16);
		test(10000, b, 40000.03);
	}
	*/
	
	/*
	public static void main(String[] args) {
		test(222.89291150684895, 2240);
	}
	*/
	
}
