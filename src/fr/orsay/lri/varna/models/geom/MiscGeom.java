/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.geom;

import java.awt.geom.Point2D;


/**
 * @author Raphael Champeimont
 * Misc geometry functions.
 */
public class MiscGeom {

	/**
	 * Compute the angle made by a vector.
	 */
	public static double angleFromVector(Point2D.Double v) {
		return MiscGeom.angleFromVector(v.x, v.y);
	}

	public static double angleFromVector(double x, double y) {
		double l = Math.hypot(x, y);
		if (y > 0) {
			return Math.acos(x / l);
		} else if (y < 0) {
			return - Math.acos(x / l);
		} else {
			return x > 0 ? 0 : Math.PI;
		}
	}

}
