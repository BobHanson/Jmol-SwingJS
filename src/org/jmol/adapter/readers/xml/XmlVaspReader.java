/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.readers.xml;



import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Logger;
import javajs.util.PT;

import javajs.util.SB;
import javajs.util.V3;

/**
 * 
 * Vasp vasprun.xml reader
 * 
 * @author hansonr
 * 
 */

public class XmlVaspReader extends XmlReader {
  
  private SB data;
  private String name;
  private int ac;
  private int iAtom;
  private boolean isE_wo_entrp = false;
  private boolean isE_fr_energy = false;
  private String enthalpy = null;
  private String gibbsEnergy = null;
//  private String[] myAttributes = { "name" }; 
  
  public XmlVaspReader() {
  }
  
//  @Override
//  protected String[] getDOMAttributes() {
//    return myAttributes;
//  }

  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    parent.doProcessLines = true;
    processXml2(parent, saxReader);
  }

  @Override
  public void processStartElement(String localName, String nodeName) {
    if (debugging) 
      Logger.debug("xmlvasp: start " + localName);

    if (!parent.continuing)
      return;

    if ("calculation".equals(localName)) {
      enthalpy = null;
      gibbsEnergy = null;
      return;
    }
        
    if ("i".equals(localName)) {
      String s = atts.get("name");
      if (s.charAt(0) != 'e')
        return;
      isE_wo_entrp = s.equals("e_wo_entrp");
      isE_fr_energy = s.equals("e_fr_energy");
      setKeepChars(isE_wo_entrp || isE_fr_energy);
      return;
    }

    if ("structure".equals(localName)) {
      if (!parent.doGetModel(++parent.modelNumber, null)) {
        parent.checkLastModel();
        return;
      }
      parent.setFractionalCoordinates(true);
      asc.doFixPeriodic = true;
      asc.newAtomSet();
      if (enthalpy != null) {
        asc.setCurrentModelInfo("enthalpy", Double.valueOf(enthalpy));
      }
      if (gibbsEnergy != null) {
        asc.setAtomSetEnergy("" + gibbsEnergy, parseFloatStr(gibbsEnergy));
        asc.setCurrentModelInfo("gibbsEnergy", Double.valueOf(gibbsEnergy));
      }
      if (enthalpy != null && gibbsEnergy != null)
        asc.setAtomSetName("Enthalpy = " + enthalpy + " eV Gibbs Energy = " + gibbsEnergy + " eV");
      return;
    }
    if (!parent.doProcessLines)
      return;
    
    if ("v".equals(localName)) {
      setKeepChars(data != null);
      return;
    }

    if ("c".equals(localName)) {
      setKeepChars(iAtom < ac);
      return;
    }

    if ("varray".equals(localName)) {
      name = atts.get("name");
      if (name != null && PT.isOneOf(name, ";basis;positions;forces;"))
        data = new SB();
      return;
    }

    if ("atoms".equals(localName)) {
      setKeepChars(true);
      return;
    }
    
  }

  boolean haveUnitCell = false;
  String[] atomNames;
  String[] atomSyms;
  String atomName;
  String atomSym;
  float a;
  float b;
  float c;
  float alpha;
  float beta;
  float gamma;
  
  @Override
  void processEndElement(String localName) {

    if (debugging) 
      Logger.debug("xmlvasp: end " + localName);

    while (true) {

      if (!parent.doProcessLines)
        break;

      if (isE_wo_entrp) {
        isE_wo_entrp = false;
        enthalpy = chars.toString().trim();
        break;
      }

      if (isE_fr_energy) {
        isE_fr_energy = false;
        gibbsEnergy = chars.toString().trim();
        break;
      }

      if ("v".equals(localName) && data != null) {
        data.append(chars.toString());
        break;
      }

      if ("c".equals(localName)) {
        if (iAtom < ac) {
          if (atomName == null) {
            atomName = atomSym = chars.toString().trim();
          } else {
            atomNames[iAtom++] = atomName + chars.toString().trim();
            atomName = null;
          }
        }
        break;
      }

      if ("atoms".equals(localName)) {
        ac = parseIntStr(chars.toString());
        atomNames = new String[ac];
        atomSyms = new String[ac];
        iAtom = 0;
        break;
      }

      if ("varray".equals(localName) && data != null) {
        if (name == null) {
        } else if ("basis".equals(name) && !haveUnitCell) {
          haveUnitCell = true;
          float[] ijk = getTokensFloat(data.toString(), null, 9);
          V3 va = V3.new3(ijk[0], ijk[1], ijk[2]);
          V3 vb = V3.new3(ijk[3], ijk[4], ijk[5]);
          V3 vc = V3.new3(ijk[6], ijk[7], ijk[8]);
          a = va.length();
          b = vb.length();
          c = vc.length();
          va.normalize();
          vb.normalize();
          vc.normalize();
          alpha = (float) (Math.acos(vb.dot(vc)) * 180 / Math.PI);
          beta = (float) (Math.acos(va.dot(vc)) * 180 / Math.PI);
          gamma = (float) (Math.acos(va.dot(vb)) * 180 / Math.PI);
        } else if ("positions".equals(name)) {
          parent.setUnitCell(a, b, c, alpha, beta, gamma);
          float[] fdata = new float[ac * 3];
          getTokensFloat(data.toString(), fdata, ac * 3);
          int fpt = 0;
          for (int i = 0; i < ac; i++) {
            Atom atom = asc.addNewAtom();
            parent.setAtomCoordXYZ(atom, fdata[fpt++], fdata[fpt++], fdata[fpt++]);
            atom.elementSymbol = atomSyms[i];
            atom.atomName = atomNames[i];
          }
        } else if ("forces".equals(name)) {
          float[] fdata = new float[ac * 3];
          getTokensFloat(data.toString(), fdata, ac * 3);
          int fpt = 0;
          int i0 = asc.getLastAtomSetAtomIndex();

          //TODO question here as to whether these need transformation

          for (int i = 0; i < ac; i++)
            asc.addVibrationVector(i0 + i, fdata[fpt++],
                fdata[fpt++], fdata[fpt++]);
        }
        data = null;
        break;
      }
      if ("structure".equals(localName)) {
        try {
          parent.applySymmetryAndSetTrajectory();
        } catch (Exception e) {
          // TODO
        }
        break;
      }
      
      return;
    }
    setKeepChars(false);
  }

}
