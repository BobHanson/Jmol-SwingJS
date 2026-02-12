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

public class TikzExport extends SecStrDrawingProducer {


	public TikzExport()
	{
		super();
		super.setScale(0.2);
	}
	

	private String TikzHeader() {
		return   "\\documentclass[tikz,border=10pt]{standalone}\n"
				+"\\usepackage{tikz,relsize}\n"
				+"\\usetikzlibrary{positioning}\n"
				+"\\begin{document}\n"
				+"\\begin{tikzpicture}[inner sep=0, fill=none,draw=none,text=none,font={\\sf}]\n";
	}

	private String formatPoint(Point2D.Double p)
	{
		return formatPoint(p.x,p.y);
	}

	private String formatPoint(double x, double y)
	{
		return "("+(super.getScale()*x)+","+(super.getScale()*y)+")";
	}

	public String drawCircleS(Point2D.Double p, double radius, double thickness) {
		return "  \\draw[draw=currColor] "+formatPoint(p)+" circle ("+(radius*super.getScale())+");\n";
	}

	public String fillCircleS(Point2D.Double p, double radius,
			double thickness, Color col) {
		return setColorS(col)
			+"  \\fill[fill=currColor] "+formatPoint(p)+" circle ("+(radius*super.getScale())+");\n";
	}


	public String drawLineS(Point2D.Double p0, Point2D.Double p1,
			double thickness) {
		return "  \\draw[draw=currColor] "+formatPoint(p0)+" -- "+formatPoint(p1)+";\n";
	}

	public String drawRectangleS(Point2D.Double p, Point2D.Double dims,
			double thickness) {
		return "  \\draw[draw=currColor] "+formatPoint(p)+" -- "+formatPoint(p.x+dims.x,p.y)+" -- "+formatPoint(p.x+dims.x,p.y+dims.y)+" -- "+formatPoint(p.x,p.y+dims.y)+" -- "+formatPoint(p)+";\n";
	}

	public String drawTextS(Point2D.Double p, String txt) {
		return "  \\node[text=currColor] at "+formatPoint(p)+" {"+txt+"};\n";
	}

	public String setFontS(int font, double size) {
		_font = font;
		_fontsize = 1.2 * size;
		return "";
	}

	public String setColorS(Color col) {
		super.setColorS(col);
		return "\\definecolor{currColor}{rgb}{"+(((double)col.getRed())/255.)+","+(((double)col.getGreen())/255.)+","+(((double)col.getBlue())/255.)+"}\n";
	}

	public String footerS() {
		return "\\end{tikzpicture}\n"
			+  "\\end{document}";
	}

	public String headerS(Rectangle2D.Double bb) {
		return TikzHeader();
	}

	@Override
	public String drawArcS(Point2D.Double origine, double width, double height,
			double startAngle, double endAngle) {
		return "";

	}

	@Override
	public String drawPolygonS(Double[] points, double thickness) {
		if (points.length > 0) {
			String result = "\\draw[draw=currColor] ";
			for (int i = 0; i < points.length; i++) {
				result += ""+formatPoint(points[i])+" -- ";
			}
			result += ""+formatPoint(points[0])+";";
			return result;
		} else {
			return "";
		}
	}

	@Override
	public String fillPolygonS(Double[] points,  Color col) {
		if (points.length > 0) {
			String result = "\\fill[fill=currColor] ";
			for (int i = 0; i < points.length; i++) {
				result += ""+formatPoint(points[i])+" -- ";
			}
			result += ""+formatPoint(points[0])+";";
			return setColorS(col)
					+result;
		} else {
			return setColorS(col);
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