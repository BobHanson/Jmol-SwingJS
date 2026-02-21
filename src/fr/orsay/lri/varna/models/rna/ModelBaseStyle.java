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
import java.awt.Font;
import java.io.Serializable;
import java.util.ArrayList;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.utils.XMLUtils;

/**
 * The display Style of a rna base with the base name font, the ouline,
 * innerline, number and name color
 * 
 * @author darty
 * 
 */
public class ModelBaseStyle implements Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4331494086323517208L;

	private Color _base_outline_color, _base_inner_color, _base_number_color,
			_base_name_color;

	private boolean _selected;
	
	
	public static String XML_ELEMENT_NAME = "basestyle";
	public static String XML_VAR_OUTLINE_NAME = "outline";
	public static String XML_VAR_INNER_NAME = "inner";
	public static String XML_VAR_NUMBER_NAME = "num";
	public static String XML_VAR_NAME_NAME = "name";

	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_OUTLINE_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_base_outline_color));
		atts.addAttribute("","",XML_VAR_INNER_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_base_inner_color));
		atts.addAttribute("","",XML_VAR_NUMBER_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_base_number_color));
		atts.addAttribute("","",XML_VAR_NAME_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_base_name_color));
		
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		hd.endElement("","",XML_ELEMENT_NAME);
	}	
	
	public ModelBaseStyle clone()
	{
		ModelBaseStyle result = new ModelBaseStyle();
		result._base_inner_color  = this._base_inner_color;
		result._base_name_color  = this._base_name_color;
		result._base_number_color  = this._base_number_color;
		result._base_outline_color  = this._base_outline_color;
		result._selected  = this._selected;
		return result;
	}
	
	
	
	/**
	 * Creates a new base style with default colors and font
	 * 
	 * @see VARNAConfig#BASE_OUTLINE_COLOR_DEFAULT
	 * @see VARNAConfig#BASE_INNER_COLOR_DEFAULT
	 * @see VARNAConfig#BASE_NUMBER_COLOR_DEFAULT
	 * @see VARNAConfig#BASE_NAME_COLOR_DEFAULT
	 * 
	 */
	public ModelBaseStyle() {
		_base_outline_color = VARNAConfig.BASE_OUTLINE_COLOR_DEFAULT;
		_base_inner_color = VARNAConfig.BASE_INNER_COLOR_DEFAULT;
		_base_number_color = VARNAConfig.BASE_NUMBER_COLOR_DEFAULT;
		_base_name_color = VARNAConfig.BASE_NAME_COLOR_DEFAULT;
		_selected = false;
	}

	/**
	 * Creates a new base style with custom colors and custom font
	 * 
	 * @param outline
	 *            The out line color of the base
	 * @param inner
	 *            The inner line color of the base
	 * @param number
	 *            The number color of the base
	 * @param name
	 *            The name color of the base
	 * @param font
	 *            The name font of the base
	 */
	public ModelBaseStyle(Color outline, Color inner, Color number,
			Color name, Font font) {
		_base_outline_color = outline;
		_base_inner_color = inner;
		_base_number_color = number;
		_base_name_color = name;
	}

	public ModelBaseStyle(String parameterValue)
			throws ExceptionModeleStyleBaseSyntaxError, ExceptionParameterError {
		this();
		assignParameters(parameterValue);
	}

	public ModelBaseStyle(ModelBaseStyle msb) {
		_base_outline_color = msb.getBaseOutlineColor();
		_base_inner_color = msb.getBaseInnerColor();
		_base_number_color = msb.getBaseNumberColor();
		_base_name_color = msb.getBaseNameColor();
	}

	public Color getBaseOutlineColor() {
		return _base_outline_color;
	}

	public void setBaseOutlineColor(Color _base_outline_color) {
		this._base_outline_color = _base_outline_color;
	}

	public Color getBaseInnerColor() {
		return _base_inner_color;
	}

	public void setBaseInnerColor(Color _base_inner_color) {
		this._base_inner_color = _base_inner_color;
	}

	public Color getBaseNumberColor() {
		return _base_number_color;
	}

	public void setBaseNumberColor(Color _base_numbers_color) {
		this._base_number_color = _base_numbers_color;
	}

	public Color getBaseNameColor() {
		return _base_name_color;
	}

	public void setBaseNameColor(Color _base_name_color) {
		this._base_name_color = _base_name_color;
	}

	public static final String PARAM_INNER_COLOR = "fill";
	public static final String PARAM_OUTLINE_COLOR = "outline";
	public static final String PARAM_TEXT_COLOR = "label";
	public static final String PARAM_NUMBER_COLOR = "number";

	public static Color getSafeColor(String col) {
		Color result;
		try {
			result = Color.decode(col);

		} catch (NumberFormatException e) {

			result = Color.getColor(col, Color.green);
		}
		return result;
	}

	public void assignParameters(String parametersValue)
			throws ExceptionModeleStyleBaseSyntaxError, ExceptionParameterError {
		if (parametersValue.equals(""))
			return;

		String[] parametersL = parametersValue.split(",");

		ArrayList<String> namesArray = new ArrayList<String>();
		ArrayList<String> valuesArray = new ArrayList<String>();
		String[] param;
		for (int i = 0; i < parametersL.length; i++) {
			param = parametersL[i].split("=");
			if (param.length != 2)
				throw new ExceptionModeleStyleBaseSyntaxError(
						"Bad parameter: '" + param[0] + "' ...");
			namesArray.add(param[0].replace(" ", ""));
			valuesArray.add(param[1].replace(" ", ""));

		}

		for (int i = 0; i < namesArray.size(); i++) {
			if (namesArray.get(i).toLowerCase().equals(PARAM_INNER_COLOR)) {
				try {
					setBaseInnerColor(getSafeColor(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad inner color Syntax:" + valuesArray.get(i));
				}
			} else if (namesArray.get(i).toLowerCase().equals(PARAM_TEXT_COLOR)) {
				try {
					setBaseNameColor(getSafeColor(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad name color Syntax:" + valuesArray.get(i));
				}
			} else if (namesArray.get(i).toLowerCase().equals(
					PARAM_NUMBER_COLOR)) {
				try {
					setBaseNumberColor(getSafeColor(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad numbers color Syntax:" + valuesArray.get(i));
				}
			} else if (namesArray.get(i).toLowerCase().equals(
					PARAM_OUTLINE_COLOR)) {
				try {
					setBaseOutlineColor(getSafeColor(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad outline color Syntax:" + valuesArray.get(i));
				}
			} else
				throw new ExceptionModeleStyleBaseSyntaxError(
						"Unknown parameter:" + namesArray.get(i));
		}
	}

	/**
	 * Find the font style integer from a string. Return <code>null</code> if
	 * the font style is unknown.
	 * 
	 * @param s
	 *            The <code>string</code> to decode
	 * @return The font style integer as <code>Font.PLAIN<code>.
	 */
	public static Integer StyleToInteger(String s) {
		Integer style;
		if (s.toLowerCase().equals("italic"))
			style = Font.ITALIC;
		else if (s.toLowerCase().equals("bold"))
			style = Font.BOLD;
		else if (s.toLowerCase().equals("plain"))
			style = Font.PLAIN;
		else
			style = null;
		return style;
	}

}
