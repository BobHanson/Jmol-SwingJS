package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Vector;

public class ModeleColorMap implements Cloneable, Serializable{
  /**
	 * 
	 */
	private static final long serialVersionUID = 4055062096061553106L;
private Vector<Color> _map;
  private Vector<Double> _values;
  
  public static final Color DEFAULT_COLOR = Color.GREEN; 
  
  public enum NamedColorMapTypes {
	  RED ("red",ModeleColorMap.redColorMap()),
	  BLUE ("blue",ModeleColorMap.blueColorMap()),
	  GREEN ("green",ModeleColorMap.greenColorMap()),
	  HEAT ("heat",ModeleColorMap.heatColorMap()),
	  ENERGY ("energy",ModeleColorMap.energyColorMap()),
	  ROCKNROLL ("rocknroll",ModeleColorMap.rockNRollColorMap()),
	  VIENNA ("vienna",ModeleColorMap.viennaColorMap()),
	  BW ("bw",ModeleColorMap.bwColorMap());
	  
	  String _id;
	  ModeleColorMap _cm;
	  
	  private NamedColorMapTypes(String id, ModeleColorMap cm)
	  {
		_id = id;
		_cm = cm;
	  }
	  
	  public String getId()
	  {
		  return _id;
	  }
	  public ModeleColorMap getColorMap()
	  {
		  return _cm;
	  }
	  public String toString()
	  {
		return _id; 
	  }
  }
  
  
  public ModeleColorMap()
  {
    this(new Vector<Color>(),new Vector<Double>());
  }

  
  public ModeleColorMap(Vector<Color> map,
		  Vector<Double> values)
  {
	  _map = map;
	  _values = values;
  }
  
  public void addColor(double val, Color col)
  {
	  int offset = Arrays.binarySearch(_values.toArray(), val) ;
	  if (offset<0)
	  {
		  int inspoint = (-offset)-1;
		  _map.insertElementAt(col, inspoint);		  
		  _values.insertElementAt(val,inspoint);		  
	  }
  }

  public double getMinValue()
  {
	  if (_values.size()>0)
	    return _values.get(0);
	  return 0.0;
  }
 
  public double getMaxValue()
  {
	  if (_values.size()>0)
	    return _values.get(_values.size()-1);
	  return 0.0;
  }
 
  
  public Color getMinColor()
  {
	  if (_map.size()>0)
	    return _map.get(0);
	  return DEFAULT_COLOR;
  }

  public Color getMaxColor()
  {
	  if (_map.size()>0)
	    return _map.get(_map.size()-1);
	  return DEFAULT_COLOR;
  }
 

  public int getNumColors()
  {
	  return (_map.size());
  }
 
  public Color getColorAt(int i)
  {
	  return (_map.get(i));
  }

  public Double getValueAt(int i)
  {
	  return (_values.get(i));
  }
  
  public Color getColorForValue(double val)
  {
	  Color result; 
	  if (val<=getMinValue())
	  {	  result = getMinColor(); }
	  else if (val>=getMaxValue())
	  {
		  result = getMaxColor();
	  }
	  else
	  {
	  int offset = Arrays.binarySearch(_values.toArray(), val) ;
	  if (offset>=0)
	  {
		  result =  _map.get(offset);
	  }
	  else
	  {
		  int inspoint = (-offset)-1;
		  Color c1 = _map.get(inspoint);
		  double v1 = _values.get(inspoint);
		  if (inspoint>0)
		  {
			  Color c2 = _map.get(inspoint-1);
			  double v2 = _values.get(inspoint-1);
			  double blendCoeff = (v2-val)/(v2-v1);
			  result =  new Color((int)(blendCoeff*c1.getRed()+(1.0-blendCoeff)*c2.getRed()),
				  (int)(blendCoeff*c1.getGreen()+(1.0-blendCoeff)*c2.getGreen()),
				  (int)(blendCoeff*c1.getBlue()+(1.0-blendCoeff)*c2.getBlue()));
			 
		  }
		  else
		  {
			result = c1;  
		  }
	  }
	  }
	  return result;
  }
  
