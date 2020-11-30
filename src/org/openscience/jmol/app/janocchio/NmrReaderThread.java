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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JOptionPane;

class NmrReaderThread extends Thread {

  File file;
  NMR_JmolPanel nmr;

  NmrReaderThread(File file, NMR_JmolPanel nmr) {
    this.file = file;
    this.nmr = nmr;
  }

  @Override
  public void run() {
    BufferedReader inp = null;
    try {
      inp = new BufferedReader(new FileReader(file));
      String line;

      //Structure File
      String currentStructureFile = nmr.getCurrentStructureFile();
      line = inp.readLine().trim();
      if (currentStructureFile == null) {
        int opt = JOptionPane.showConfirmDialog(nmr,
            "No Structure File currently loaded.\nLoad Structure File " + line
                + "\ndefined in this NMR Data File?", "No Structure Warning",
            JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
          nmr.vwr.scriptWait("load " + line);
          //  ScriptWaitThread thread = new ScriptWaitThread("load " + line, viewer);
          //  thread.start();
        } else {
          return;
        }
        nmr.vwr.openFile(line);
      } else {
        if (!currentStructureFile.equals(line)) {

          //Warn that the structure data was saved from a different file name from the current model
          int opt = JOptionPane
              .showConfirmDialog(
                  nmr,
                  "This NMR Data file was saved from a different structure file from that currently loaded.\nContinue Reading Data?",
                  "Read NMR Data Warning", JOptionPane.YES_NO_OPTION);
          if (opt == JOptionPane.NO_OPTION) {
            return;
          }

        }
      }

      String command = new String();

      //labels
      while ((line = inp.readLine()).trim().length() != 0) {
        String[] l = line.split("\\s+");
        int i = (new Integer(l[0])).intValue();
        String com = nmr.labelSetter.setLabel(i - 1, l[1]);
        command = command + ";" + com;
      }
      String[] labelArray = nmr.labelSetter.getLabelArray();
      nmr.noeTable.setLabelArray(labelArray);
      nmr.coupleTable.setLabelArray(labelArray);

      // Noes
      while ((line = inp.readLine()).trim().length() != 0) {
        String[] l = line.split("\\s+");
        command = command + ";measure " + l[0] + " " + l[1];
        if (l[2] != null) {
          if (!l[2].equals("null")) {
            int i = (new Integer(l[0])).intValue();
            int j = (new Integer(l[1])).intValue();
            nmr.noeTable.setExpNoe(l[2], i - 1, j - 1);
          }
        }
      }

      //Couples
      while ((line = inp.readLine()) != null) {
        if (line.trim().length() == 0)
          break;
        String[] l = line.split("\\s+");
        command = command + ";measure " + l[0] + " " + l[1] + " " + l[2] + " "
            + l[3];
        if (l[4] != null) {
          if (!l[4].equals("null")) {
            int i = (new Integer(l[0])).intValue();
            int j = (new Integer(l[3])).intValue();
            nmr.coupleTable.setExpCouple(l[4], i - 1, j - 1);
          }
        }
      }

      nmr.noeTable.updateTables();
      nmr.coupleTable.updateTables();
      nmr.vwr.script(command);

    } catch (Exception ie) {
    } finally {
      try {
        if (inp != null)
          inp.close();
      } catch (IOException e) {
      }
    }
  }
}
