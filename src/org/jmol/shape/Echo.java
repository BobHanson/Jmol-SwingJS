/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-06-14 23:36:56 -0500 (Tue, 14 Jun 2016) $
 * $Revision: 21144 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;

import javajs.util.BS;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.viewer.JC;

public class Echo extends TextShape {

  /*
   * set echo Text.TOP    [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo MIDDLE [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo BOTTOM [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name   [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name  x-position y-position
   * set echo none  to initiate setting default settings
   * 
   */

  private final static String FONTFACE = "Serif";
  private final static int FONTSIZE = 20;
  private final static short COLOR = C.RED;

  public Text scaleObject;
  
  @Override
  public void initShape() {
    setProperty("target", "top", null);
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

     if ("thisID" == propertyName) {
      if (value == null) {
        currentObject = null;
        thisID = null;
        return;
      }
      String target = (String) value;
      if (target == "%SCALE") {
        currentObject = scaleObject;
        thisID = target;
      } else {
        currentObject = objects.get(target);
        if (currentObject == null && PT.isWild(target))
          thisID = target.toUpperCase();
      }
      return;
    }

    if ("%SCALE" == propertyName) {
      currentObject = scaleObject = (Text) value;
      thisID = "%SCALE";
      return;
    }

    if ("color" == propertyName || "font" == propertyName) {
      if (scaleObject != null && currentObject == scaleObject) {
        Font f = currentFont;
        Object c = currentColor;
        setPropTS(propertyName, value, bs);
        currentFont = f;
        currentColor = c;
        return;
      }
      // continue to TextShape
    }

    if ("off" == propertyName) {
      if (currentObject == scaleObject) {
        currentObject = scaleObject = null;
        return;
      }
      // continue to TextShape
    }

    if ("text" == propertyName) {
      if (((String) value).startsWith("%SCALE")) {
        thisID = "%SCALE";
        setPropTS("text", value, null);
        scaleObject = currentObject;
        if (scaleObject != null && objects.get(scaleObject.target) == scaleObject)
          setPropTS("delete", scaleObject, null);
        currentObject = scaleObject;
        return;
      }
      // continue to TextShape
    }
    if ("target" == propertyName) {
      if ("%SCALE".equals(value)) {
        currentObject = scaleObject;
        thisID = "%SCALE";
        if (currentObject != null)
          return;
      }
      String target = ((String) value).intern().toLowerCase();
      if (target != "none" && target != "all") {
        isAll = false;
        Text text = (thisID == "%SCALE" ? scaleObject : objects.get(target));
        if (text == null) {
          int valign = JC.ECHO_XY;
          int halign = JC.TEXT_ALIGN_LEFT;
          if ("top" == target) {
            valign = JC.ECHO_TOP;
            halign = JC.TEXT_ALIGN_CENTER;
          } else if ("middle" == target) {
            valign = JC.ECHO_MIDDLE;
            halign = JC.TEXT_ALIGN_CENTER;
          } else if ("bottom" == target) {
            valign = JC.ECHO_BOTTOM;
          } else if ("error" == target) {
            valign = JC.ECHO_TOP;
          }
          //          if (scaleObject != null && scaleObject.valign == valign
          //              && scaleObject.align == halign) {
          //            text = scaleObject;
          //          } else {
          text = Text.newEcho(vwr, vwr.gdata.getFont3DFS(FONTFACE, FONTSIZE),
              target, COLOR, valign, halign, 0);
          text.adjustForWindow = true;
          if (thisID == "%SCALE")
            scaleObject = text;
          else
            objects.put(target, text);
          if (currentFont != null)
            text.setFont(currentFont, true);
          if (currentColor != null)
            text.colix = C.getColixO(currentColor);
          if (currentBgColor != null)
            text.bgcolix = C.getColixO(currentBgColor);
          if (currentTranslucentLevel != 0)
            text.setTranslucent(currentTranslucentLevel, false);
          if (currentBgTranslucentLevel != 0)
            text.setTranslucent(currentBgTranslucentLevel, true);
          //          }
        }
        currentObject = text;
        if (thisID != "%SCALE")
          thisID = null;
        return;
      }
    }

    if ("scalereference" == propertyName) {
      if (currentObject != null) {
        double val = ((Number) value).doubleValue();
        currentObject.setScalePixelsPerMicron(val == 0 ? 0 : 10000d / val);
      }
      return;
    }

    if ("point" == propertyName) {
      if (currentObject != null) {
        Text t = currentObject;
        t.pointerPt = (value == null ? null : (P3d) value); // could be an atom.
        t.pointer = (value == null ? JC.LABEL_POINTER_NONE
            : JC.LABEL_POINTER_ON);
      }
      return;
    }

    if ("xyz" == propertyName) {
      if (currentObject != null && vwr.getBoolean(T.fontscaling))
        currentObject.setScalePixelsPerMicron(
            vwr.getScalePixelsPerAngstrom(false) * 10000d);
      // continue on to Object2d setting
    }

    if ("scale" == propertyName) {
      if (currentObject != null) {
        (currentObject).setScale(((Number) value).doubleValue());
      } else if (isAll) {
        for (Text t : objects.values())
          t.setScale(((Number) value).doubleValue());
      }
      return;
    }

    if ("image" == propertyName) {
      if (currentObject != null) {
        (currentObject).setImage(value);
      } else if (isAll) {
        for (Text t : objects.values())
          t.setImage(value);
      }
      return;
    }

    if ("hidden" == propertyName) {
      boolean isHidden = ((Boolean) value).booleanValue();
      if (currentObject != null) {
        (currentObject).hidden = isHidden;
      } else if (isAll || thisID != null) {
        for (Text t : objects.values())
          if (isAll || PT.isMatch(t.target.toUpperCase(), thisID, true, true))
            t.hidden = isHidden;
      }
      return;
    }

    if ("script" == propertyName) {
      if (currentObject != null)
        currentObject.setScript((String) value);
      return;
    }

    if ("xpos" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ(null, true);
        currentObject.setMovableX(((Integer) value).intValue());
      }
      return;
    }

