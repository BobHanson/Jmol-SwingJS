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

import fr.orsay.lri.varna.models.rna.ModeleBP;

/**
 * @author ponty
 * 
 */
public class PSExport extends SecStrDrawingProducer {

	public PSExport()
	{
		super();
		super.setScale(0.4);
	}
	
	private String PSMacros() {
		String setFontSize =
		// Params [fontsize|...]
		"/setbasefont \n" + "{ /Helvetica-Bold findfont\n" + // =>
				// [font|scale|...]
				"  exch scalefont\n" + // => [scaled_font|...]
				"  setfont \n" + // => [...]
				"  } def\n\n";

		String writeTextCentered =
		// Params [txt|size|...]
		"/txtcenter \n" + "{ dup \n" + // => [txt|txt|size|...]
				"  stringwidth pop\n" + // => [wtxt|txt|size|...]
				"  2 div neg \n" + // => [-wtxt/2|txt|size|...]
				"  3 -1 roll \n" + // => [size|-wtxt/2|txt...]
				"  2 div neg\n" + // => [-size/2|-wtxt/2|txt|...]
				"  rmoveto\n" + // => [txt|...]
				"  show\n" + // => [...]
				"  } def\n\n";

		String drawEllipse = "/ellipse {\n" + "  /endangle exch def\n"
				+ "  /startangle exch def\n" + "  /yrad exch def\n"
				+ "  /xrad exch def\n" + "  /y exch def\n" + "  /x exch def\n"
				+ "  /savematrix matrix currentmatrix def\n"
				+ "  x y translate\n" + "  xrad yrad scale\n"
				+ "  0 0 1 startangle endangle arc\n"
				+ "  savematrix setmatrix\n" + "  } def\n\n";
		return setFontSize + writeTextCentered + drawEllipse;
	}

	private String EPSHeader(double minX, double maxX, double minY, double maxY) {
		String bbox = PSBBox(minX, minY, maxX, maxY);
		String init = "%!PS-Adobe-3.0\n" + "%%Pages: 1\n" + bbox
				+ "%%EndComments\n" + "%%Page: 1 1\n";
		String macros = PSMacros();
		return init + macros;
	}

	private String EPSFooter() {
		return "showpage\n" + "%%EndPage: 1\n" + "%%EOF";
	}

	private String PSNewPath() {
		return ("newpath\n");
	}

	private String PSMoveTo(double x, double y) {
		return ("" + x + " " + y + " moveto\n");
	}

	private String PSLineTo(double dx, double dy) {
		return ("" + dx + " " + dy + " lineto\n");
	}

	private String PSRLineTo(double dx, double dy) {
		return ("" + dx + " " + dy + " rlineto\n");
	}

	private String PSSetLineWidth(double thickness) {
		thickness /= 2;
		return ("" + thickness + " setlinewidth\n");
	}

	private String PSStroke() {
		return ("stroke\n");
	}

	private String PSArc(double x, double y, double radiusX, double radiusY,
			double angleFrom, double angleTo) {

		double centerX = x;
		double centerY = y;

		// return (centerX + " " + centerY + " "+ radiusX/2.0+" " + angleFrom +
		// " " + angleTo + "  arc\n");

		return (centerX + " " + centerY + " " + radiusX / 2.0 + " " + radiusY
				/ 2.0 + " " + angleTo + " " + angleFrom + " ellipse\n");

	}

	private String PSArc(double x, double y, double radius, double angleFrom,
			double angleTo) {

		return ("" + x + " " + y + " " + radius + " " + angleFrom + " "
				+ angleTo + "  arc\n");
	}

	private String PSBBox(double minX, double maxX, double minY, double maxY) {
		String norm = ("%%BoundingBox: " + (long) Math.floor(minX) + " "
				+ (long) Math.floor(minY) + " " + (long) Math.ceil(maxX) + " "
				+ (long) Math.ceil(maxY) + "\n");
		String high = ("%%HighResBoundingBox: " + (long) Math.floor(minX) + " "
				+ (long) Math.floor(minY) + " " + (long) Math.ceil(maxX) + " "
				+ (long) Math.ceil(maxY) + "\n");
		return norm + high;
	}

	private String PSText(String txt) {
		return ("(" + txt + ") ");
	}

	@SuppressWarnings("unused")
	private String PSShow() {
		return ("show\n");
	}

	private String PSClosePath() {
		return ("closepath\n");
	}

	private String PSFill() {
		return ("fill\n");
	}

	private String PSSetColor(Color col) {
		return ("" + (((double) col.getRed()) / 255.0) + " "
				+ (((double) col.getGreen()) / 255.0) + " "
				+ (((double) col.getBlue()) / 255.0) + " setrgbcolor\n");
	}

