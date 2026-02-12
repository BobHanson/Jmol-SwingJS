/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.geom;

public class ComputeArcCenter {
	
	/**
	 * Given an arc length (l) and segment length (delta) of the arc,
	 * find where to put the center, returned as a position of the perpendicular
	 * bisector of the segment. The positive side is the one where the arc is drawn.
	 * It works using Newton's method.
	 */
	public static double computeArcCenter(double delta, double l) {
		double x_n = 0;
		double x_n_plus_1, f_x_n, f_x_n_plus_1;
		f_x_n = f(x_n,delta);
		while (true) {
			x_n_plus_1 = x_n - (f_x_n - l)/fprime(x_n,delta);
			f_x_n_plus_1 = f(x_n_plus_1,delta);
			// We want a precision of 0.1 on arc length
			if (x_n_plus_1 == Double.NEGATIVE_INFINITY || Math.abs(f_x_n_plus_1 - f_x_n) < 0.1) {
				//System.out.println("computeArcCenter: steps = " + steps + "    result = " + x_n_plus_1);
				return x_n_plus_1;
			}
			x_n = x_n_plus_1;
			f_x_n = f_x_n_plus_1;
		}
	}
	
	private static double f(double c, double delta) {
		if (c < 0) {
			return 2*Math.atan(delta/(-2*c)) * Math.sqrt(delta*delta/4 + c*c);
		} else if (c != 0) { // c > 0
			return (2*Math.PI - 2*Math.atan(delta/(2*c))) * Math.sqrt(delta*delta/4 + c*c);
		} else { // c == 0
			return Math.PI * Math.sqrt(delta*delta/4 + c*c);
		}
	}
	
	/**
	 * d/dc f(c,delta)
	 */
	private static double fprime(double c, double delta) {
		if (c < 0) {
			return delta/(c*c + delta/4)*Math.sqrt(delta*delta/4 + c*c) + 2*Math.atan(delta/(-2*c))*c/Math.sqrt(delta*delta/4 + c*c);
		} else if (c != 0) { // c > 0
			return delta/(c*c + delta/4)*Math.sqrt(delta*delta/4 + c*c) + (2*Math.PI - 2*Math.atan(delta/(-2*c)))*c/Math.sqrt(delta*delta/4 + c*c);
		} else { // c == 0
			return 2;
		}
	}
}