/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-13 00:06:10 -0500 (Wed, 13 Sep 2006) $
 * $Revision: 5516 $
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
import java.util.Iterator;
import java.util.Map;

import javajs.util.P3;
import javajs.util.PT;

import org.jmol.api.GenericPlatform;

import javajs.util.BS;

import org.jmol.modelset.Text;
import org.jmol.util.C;
import org.jmol.util.Font;
import org.jmol.util.Logger;

public abstract class TextShape extends Shape {

  // Hover and Echo
  
  public Map<String, Text> objects = new Hashtable<String, Text>();
  Text currentObject;
  Font currentFont;
  Object currentColor;
  Object currentBgColor;
  float currentTranslucentLevel;
  float currentBgTranslucentLevel;
  protected String thisID;
  
  boolean isHover;
  boolean isAll;

//  @Override
//  public void setProperty(String propertyName, Object value, BS bsSelected) {
//    setPropTS(propertyName, value, bsSelected);
//  }

  protected void setPropTS(String propertyName, Object value, BS bsSelected) {
    if ("text" == propertyName) {
      String text = (String) value;
      if (currentObject != null) {
        currentObject.setText(text);
      } else if (isAll) {
        for (Text t : objects.values())
          t.setText(text);
      }
      return;
    }

    if ("font" == propertyName) {
      currentFont = (Font) value;
      if (currentObject != null) {
        currentObject.setFont(currentFont, true);
        currentObject.setFontScale(0);
      } else if (isAll) {
        for (Text t : objects.values())
          t.setFont(currentFont, true);
      }
      return;
    }

    if ("allOff" == propertyName) {
      currentObject = null;
      isAll = true;
      objects = new Hashtable<String, Text>();
      return;
    }

    if ("delete" == propertyName) {
      if (value instanceof Text) {
        currentObject = (Text) value;
      }
      if (currentObject != null) {
          objects.remove(currentObject.target);
        currentObject = null;
      } else if (isAll || thisID != null) {
        Iterator<Text> e = objects.values().iterator();
        while (e.hasNext()) {
          Text text = e.next();
          if (isAll
              || PT.isMatch(text.target.toUpperCase(), thisID, true, true)) {
            e.remove();
          }
        }
      }
      return;
    }

    if ("off" == propertyName) {
      if (isAll) {
        objects = new Hashtable<String, Text>();
        isAll = false;
        currentObject = null;
      }
      if (currentObject == null) {
        return;
      }

      objects.remove(currentObject.target);
      currentObject = null;
      return;
    }

    if ("model" == propertyName) {
      int modelIndex = ((Integer) value).intValue();
      if (currentObject != null) {
        currentObject.modelIndex = modelIndex;
      } else if (isAll) {
        for (Text t : objects.values())
          t.modelIndex = modelIndex;
      }
      return;
    }

    if ("align" == propertyName) {
      String align = (String) value;
      if (currentObject != null) {
        if (!currentObject.setAlignmentLCR(align))
          Logger.error("unrecognized align:" + align);
      } else if (isAll) {
        for (Text obj : objects.values())
          obj.setAlignmentLCR(align);
      }
      return;
    }

    if ("bgcolor" == propertyName) {
      currentBgColor = value;
      if (currentObject != null) {
        currentObject.bgcolix = C.getColixO(value);
      } else if (isAll) {
        Iterator<Text> e = objects.values().iterator();
        while (e.hasNext()) {
          e.next().bgcolix = C.getColixO(value);
        }
      }
      return;
    }

    if ("color" == propertyName) {
      currentColor = value;
      if (currentObject != null) {
        currentObject.colix = C.getColixO(value);
      } else if (isAll || thisID != null) {
        Iterator<Text> e = objects.values().iterator();
        while (e.hasNext()) {
          Text text = e.next();
          if (isAll
              || PT.isMatch(text.target.toUpperCase(), thisID, true, true)) {
            text.colix = C.getColixO(value);
          }
        }
      }
      return;
    }

    if ("target" == propertyName) {
      String target = (String) value;
      isAll = target.equals("all");
      if (isAll || target.equals("none")) {
        currentObject = null;
      }
      //handled by individual types -- echo or hover
      return;
    }

    boolean isBackground;
    if ((isBackground = ("bgtranslucency" == propertyName))
        || "translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      if (isBackground)
        currentBgTranslucentLevel = (isTranslucent ? translucentLevel : 0);
      else
        currentTranslucentLevel = (isTranslucent ? translucentLevel : 0);
      if (currentObject != null) {
        currentObject.setTranslucent(translucentLevel, isBackground);
      } else if (isAll) {
        Iterator<Text> e = objects.values().iterator();
        while (e.hasNext()) {
          e.next().setTranslucent(translucentLevel, isBackground);
        }
      }
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Text> e = objects.values().iterator();
      while (e.hasNext()) {
        Text text = e.next();
        if (text.modelIndex == modelIndex) {
          e.remove();
        } else if (text.modelIndex > modelIndex) {
          text.modelIndex--;
        }
      }
      return;
    }

    setPropS(propertyName, value, bsSelected);
  }

  @Override
  public String getShapeState() {
    // not implemented -- see org.jmol.viewer.StateCreator
    return null;
  }
  
  @Override
  protected void initModelSet() {
    currentObject = null;
    isAll = false;
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    if (!isHover)
      for (Text t : objects.values())
        t.visible = (t.modelIndex < 0 || bsModels.get(t.modelIndex));
  }

  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int modifiers, BS bsVisible, boolean drawPicking) {
    if (isHover || modifiers == 0)
      return null;
    boolean isAntialiased = vwr.antialiased;
    for (Text obj: objects.values()) {
      if (obj.checkObjectClicked(isAntialiased, x, y, bsVisible)) {
        if (obj.script != null)
          vwr.evalStringQuiet(obj.script);
        Map<String, Object> map = new Hashtable<String, Object>();
        map.put("pt", (obj.xyz == null ? new P3() : obj.xyz));
        int modelIndex = obj.modelIndex;
        if (modelIndex < 0)
          modelIndex = 0;
        map.put("modelIndex", Integer.valueOf(modelIndex));
        map.put("model", vwr.getModelNumberDotted(modelIndex));
        map.put("id", obj.target);
        map.put("type", "echo");
        return map;
      }
    }
    return null;
  }

  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (isHover)
      return false;
    boolean haveScripts = false;
    boolean isAntialiased = vwr.antialiased;
    for (Text obj: objects.values()) {
      if (obj.script != null) {
        haveScripts = true;
        if (obj.checkObjectClicked(isAntialiased, x, y, bsVisible)) {
          vwr.setCursor(GenericPlatform.CURSOR_HAND);
          return true;
        }
      }
    }
    if (haveScripts)
      vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    return false;
  }
  
}

