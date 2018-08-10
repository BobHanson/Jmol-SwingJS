/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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
package org.gennbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;


public class NBOUtil {

  public static void postAddGlobalC(SB sb, String label, String val) {
    sb.append("GLOBAL C_").append(label).append(" ").append(val).append(sep);  
   }

  public static void postAddGlobalI(SB sb, String label, int offset, JComboBox<String> cb) {
    sb.append("GLOBAL I_").append(label).append(" ").appendI(cb == null ? offset : cb.getSelectedIndex() + offset).append(sep);  
   }

  public static void postAddGlobalT(SB sb, String key, JTextField t) {
    sb.append("GLOBAL ").append(key).append(" ").append(t.getText()).append(sep);
  }

  public static void postAddGlobal(SB sb, String key, String val) {
    sb.append("GLOBAL ").append(key).append(" ").append(val).append(sep);
  }

  public static SB postAddCmd(SB sb, String cmd) {
    return sb.append("CMD ").append(cmd).append(sep);
  }

  /**
   * Creates the title blocks with background color for headers.
   * 
   * @param title
   *        - title for the section
   * @param rightSideComponent
   *        help button, for example
   * @return Box formatted title box
   */
  public static Box createTitleBox(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(NBOConfig.titleColor);
    label.setForeground(Color.white);
    label.setFont(NBOConfig.titleFont);
    label.setOpaque(true);
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.add(label, BorderLayout.WEST);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box2.setMaximumSize(new Dimension(360, 25));
      box.add(box2);
    } else
      box.add(label);
    box.setAlignmentX(0.0f);
  
