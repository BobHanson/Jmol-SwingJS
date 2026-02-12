/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.models.export;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.util.Hashtable;

import fr.orsay.lri.varna.models.rna.ModeleBP;

public class XFIGExport extends SecStrDrawingProducer {

	private int _font = SecStrDrawingProducer.FONT_TIMES_ROMAN;
	@SuppressWarnings("unused")
	private StringBuffer buf = new StringBuffer();

	private Hashtable<Color, Integer> _definedCols = new Hashtable<Color, Integer>();

	// From XFig 3.2 file format, indexed RGB colors are in the range [32,543]
	private int _nextColCode = 32;
	private final static int UPPER_BOUND_COLOR_CODE = 543;

	public XFIGExport()
	{
		super();
		super.setScale(20.0);
	}
	
	
	private String ensureColorDefinition(Color col) {
		if (!_definedCols.containsKey(col)) {
			if (_nextColCode < UPPER_BOUND_COLOR_CODE) {
				int curColorCode = _nextColCode;
				_definedCols.put(col, curColorCode);
				_nextColCode++;

				String RGBR = Integer.toHexString(col.getRed());
				if (RGBR.length() < 2) {
					RGBR = "0" + RGBR;
				}
				String RGBG = Integer.toHexString(col.getGreen());
				if (RGBG.length() < 2) {
					RGBG = "0" + RGBG;
				}
				String RGBB = Integer.toHexString(col.getBlue());
				if (RGBB.length() < 2) {
					RGBB = "0" + RGBB;
				}
				String RGBHex = "#" + RGBR + RGBG + RGBB;
				RGBHex = RGBHex.toUpperCase();
				return "0 " + curColorCode + " " + RGBHex + "\n";
			}
		}
		return "";
	}

	private int getColorCode(Color col) {
		if (_definedCols.containsKey(col)) {
			return _definedCols.get(col);
		}
		return 0;
	}

	private int getCurColorCode() {
		if (_definedCols.containsKey(_curColor)) {
			return _definedCols.get(_curColor);
		}
		return 0;
	}

	private String XFIGHeader() {
		return "#FIG 3.2\n" + "Landscape\n" + "Center\n" + "Inches\n"
				+ "Letter  \n" + "100.00\n" + "Single\n" + "-2\n" + "1200 2\n";
	}

	public String drawCircleS(Point2D.Double p, double radius, double thickness) {
		return ("1 3 0 " + (long) thickness + " " + getCurColorCode()
				+ " 7 50 -1 -1 0.000 1 0.0000 " + (long) p.x + " "
				+ (long) -p.y + " " + (long) radius + " " + (long) radius + " 1 1 1 1\n");
	}

	public String drawLineS(Point2D.Double p0, Point2D.Double p1,
			double thickness) {
		return ("2 1 0 " + (long) thickness + " " + getCurColorCode()
				+ " 7 60 -1 -1 0.000 0 0 -1 0 0 2\n" + " " + (long) p0.x + " "
				+ (long) -p0.y + " " + (long) p1.x + " " + (long) -p1.y + "\n");
	}

	public String drawRectangleS(Point2D.Double p, Point2D.Double dims,
			double thickness) {
		return ("2 2 0 " + (long) thickness + " " + getCurColorCode()
				+ " 7 50 -1 -1 0.000 0 0 -1 0 0 5\n" + "\t " + (long) (p.x)
				+ " " + (long) (-p.y) + " " + (long) (p.x + dims.x) + " "
				+ (long) (-p.y) + " " + (long) (p.x + dims.x) + " "
				+ (long) -(p.y + dims.y) + " " + (long) (p.x) + " "
				+ (long) -(p.y + dims.y) + " " + (long) (p.x) + " "
				+ (long) -(p.y) + "\n");
	}

	public String drawTextS(Point2D.Double p, String txt) {
		return ("4 1 " + getCurColorCode() + " 40 -1 " + _font + " "
				+ (long) _fontsize + " 0.0000 6 " + (long) 4 * _fontsize + " "
				+ (long) (2 * _fontsize) + " " + (long) (p.x) + " "
				+ (long) -(p.y - 6 * _fontsize) + " " + txt + "\\001\n");
	}

	public String fillCircleS(Point2D.Double p, double radius,
			double thickness, Color col) {
		String coldef = ensureColorDefinition(col);
		return (coldef + "1 3 0 " + (long) thickness + " 0 "
				+ getColorCode(col) + " 50 0 20 0.000 1 0.0000 " + (long) p.x
				+ " " + (long) -p.y + " " + (long) radius + " " + (long) radius + " 1 1 1 1\n");
	}

	public String setFontS(int font, double size) {
		_font = font;
		_fontsize = 1.2 * size;
		return "";
	}

	public String setColorS(Color col) {
		super.setColorS(col);
		return (ensureColorDefinition(col));
	}

	public String footerS() {
		return "";
	}

	public String headerS(Rectangle2D.Double bb) {
		return XFIGHeader();
	}

	@Override
	public String drawArcS(Point2D.Double origine, double width, double height,
			double startAngle, double endAngle) {
		double p1x = origine.x;
		double p1y = -origine.y;
		double p2x = origine.x + width / 2.0;
		double p2y = -origine.y - height / 2.0;
		double p3x = origine.x + width;
		double p3y = p1y;
		double cx = (p1x + p3x) / 2.0;
		double cy = p3y + height / 2.0;
		return ("5 1 0 1 " + getCurColorCode() + " 7 50 0 -1 4.000 0 0 0 0 "
				+ cx + " " + cy + " " + (int) p1x + " " + (int) p1y + " "
				+ (int) p2x + " " + (int) p2y + " " + (int) p3x + " "
				+ (int) p3y + "\n");

	}

	@Override
	public String drawPolygonS(Double[] points, double thickness) {
		if (points.length > 0) {
			String result = "2 3 0 1 " + getCurColorCode()
					+ " 7 40 0 -1 4.000 0 0 0 0 0 " + (points.length + 1)
					+ "\n";
			for (int i = 0; i < points.length; i++) {
				result += (int) Math.round(points[i].x) + " "
						+ (int) Math.round(-points[i].y) + " ";
			}
			result += (int) Math.round(points[0].x) + " "
					+ (int) Math.round(-points[0].y) + " ";
			result += "\n";
			return result;
		} else {
			return "";
		}
	}

	@Override
	public String fillPolygonS(Double[] points,  Color col) {
		if (points.length > 0) {
			String coldef = ensureColorDefinition(col);
			String result = "2 3 0 1 0 " + getColorCode(col)
					+ " 35 0 0 4.000 0 0 0 0 0 " + (points.length + 1) + "\n";
			for (int i = 0; i < points.length; i++) {
				result += (int) Math.round(points[i].x) + " "
						+ (int) Math.round(-points[i].y) + " ";
			}
			result += (int) Math.round(points[0].x) + " "
					+ (int) Math.round(-points[0].y) + " ";
			result += "\n";
			return coldef + result;
		} else {
			return "";
		}
	}

	@Override
	public String drawBaseStartS(int index) {
		return "";
	}

	@Override
	public String drawBaseEndS(int index) {
		return "";
	}

	@Override
	public String drawBasePairStartS(int i, int j, ModeleBP bps) {
		return "";
	}

	@Override
	public String drawBasePairEndS(int index) {
		return "";
	}

	@Override
	public String drawBackboneStartS(int i, int j) {
		return "";
	}

	@Override
	public String drawBackboneEndS(int index) {
		return "";
	}

}