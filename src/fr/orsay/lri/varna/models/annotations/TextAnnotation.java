/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.models.annotations;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.VARNAPoint;
import fr.orsay.lri.varna.utils.XMLUtils;



/**
 * The annotated text model
 * 
 * @author Darty@lri.fr
 * 
 */
public class TextAnnotation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 465236085501860747L;
	
	public enum AnchorType{
		POSITION,
		BASE,
		HELIX,
		LOOP
	};
	

	public static final String HEADER_TEXT = "TextAnnotation";
	
	/**
	 * default text color
	 */
	public static final Color DEFAULTCOLOR = Color.black;
	/**
	 * default text font
	 */
	public static final Font DEFAULTFONT = new Font("Arial", Font.PLAIN, 12);

	private String _text;
	private AnchorType _typeAnchor;
	private Color _color;
	private double _angle;
	private Object _anchor;
	private Font _font;
	
	public static String XML_ELEMENT_NAME = "textAnnotation";
	public static String XML_VAR_TYPE_NAME = "type";
	public static String XML_VAR_COLOR_NAME = "color";
	public static String XML_VAR_ANGLE_NAME = "angle";
	public static String XML_VAR_TEXT_NAME = "text";

	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_TYPE_NAME,"CDATA",""+_typeAnchor);
		atts.addAttribute("","",XML_VAR_COLOR_NAME,"CDATA",""+XMLUtils.toHTMLNotation(_color));
		atts.addAttribute("","",XML_VAR_ANGLE_NAME,"CDATA",""+_angle);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		atts.clear();
		hd.startElement("","",XML_VAR_TEXT_NAME,atts);
		XMLUtils.exportCDATAString(hd, _text);
		hd.endElement("","",XML_VAR_TEXT_NAME);
		switch (_typeAnchor)
		{
		case POSITION:
			((VARNAPoint)_anchor).toXML(hd,"pos");
			break;
		case BASE:
			XMLUtils.toXML(hd, (ModeleBase)_anchor);
			break;
		case HELIX:
			XMLUtils.toXML(hd, (ArrayList<ModeleBase>)_anchor);
			break;
		case LOOP:
			XMLUtils.toXML(hd, (ArrayList<ModeleBase>)_anchor);
			break;
		}		
		XMLUtils.toXML(hd, _font);
		hd.endElement("","",XML_ELEMENT_NAME);
	}

	/**
	 * creates an annoted text on a VARNAPanel with the specified text
	 * 
	 * @param texte Textual content of the annotation
	 */
	public TextAnnotation(String texte) {
		_text = texte;
		_color = DEFAULTCOLOR;
		_font = DEFAULTFONT;
		_angle = 0;
	}

	/**
	 * /** creates an annoted text on a VARNAPanel with the specified text and
	 * is static position
	 * 
	 * @param texte
	 * @param x
	 * @param y
	 */
	public TextAnnotation(String texte, double x, double y) {
		this(texte);
		_anchor = new VARNAPoint(x, y);
		_typeAnchor = AnchorType.POSITION;
	}

	/**
	 * creates an annoted text on a VARNAPanel with the specified text fixed to
	 * a base
	 * 
	 * @param texte
	 * @param mb
	 */
	public TextAnnotation(String texte, ModeleBase mb) {
		this(texte);
		_anchor = mb;
		_typeAnchor = AnchorType.BASE;
	}

	/**
	 * creates an annoted text on a VARNAPanel with the specified text fixed to
	 * a helix (if type is HELIX) or to a loop (if type is LOOP)
	 * 
	 * @param texte
	 * @param listeBase
	 * @param type
	 * @throws Exception
	 */
	public TextAnnotation(String texte, ArrayList<ModeleBase> listeBase,
			AnchorType type) throws Exception {
		this(texte);
		_anchor = listeBase;

		if (type == AnchorType.HELIX)
			_typeAnchor = AnchorType.HELIX;
		else if (type == AnchorType.LOOP)
			_typeAnchor = AnchorType.LOOP;
		else
			throw new Exception("Bad argument");
	}

	/**
	 * creates an annoted text from another one
	 * 
	 * @param textAnnotation
	 */
	public TextAnnotation(TextAnnotation textAnnotation) {
		_anchor = textAnnotation.getAncrage();
		_font = textAnnotation.getFont();
		_text = textAnnotation.getTexte();
		_typeAnchor = textAnnotation.getType();
	}

	/**
	 * 
	 * @return the text
	 */
	public String getTexte() {
		return _text;
	}

	public void setText(String _texte) {
		this._text = _texte;
	}

	/**
	 * 
	 * @return the font
	 */
	public Font getFont() {
		return _font;
	}

	public void setFont(Font _font) {
		this._font = _font;
	}

	public Object getAncrage() {
		return _anchor;
	}

	public void setAncrage(ModeleBase mb) {
		_anchor = mb;
		_typeAnchor = AnchorType.BASE;
	}

	public void setAncrage(double x, double y) {
		_anchor = new VARNAPoint(x, y);
		_typeAnchor = AnchorType.POSITION;
	}

	public void setAncrage(ArrayList<ModeleBase> list, AnchorType type)
			throws Exception {
		_anchor = list;
		if (type == AnchorType.HELIX)
			_typeAnchor = AnchorType.HELIX;
		else if (type == AnchorType.LOOP)
			_typeAnchor = AnchorType.LOOP;
		else
			throw new Exception("Bad argument");
	}

	public AnchorType getType() {
		return _typeAnchor;
	}

	public void setType(AnchorType t) {
		_typeAnchor = t;
	}


	public Color getColor() {
		return _color;
	}

	public void setColor(Color color) {
		this._color = color;
	}

	
	public String getHelixDescription()
	{
		ArrayList<ModeleBase> listeBase =  ((ArrayList<ModeleBase>)_anchor);
		int minA = Integer.MAX_VALUE,maxA = Integer.MIN_VALUE;
		int minB = Integer.MAX_VALUE,maxB = Integer.MIN_VALUE;
		for(ModeleBase mb : listeBase)
		{
			int i = mb.getBaseNumber();
			if (mb.getElementStructure()>i)
			{
				minA = Math.min(minA, i);
				maxA = Math.max(maxA, i);
			}
			else
			{
				minB = Math.min(minB, i);
				maxB = Math.max(maxB, i);				
			}
		}
		return "["+minA+","+maxA+"] ["+minB+","+maxB+"]";
	}
	
	public String getLoopDescription()
	{
		ArrayList<ModeleBase> listeBase =  ((ArrayList<ModeleBase>)_anchor);
		int min = Integer.MAX_VALUE,max = Integer.MIN_VALUE;
		for(ModeleBase mb : listeBase)
		{
			int i = mb.getBaseNumber();
				min = Math.min(min, i);
				max = Math.max(max, i);
		}
		return "["+min+","+max+"]";
	}
	
	public String toString() {
		String tmp = "["+_text+"] ";
		switch (_typeAnchor) {
		case POSITION:
			NumberFormat formatter = new DecimalFormat(".00"); 
			return tmp+" at ("+formatter.format(getCenterPosition().x)+","+formatter.format(getCenterPosition().y)+")";
		case BASE:
			return tmp+" on base "+((ModeleBase) _anchor).getBaseNumber();
		case HELIX:
			return tmp+" on helix "+getHelixDescription();
		case LOOP:
			return tmp+" on loop "+getLoopDescription();
		default:
			return tmp;
		}		
	}

	/**
	 * 
	 * @return the text position center
	 */
	public Point2D.Double getCenterPosition() {
		switch (_typeAnchor) {
		case POSITION:
			return ((VARNAPoint) _anchor).toPoint2D();
		case BASE:
			return ((ModeleBase) _anchor).getCoords();
		case HELIX:
			return calculLoopHelix();
		case LOOP:
			return calculLoop();
		default:
			return new Point2D.Double(0., 0.);
		}
	}

	private Point2D.Double calculLoop() {
		ArrayList<ModeleBase> liste = extractedArrayListModeleBaseFromAncrage();
		double totalX = 0., totalY = 0.;
		for (ModeleBase base : liste) {
			totalX += base.getCoords().x;
			totalY += base.getCoords().y;
		}
		return new Point2D.Double(totalX / liste.size(), totalY / liste.size());
	}

	private Point2D.Double calculLoopHelix() {
		ArrayList<ModeleBase> liste = extractedArrayListModeleBaseFromAncrage();
		Collections.sort(liste);
		double totalX = 0., totalY = 0.;
		double num=0.0;
		for (int i=0;i<liste.size(); i++) {
			ModeleBase base =liste.get(i);
			if ((i>0 && (i<liste.size()-1)) || ((liste.size()/2)%2==0))
			{
				totalX += base.getCoords().x;
				totalY += base.getCoords().y;
				num += 1;
			}
		}
		return new Point2D.Double(totalX / num, totalY / num);
	}

	
	private ArrayList<ModeleBase> extractedArrayListModeleBaseFromAncrage() {
		return (ArrayList<ModeleBase>) _anchor;
	}

	/**
	 * clone a TextAnnotation
	 */
	public TextAnnotation clone() {
		TextAnnotation textAnnot = null;
		try {
			switch (_typeAnchor) {
			case BASE:
				textAnnot = new TextAnnotation(_text, (ModeleBase) _anchor);
				break;
			case POSITION:
				textAnnot = new TextAnnotation(_text,
						((VARNAPoint) _anchor).x,
						((VARNAPoint) _anchor).y);
				break;
			case LOOP:
				textAnnot = new TextAnnotation(_text,
						extractedArrayListModeleBaseFromAncrage(), AnchorType.LOOP);
				break;
			case HELIX:
				textAnnot = new TextAnnotation(_text,
						extractedArrayListModeleBaseFromAncrage(), AnchorType.HELIX);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		textAnnot.setFont(_font);
		textAnnot.setColor(_color);
		return textAnnot;

	}

	/**
	 * copy a textAnnotation
	 * 
	 * @param textAnnotation
	 */
	public void copy(TextAnnotation textAnnotation) {
		_anchor = textAnnotation.getAncrage();
		_font = textAnnotation.getFont();
		_text = textAnnotation.getTexte();
		_typeAnchor = textAnnotation.getType();
		_color = textAnnotation.getColor();
		_angle = textAnnotation.getAngleInDegres();
	}


	/**
	 * 
	 * @return the angle in degrees
	 */
	public double getAngleInDegres() {
		// if (_typeAncrage == TextAnnotation.HELIX)
		// _angle = calculAngleDegres();
		return _angle;
	}

	/**
	 * 
	 * @return the angle in radians
	 */
	public double getAngleInRadians() {
		return (getAngleInDegres() * Math.PI) / 180.;
	}

	public void setAngleInDegres(double _angle) {
		this._angle = _angle;
	}

	public void setAngleInRadians(double _angle) {
		this._angle = _angle * 180 / Math.PI;
	}
	
	
	public static TextAnnotation parse(String thisAnn, VARNAPanel vp)
	{
		String[] data = thisAnn.split(":");

		String text = "";
		int anchor = -1;
		int x = -1;
		int y = -1;
		TextAnnotation.AnchorType type = TextAnnotation.AnchorType.LOOP;
		Font font = TextAnnotation.DEFAULTFONT;
		Color color = TextAnnotation.DEFAULTCOLOR;
		TextAnnotation ann = null;
		try {
			if (data.length == 2) {
				text = data[0];
				String[] data2 = data[1].split(",");
				for (int j = 0; j < data2.length; j++) {
					String opt = data2[j];
					String[] data3 = opt.split("=");
					if (data3.length == 2) {
						String name = data3[0].toLowerCase();
						String value = data3[1];
						if (name.equals("type")) {
							if (value.toUpperCase().equals("H")) {
								type = TextAnnotation.AnchorType.HELIX;
							} else if (value.toUpperCase().equals("L")) {
								type = TextAnnotation.AnchorType.LOOP;
							} else if (value.toUpperCase().equals("P")) {
								type = TextAnnotation.AnchorType.POSITION;
							} else if (value.toUpperCase().equals("B")) {
								type = TextAnnotation.AnchorType.BASE;
							}
						} else if (name.equals("x")) {
							x = Integer.parseInt(value);
						} else if (name.equals("y")) {
							y = Integer.parseInt(value);
						} else if (name.equals("anchor")) {
							anchor = Integer.parseInt(value);
						} else if (name.equals("size")) {
							font = font.deriveFont((float) Integer
									.parseInt(value));
						} else if (name.equals("color")) {
							color = VARNAConfigLoader.getSafeColor(value, color);
						}
					}
				}
				switch (type) {
				case POSITION:
					if ((x != -1) && (y != -1)) {
						Point2D.Double p = vp
								.panelToLogicPoint(new Point2D.Double(x, y));
						ann = new TextAnnotation(text, p.x, p.y);
					}
					break;
				case BASE:
					if (anchor != -1) {
						int index = vp.getRNA().getIndexFromBaseNumber(
								anchor);
						ModeleBase mb = vp.getRNA().get_listeBases()
								.get(index);
						ann = new TextAnnotation(text, mb);
					}
					break;
				case HELIX:
					if (anchor != -1) {
						ArrayList<ModeleBase> mbl = new ArrayList<ModeleBase>();
						int index = vp.getRNA().getIndexFromBaseNumber(
								anchor);
						ArrayList<Integer> il = vp.getRNA()
								.findHelix(index);
						for (int k : il) {
							mbl.add(vp.getRNA().get_listeBases().get(k));
						}
						ann = new TextAnnotation(text, mbl, type);
					}
					break;
				case LOOP:
					if (anchor != -1) {
						ArrayList<ModeleBase> mbl = new ArrayList<ModeleBase>();
						int index = vp.getRNA().getIndexFromBaseNumber(
								anchor);
						ArrayList<Integer> il = vp.getRNA().findLoop(index);
						for (int k : il) {
							mbl.add(vp.getRNA().get_listeBases().get(k));
						}
						ann = new TextAnnotation(text, mbl, type);
					}
					break;
				}
				if (ann != null) {
					ann.setColor(color);
					ann.setFont(font);
				}
			}
		} catch (Exception e) {
			System.err.println("Apply Annotations: " + e.toString());
		}
		return ann;
		}
}