    return box;
  }
  
  /**
   * Creates the title blocks with background color for headers for NBOModel.
   * Difference of this method in compared to for other models is that the "Help" is being shifted
   * to stick to the right position x=400 (NBOModel Divider's size is different from other models)
   * 
   * @param title
   *        - title for the section
   * @param rightSideComponent
   *        help button, for example
   * @return Box formatted title box
   */
  public static Box createTitleBoxForNBOModel(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(NBOConfig.titleColor);
    label.setForeground(Color.white);
    label.setFont(NBOConfig.titleFont);
    label.setOpaque(true);
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.add(label, BorderLayout.WEST);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box2.setMaximumSize(new Dimension(400, 25));
      box.add(box2);
    } else
      box.add(label);
    box.setAlignmentX(0.0f);
  
    return box;
  }
  
  
  /**
   * create a bordered box, either vertical or horizontal
   * 
   * @param isVertical
   * @return a box
   */
  public static Box createBorderBox(boolean isVertical) {
    Box box = isVertical ? Box.createVerticalBox() : Box.createHorizontalBox();
    box.setAlignmentX(0.0f);
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    return box;
  }

  public static final String sep = System.getProperty("line.separator");

  public static String round(double value, int places) {
    return PT.formatD(value, places, -1 - places, false, false, true);
//    if (places < 0)
//      throw new IllegalArgumentException();
//    BigDecimal bd = new BigDecimal(value);
//    bd = bd.setScale(places, RoundingMode.HALF_UP);
//    return bd.doubleValue();
  }

  /**
   * Get the listing  of the atoms picked -- in order of picking, if a bond
   * @param data
   * @return [atom1, Integer.MIN_VALUE] (atom) or [atom1, atom2] (bond) or [Integer.MIN_VALUE, 0] (other)
   */
  public static int[] getAtomsPicked(Object[] data) {
    int atom1 = ((Integer) data[2]).intValue();
    //System.out.println("----" + type.toString() + ":  " + atomIndex);
    if (atom1 >= 0)
      return new int[] { atom1 + 1, Integer.MIN_VALUE };
    if (atom1 != -3)
      return new int[] { Integer.MIN_VALUE, 0 };
    // bond
    String[] sdata = PT.split(data[1].toString(), "#");
    atom1 = PT.parseInt(sdata[1]);
    int atom2 = PT.parseInt(sdata[2]);
    System.out.println(atom1 + " picked " + atom2);
    return new int[] { atom1, atom2};
  }

  public static String addNBOKeyword(String tmp, String s) {
    int pt;
    if ((pt = findKeyword(tmp, s, false)) >= 0)
      return tmp.substring(0, pt) + tmp.substring(pt + 1); 
    if (findKeyword(tmp, s, true) >= 0)
      return tmp;
    if (tmp.length() + s.length() - tmp.lastIndexOf(NBOUtil.sep) >= 80)
      tmp += NBOUtil.sep + " ";
    if (tmp.length() == 0)
      tmp = " ";
    tmp += s.toUpperCase() + " ";
    return tmp;
  }

  /**
   * check for a full keyword; all must be caps
   * @param keywords
   * @param s
   * @param ifPresent true for is in the list; false for is not in the list
   * @return point of " " or KEYWORD_NEGATE PRIOR TO KEYWORD or -1 if not present
   */
  public static int findKeyword(String keywords, String s, boolean ifPresent) {
    int pt;
    if (!keywords.startsWith(" "))
      keywords = " " + keywords + " ";
    String prefix = (ifPresent ? " " : KEYWORD_IGNORE);
    return ((pt = keywords.indexOf(prefix + s + " ")) >= 0  ? pt 
        : (pt = keywords.indexOf(prefix + s + "=")) >= 0 ? pt : -1);
  }

  private static String KEYWORD_IGNORE = "_";
  
  public static String removeNBOKeyword(String keywords, String name) {
    // look for "_NAME " or "_NAME="
    int pt = findKeyword(keywords, name, false);
    if (pt >= 0)
      return keywords;
    // look for " NAME " or " NAME=" 
    pt = findKeyword(keywords, name, true);
    if (pt < 0)
      return keywords;
    // found that -- must negate
    return keywords.substring(0, pt) + " " + KEYWORD_IGNORE + keywords.substring(pt + 1);
  }

  public static String removeNBOFileKeyword(String nboKeywords, String[] fnameRet) {
    
    // TODO Q: what about "FILE xxxx" ?
    
    String[] tokens = PT.getTokens(nboKeywords);
    nboKeywords = "";
    for (int i = 0; i < tokens.length; i++)
      if (tokens[i].indexOf("=") < 0  || tokens[i].toUpperCase().indexOf("FILE=") < 0)
        nboKeywords += " " + tokens[i].toUpperCase();
      else if (fnameRet != null)
        fnameRet[0] = tokens[i].substring(5);
    return nboKeywords.trim();
  }

  public static boolean lineContainsUncommented(String line, String key) {
    int ptComment;
    int ptKey = line.indexOf(key);
    return (ptKey >= 0 && ((ptComment = line.indexOf("!")) < 0 || ptComment > ptKey));
  }

  public static String removeNBOComment(String line) {
    int ptComment = line.indexOf("!");
    return (ptComment < 0 ? line : line.substring(0, ptComment));
  }

  /**
   * Read a file reducing lines to
   * 
   * @param inputFile
   * @param data
   * @param doAll set false to only read through $NBO keyword 
   * @return true if successful; false if not
   */
  public static boolean read47FileBuffered(File inputFile, SB data, boolean doAll) {
    try {
      boolean have$NBO = false, haveNBO$END = false;
      BufferedReader b = null;
      b = new BufferedReader(new FileReader(inputFile));
      String line;
      while ((line = b.readLine()) != null && (doAll || !line.contains("$COORD"))) {
        if (have$NBO && !haveNBO$END || !have$NBO && (have$NBO = lineContainsUncommented(line, "$NBO")) == true) {
          line = removeNBOComment(line);
          if (line.indexOf("$END") >= 0) {
            haveNBO$END = true;
          }
        }
        data.append(line + NBOFileHandler.sep);
      }
      b.close();
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  public static String getExt(File newFile) {
    String fname = newFile.toString();
    return fname.substring(fname.lastIndexOf(".") + 1);
  }

  public static String getJobStem(File inputFile) {
    String fname = inputFile.getName();
    return fname.substring(0, fname.lastIndexOf("."));
  }

  public static String pathWithoutExtension(String fname) {
    int pt = fname.lastIndexOf(".");
    return (pt < 0 ? fname : fname.substring(0, pt));
  }

  public static File newNBOFile(File f, String ext) {
    return new File(pathWithoutExtension(NBOUtil.fixPath(f.toString())) + "." + ext);
  }

  public static String fix47File(String data) {
    return PT.rep(data, "FORMAT=PRECISE", ""); 
    
  }

  /**
   * change \ to /
   * 
   * @param path
   * @return fixed path
   */
  public static String fixPath(String path) {
    return path.replace('\\',  '/');
  }

  /**
   * Windows-centric elaboration of path to default to C: drive
   * 
   * @param folder
   * @param fullFilePath
   * @return full file path, using "C:/" for the default.
   */
  public static String getWindowsFolderFor(String folder, String fullFilePath) {
    return (folder.equals("") ? new File(fullFilePath).getParent()
        : !folder.contains(":") ? folder = "C:/" + folder : folder); 
  }

  /**
   * if folder is not blank, create a full path name to the designated file
   * @param folder
   * @param name root name or root.ext
   * @param ext ext or null if name already contains it
   * @return full Windows path to file
   */
  public static String getWindowsFullNameFor(String folder, String name,
                                             String ext) {
    if (!folder.equals("")) {
      if (!folder.contains(":"))
        folder = "C:/" + folder;
      folder = folder + "/" + (name.equals("") ? "" : name + (ext == null ? "" : "." + ext));
    }
    return folder;
  }

  ///**
  //* Centers the dialog on the screen.
  //* 
  //* @param d
  //*/
  //public void centerDialog(JDialog d) {
  // int x = getWidth() / 2 - d.getWidth() / 2 + this.getX();
  // int y = getHeight() / 2 - d.getHeight() / 2;
  // d.setLocation(x, y);
  //}
  //

}

class StyledComboBoxUI extends MetalComboBoxUI {

  public int height;
  public int width;

  public StyledComboBoxUI(int h, int w) {
    super();
    height = h;
    width = w;
  }

  @Override
  public ComboPopup createPopup() {
    BasicComboPopup popup = new BasicComboPopup(comboBox) {
      @Override
      public Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        return super.computePopupBounds(px, py, Math.max(width, pw), height);
      }
    };
    popup.getAccessibleContext().setAccessibleParent(comboBox);
    return popup;
  }
}
