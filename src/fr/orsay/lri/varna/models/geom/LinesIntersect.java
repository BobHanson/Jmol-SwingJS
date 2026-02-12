/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.geom;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Like Line2D.Double.linesIntersect() of the standard library, but without the bug!
 * See this: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6457965
 * This result is incorrect with, for example:
 * System.out.println(Line2D.Double.linesIntersect(179.2690296114372, 1527.2309703885628, 150.9847583639753, 1498.946699141101, 94.4162158690515, 1442.378156646177, 66.1319446215896, 1414.0938853987152));
 * (real example observed on an RNA with no intersection at all)
 * This lines obviously don't intersect (both X and Y ranges are clearly disjoint!)
 * but linesIntersect() returns true.
 * Here we provide a bug-free function.
 */
public class LinesIntersect {
	/**
	 * Returns whether segment from (x1,y1) to (x2,y2)
	 * intersect with segment from (x3,y3) to (x4,y4).
	 */
	public static boolean linesIntersect(
			double x1,
            double y1,
            double x2,
            double y2,
            double x3,
            double y3,
            double x4,
            double y4) {
		
	      double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
	      if (denom == 0.0) { // Lines are parallel.
	         return false;
	      }
	      double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
	      double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
	      return (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0);
	}
	
	/**
	 * Returns whether segment from p1 to p2 intersects segment from p3 to p4.
	 */
	public static boolean linesIntersect(
			Point2D.Double p1,
			Point2D.Double p2,
			Point2D.Double p3,
			Point2D.Double p4) {
		return linesIntersect(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
	}
	
	/**
	 * Returns whether the two segment intersect.
	 */
	public static boolean linesIntersect(Line2D.Double line1, Line2D.Double line2) {
		return linesIntersect(
				line1.x1, line1.y1, line1.x2, line1.y2,
				line2.x1, line2.y1, line2.x2, line2.y2);
	}
	
	
	private static void test(
			double x1,
            double y1,
            double x2,
            double y2,
            double x3,
            double y3,
            double x4,
            double y4,
            boolean expectedResult) {
		boolean a = linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4);
		boolean b = Line2D.Double.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4);
		System.out.println("ours says " + a + " which is " + (a == expectedResult ? "correct" : "INCORRECT") +
				" / Line2D.Double says " + b + " which is " + (b == expectedResult ? "correct" : "INCORRECT"));
	}
	
	public static void main(String[] args) {
		test(179.2690296114372, 1527.2309703885628, 150.9847583639753, 1498.946699141101, 94.4162158690515, 1442.378156646177, 66.1319446215896, 1414.0938853987152, false);
		test(0, 0, 0, 0, 1, 1, 1, 1, false);
		test(0, 0, 0.5, 0.5, 1, 1, 2, 2, false);
		test(0, 0, 2, 2, 0, 2, 2, 0, true);
		test(0, 0, 2, 2, 4, 0, 3, 2, false);
	}
}
