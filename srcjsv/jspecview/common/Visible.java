/* Copyright (c) 2008-2014 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'visible.java' - module to predict colour of visible spectrum
// created July 2008 based on concept published by Darren L. Williams
// in J. Chem. Educ., 84(11), 1873-1877, 2007
//
// Judd-Vos-modified 1931 CIE 2-deg color matching functions (1978)
// The CIE standard observer functions were curve fitted using FitYK
// and the equations used for these calculations. The values obtained
// do not seem to vary appreciably from those published in the JChemEd article

package jspecview.common;

import java.lang.Math;

import javajs.util.CU;

import jspecview.api.VisibleInterface;

/**
 * Visible class - for prediction of colour from visible spectrum
 * 
 * see 
 * 
 * Robert J. Lancashire and Craig A.D. Walters
 * Colour prediction with JSpecView
 * http://www.chemmantis.com/Article.aspx?id=850
 * 
 * 
 * and
 * 
 * Darren L. Williams, Thomas J. Flaherty, Casie L. Jupe, 
 * Stephanie A. Coleman, Kara A. Marquez, and Jamie J. Stanton
 * Beyond [lambda]max: Transforming Visible Spectra into 24-Bit Color Values
 * J. Chem. Educ., 2007, 84 (11), p 1873 DOI: 10.1021/ed084p1873
 * http://pubs.acs.org/doi/abs/10.1021/ed084p1873
 * 
 * and
 * 
 * Michael Stokes (Hewlett-Packard), Matthew Anderson (Microsoft),
 * Srinivasan Chandrasekar (Microsoft), Ricardo Motta (Hewlett-Packard)
 * A Standard Default Color Space for the Internet: sRGB
 * Version 1.10, November 5, 1996 
 * http://www.color.org/sRGB.xalter
 * 
 * @author Bob Hanson
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class Visible implements VisibleInterface {

	public Visible() {
		// for reflection
	}

	/**
	 * Returns the integer color of a solution based on its visible spectrum.
	 * 
	 * @param spec
	 * @param useFitted
	 *          if true,  use curve fitted equations for CIE curves and every point; 
	 *          if false, use exact CIE 5-nm data and interpolated values
	 * @return 0xFFRRGGBB
	 * 
	 */
	@Override
	public int getColour(Spectrum spec, boolean useFitted) {
		Coordinate xyCoords[] = spec.getXYCoords();
		boolean isAbsorbance = spec.isAbsorbance();
		double[] xyzd = new double[4];

		// the spectrum has been checked to ensure that
		// (a) it has a nm axis
		// (b) it can be switched between absorbance and transmittance
		// (c) it is continuous
		// (d) it has more than 30 values
		// (e) it has a range at least 400 - 700 nm
		
		// Step 1. Determine the CIE tristimulus values

		//if (useFitted) {
			getXYZfitted(xyCoords, isAbsorbance, xyzd);
		//} else {
			//getXYZinterpolated(xyCoords, isAbsorbance, xyzd);
		//}
			
		xyzd[0] /= xyzd[3];
		xyzd[1] /= xyzd[3];
		xyzd[2] /= xyzd[3];
		//System.out.println(xyzd[0] + " " + xyzd[1] + " " + xyzd[2]);

		// Step 2. Transform XYZ to ICC standard RGB values.

		double rgb[] = new double[] { 
				xyzd[0] *  3.2410 + xyzd[1] * -1.5374 + xyzd[2] * -0.4986,
				xyzd[0] * -0.9692 + xyzd[1] *  1.8760 + xyzd[2] *  0.0416,
				xyzd[0] *  0.0556 + xyzd[1] * -0.2040 + xyzd[2] *  1.0570 };
		//System.out.println(rgb[0] + " " + rgb[1] + " " + rgb[2]);

		// Step 3. For CRT monitors, add gamma correction to the sRGB values.
		
		double gamma = 2.4;
		for (int i = 0; i < 3; i++)
			rgb[i] = (rgb[i] > 0.00304 ? 1.055 * Math.pow(rgb[i], 1 / gamma) - 0.055
							: 12.92 * rgb[i]);
		//System.out.println(rgb[0] + " " + rgb[1] + " " + rgb[2]);

		// Step 4. Convert gamma-corrected sRGB' to 8-bit (0-255) values.		
		// Step 5. Package as 0xFFRRGGBB
		
		int c = CU.rgb(fix(rgb[0]), fix(rgb[1]), fix(rgb[2]));
		//System.out.println(CU.colorPtFromInt(c));
		return c;

	}

	private static int fix(double d) {
		return (d <= 0 ? 0 : d >= 1 ? 255 : (int) Math.round(255 * d));
	}

	private static void getXYZfitted(Coordinate[] xyCoords, boolean isAbsorbance,
			double[] xyzd) {
		// Lancashire method -- using actual data and curve-fit CIE data
        // 1931 data used to match the J Chem Educ article
		// Approximate x-bar, y-bar, z-bar, and CIE D65 curves.

		double cie, xb, yb, zb;
		
		for (int i = xyCoords.length; --i >= 0;) {
			double x = xyCoords[i].getXVal();
			if (x < 400 || x > 700)
				continue;
			
			cie = gauss(85.7145, 2.05719E-5, x - 607.263)
					+ gauss(57.7256, 0.000126451, x - 457.096);
			xb = gauss(1.06561, 0.000500819, x - 598.623)
					+ gauss(0.283831, 0.00292745, x - 435.734)
					+ gauss(0.113771, 0.00192849, x - 549.271)
					+ gauss(0.239103, 0.00255944, x - 460.547);
			yb = gauss(0.239617, 0.00117296, x - 530.517)
					+ gauss(0.910377, 0.000300984, x - 565.635)
					+ gauss(0.0311013, 0.00152386, x - 463.833);
			zb = gauss(0.988366, 0.00220336, x - 456.345)
					+ gauss(0.381551, 0.000848554, x - 450.871)
					+ gauss(0.355693, 0.000628546, x - 470.668)
					+ gauss(0.81862, 0.00471059, x - 433.144);

			double y = xyCoords[i].getYVal();
			if (isAbsorbance)
				y = Math.pow(10, -Math.max(y, 0));
//			y = 1; // test for 255 255 255 gives 255 255 254
			xyzd[0] += y * xb * cie;
			xyzd[1] += y * yb * cie;
			xyzd[2] += y * zb * cie;
			xyzd[3] += yb * cie;
		}
	}

	private static double gauss(double a, double b, double x) {
		return a * Math.exp(-b * x * x);
	}

