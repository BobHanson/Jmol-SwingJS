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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.writers.CDXMLWriter;
import org.jmol.api.JmolAdapter;
import org.jmol.viewer.Viewer;

import javajs.util.Rdr;
import jme.JME;

public class JmolJME extends JME implements WindowListener {

  Viewer vwr;
  
  public JmolJME() {
    super(null, true);
  }
  
  public void setViewer(JFrame frame, Viewer vwr) {
    this.vwr = vwr;
    if (frame == null) {
      frame = new JFrame("Jmol/JME 2D Molecular Editor");
      JPanel p = new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 2));
      JButton b;
      p.add(b = new JButton("from Jmol"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          fromJmol();
        }
        
      });
      p.add(b = new JButton("replace 3D model"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          to3D(false);
        }
        
      });
      p.add(b = new JButton("add 3D model"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          to3D(true);
        }
        
      });
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
      frame.add("South", p);
      frame.setBounds(300, 300, 600, 400);
      frame.addWindowListener(this);
      setFrame(frame);
    }
    frame.setResizable(true);
    frame.setVisible(true);
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
      loadHydrogensOnCarbon = false;
      readMolFile(mol);
    } catch (Exception e) {
      sorry(e.toString());
      e.printStackTrace();
    }
  }

  private String getMolFromSmiles(String smiles, boolean is3D) {
    System.out.println("JmolJME using SMILES " + smiles);
    String url = (String) vwr.setLoadFormat((is3D ? "$" : "$$") + smiles, '$', false);
    return vwr.getFileAsString(url);
  }

  private void sorry(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Sorry, can't do that.", JOptionPane.INFORMATION_MESSAGE);
  }

  public void to3D(boolean isAppend) {
    String mol = getMolFromSmiles(smiles(), true);
    vwr.openStringInlineParamsAppend(mol, null, isAppend);
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


}