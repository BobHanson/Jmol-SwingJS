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
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import fr.orsay.lri.varna.models.rna.ModeleBP;

public abstract class SecStrDrawingProducer {
	public static final int FONT_TIMES_ROMAN = 0;
	public static final int FONT_TIMES_BOLD = 1;
	public static final int FONT_TIMES_ITALIC = 2;
	public static final int FONT_TIMES_BOLD_ITALIC = 3;
	public static final int FONT_HELVETICA = 16;
	public static final int FONT_HELVETICA_OBLIQUE = 17;
	public static final int FONT_HELVETICA_BOLD = 18;
	public static final int FONT_HELVETICA_BOLD_OBLIQUE = 19;
	public static final int FONT_COURIER = 12;
	public static final int FONT_COURIER_BOLD = 13;
	public static final int FONT_COURIER_OBLIQUE = 14;
	public static final int FONT_COURIER_BOLD_OBLIQUE = 15;

	private Vector<GraphicElement> _commands = new Vector<GraphicElement>();

	private double _scale = 1.0;
	private double _xmin = Double.MAX_VALUE;
	private double _ymin = Double.MAX_VALUE;
	private double _xmax = -Double.MAX_VALUE;
	private double _ymax = -Double.MAX_VALUE;

	protected Color _curColor = Color.black;
	protected Color _backgroundColor = null;
	
	protected double _fontsize = 10.0;
	protected int _font = FONT_HELVETICA_BOLD;

	
	public Color getCurrentColor() {
		return _curColor;
	}

	public double getCurFontSize() {
		return _fontsize;
	}

	public int getCurrentFont() {
		return _font;
	}
	

	public abstract String drawBaseStartS(int index);
	public abstract String drawBaseEndS(int index);
	public abstract String drawBasePairStartS(int i, int j, ModeleBP bps);
	public abstract String drawBasePairEndS(int index);
	public abstract String drawBackboneStartS(int i, int j);
	public abstract String drawBackboneEndS(int index);

	public abstract String drawLineS(Point2D.Double orig,
			Point2D.Double dest, double thickness);

	
	public abstract String drawArcS(Point2D.Double origine, double width,
			double height, double startAngle, double endAngle);

	public abstract String drawTextS(Point2D.Double base, String txt);

	public abstract String drawRectangleS(Point2D.Double orig,
			Point2D.Double dims, double thickness);
	
	public abstract String drawCircleS(Point2D.Double base, double radius,
			double thickness);

	public abstract String fillCircleS(Point2D.Double base, double radius,
			double thickness, Color color);

	public abstract String drawPolygonS(Point2D.Double[] points,
			double thickness);

	public abstract String fillPolygonS(Point2D.Double[] points,
		 Color color);

	public abstract String setFontS(int font, double size);

	public String setColorS(Color col) {
		_curColor = col;
		return "";
	}

	public abstract String headerS(Rectangle2D.Double bb);

	public abstract String footerS();

	@SuppressWarnings("unused")
	private void resetBoundingBox() {
		_xmin = Double.MAX_VALUE;
		_ymin = Double.MAX_VALUE;
		_xmax = -Double.MAX_VALUE;
		_ymax = -Double.MAX_VALUE;
	}

	private void updateBoundingBox(double x, double y) {
		_xmin = Math.min(_xmin, x - 10);
		_ymin = Math.min(_ymin, y - 10);
		_xmax = Math.max(_xmax, x + 10);
		_ymax = Math.max(_ymax, y + 10);
	}

	public void drawLine(double x0, double y0, double x1, double y1,
			double thickness) {
		updateBoundingBox(x0, y0);
		updateBoundingBox(x1, y1);
		_commands.add(new LineCommand(new Point2D.Double(x0, y0),
				new Point2D.Double(x1, y1), thickness));
	}

	public void drawArc(Point2D.Double origine, double width, double height,
			double startAngle, double endAngle) {
		updateBoundingBox(origine.x + width/2., origine.y + height/2.);
		updateBoundingBox(origine.x - width/2., origine.y - height/2.);
		_commands.add(new ArcCommand(origine, width, height, startAngle,
				endAngle));
	}

	public void drawText(double x, double y, String txt) {
		updateBoundingBox(x, y);

		_commands.add(new TextCommand(new Point2D.Double(x, y), new String(txt)));
	}

