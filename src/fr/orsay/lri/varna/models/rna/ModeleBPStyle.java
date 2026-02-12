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
package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.utils.XMLUtils;


public class ModeleBPStyle implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3006493290669550139L;
	/**
	 * 
	 */
	private boolean _isCustomColored = false;
	private Color _color = VARNAConfig.DEFAULT_BOND_COLOR;

	private double _thickness = -1.0;
	private double _bent = 0.0;
	
	public static String XML_ELEMENT_NAME = "BPstyle";
	public static String XML_VAR_CUSTOM_STYLED_NAME = "custom";
	public static String XML_VAR_COLOR_NAME = "color";
	public static String XML_VAR_THICKNESS_NAME = "thickness";
	public static String XML_VAR_BENT_NAME = "bent";

	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_CUSTOM_STYLED_NAME,"CDATA",""+_isCustomColored);
		atts.addAttribute("","",XML_VAR_COLOR_NAME,"CDATA",XMLUtils.toHTMLNotation(_color));
		atts.addAttribute("","",XML_VAR_THICKNESS_NAME,"CDATA",""+_thickness);
		atts.addAttribute("","",XML_VAR_BENT_NAME,"CDATA",""+_bent);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		hd.endElement("","",XML_ELEMENT_NAME);
	}

	
	public double getBent()
	{
		return _bent;
	}

	
	public boolean isBent()
	{
		return (_bent!=0.0);
	}

	public void setBent(double b)
	{
		_bent = b;
	}

	
	public ModeleBPStyle() {
	}


	public void setCustomColor(Color c) {
		_isCustomColored = true;
		_color = c;
	}

	public void useDefaultColor() {
		_isCustomColored = false;
	}

	public boolean isCustomColored() {
		return _isCustomColored;
	}

	public Color getCustomColor() {
		return _color;
	}

	/**
	 * Returns the current custom color if such a color is defined to be used
	 * (through setCustomColor), or returns the default color.
	 * 
	 * @param def
	 *            - The default color is no custom color is defined
	 * @return The color to be used to draw this base-pair
	 */
	public Color getColor(Color def) {
		if (isCustomColored()) {
			return _color;
		} else {
			return def;
		}
	}

	public double getThickness(double def) {
		if (_thickness > 0)
			return _thickness;
		else
			return def;
	}

	public void setThickness(double thickness) {
		_thickness = thickness;
	}

}
