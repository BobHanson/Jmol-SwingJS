/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-02-02 22:24:37 -0600 (Sun, 02 Feb 2014) $
 * $Revision: 19253 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.c.CBK;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Logger;

/**
 * 
 * A legacy Chime-compatible messenger. Enabled using set messageStyleChime.
 * Probably used only by ProteinExplorer. Consolidates all Chime business and
 * gets this code out of Viewer and other more commonly used classes.
 * 
 */

public class ChimeMessenger implements JmolChimeMessenger {

  private Viewer vwr;

  public ChimeMessenger() {
    // for reflection
  }
  
  @Override
  public JmolChimeMessenger set(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }
  
  /**
   * called when an atom is picked
   * 
   */
  @Override
  public String getInfoXYZ(Atom a) {
    String group3 = a.getGroup3(true);
    int chainID = a.group.chain.chainID;
    return "Atom: "
        + (group3 == null ? a.getElementSymbol() : a.getAtomName())
        + " "
        + a.getAtomNumber()
        + (group3 != null && group3.length() > 0 ? (a.isHetero() ? " Hetero: "
            : " Group: ")
            + group3
            + " "
            + a.getResno()
            + (chainID != 0 && chainID != 32 ? " Chain: "
                + a.group.chain.getIDStr() : "") : "") + " Model: "
        + a.getModelNumber() + " Coordinates: " + a.x + " " + a.y + " " + a.z;
  }

  /**
   * #xxxx command output
   */
  @Override
  public void showHash(SB outputBuffer, String s) {
    if (s == null)
      return;
    if (outputBuffer == null) {
      if (!vwr.isPrintOnly)
        Logger.warn(s);
      vwr.scriptStatus(s);
    } else {
      outputBuffer.append(s).appendC('\n');
    }
  }

  /**
   * report atom selection in Chime format
   * 
   */
  @Override
  public void reportSelection(int n) {
    vwr.reportSelection((n == 0 ? "No atoms" : n == 1 ? "1 atom" : n + " atoms")
        + " selected!");
  }

  /**
   * called upon script exit and file opening
   */
  @Override
  public void update(String msg) {
    if (msg == null)
      msg = "script <exiting>";
    else 
      msg = "Requesting " + msg;
    vwr.scriptStatus(msg);    
  }

  /**
   * called when a script exits.
   * 
   */
  @Override
  public String scriptCompleted(StatusManager sm, String statusMessage,
                                String strErrorMessageUntranslated) {
    Object[] data = new Object[] { null, "script <exiting>", statusMessage,
        Integer.valueOf(-1), strErrorMessageUntranslated };
    if (sm.notifyEnabled(CBK.SCRIPT))
      sm.cbl.notifyCallback(CBK.SCRIPT, data);
    sm.processScript(data);
    return "Jmol script completed.";
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public void getAllChimeInfo(SB sb) {
    int nHetero = 0;
    int nH = -1;
    int nS = 0;
    int nT = 0;
    ModelSet ms = vwr.ms;
    if (ms.haveBioModels) {
      int n = 0;
      Model[] models = ms.am;
      int modelCount = ms.mc;
      int ac = ms.ac;
      Atom[] atoms = ms.at;
      sb.append("\nMolecule name ....... " + ms.getInfoM("COMPND"));
      sb.append("\nSecondary Structure . PDB Data Records");
      sb.append("\nBrookhaven Code ..... " + ms.modelSetName);
      for (int i = modelCount; --i >= 0;)
        n += models[i].getChainCount(false);
      sb.append("\nNumber of Chains .... " + n);
      int ng = 0;
      int ngHetero = 0;
      Map<Group, Boolean> map = new Hashtable<Group, Boolean>();
      int id;
      int lastid = -1;
      nH = 0;
      for (int i = ac; --i >= 0;) {
        Atom a = atoms[i];
        if (a == null)
          continue;
        boolean isHetero = a.isHetero();
        if (isHetero)
          nHetero++;
        Group g = a.group;
        if (!map.containsKey(g)) {
          map.put(g, Boolean.TRUE);
          if (isHetero)
            ngHetero++;
          else
            ng++;
        }
        if (a.mi == 0) {
          if ((id = g.getStrucNo()) != lastid && id != 0) {
            lastid = id;
            switch (g.getProteinStructureType()) {
            case HELIX:
              nH++;
              break;
            case SHEET:
              nS++;
              break;
            case TURN:
              nT++;
              break;
            }
          }
        }
      }
      sb.append("\nNumber of Groups .... " + ng);
      if (ngHetero > 0)
        sb.append(" (" + ngHetero + ")");
    }
    sb.append("\nNumber of Atoms ..... " + (ms.ac - nHetero));
    if (nHetero > 0)
      sb.append(" (" + nHetero + ")");
    sb.append("\nNumber of Bonds ..... " + ms.bondCount);
    sb.append("\nNumber of Models ...... " + ms.mc);

    if (nH >= 0) {
      sb.append("\nNumber of Helices ... " + nH);
      sb.append("\nNumber of Strands ... " + nS);
      sb.append("\nNumber of Turns ..... " + nT);
    }
  }

  @Override
  public String getChimeInfoA(Atom[] atoms, int tok, BS bs) {
    SB info = new SB();
    info.append("\n");
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        switch (tok) {
        default:
          return "";
        case T.selected:
          s = a.getInfo();
          break;
        case T.atoms:
          s = "" + a.getAtomNumber();
          break;
        case T.group:
          s = a.getGroup3(false);
          break;
        case T.chain:
        case T.residue:
        case T.sequence:
        case T.group1:
          int id = a.getChainID();
          s = (id == 0 ? " " : a.getChainIDStr());
          if (id >= 300)
            s = PT.esc(s);
          switch (tok) {
          case T.residue:
            s = "[" + a.getGroup3(false) + "]" + a.group.getSeqcodeString() + ":" + s;
            break;
          case T.sequence:
          case T.group1:
            if (a.mi != modelLast) {
              info.appendC('\n');
              n = 0;
              modelLast = a.mi;
              info.append("Model " + a.getModelNumber());
              glast = null;
              clast = null;
            }
            if (a.group.chain != clast) {
              info.appendC('\n');
              n = 0;
              clast = a.group.chain;
              info.append("Chain " + s + ":\n");
              glast = null;
            }
            Group g = a.group;
            if (g != glast) {
              glast = g;
              if (tok == T.group1) {
                info.append(a.getGroup1('?'));
              } else {
                if ((n++) % 5 == 0 && n > 1)
                  info.appendC('\n');
                PT.leftJustify(info, "          ", "[" + a.getGroup3(false)
                    + "]" + a.getResno() + " ");
              }
            }
            continue;
          }
          break;
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).appendC('\n');
      }
    if (tok == T.sequence)
      info.appendC('\n');
    return info.toString().substring(1);
  }


}
