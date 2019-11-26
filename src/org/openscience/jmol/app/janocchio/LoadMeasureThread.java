/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class LoadMeasureThread extends Thread {

  protected NMR_JmolPanel nmrPanel;
  protected BufferedReader inp;

  String command;
  
  public LoadMeasureThread() {}
  
  public LoadMeasureThread(NMR_JmolPanel nmrPanel, String data) {
    this.nmrPanel = nmrPanel;
    this.inp = new BufferedReader(new StringReader(data));
  }

  protected void addCommand(int i, String l) {
    command += ";" + nmrPanel.labelSetter.setLabel(i, l);
  }


  @Override
  public void run() {

    try {
      if (setLabels()) {
        String[] labelArray = nmrPanel.labelSetter.getLabelArray();
        nmrPanel.noeTable.setLabelArray(labelArray);
        nmrPanel.coupleTable.setLabelArray(labelArray);
      }
      setCouples();
      setNOEs();
      setMore();
      nmrPanel.noeTable.addMol();
      nmrPanel.noeTable.updateTables();
      nmrPanel.coupleTable.addMol();
      nmrPanel.coupleTable.updateTables();
      nmrPanel.vwr.scriptWait(command);
    } catch (Exception ie) {
      //Logger.debug("execution command interrupted!"+ie);
    } finally {
      try {
        inp.close();
      } catch (IOException e) {
      }
    }
  }

  protected void setMore() {
  }

  protected void setCouples() throws Exception {
    //Couples
    // ia ib ic id one-word-label
    
    String line;
    while ((line = inp.readLine()) != null) {
      if (line.trim().length() == 0)
        break;
      String[] l = line.split("\\s+");
      int ia = (new Integer(l[0])).intValue();
      int ib = (new Integer(l[1])).intValue();
      int ic = (new Integer(l[2])).intValue();
      int id = (new Integer(l[3])).intValue();
      addCouple(ia, ib, ic, id, l[4]);
    }
  }

  /**
   * 
   * @param ia
   * @param ib
   * @param ic
   * @param id
   * @param exp
   */
  protected void addCouple(int ia, int ib, int ic, int id, String exp) {
    command += ";measure @@" + ia + " @@" + ib + " @@" + ic + " @@" + id;
    if (exp != null && !exp.equals("null")) {
        nmrPanel.coupleTable.setExpCouple(exp, ia - 1, id - 1);
    }
  }

  protected void setNOEs() throws Exception {
    String line;
    // ia ib one-word-label
    while ((line = inp.readLine()).trim().length() != 0) {
      String[] l = line.split("\\s+");
      int ia = (new Integer(l[0])).intValue();
      int ib = (new Integer(l[1])).intValue();
      addNOE(ia, ib, l[2], null);
    }
  }

  protected void addNOE(int ia, int ib, String exp, String expd) {
    command += ";measure @@" + ia + " @@" + ib;
    if (exp != null && !exp.equals("null")) {
        nmrPanel.noeTable.setExpNoe(exp, ia - 1, ib - 1);
    }
    command = command + ";measure @@" + ia + " @@" + ib;
    if (exp != null) {
      nmrPanel.noeTable.setExpNoe(exp, ia - 1, ib - 1);
    }
    if (expd != null) {
      nmrPanel.noeTable.setExpDist(expd, ia - 1, ib - 1);
    }
  }

  protected boolean setLabels() throws Exception {
    String line;
    while ((line = inp.readLine()).trim().length() != 0) {
      String[] l = line.split("\\s+");
      int i = (new Integer(l[0])).intValue();
      command += ";" + nmrPanel.labelSetter.setLabel(i - 1, l[1]);
    }
    return true;
  }

  public void loadAndRun(String structureFile) {
    nmrPanel.runScriptWithCallback(this, "load \"" + structureFile + "\"");
  }
}