  public static ModeleColorMap energyColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(1.0,new Color(128,50,50).brighter());
	  cm.addColor(0.9,new Color(255,50,50).brighter());
	  cm.addColor(0.65,new Color(255,255,50).brighter());
	  cm.addColor(0.55,new Color(20,255,50).brighter());
	  cm.addColor(0.2,new Color(50,50,255).brighter());
	  cm.addColor(0.0,new Color(50,50,128).brighter());
	  return cm;
  }
  
  public static ModeleColorMap viennaColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,new Color(0,80,220));
	  cm.addColor(0.1,new Color(0,139,220));
	  cm.addColor(0.2,new Color(0,220,218));
	  cm.addColor(0.3,new Color(0,220,123));
	  cm.addColor(0.4,new Color(0,220,49));
	  cm.addColor(0.5,new Color(34,220,0));
	  cm.addColor(0.6,new Color(109,220,0));
	  cm.addColor(0.7,new Color(199,220,0));
	  cm.addColor(0.8,new Color(220,165,0));
	  cm.addColor(0.9,new Color(220,86,0));
	  cm.addColor(1.0,new Color(220,0,0));
	  return cm;
  }
  
  public static ModeleColorMap bwColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.white);
	  cm.addColor(1.0,Color.gray.darker());
	  return cm;
  }

  public static ModeleColorMap greenColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.gray.brighter().brighter());
	  cm.addColor(1.0,Color.green.darker());
	  return cm;
  }
  
  public static ModeleColorMap blueColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.gray.brighter().brighter());
	  cm.addColor(1.0,Color.blue);
	  return cm;
  }
  
  public static ModeleColorMap redColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.gray.brighter().brighter());
	  cm.addColor(1.0,Color.red);
	  return cm;
  }

  public static ModeleColorMap heatColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.yellow);
	  cm.addColor(1.0,Color.red);
	  return cm;
  }
  
  public static ModeleColorMap rockNRollColorMap()
  {
	  ModeleColorMap cm = new ModeleColorMap();
	  cm.addColor(0.0,Color.red.brighter());
	  cm.addColor(1.0,Color.black);
	  cm.addColor(2.0,Color.green.brighter());
	  return cm;	  
  }

  
  public static ModeleColorMap defaultColorMap()
  {
	  return energyColorMap();
  }
  
  public static ModeleColorMap parseColorMap(String s)
  {
	  String[] data = s.split("[;,]");
	  if (data.length==1)
	  {
		  String name = data[0].toLowerCase();
		  for (NamedColorMapTypes p : NamedColorMapTypes.values())
		  {
			  if (name.equals(p.getId().toLowerCase()))
			  {
				  return p.getColorMap();
			  }
		  }
		  return ModeleColorMap.defaultColorMap();
	  }
	  else
	  {
		  ModeleColorMap cm = new ModeleColorMap();
		  
		  for(int i=0;i<data.length;i++)
		  {
			  String[] data2 = data[i].split(":");
			  if (data2.length==2)
			  {
				  try{
					  Double val = Double.parseDouble(data2[0]);
					  Color col = Color.decode(data2[1]);
					  cm.addColor(val, col);
				  }
				  catch(Exception e)
				  {  
				  }
			  }
		  }
		  if (cm.getNumColors()>1)
			  return cm;
	  }
	  return ModeleColorMap.defaultColorMap();
  }
  
  public void setMinValue(double newMin)
  {
	  rescale(newMin,getMaxValue());
  }

  public void setMaxValue(double newMax)
  {
	  rescale(getMinValue(),newMax);
  }

  public void rescale(double newMin, double newMax)
  {
	  double minBck = getMinValue();
	  double maxBck = getMaxValue();
	  double spanBck = maxBck-minBck;
	  if (newMax!=newMin)
	  {
	      newMax = Math.max(newMax,newMin+1.0);
		  for (int i=0;i<_values.size();i++)
		  {
			  double valBck = _values.get(i);
			  _values.set(i, newMin+(newMax-newMin)*(valBck-minBck)/(spanBck));
		  }
	  }
  }
  
  public ModeleColorMap clone()
  {
	  ModeleColorMap cm = new ModeleColorMap(); 
	  cm._map = (Vector<Color>) _map.clone();
	  cm._values = (Vector<Double>)_values.clone();
	  return cm;
  }
  
  public boolean equals(ModeleColorMap cm)
  {
	  if ( getNumColors()!=cm.getNumColors())
		  return false;
	  for (int i=0;i<getNumColors();i++)
	  {
		  if ((!getColorAt(i).equals(cm.getColorAt(i))) || (!getValueAt(i).equals(cm.getValueAt(i))) )
				  return false;
	  }
	  return true;
		  
  }
  
  public String getParamEncoding()
  {
	  String result = "";
	  Formatter f = new Formatter();
	  for(int i=0;i<getNumColors();i++)
	  {
		  if (i!=0)
			  f.format(",");
		  f.format("%.2f:#%02X%02X%02X", _values.get(i),_map.get(i).getRed(),_map.get(i).getGreen(),_map.get(i).getBlue());
	  }
	  return f.out().toString();
  }
  

  
  public String toString()
  {
	  return getParamEncoding();
  }
}

