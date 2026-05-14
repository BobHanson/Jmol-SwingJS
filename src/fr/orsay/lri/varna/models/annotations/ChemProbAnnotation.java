package fr.orsay.lri.varna.models.annotations;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.utils.XMLUtils;

public class ChemProbAnnotation implements Serializable {

  public static final String HEADER_TEXT = "ChemProbAnnotation";

  /**
   * 
   */
  private static final long serialVersionUID = 5833315460145031242L;

  public enum ChemProbAnnotationType {
    TRIANGLE, ARROW, PIN, DOT;
    
    public static ChemProbAnnotationType annotTypeFromString(String name) {
      return (name.equals("NULL") ? DEFAULT_TYPE : valueOf(name.toUpperCase()));
    }

  }

  public final static double DEFAULT_INTENSITY = 1.0;
  public final static ChemProbAnnotationType DEFAULT_TYPE = ChemProbAnnotationType.ARROW;
  public final static Color DEFAULT_COLOR = Color.blue.darker();

  private ModeleBase _firstBase;
  private ModeleBase _lastBase;
  private Color _color;
  private double _intensity;
  private ChemProbAnnotationType _type;
  private boolean _outward;

  public static final String XML_ELEMENT_NAME = "ChemProbAnnotation";
  public static final String XML_VAR_INDEX5_NAME = "Index5";
  public static final String XML_VAR_INDEX3_NAME = "Index3";
  public static final String XML_VAR_COLOR_NAME = "Color";
  public static final String XML_VAR_INTENSITY_NAME = "Intensity";
  public static final String XML_VAR_TYPE_NAME = "Type";
  public static final String XML_VAR_OUTWARD_NAME = "Outward";

  public void toXML(TransformerHandler hd) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    atts.addAttribute("", "", XML_VAR_INDEX5_NAME, "CDATA",
        "" + _firstBase.getIndex());
    atts.addAttribute("", "", XML_VAR_INDEX3_NAME, "CDATA",
        "" + _lastBase.getIndex());
    atts.addAttribute("", "", XML_VAR_COLOR_NAME, "CDATA",
        XMLUtils.toHTMLNotation(_color));
    atts.addAttribute("", "", XML_VAR_INTENSITY_NAME, "CDATA", "" + _intensity);
    atts.addAttribute("", "", XML_VAR_TYPE_NAME, "CDATA", "" + _type);
    atts.addAttribute("", "", XML_VAR_OUTWARD_NAME, "CDATA", "" + _outward);
    hd.startElement("", "", XML_ELEMENT_NAME, atts);
    hd.endElement("", "", XML_ELEMENT_NAME);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase,
      String styleDesc) {
    this(firstBase, lastBase);
    applyStyle(styleDesc);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase) {
    this(firstBase, lastBase, ChemProbAnnotation.DEFAULT_TYPE,
        ChemProbAnnotation.DEFAULT_INTENSITY);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase,
      double intensity) {
    this(firstBase, lastBase, ChemProbAnnotation.DEFAULT_TYPE, intensity);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase,
      ChemProbAnnotationType type) {
    this(firstBase, lastBase, type, ChemProbAnnotation.DEFAULT_INTENSITY);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase,
      ChemProbAnnotationType type, double intensity) {
    this(firstBase, lastBase, type, intensity, DEFAULT_COLOR, true);
  }

  public ChemProbAnnotation(ModeleBase firstBase, ModeleBase lastBase,
      ChemProbAnnotationType type, double intensity, Color color, boolean out) {
    if (firstBase.getIndex() > lastBase.getIndex()) {
      ModeleBase tmp = lastBase;
      lastBase = firstBase;
      firstBase = tmp;
    }
    _firstBase = firstBase;
    _lastBase = lastBase;
    _type = type;
    _intensity = intensity;
    _color = color;
    _outward = out;
  }

  public boolean isOut() {
    return _outward;
  }

  public void setOut(boolean b) {
    _outward = b;
  }

  public Color getColor() {
    return _color;
  }

  public double getIntensity() {
    return _intensity;
  }

  public ChemProbAnnotationType getType() {
    return _type;
  }

  public void setColor(Color c) {
    _color = c;
  }

  public void setIntensity(double d) {
    _intensity = d;
  }

  public Point2D.Double getAnchorPosition() {
    Point2D.Double result = new Point2D.Double(
        (_firstBase.getCoords().x + _lastBase.getCoords().x) / 2.0,
        (_firstBase.getCoords().y + _lastBase.getCoords().y) / 2.0);
    return result;
  }

  public Point2D.Double getDirVector() {
    Point2D.Double norm = getNormalVector();
    Point2D.Double result = new Point2D.Double(-norm.y, norm.x);
    Point2D.Double anchor = getAnchorPosition();
    Point2D.Double center = new Point2D.Double(
        (_firstBase.getCenter().x + _lastBase.getCenter().x) / 2.0,
        (_firstBase.getCenter().y + _lastBase.getCenter().y) / 2.0);
    Point2D.Double vradius = new Point2D.Double((center.x - anchor.x) / 2.0,
        (center.y - anchor.y) / 2.0);
    if (_outward) {
      if (result.x * vradius.x + result.y * vradius.y > 0) {
        return new Point2D.Double(-result.x, -result.y);
      }
    } else {
      if (result.x * vradius.x + result.y * vradius.y < 0) {
        return new Point2D.Double(-result.x, -result.y);
      }
    }
    return result;
  }

  public Point2D.Double getNormalVector() {
    Point2D.Double tmp;
    if (_firstBase == _lastBase) {
      tmp = new Point2D.Double(
          (-(_lastBase.getCenter().y - _lastBase.getCoords().y)),
          ((_lastBase.getCenter().x - _lastBase.getCoords().x)));
    } else {
      tmp = new Point2D.Double(
          (_lastBase.getCoords().x - _firstBase.getCoords().x) / 2.0,
          (_lastBase.getCoords().y - _firstBase.getCoords().y) / 2.0);
    }

    double norm = tmp.distance(0, 0);
    Point2D.Double result = new Point2D.Double(tmp.x / norm, tmp.y / norm);
    return result;
  }

  public void applyStyle(String styleDesc) {
    String[] chemProbs = styleDesc.split(",");
    for (int i = 0; i < chemProbs.length; i++) {
      String thisStyle = chemProbs[i];
      String[] data = thisStyle.split("=");
      if (data.length == 2) {
        String name = data[0];
        String value = data[1];
        switch (name.toLowerCase()) {
        case "color":
          Color c = Color.decode(value);
          setColor(c == null ? _color : c);
          break;
        case "intensity":
          _intensity = Double.parseDouble(value);
          break;
        case "dir":
          _outward = value.toLowerCase().equals("out");
          break;
        case "glyph":
          _type = ChemProbAnnotationType.annotTypeFromString(value);
          break;
        }
      }
    }
  }

  public void setType(ChemProbAnnotationType s) {
    _type = s;
  }

  @Override
  public ChemProbAnnotation clone() {
    ChemProbAnnotation result = new ChemProbAnnotation(this._firstBase,
        this._lastBase);
    result._intensity = _intensity;
    result._type = _type;
    result._color = _color;
    result._outward = _outward;
    return result;
  }

  @Override
  public String toString() {
    return "Chem. prob. " + this._type + " Base#"
        + this._firstBase.getResidueNumber() + "-"
        + this._lastBase.getResidueNumber();
  }

}
