/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.simple;

import javajs.util.P3;
import javajs.util.PT;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Atom;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

public class JSONReader extends AtomSetCollectionReader {
  /*
   * a simple JSON-format file reader
   * See, for example, http://web.chemdoodle.com/docs/chemdoodle-json-format
   * 
   * feature request #210
   * 
   * Bob Hanson hansonr@stolaf.edu 11/13/2013 
   */

  /*

    {"mol":{
      "_scale":{"x":1,"y":1,"z":1},
      "a":[{"x":3.9236999,"y":-0.9222,"z":0.1835},{"x":3.2479,"y":-3.2106004,"z":0.3821},{"x":5.1731,"y":-1.3284999,"z":-0.24640003},{"x":4.4973,"y":-3.6169,"z":-0.0478},{"x":5.4598002,"y":-2.6759,"z":-0.3623},{"x":1.599,"y":-1.4203,"z":0.9663},{"x":-4.2137,"y":0.8188001,"z":2.5929},{"x":-5.7525997,"y":0.1604,"z":0.70350003},{"l":"H","x":-0.92130005,"y":-0.6858,"z":0.8503},{"x":2.961,"y":-1.8632,"z":0.49760002},{"l":"O","x":-4.989,"y":2.5026002,"z":-1.2333001},{"l":"O","x":-1.2756001,"y":1.6640999,"z":-1.9360001},{"l":"O","x":1.104,"y":-1.4738001,"z":-1.3405999},{"l":"O","x":-4.604,"y":3.4702,"z":0.7158},{"x":-4.4305005,"y":2.47,"z":-0.1623},{"x":0.68810004,"y":-1.2541,"z":-0.2227},{"x":-3.5391,"y":1.3063,"z":0.1875},{"x":-1.4742,"y":-0.7,"z":-1.1997},{"x":-1.8847001,"y":0.7218999,"z":-1.4753001},{"l":"H","x":-5.185,"y":4.1949,"z":0.44660002},{"l":"N","x":-0.5887,"y":-0.86149997,"z":-0.043799996},{"x":-2.9578,"y":-0.84800005,"z":-0.8823999},{"x":-4.298,"y":0.3443,"z":1.1408},{"l":"S","x":-3.3189998,"y":-1.1949,"z":0.8809},{"l":"N","x":-3.159,"y":0.59889996,"z":-1.0386},{"l":"H","x":-2.6423,"y":1.6747,"z":0.6855},{"l":"H","x":-3.5207,"y":-1.4693998,"z":-1.5789001},{"l":"H","x":-4.6569,"y":1.8111,"z":2.6771998},{"l":"H","x":-4.7551003,"y":0.123500004,"z":3.2344003},{"l":"H","x":-3.1692,"y":0.86,"z":2.9017},{"l":"H","x":-5.7794,"y":-0.2569,"z":-0.3031},{"l":"H","x":-6.2558002,"y":-0.5187,"z":1.3918},{"l":"H","x":-6.2588997,"y":1.1256,"z":0.71029997},{"l":"H","x":-1.1443,"y":-1.2523,"z":-2.0796},{"l":"H","x":1.1846,"y":-2.1707997,"z":1.6393999},{"l":"H","x":1.6871,"y":-0.46960002,"z":1.4921},{"l":"H","x":3.7012,"y":0.1303,"z":0.2784},{"l":"H","x":2.4957001,"y":-3.9457002,"z":0.62750006},{"l":"H","x":5.9251003,"y":-0.5933,"z":-0.4921},{"l":"H","x":4.7215,"y":-4.6695,"z":-0.13759999},{"l":"H","x":6.4357004,"y":-2.9933,"z":-0.6989}],
      
      "b":[{"b":10,"e":14,"o":2},{"b":13,"e":14},{"b":14,"e":16},{"b":16,"e":24},{"b":16,"e":22},{"b":21,"e":24},{"b":18,"e":24},{"b":6,"e":22},{"b":7,"e":22},{"b":22,"e":23},{"b":21,"e":23},{"b":17,"e":21},{"b":17,"e":18},{"b":11,"e":18,"o":2},{"b":17,"e":20},{"b":15,"e":20},{"b":5,"e":15},{"b":12,"e":15,"o":2},{"b":5,"e":9},{"b":0,"e":9,"o":2},{"b":1,"e":9},{"b":0,"e":2},{"b":1,"e":3,"o":2},{"b":2,"e":4,"o":2},{"b":3,"e":4},{"b":13,"e":19},{"b":16,"e":25},{"b":21,"e":26},{"b":6,"e":27},{"b":6,"e":28},{"b":6,"e":29},{"b":7,"e":30},{"b":7,"e":31},{"b":7,"e":32},{"b":17,"e":33},{"b":8,"e":20},{"b":5,"e":34},{"b":5,"e":35},{"b":0,"e":36},{"b":1,"e":37},{"b":2,"e":38},{"b":3,"e":39},{"b":4,"e":40}]
      
    }}

   */

