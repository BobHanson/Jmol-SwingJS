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

import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.utils.XMLUtils;


/**
 * The rna base model with the first character of the nitrogenous base and it
 * display
 * 
 * @author darty
 * 
 */
public class ModeleBaseNucleotide extends ModeleBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5493938366569588113L;
	private String _c;
	private int _index;
	

	
	public static String XML_ELEMENT_NAME = "nt";
	public static String XML_VAR_CONTENT_NAME = "base";
	
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_INDEX_NAME,"CDATA",""+_index);
		atts.addAttribute("","",XML_VAR_NUMBER_NAME,"CDATA",""+_realIndex);
		atts.addAttribute("","",XML_VAR_CUSTOM_DRAWN_NAME,"CDATA",""+_colorie);
		atts.addAttribute("","",XML_VAR_VALUE_NAME,"CDATA",""+_value);
		atts.addAttribute("","",XML_VAR_LABEL_NAME,"CDATA",""+_label);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		atts.clear();
		hd.startElement("","",XML_VAR_CONTENT_NAME,atts);
		XMLUtils.exportCDATAString(hd, _c);
		hd.endElement("","",XML_VAR_CONTENT_NAME);

		_coords.toXML(hd,XML_VAR_POSITION_NAME);
		_center.toXML(hd,XML_VAR_CENTER_NAME);
		if (_colorie)
		{ _styleBase.toXML(hd); }
		hd.endElement("","",XML_ELEMENT_NAME);
	}

	/**
	 * Creates a new rna base with the default display style and a space as
	 * nitrogenous base
	 * @param index The index of this base
	 */
	public ModeleBaseNucleotide(int index) {
		this(" ", index);
	}

	/**
	 * Creates a new rna base with the nitrogenous base
	 * 
	 * @param s
	 *            The code of this base
	 * @param index The index of this base
	 */
	public ModeleBaseNucleotide(String s, int index) {
		this(s, new ModelBaseStyle(), index);
	}


	/**
	 * Creates a new rna base with the nitrogenous base
	 * 
	 * @param s
	 *            The full label, potentially requiring further decoding
	 * @param index The index of this base
	 * @param baseNumber The number of this base, which may differ from the index (e.g. discontinuous numbering)
	 */
	public ModeleBaseNucleotide(String s, int index, int baseNumber) {
		this(s, new ModelBaseStyle(), index);
		_realIndex = baseNumber;
	}

	/**
	 * Creates a new rna base with the nitrogenous base and the display style
	 * 
	 * @param s
	 *            The full label, potentially requiring further decoding
	 * @param msb
	 *            The display style
	 * @param index The index of this base
	 */
	public ModeleBaseNucleotide(String s, ModelBaseStyle msb, int index) {
		this(new Point2D.Double(), new Point2D.Double(), true, s, msb, -1,
				index);
	}

	/**
	 * Creates a new rna base with a display style
	 * 
	 * @param msb
	 *            The display style
	 */
	public ModeleBaseNucleotide(ModelBaseStyle msb, int index, int baseNumber) {
		this("", msb, index);
		_realIndex = baseNumber;
	}

	/**
	 * Creates a new rna base with a space as the nitrogenous base and the
	 * display style
	 * 
	 * @param coord
	 * @param index
	 */
	public ModeleBaseNucleotide(Point2D.Double coord, int index) {
		this(new Point2D.Double(coord.getX(), coord.getY()),
				new Point2D.Double(), true, "", new ModelBaseStyle(), -1,
				index);
	}

	/**
	 * Creates a new rna base from another one with the same attributes
	 * 
	 * @param mb
	 *            The base to copy
	 */
	public ModeleBaseNucleotide(ModeleBaseNucleotide mb, int index) {
		this(
				new Point2D.Double(mb.getCoords().getX(), mb.getCoords()
						.getY()), new Point2D.Double(mb.getCenter().getX(), mb
						.getCenter().getY()), true, mb.getBase(), mb
						.getStyleBase(), mb.getElementStructure(), index);
	}

	public ModeleBaseNucleotide(Point2D.Double coords, Point2D.Double center,
			boolean colorie, String label, ModelBaseStyle mb, int elementStruct,
			int index) {
		_colorie = colorie;
		_c = label;
		_styleBase = mb;
		_coords = new VARNAPoint(coords);
		_center = new VARNAPoint(center);
		_index = index;
		_realIndex = index + 1;
		_value = 0.0;
	}

	
	public ModelBaseStyle getStyleBase() {
		if (_colorie)
			return _styleBase;
		return new ModelBaseStyle();
	}

	public String getBase() {
		return decode(_c);
	}

	public void setBase(String _s) {
		this._c = _s;
	}



	public String getContent() {
		return getBase();
	}

	public void setContent(String s) {
		setBase(s);
	}

	public int getIndex() {
		return _index;
	}
	
	public String toString()
	{
		return ""+this._realIndex+" ("+_index+") (x,y):"+this._coords +" C:"+_center;
	}

	
	private enum STATE_SPECIAL_CHARS_STATES{ NORMAL,SUBSCRIPT, SUPERSCRIPT, COMMAND};

	private static HashMap<Character,Character> _subscripts = new HashMap<Character,Character>();
	private static HashMap<Character,Character> _superscripts = new HashMap<Character,Character>();
	private static HashMap<String,Character> _commands = new HashMap<String,Character>();
	{
		_subscripts.put('0', '\u2080');
		_subscripts.put('1', '\u2081');
		_subscripts.put('2', '\u2082');
		_subscripts.put('3', '\u2083');
		_subscripts.put('4', '\u2084');
		_subscripts.put('5', '\u2085');
		_subscripts.put('6', '\u2086');
		_subscripts.put('7', '\u2087');
		_subscripts.put('8', '\u2088');
		_subscripts.put('9', '\u2089');
		_subscripts.put('+', '\u208A');
		_subscripts.put('-', '\u208B');
		_subscripts.put('a', '\u2090');
		_subscripts.put('e', '\u2091');
		_subscripts.put('o', '\u2092');
		_subscripts.put('i', '\u1D62');
		_subscripts.put('r', '\u1D63');
		_subscripts.put('u', '\u1D64');
		_subscripts.put('v', '\u1D65');
		_subscripts.put('x', '\u2093');
		_superscripts.put('0', '\u2070');
		_superscripts.put('1', '\u00B9');
		_superscripts.put('2', '\u00B2');
		_superscripts.put('3', '\u00B3');
		_superscripts.put('4', '\u2074');
		_superscripts.put('5', '\u2075');
		_superscripts.put('6', '\u2076');
		_superscripts.put('7', '\u2077');
		_superscripts.put('8', '\u2078');
		_superscripts.put('9', '\u2079');
		_superscripts.put('+', '\u207A');
		_superscripts.put('-', '\u207B');
		_superscripts.put('i', '\u2071');
		_superscripts.put('n', '\u207F');
		_commands.put("alpha",  '\u03B1');
		_commands.put("beta",   '\u03B2');
		_commands.put("gamma",  '\u03B3');
		_commands.put("delta",  '\u03B4');
		_commands.put("epsilon",'\u03B5');
		_commands.put("zeta",   '\u03B6');
		_commands.put("eta",    '\u03B7');
		_commands.put("theta",  '\u03B8');
		_commands.put("iota",   '\u03B9');
		_commands.put("kappa",  '\u03BA');
		_commands.put("lambda", '\u03BB');
		_commands.put("mu",     '\u03BC');
		_commands.put("nu",     '\u03BD');
		_commands.put("xi",     '\u03BE');
		_commands.put("omicron",'\u03BF');
		_commands.put("pi",     '\u03C1');
		_commands.put("rho",    '\u03C2');
		_commands.put("sigma",  '\u03C3');
		_commands.put("tau",    '\u03C4');
		_commands.put("upsilon",'\u03C5');
		_commands.put("phi",    '\u03C6');
		_commands.put("chi",    '\u03C7');
		_commands.put("psi",    '\u03C8');
		_commands.put("omega",  '\u03C9');
		_commands.put("Psi",    '\u03A8');
		_commands.put("Phi",    '\u03A6');
		_commands.put("Sigma",  '\u03A3');
		_commands.put("Pi",     '\u03A0');
		_commands.put("Theta",  '\u0398');
		_commands.put("Omega",  '\u03A9');
		_commands.put("Gamma",  '\u0393');
		_commands.put("Delta",  '\u0394');
		_commands.put("Lambda", '\u039B');
	}

	
	
	private static String decode(String s)
	{
		if (s.length()<=1)
		{
			return s;
		}
		STATE_SPECIAL_CHARS_STATES state = STATE_SPECIAL_CHARS_STATES.NORMAL;
		
		String result = "";
		String buffer = ""; 
		for(int i=0;i<s.length();i++)
		{
			char c = s.charAt(i);
			switch (state)
			{
				case NORMAL:
				{
					switch(c)
					{
						case '_':
							state = STATE_SPECIAL_CHARS_STATES.SUBSCRIPT;
						break;
						case '^':
							state = STATE_SPECIAL_CHARS_STATES.SUPERSCRIPT;
						break;
						case '\\':
							buffer = "";
							state = STATE_SPECIAL_CHARS_STATES.COMMAND;
						break;
						default:
							result += c;
							state = STATE_SPECIAL_CHARS_STATES.NORMAL;
						break;
					}
				}
				break;
				case SUBSCRIPT:
				case SUPERSCRIPT:
				{
					switch(c)
					{
						case '_':
							state = STATE_SPECIAL_CHARS_STATES.SUBSCRIPT;
						break;
						case '^':
							state = STATE_SPECIAL_CHARS_STATES.SUPERSCRIPT;
						break;
						case '\\':
							buffer = "";
							state = STATE_SPECIAL_CHARS_STATES.COMMAND;
						break;
						default:
							if ((state==STATE_SPECIAL_CHARS_STATES.SUBSCRIPT) && _subscripts.containsKey(c))
								result += _subscripts.get(c); 
							else if ((state==STATE_SPECIAL_CHARS_STATES.SUPERSCRIPT) && _superscripts.containsKey(c))
								result += _superscripts.get(c); 
							else
								result += c;
							state = STATE_SPECIAL_CHARS_STATES.NORMAL;
						break;
					}
				}
				break;
				case COMMAND:
				{
					switch(c)
					{
						case '_':
							if (_commands.containsKey(buffer))
							{ result += _commands.get(buffer); }
							else
							{ result += buffer; }
							buffer = "";
							state = STATE_SPECIAL_CHARS_STATES.SUBSCRIPT;
						break;
						case '^':
							if (_commands.containsKey(buffer))
							{ result += _commands.get(buffer); }
							else
							{ result += buffer; }
							buffer = "";
							state = STATE_SPECIAL_CHARS_STATES.SUPERSCRIPT;
						break;
						case '\\':
							if (_commands.containsKey(buffer))
							{ result += _commands.get(buffer); }
							else
							{ result += buffer; }
							buffer = "";
							state = STATE_SPECIAL_CHARS_STATES.COMMAND;
						break;
						case ' ':
							state = STATE_SPECIAL_CHARS_STATES.NORMAL;
							if (_commands.containsKey(buffer))
							{ 
								result += _commands.get(buffer);
							}
							else
							{
								result += buffer;
							}
							buffer = "";
						break;
						default:
							buffer += c;
						break;
					}
				}
				break;
			}
		}
		if (_commands.containsKey(buffer))
		{ 
			result += _commands.get(buffer);
		}
		else
		{
			result += buffer;
		}
		return result;
	}
	
	
}