    if ("ypos" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ(null, true);
        currentObject.setMovableY(((Integer) value).intValue());
      }
      return;
    }

    if ("%xpos" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ(null, true);
        currentObject.setMovableXPercent(((Integer) value).intValue());
      }
      return;
    }

    if ("%ypos" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ(null, true);
        currentObject.setMovableYPercent(((Integer) value).intValue());
      }
      return;
    }

    if ("%zpos" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ(null, true);
        currentObject.setMovableZPercent(((Integer) value).intValue());
      }
      return;
    }

    if ("xypos" == propertyName) {
      if (currentObject != null) {
        P3d pt = (P3d) value;
        currentObject.setXYZ(null, true);
        if (pt.z == Double.MAX_VALUE) {
          currentObject.setMovableX((int) pt.x);
          currentObject.setMovableY((int) pt.y);
        } else {
          currentObject.setMovableXPercent((int) pt.x);
          currentObject.setMovableYPercent((int) pt.y);
        }
      }
      return;
    }

    if ("xyz" == propertyName) {
      if (currentObject != null) {
        currentObject.setXYZ((P3d) value, true);
        if (currentObject.pymolOffset == null)
          currentObject.pymolOffset = new double[] { -1, 2, 0, 0, 0, 0, 0 };        
      }
      return;
    }

    if ("offset" == propertyName) {
      if (currentObject != null) {
        currentObject.pymolOffset = (double[]) value;
      }
      return;
    }

    setPropTS(propertyName, value, null);
  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if ("currentTarget" == property) {
      return (currentObject != null && (data[0] = currentObject.target) != null);
    }
    if (property == "%SCALE") {
      data[0] = scaleObject;
      return (data[0] != null);
    }
    if (property == "checkID") {
      String key = ((String) data[0]).toUpperCase();
      boolean isWild = PT.isWild(key);
      for (Text t: objects.values()) {
        String id = t.target;
        if (id.equalsIgnoreCase(key) || isWild
            && PT.isMatch(id.toUpperCase(), key, true, true)) {
          data[1] = id;
          return true;
        }
      }
      return false;
    }
    return getPropShape(property, data);
  }

  //  @Override
  //  public String getShapeState() {
  //    // not implemented -- see org.jmol.viewer.StateCreator
  //    return null;
  //  }

  @Override
  public Object getShapeDetail() {
    Map<String, Object> lst = new Hashtable<String, Object>();
    for (Entry<String, Text> e : objects.entrySet()) {
      Map<String, Object> info = new Hashtable<String, Object>();
      Text t = e.getValue();
      String name = e.getKey();
      info.put("boxXY", t.boxXY);
      if (t.xyz != null)
        info.put("xyz", t.xyz);
      Object o = t.image;
      if (o == null) {
        info.put("text", t.text == null ? "" : t.text);
      } else {
        info.put("imageFile", t.text);
        info.put("imageWidth",
            Integer.valueOf(vwr.apiPlatform.getImageWidth(o)));
        info.put("imageHeight",
            Integer.valueOf(vwr.apiPlatform.getImageHeight(o)));
      }
      lst.put(name, info);
    }
    Lst<Map<String, Object>> lst2 = new Lst<Map<String, Object>>();
    lst2.addLast(lst);
    return lst2;
  }

}