	public void drawRectangle(double x, double y, double w, double h,
			double thickness) {
		updateBoundingBox(x, y);
		updateBoundingBox(x + w, y + h);

		_commands.add(new RectangleCommand(new Point2D.Double(x, y),
				new Point2D.Double(w, h), thickness));
	}
	
	public void fillRectangle(double x, double y, double w, double h, Color color) {
		double [] xtab = new double[4];
		double [] ytab = new double[4];
		xtab[0] = x;
		xtab[1] = x+w;
		xtab[2] = x+w;
		xtab[3] = x;
		ytab[0] = y;
		ytab[1] = y;
		ytab[2] = y+h;
		ytab[3] = y+h;
		fillPolygon(xtab, ytab, color);
	}

	public void drawCircle(double x, double y, double radius, double thickness) {
		updateBoundingBox(x - radius, y - radius);
		updateBoundingBox(x + radius, y + radius);

		_commands.add(new CircleCommand(new Point2D.Double(x, y), radius,
				thickness));

	}

	public void setColor(Color col) {
		_curColor = col;
		_commands.add(new ColorCommand(col));
	}
	
	public void setBackgroundColor(Color col){
		_backgroundColor = col;
	}

	public void removeBackgroundColor(){
		_backgroundColor = null;
	}

	public void fillCircle(double x, double y, double radius, double thickness,
			Color color) {
		updateBoundingBox(x - radius, y - radius);
		updateBoundingBox(x + radius, y + radius);

		_commands.add(new FillCircleCommand(new Point2D.Double(x, y), radius,
				thickness, color));
	}

	public void drawPolygon(double[] xtab, double[] ytab, double thickness) {
		if (xtab.length == ytab.length) {
			Point2D.Double points[] = new Point2D.Double[xtab.length];
			for (int i = 0; i < xtab.length; i++) {
				points[i] = new Point2D.Double(xtab[i], ytab[i]);
				updateBoundingBox(xtab[i], ytab[i]);
			}
			_commands.add(new PolygonCommand(points, thickness));
		}
	}

	public void drawPolygon(GeneralPath p, 
			double thickness) {
		PathIterator pi = p.getPathIterator(null);
		Vector<Point2D.Double> v = new Vector<Point2D.Double>();  
		double[] coords = new double[6];
		while (!pi.isDone())
		{
			int code = pi.currentSegment(coords);
			if (code == PathIterator.SEG_MOVETO)
			{ v.add(new Point2D.Double(coords[0],coords[1])); }
			if (code == PathIterator.SEG_LINETO)
			{ v.add(new Point2D.Double(coords[0],coords[1])); }			
			pi.next();
		}
		double[] xtab = new double[v.size()];
		double[] ytab = new double[v.size()];
		for(int i=0;i<v.size();i++)
		{
			xtab[i] = v.get(i).x;
			ytab[i] = v.get(i).y;
		}
		drawPolygon(xtab,ytab, thickness);
	}

		
	public void fillPolygon(GeneralPath p, 
			Color color) {
		PathIterator pi = p.getPathIterator(null);
		Vector<Point2D.Double> v = new Vector<Point2D.Double>();  
		double[] coords = new double[6];
		while (!pi.isDone())
		{
			int code = pi.currentSegment(coords);
			if (code == PathIterator.SEG_MOVETO)
			{ v.add(new Point2D.Double(coords[0],coords[1])); }
			if (code == PathIterator.SEG_LINETO)
			{ v.add(new Point2D.Double(coords[0],coords[1])); }			
			pi.next();
		}
		double[] xtab = new double[v.size()];
		double[] ytab = new double[v.size()];
		for(int i=0;i<v.size();i++)
		{
			xtab[i] = v.get(i).x;
			ytab[i] = v.get(i).y;
		}
		fillPolygon(xtab,ytab, color);
	}

	
	public void fillPolygon(double[] xtab, double[] ytab, 
			Color color) {
		if (xtab.length == ytab.length) {
			Point2D.Double points[] = new Point2D.Double[xtab.length];
			for (int i = 0; i < xtab.length; i++) {
				points[i] = new Point2D.Double(xtab[i], ytab[i]);
				updateBoundingBox(xtab[i], ytab[i]);
			}
			_commands.add(new FillPolygonCommand(points, color));
		}
	}

	public void setFont(int font, double size) {
		_fontsize = size;
		_font = font;
		_commands.add(new FontCommand(font, size));
	}

	public void setScale(double sc) {
		_scale = sc;
	}

	public double getScale() {
		return _scale;
	}