  private P3 scale;

  @Override
  public void initializeReader() throws Exception {
    asc.setCollectionName("JSON");
    asc.newAtomSet();
    String s = "";
    while (rd() != null)
      s += line;
    s = PT.replaceAllCharacters(s, "\" ", "").replace(',', ':');
    if (s.contains("_is2D:true"))
      set2D();
    if (s.contains("_scale:"))
      getScaling(getSection(s, "_scale", false));
    s = PT.replaceAllCharacters(s, "}", "").replace(',', ':');
    readAtoms(getSection(s, "a", true));
    readBonds(getSection(s, "b", true));
    continuing = false;
  }

  private void getScaling(String[] s) {
    String[] xyz = PT.split(s[0], ":");
    scale = P3.new3(1, 1, 1);
    for (int j = 0; j < xyz.length; j += 2)
      if (xyz[j].length() == 1)
        switch (xyz[j].charAt(0)) {
        case 'x':
          scale.x = parseFloatStr(xyz[j + 1]);
          break;
        case 'y':
          scale.y = parseFloatStr(xyz[j + 1]);
          break;
        case 'z':
          scale.z = parseFloatStr(xyz[j + 1]);
          break;
        }
    Logger.info("scale set to " + scale);
  }

  private String[] getSection(String json, String key, boolean isArray) {
    String[] a = PT.split(json, key + ":" + (isArray ? "[" : "") + "{");
    if (a.length < 2)
      return a;
    String data = a[1];
    data = data.substring(0, data.indexOf((isArray ? "]" : "}"))) + ":";
    return PT.split(data, "{");
  }

  private void readAtoms(String[] atoms) throws Exception {
    //  x :78.474: y :18.444: z :3.67
    //  l : H : x :-18.426 :  y :13.716001 : z :17.006

    for (int i = 0; i < atoms.length; ++i) {
      String[] lxyz = PT.split(atoms[i],":");
      Atom atom = asc.addNewAtom();
      float x = 0, y = 0, z = 0;
      String l = "C";
      for (int j = 0; j < lxyz.length; j += 2)
        if (lxyz[j].length() == 1)
          switch (lxyz[j].charAt(0)) {
          case 'x':
            x = parseFloatStr(lxyz[j + 1]);
            break;
          case 'y':
            y = parseFloatStr(lxyz[j + 1]);
            break;
          case 'z':
            z = parseFloatStr(lxyz[j + 1]);
            break;
          case 'l':
            l = lxyz[j + 1];
            break;
          }
      if (scale != null) {
        x /= scale.x;
        y /= scale.y;
        z /= scale.z;
      }
      setAtomCoordXYZ(atom, x, y, z);
      atom.elementSymbol = l;
    }
  }

  private void readBonds(String[] bonds) throws Exception {
    for (int i = 0; i < bonds.length; ++i) {
      String[] beo = PT.split(bonds[i],":");
      int b = 0, e = 0, order = 1;
      for (int j = 0; j < beo.length; j += 2)
        if (beo[j].length() == 1)
          switch (beo[j].charAt(0)) {
          case 'b':
            b = parseIntStr(beo[j + 1]);
            break;
          case 'e':
            e = parseIntStr(beo[j + 1]);
            break;
          case 'o':
            // allows 0, 1, 1.5, 2, 2.5, 3, 4
            int o = (int) (parseFloatStr(beo[j + 1]) * 2);
            switch (o) {
            case 0:
              continue;
            case 2:
            case 4:
            case 6:
            case 8:
              order = o / 2;
              break;
            case 1:
              order = JmolAdapter.ORDER_PARTIAL01;
              break;
            case 3:
              order = JmolAdapter.ORDER_PARTIAL12;
              break;
            case 5:
              order = JmolAdapter.ORDER_PARTIAL23;
              break;
            default:
              order = 1;
              break;
            }
            break;
          }
      asc.addBond(new Bond(b, e, order));
    }
  }
}