	private String fontName(int font) {
		switch (font) {
		case (FONT_TIMES_ROMAN):
			return "/Times-Roman";
		case (FONT_TIMES_BOLD):
			return "/Times-Bold";
		case (FONT_TIMES_ITALIC):
			return "/Times-Italic";
		case (FONT_TIMES_BOLD_ITALIC):
			return "/Times-BoldItalic";
		case (FONT_HELVETICA):
			return "/Helvetica";
		case (FONT_HELVETICA_BOLD):
			return "/Helvetica-Bold";
		case (FONT_HELVETICA_OBLIQUE):
			return "/Helvetica-Oblique";
		case (FONT_HELVETICA_BOLD_OBLIQUE):
			return "/Helvetica-BoldOblique";
		case (FONT_COURIER):
			return "/Courier";
		case (FONT_COURIER_BOLD):
			return "/Courier-Bold";
		case (FONT_COURIER_OBLIQUE):
			return "/Courier-Oblique";
		case (FONT_COURIER_BOLD_OBLIQUE):
			return "/Courier-BoldOblique";
		}
		return "/Helvetica";
	}

	private String PSSetFont(int font, double size) {
		return (fontName(font) + " findfont " + size + " scalefont setfont\n");
	}

	public String setFontS(int font, double size) {
		_fontsize = (long) (0.4 * size);
		return PSSetFont(font, _fontsize);
	}

	public String setColorS(Color col) {
		super.setColorS(col);
		String result = PSSetColor(col);
		return result;
	}

	public String drawLineS(Point2D.Double p0, Point2D.Double p1,
			double thickness) {
		String tmp = "";
		tmp += PSMoveTo(p0.x, p0.y);
		tmp += PSLineTo(p1.x, p1.y);
		tmp += PSSetLineWidth(thickness);
		tmp += PSStroke();
		return tmp;
	}

	public String drawTextS(Point2D.Double p, String txt) {
		String tmp = "";
		tmp += PSMoveTo(p.x, p.y);
		tmp += ("" + (_fontsize / 2.0 + 1) + " \n");
		tmp += PSText(txt);
		tmp += (" txtcenter\n");
		return tmp;
	}

	public String drawRectangleS(Point2D.Double orig, Point2D.Double dims,
			double thickness) {
		String tmp = PSNewPath();
		tmp += PSMoveTo(orig.x, orig.y);
		tmp += PSRLineTo(0, dims.y);
		tmp += PSRLineTo(dims.x, 0);
		tmp += PSRLineTo(0, -dims.y);
		tmp += PSClosePath();
		tmp += PSSetLineWidth(thickness);
		tmp += PSStroke();
		return tmp;
	}

	public String drawCircleS(Point2D.Double p, double radius, double thickness) {
		String tmp = PSNewPath();
		tmp += PSArc(p.x, p.y, radius, 0, 360);
		tmp += PSSetLineWidth(thickness);
		tmp += PSStroke();
		return tmp;
	}

	public String fillCircleS(Point2D.Double p, double radius,
			double thickness, Color color) {
		String tmp = PSNewPath();
		tmp += PSArc(p.x, p.y, radius, 0, 360);
		tmp += PSSetLineWidth(thickness);
		tmp += PSSetColor(color);
		tmp += PSFill();
		return tmp;
	}

	public String footerS() {
		return EPSFooter();
	}

	public String headerS(Rectangle2D.Double bb) {
		return EPSHeader(bb.x, bb.y, bb.x + bb.width, bb.y + bb.height);
	}

	@Override
	public String drawArcS(Point2D.Double origine, double width, double height,
			double startAngle, double endAngle) {
		return PSArc(origine.x, origine.y, width, height, startAngle, endAngle)
				+ PSStroke();
	}

	@Override
	public String drawPolygonS(Double[] points, double thickness) {
		String tmp = PSNewPath();
		tmp += PSSetLineWidth(thickness);
		for (int i = 0; i < points.length; i++) {
			if (i == 0) {
				tmp += PSMoveTo(points[i].x, points[i].y);
			} else {
				tmp += PSLineTo(points[i].x, points[i].y);
			}
		}
		tmp += PSClosePath();
		tmp += PSStroke();
		return tmp;
	}

	@Override
	public String fillPolygonS(Double[] points, Color color) {
		Color bck = _curColor;
		String tmp = PSNewPath();
		for (int i = 0; i < points.length; i++) {
			if (i == 0) {
				tmp += PSMoveTo(points[i].x, points[i].y);
			} else {
				tmp += PSLineTo(points[i].x, points[i].y);
			}
		}
		tmp += PSClosePath();
		tmp += PSSetColor(color);
		tmp += PSFill();
		tmp += PSSetColor(bck);
		return tmp;
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