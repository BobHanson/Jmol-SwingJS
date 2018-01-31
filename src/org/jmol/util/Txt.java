/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import javajs.util.P3;
import javajs.util.PT;

import org.jmol.api.JmolViewer;

public class Txt {

  public static String formatText(JmolViewer vwr, String text0) {
    int i;
    if ((i = text0.indexOf("@{")) < 0 && (i = text0.indexOf("%{")) < 0)
      return text0;

    // old style %{ now @{

    String text = text0;
    boolean isEscaped = (text.indexOf("\\") >= 0);
    if (isEscaped) {
      text = PT.rep(text, "\\%", "\1");
      text = PT.rep(text, "\\@", "\2");
      isEscaped = !text.equals(text0);
    }
    text = PT.rep(text, "%{", "@{");
    String name;
    while ((i = text.indexOf("@{")) >= 0) {
      i++;
      int i0 = i + 1;
      int len = text.length();
      // push to math terminator
      int nP = 1;
      char chFirst = '\0';
      char chLast = '\0';
      while (nP > 0 && ++i < len) {
        char ch = text.charAt(i);
        if (chFirst != '\0') {
          if (chLast == '\\') {
            ch = '\0';
          } else if (ch == chFirst) {
            chFirst = '\0';
          }
          chLast = ch;
          continue;
        }
        switch(ch) {
        case '\'':
        case '"':
          chFirst = ch;
          break;
        case '{':
          nP++;
          break;
        case '}':
          nP--;
          break;
        }
      }
      if (i >= len)
        return text;
      name = text.substring(i0, i);
      if (name.length() == 0)
        return text;
      Object v = vwr.evaluateExpression(name);
      if (v instanceof P3)
        v = Escape.eP((P3) v);
      text = text.substring(0, i0 - 2) + v.toString() + text.substring(i + 1);
    }
    if (isEscaped) {
      text = PT.rep(text, "\2", "@");
      text = PT.rep(text, "\1", "%");
    }
    return text;
  }

}
