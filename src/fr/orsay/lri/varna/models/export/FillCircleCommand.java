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

public class FillCircleCommand extends GraphicElement {

	private Point2D.Double _base;
	private double _radius;
	private double _thickness;
	private Color _color;

	public FillCircleCommand(Point2D.Double base, double radius,
			double thickness, Color color) {
		_base = base;
		_radius = radius;
		_thickness = thickness;
		_color = color;
	}

	public Point2D.Double get_base() {
		return _base;
	}

	public double get_radius() {
		return _radius;
	}

	public double get_thickness() {
		return _thickness;
	}

	public Color get_color() {
		return _color;
	}
}