//	private static void getXYZinterpolated(Coordinate[] xyCoords, boolean isAbsorbance, double[] xyzd) {
//		// Williams method -- using 5-nm interpolations and actual CIE data
//		for (int i = cie1931_D65.length / 5; --i >= 0;) {
//			int pt = (i + 1) * 5;
//			double cie = cie1931_D65[--pt];
//			double zb = cie1931_D65[--pt];
//			double yb = cie1931_D65[--pt];
//			double xb = cie1931_D65[--pt];
//			double x = cie1931_D65[--pt];
//			double y = Coordinate.getYValueAt(xyCoords, x);
//			if (isAbsorbance)
//				y = Math.pow(10, -Math.max(y, 0));
//			//y = 1;
//			xyzd[0] += y * xb * cie;
//			xyzd[1] += y * yb * cie;
//			xyzd[2] += y * zb * cie;
//			xyzd[3] += yb * cie;			
//		}
//	}
//
//	private static double[] cie1931_D65 = {
//		// http://files.cie.co.at/204.xls
//		//  x_      y_       z_      D65
//		400,0.01431,0.000396,0.06785,82.7549,
//		405,0.02319,0.00064,0.1102,87.1204,
//		410,0.04351,0.00121,0.2074,91.486,
//		415,0.07763,0.00218,0.3713,92.4589,
//		420,0.13438,0.004,0.6456,93.4318,
//		425,0.21477,0.0073,1.03905,90.057,
//		430,0.2839,0.0116,1.3856,86.6823,
//		435,0.3285,0.01684,1.62296,95.7736,
//		440,0.34828,0.023,1.74706,104.865,
//		445,0.34806,0.0298,1.7826,110.936,
//		450,0.3362,0.038,1.77211,117.008,
//		455,0.3187,0.048,1.7441,117.41,
//		460,0.2908,0.06,1.6692,117.812,
//		465,0.2511,0.0739,1.5281,116.336,
//		470,0.19536,0.09098,1.28764,114.861,
//		475,0.1421,0.1126,1.0419,115.392,
//		480,0.09564,0.13902,0.81295,115.923,
//		485,0.05795,0.1693,0.6162,112.367,
//		490,0.03201,0.20802,0.46518,108.811,
//		495,0.0147,0.2586,0.3533,109.082,
//		500,0.0049,0.323,0.272,109.354,
//		505,0.0024,0.4073,0.2123,108.578,
//		510,0.0093,0.503,0.1582,107.802,
//		515,0.0291,0.6082,0.1117,106.296,
//		520,0.06327,0.71,0.07825,104.79,
//		525,0.1096,0.7932,0.05725,106.239,
//		530,0.1655,0.862,0.04216,107.689,
//		535,0.22575,0.91485,0.02984,106.047,
//		540,0.2904,0.954,0.0203,104.405,
//		545,0.3597,0.9803,0.0134,104.225,
//		550,0.43345,0.99495,0.00875,104.046,
//		555,0.51205,1,0.00575,102.023,
//		560,0.5945,0.995,0.0039,100,
//		565,0.6784,0.9786,0.00275,98.1671,
//		570,0.7621,0.952,0.0021,96.3342,
//		575,0.8425,0.9154,0.0018,96.0611,
//		580,0.9163,0.87,0.00165,95.788,
//		585,0.9786,0.8163,0.0014,92.2368,
//		590,1.0263,0.757,0.0011,88.6856,
//		595,1.0567,0.6949,0.001,89.3459,
//		600,1.0622,0.631,0.0008,90.0062,
//		605,1.0456,0.5668,0.0006,89.8026,
//		610,1.0026,0.503,0.00034,89.5991,
//		615,0.9384,0.4412,0.00024,88.6489,
//		620,0.85445,0.381,0.00019,87.6987,
//		625,0.7514,0.321,0.0001,85.4936,
//		630,0.6424,0.265,0.00005,83.2886,
//		635,0.5419,0.217,0.00003,83.4939,
//		640,0.4479,0.175,0.00002,83.6992,
//		645,0.3608,0.1382,0.00001,81.863,
//		650,0.2835,0.107,0,80.0268,
//		655,0.2187,0.0816,0,80.1207,
//		660,0.1649,0.061,0,80.2146,
//		665,0.1212,0.04458,0,81.2462,
//		670,0.0874,0.032,0,82.2778,
//		675,0.0636,0.0232,0,80.281,
//		680,0.04677,0.017,0,78.2842,
//		685,0.0329,0.01192,0,74.0027,
//		690,0.0227,0.00821,0,69.7213,
//		695,0.01584,0.005723,0,70.6652,
//		700,0.011359,0.004102,0,71.6091,
//	};
}
