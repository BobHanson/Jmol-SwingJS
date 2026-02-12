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
package fr.orsay.lri.varna.models.rna;

import java.awt.geom.Point2D;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;


/**
 * The abstract rna base model
 * 
 * @author darty
 * 
 */
public abstract class ModeleBase implements Serializable, java.lang.Comparable<ModeleBase> {

	private ModeleBP _BP;
	/**
	 * The base style.
	 */
	protected ModelBaseStyle _styleBase = new ModelBaseStyle();
	/**
	 * TRUE if this InterfaceBase has to be colored, else FALSE.
	 */
	protected Boolean _colorie = new Boolean(true);
	/**
	 * The coordinate representation of this InterfaceBase on the final graphic.
	 */
	protected VARNAPoint _coords = new VARNAPoint();
	/**
	 * The nearest loop center of this InterfaceBase.
	 */
	protected VARNAPoint _center = new VARNAPoint();

	/**
	 * The label of this base.
	 */
	protected String _label = "";

	protected double _value;
	protected int _realIndex = -1;

	public abstract void toXML(TransformerHandler hd) throws SAXException;
	
	
	
	/**
	 * The internal index for this Base
	 */
	public abstract int getIndex();

	public abstract String getContent();
	public abstract void setContent(String s);

	/**
	 * Gets this InterfaceBase style.
	 * 
	 * @return this InterfaceBase style.
	 */
	public ModelBaseStyle getStyleBase() {
		return _styleBase;
	}

	public double getValue()
	{
		return _value;
	}

	public void setValue(double d)
	{
		_value = d;
	}
	
	
	/**
	 * Sets this InterfaceBase style.
	 * 
	 * @param base
	 *            - This InterfaceBase new style.
	 */
	public void setStyleBase(ModelBaseStyle base) {
		_styleBase = new ModelBaseStyle(base);
	}

	/**
	 * Gets this InterfaceBase color statement.
	 * 
	 * @return TRUE if this InterfaceBase has to be colored, else FALSE.
	 */
	public final Boolean getColorie() {
		return _colorie;
	}

	/**
	 * Sets this InterfaceBase color statement.
	 * 
	 * @param _colorie
	 *            - TRUE if you want this InterfaceBase to be colored, else
	 *            FALSE
	 */
	public final void setColorie(Boolean _colorie) {
		this._colorie = _colorie;
	}


	/**
	 * Gets this InterfaceBase associated structure element.
	 * 
	 * @return this InterfaceBase associated structure element.
	 */
	public int getElementStructure() {
		if (_BP==null)
			return -1;
		else
		{
		  if (_BP.getPartner5()==this)
			  return _BP.getPartner3().getIndex();
		  else
			  return _BP.getPartner5().getIndex();
		}
	}


	/**
	 * Sets this InterfaceBase assiocated structure element.
	 * 
	 * @param structure
	 *            - This new assiocated structure element.
	
	public void setElementStructure(int structure) {
		setElementStructure(structure, new ModeleBP());
	} */

	/**
	 * Sets this InterfaceBase associated structure element.
	 * 
	 * @param structure
	 *            - This new associated structure element.
	 * @param type
	 *            - The type of this base pair.
	 */
	public void setElementStructure(int structure, ModeleBP type) {
//		_elementStructure = structure;
		_BP = type;

	}

	public void removeElementStructure() {
//		_elementStructure = -1;
		_BP = null;
	}
	
	
	/**
	 * Gets the base pair type for this element.
	 * 
	 * @return the base pair type for this element.
	 */
	public ModeleBP getStyleBP() {
		return _BP;
	}

	/**
	 * Sets the base pair type for this element.
	 * 
	 * @param type
	 *            - The new base pair type for this element.
	 */
	public void setStyleBP(ModeleBP type) {
		_BP = type;
	}

	public int getBaseNumber() {
		return _realIndex;
	}

	public void setBaseNumber(int bn) {
		_realIndex = bn;
	}
	
	public Point2D.Double getCoords() {
		return new Point2D.Double(_coords.x,_coords.y);
	}

	public void setCoords(Point2D.Double coords) {
		this._coords.x = coords.x;
		this._coords.y = coords.y;
	}

	public Point2D.Double getCenter() {
		return new Point2D.Double(_center.x,_center.y);
	}

	public void setCenter(Point2D.Double center) {
		this._center.x = center.x;
		this._center.y = center.y;
	}

	public String getLabel() {
		if (_label==null || _label.equals(""))
		{
			return ""+this.getBaseNumber();
		}
		else
		{
			return _label;
		}
			
	}

	public void setLabel(String s) {
		_label= s;
	}

	public void setLabel(Point2D.Double center) {
		this._center.x = center.x;
		this._center.y = center.y;
	}


	public int compareTo(ModeleBase other) { 
	    int nombre1 = ((ModeleBase) other).getIndex(); 
	    int nombre2 = this.getIndex(); 
	    if (nombre1 > nombre2)  return -1; 
	    else if(nombre1 == nombre2) return 0; 
	    else return 1; 
	} 
	
	public static String XML_VAR_TYPE_NAME = "type";
	public static String XML_VAR_INDEX_NAME = "index";
	public static String XML_VAR_LABEL_NAME = "label";
	public static String XML_VAR_VALUE_NAME = "val";
	public static String XML_VAR_POSITION_NAME = "pos";
	public static String XML_VAR_CENTER_NAME = "center";
	public static String XML_VAR_NUMBER_NAME = "num";
	public static String XML_VAR_CUSTOM_DRAWN_NAME = "custom";
	
	
}