	public Rectangle2D.Double getBoundingBox() {
		return (new Rectangle2D.Double(_xmin, _ymin, _xmax - _xmin, _ymax
				- _ymin));
	}

	private Point2D.Double transform(Point2D.Double p, double factor,
			double dx, double dy) {
		return transform(p.x, p.y, factor, dx, dy);
	}

	private Point2D.Double transform(double x, double y, double factor,
			double dx, double dy) {

		return new Point2D.Double((x + dx) * factor, (y + dy) * factor);
	}

	public String export() {
		Rectangle2D.Double oldbb = getBoundingBox();
		double dx = -oldbb.x;
		double dy = -oldbb.y;
		Rectangle2D.Double nbb = new Rectangle2D.Double(0, 0, oldbb.width* _scale, oldbb.height * _scale);
		StringBuffer buf = new StringBuffer();
		buf.append(headerS(nbb));
		if (_backgroundColor!= null)
		{
			double w = oldbb.width* _scale;
			double h = oldbb.height* _scale;
			Point2D.Double[] tab = new 	Point2D.Double[4];
			tab[0] = new Point2D.Double(0,0);
			tab[1] = new Point2D.Double(w,0);
			tab[2] = new Point2D.Double(w,h);
			tab[3] = new Point2D.Double(0,h);
		    buf.append(this.fillPolygonS(tab, _backgroundColor));
		}
		for (int i = 0; i < _commands.size(); i++) {
			GraphicElement ge = _commands.elementAt(i);
			if (ge instanceof LineCommand) {
				LineCommand c = (LineCommand) ge;
				String tmp = drawLineS(transform(c.get_orig(), _scale, dx, dy),
						transform(c.get_dest(), _scale, dx, dy), c
								.get_thickness());
				buf.append(tmp);
			} else if (ge instanceof TextCommand) {
				TextCommand c = (TextCommand) ge;
				String tmp = drawTextS(transform(c.get_base(), _scale, dx, dy),
						c.get_txt());
				buf.append(tmp);
			} else if (ge instanceof RectangleCommand) {
				RectangleCommand c = (RectangleCommand) ge;
				String tmp = drawRectangleS(transform(c.get_orig(), _scale, dx,
						dy), transform(c.get_dims(), _scale, 0.0, 0.0), c
						.get_thickness());
				buf.append(tmp);
			} else if (ge instanceof CircleCommand) {
				CircleCommand c = (CircleCommand) ge;
				String tmp = drawCircleS(
						transform(c.get_base(), _scale, dx, dy), c.get_radius()
								* _scale, c.get_thickness());
				buf.append(tmp);
			} else if (ge instanceof FillCircleCommand) {
				FillCircleCommand c = (FillCircleCommand) ge;
				String tmp = fillCircleS(
						transform(c.get_base(), _scale, dx, dy), c.get_radius()
								* _scale, c.get_thickness(), c.get_color());
				buf.append(tmp);
			} else if (ge instanceof FontCommand) {
				FontCommand c = (FontCommand) ge;
				String tmp = setFontS(c.get_font(), c.get_size());
				buf.append(tmp);
			} else if (ge instanceof ColorCommand) {
				ColorCommand c = (ColorCommand) ge;
				String tmp = setColorS(c.getColor());
				buf.append(tmp);
			} else if (ge instanceof ArcCommand) {				
				ArcCommand c = (ArcCommand) ge;
				String tmp = drawArcS(
						transform(c.getCenter(), _scale, dx, dy), c.getWidth()
								* _scale, c.getHeight() * _scale, c
								.getStartAngle(), c.getEndAngle());
				buf.append(tmp);
			} else if (ge instanceof PolygonCommand) {
				PolygonCommand c = (PolygonCommand) ge;
				Point2D.Double[] points = c.get_points();
				for (int j = 0; j < points.length; j++) {
					points[j] = transform(points[j], _scale, dx, dy);
				}
				String tmp = drawPolygonS(points, c.get_thickness());
				buf.append(tmp);
			} else if (ge instanceof FillPolygonCommand) {
				FillPolygonCommand c = (FillPolygonCommand) ge;
				Point2D.Double[] points = c.get_points();
				for (int j = 0; j < points.length; j++) {
					points[j] = transform(points[j], _scale, dx, dy);
				}
				String tmp = fillPolygonS(points, c
						.get_color());
				buf.append(tmp);
			}
		}
		buf.append(footerS());
		return buf.toString();
	}

	public void reset() {

	}
	
	
}
