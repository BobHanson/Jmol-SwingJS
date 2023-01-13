/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-07-22 20:29:48 -0500 (Sun, 22 Jul 2018) $
 * $Revision: 21922 $
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
package org.openscience.jmol.app.jmolpanel;

import java.awt.Container;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Resolver;
import org.jmol.adapter.writers.CDXMLWriter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolDropEditor;
import org.jmol.awt.FileDropper;
import org.jmol.smiles.SmilesMatcher;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.PT;
import jme.JME;
import jme.JMEmol;

public class JmolJME extends JME implements WindowListener, JmolDropEditor {

  Viewer vwr;
  private String fileName;
  private Container window;
  
  public JmolJME() {
    super(null, true);
    keepHydrogens = false;
  }
  
  public void setViewer(JFrame frame, Viewer vwr, Container parent) {
    window = parent;
    this.vwr = vwr;
    setDropTarget(new DropTarget(this, new FileDropper(null, null, this)));

    if (frame == null) {
      frame = new JFrame(getTitle());
      frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent componentEvent) {
          mol.center();
        }
      });
      JPanel pp = new JPanel();
      JPanel p = new JPanel();
      pp.add("Center", p);
      p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.X_AXIS));
      JButton b;
      p.add(b = new JButton("clean"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          doClean();
        }

      });
      p.add(b = new JButton("from 3D"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          fromJmol();
        }

      });
      p.add(Box.createHorizontalStrut(5));
      p.add(b = new JButton("replace 3D"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          to3D(false);
        }

      });
      p.add(b = new JButton("add 3D"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          to3D(true);
        }

      });
      p.add(Box.createHorizontalStrut(5));
      p.add(b = new JButton("to MOL"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          toMOL();
        }

      });
      p.add(b = new JButton("to CDXML"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          toCDXML();
        }

      });
      p.add(b = new JButton("close"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          myFrame.setVisible(false);
        }

      });
      frame.add("South", pp);
      frame.setBounds(300, 300, 600, 400);
      frame.addWindowListener(this);
      setFrame(frame);
    }
    frame.setResizable(true);
    frame.setVisible(true);
  }

  private String getTitle() {
    return "Jmol/JME 2D Molecular Editor" + (fileName == null ? "" : " " + fileName);
  }

  @Override
  public void windowOpened(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowClosing(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowClosed(WindowEvent e) {
    myFrame.setVisible(false);
  }

  @Override
  public void windowIconified(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowActivated(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
    // TODO
    
  }

  public void dispose() {
    myFrame.dispose();
  }

  public void fromJmol() {
    if (vwr.getFrameAtoms().isEmpty())
      return;
    Map<String, Object> info = vwr.getCurrentModelAuxInfo();
    if (info == null) {
      sorry("More than one model is visible in Jmol.");
      return;
    }
    boolean is2D = "2D".equals(info.get("dimension"));
    String mol = null;
    try {
      if (is2D) {
        mol = vwr.getModelExtract("thisModel", false, false, "MOL");
      } else {
        String smiles = vwr.getSmiles(vwr.getFrameAtoms());
        mol = getMolFromSmiles(smiles, false);
      }
      if (mol == null) {
        sorry("Something went wrong.");
      }
      readMolFile(mol);
    } catch (Exception e) {
      sorry(e.toString());
      e.printStackTrace();
    }
  }

  
  @Override
  public String smiles() {
    if (mol.natoms == 0)
      return "";
    String mol = molFile();
    if (mol.length() == 0)
      return "";
    String smiles = vwr.getInchi(null, mol, "smiles");
//
//    
//    // problems with stereochemistry??
//    String smiles = vwr.getSmilesMatcher().getSmilesFromJME(jmeFile());
    repaint(); // aby ked je chyba v smilesi (stereo) aby sa objavilo info
    return smiles;
  }

  public String inchi() {
    return vwr.getInchi(null, molFile(), "standard");
  }
  /**
   * Resolve a SMILES using the NCI/CADD Chemical Identifier Resolver
   * @param smiles
   * @param is3D
   * @return SMILES
   */
  private String getMolFromSmiles(String smiles, boolean is3D) {
    System.out.println("JmolJME using SMILES " + smiles);
    String url = JC.resolveDataBase("smiles" + (is3D? "3D" : "2D"), PT.escapeUrl(smiles), null);

    //(String) vwr.setLoadFormat((is3D ? "$" : "$$") + smiles, '$', false);
    return vwr.getFileAsString(url);
  }

  private void sorry(String msg) {
    System.err.println(msg);
    JOptionPane.showMessageDialog(this, msg, "Sorry, can't do that.", JOptionPane.INFORMATION_MESSAGE);
  }

  private void say(String msg) {
    System.out.println(msg);
    infoText = msg;
//    JOptionPane.showMessageDialog(this, msg, "Hmm.", JOptionPane.INFORMATION_MESSAGE);
  }

  public void to3D(boolean isAppend) {
    String smiles = smiles();
    if (smiles == null || smiles.length() == 0) {
      sorry("There was a problem generating the SMILES from the InChI");
      return;
    }
    System.out.println("using smiles from InChI: " + smiles);
    String mol = getMolFromSmiles(smiles, true);
    Map<String, Object> htParams = new Hashtable<String, Object>();
    vwr.openStringInlineParamsAppend(mol, htParams, isAppend);
    window.requestFocus();
    vwr.refresh(Viewer.REFRESH_REPAINT, "JmolJME");
    
  }

  public void setFrameVisible(boolean b) {
    myFrame.setVisible(b);
  }

  protected void toMOL() {
    String mol = molFile();
    toFile("?jmol.mol", mol);
  }

  private void toFile(final String name, final String mol) {
    new Thread(new Runnable() {

      @Override
      public void run() {
        vwr.writeTextFile(name, mol);
      }
    }).start();
  }

  protected void toCDXML() {
    String mol = molFile();
    String xml = CDXMLWriter.fromString(vwr, "Mol", mol);
    toFile("?jmol.cdxml", xml);
  }

  /**
   * When dropped, if the data are 2D, then read directly into JME. If 3D then
   * transform via InChI to SMILES and then resolver to 2D.
   * 
   */
  @Override
  public void loadFile(String fname) {
    try {
      setFileName(fname);
      // from file dropper
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fname));
      boolean isBinary = (Resolver.getBinaryType(bis) != null);
      String type = vwr.getModelAdapter().getFileTypeName(bis);
      bis.close();
      if ("Jme".equals(type)) {
        readMolecule(vwr.getFileAsString(fname));
        mol.center();
        return;
      }
      Map<String, Object> htParams = new Hashtable<String, Object>();
      htParams.put("filter", "NOH;fileType=" + type);
      htParams.put("binary", Boolean.valueOf(isBinary));
      vwr.setLoadParameters(htParams, false);
      bis = new BufferedInputStream(new FileInputStream(fname));
      Object ret = vwr.getModelAdapter().getAtomSetCollectionFromReader(fname,
          bis, htParams);
      if (ret instanceof AtomSetCollection) {
        AtomSetCollection asc = (AtomSetCollection) ret;
        Map<String, Object> info = asc.getAtomSetAuxiliaryInfo(0);
        boolean is2D = "2D".equals(info.get("dimension"));
        if (is2D) {
          drop2D(fname, asc);
        } else {
          drop3D(fname);
        }
        mol.center();
      } else {
        sorry(ret.toString());
      }
    } catch (Exception e) {
      sorry(e.toString());
      e.printStackTrace();
    }
    repaint();
    System.out.println("JJME " + fname);
  }
  
  private void setFileName(String fname) {
    // TODO
    this.fileName = fname;
  }

  private void drop2D(String fname, AtomSetCollection asc) {
    this.fileName = fname;
    reaction = false;
     JmolAdapter a = vwr.getModelAdapter();
     readAtomSet(a.getAtomIterator(asc), vwr.getModelAdapter().getBondIterator(asc));
  }

  private void readAtomSet(JmolAdapterAtomIterator atomIterator,
                           JmolAdapterBondIterator bondIterator) {
    reset();
    mol = mols[++nmols] = new JMEmol(this,atomIterator, bondIterator);
    if (!setMol(true)) {
      say("Close atoms found; cleaning");
      doClean();
      repaint();
    }   
  }
  
  @Override
  protected boolean setMol(boolean checkMultipart) {
    super.setMol(checkMultipart);
    // have atoms; check for very close
    for (int i = mol.natoms + 1; --i >= 1;) {
      double x = mol.getX(i);
      double y = mol.getY(i);
      for (int j = i; --j >= 1;) {
        double x2 = mol.getX(j);
        double y2 = mol.getY(j);
        if (Math.abs(x - x2)
            + Math.abs(y - y2) < 0.1d) {
          System.out.println(i + " " + j + " " + x + " " + y + " " + x2 + " " + y2);
          return false;
        }
      }
    }
    return true;
  }

    
  
  private void drop3D(String fname) {
    String mol = vwr.getFileAsString(fname);
    loadSmilesCleanly(getSmiles(mol));
  }

  private String getSmiles(String mol) {
    return vwr.getInchi(null, mol, "smiles");
  }

  @Override
  public void loadContent(String script) {
    // n/a
  }

  void doClean() {
    String smiles = vwr.getInchi(null, molFile(), "smiles");
    loadSmilesCleanly(smiles);
  }

  private boolean cleaning = false;
  /**
   * SMILES to InChI to MOL
   * @param smiles
   */
  private void loadSmilesCleanly(String smiles) {
    if (smiles == null || smiles.length() == 0 || cleaning)
      return;
    System.out.println("using smiles from InChI: " + smiles);
    String mol = null;
    try {
      cleaning = true;
      mol = getMolFromSmiles(smiles, false);
      if (mol == null) {
        sorry("Something went wrong.");
      } else {
        readMolFile(mol);
      }
    } catch (Exception e) {
      sorry(e.toString());
      e.printStackTrace();
    } finally {
      cleaning = false;
    }
  }

}
