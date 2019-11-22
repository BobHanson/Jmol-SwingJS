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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;

import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.Logger;

import javajs.util.SB;
import javajs.util.P3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;


/**
 * A JFrame that allows for choosing an Atomset to view.
 * 
 * @author Ren&eacute; Kanters, University of Richmond
 */
public class AtomSetChooser extends JFrame
implements TreeSelectionListener, PropertyChangeListener,
ActionListener, ChangeListener, Runnable {
  
  private Thread animThread = null;
  
  private JTextArea propertiesTextArea;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private Viewer vwr;
  private JCheckBox repeatCheckBox;
  private JSlider selectSlider;
  private JLabel infoLabel;
  private JSlider fpsSlider;
  private JSlider amplitudeSlider;
  private JSlider periodSlider;
  private JSlider scaleSlider;
  private JSlider radiusSlider;
  
  private JFileChooser saveChooser;

  
  // Strings for the commands of the buttons and the determination
  // of the tooltips and images associated with them
  static final String REWIND="rewind";
  static final String PREVIOUS="prev";
  static final String PLAY="play";
  static final String PAUSE="pause";
  static final String NEXT="next";
  static final String FF="ff";
  static final String SAVE="save";
  
  /**
   * String for prefix/resource identifier for the collection area.
   * This value is used in the Jmol properties files.
   */
  static final String COLLECTION = "collection";
  /**
   * String for prefix/resource identifier for the vector area.
   * This value is used in the Jmol properties files.
   */
  static final String VECTOR = "vector";
  

  /**
   * Sequence of atom set indexes in current tree selection for a branch,
   * or siblings for a leaf.
   */
  private int indexes[];
  private int currentIndex=-1;
  
  /**
   * Maximum value for the fps slider.
   */
  private static final int FPS_MAX = 30;
  /**
   * Precision of the vibration scale slider
   */
  private static final float AMPLITUDE_PRECISION = 0.01f;
  /**
   * Maximum value for vibration scale. Should be in preferences?
   */
  private static final float AMPLITUDE_MAX = 1;
  /**
   * Initial value of vibration scale. Should be in preferences?
   */
  private static final float AMPLITUDE_VALUE = 0.5f;

  /**
   * Precision of the vibration period slider in seconds.
   */
  private static final float PERIOD_PRECISION = 0.001f;
  /**
   * Maximum value for the vibration period in seconds. Should be in preferences?
   */
  private static final float PERIOD_MAX = 1; // in seconds
  /**
   * Initial value for the vibration period in seconds. Should be in preferences?
   */
  private static final float PERIOD_VALUE = 0.5f;

  /**
   * Maximum value for vector radius.
   */
  private static final int RADIUS_MAX = 19;
  /**
   * Initial value of vector radius. Should be in preferences?
   */
  private static final int RADIUS_VALUE = 3;

  /**
   * Precision of the vector scale slider
   */
  private static final float SCALE_PRECISION = 0.01f;
  /**
   * Maximum value for vector scale. Should be in preferences?
   */
  private static final float SCALE_MAX = 2.0f;
  /**
   * Initial value of vector scale. Should be in preferences?
   */
  private static final float SCALE_VALUE = 1.0f;

 
  
  public AtomSetChooser(Viewer vwr, JFrame frame) {
 //   super(frame,"AtomSetChooser", false);
    super(GT.$("AtomSetChooser"));
    this.vwr = vwr;
    
    // initialize the treeModel
    treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(GT.$("No AtomSets")));
    
    layoutWindow(getContentPane());
    pack();
    setLocationRelativeTo(frame);
    
  }
  
  private void layoutWindow(Container container) {
    
    container.setLayout(new BorderLayout());
    
    //////////////////////////////////////////////////////////
    // The tree and properties panel
    // as a split pane in the center of the container
    //////////////////////////////////////////////////////////
    JPanel treePanel = new JPanel();
    treePanel.setLayout(new BorderLayout());
    tree = new JTree(treeModel);
    tree.setVisibleRowCount(5);
    // only allow single selection (may want to change this later?)
    tree.getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);
    tree.setEnabled(false);
    treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
    // the panel for the properties
    JPanel propertiesPanel = new JPanel();
    propertiesPanel.setLayout(new BorderLayout());
    propertiesPanel.setBorder(new TitledBorder(GT.$("Properties")));
    propertiesTextArea = new JTextArea();
    propertiesTextArea.setEditable(false);
    propertiesPanel.add(new JScrollPane(propertiesTextArea), BorderLayout.CENTER);
    
    // create the split pane with the treePanel and propertiesPanel
    JPanel astPanel = new JPanel();
    astPanel.setLayout(new BorderLayout());
    astPanel.setBorder(new TitledBorder(GT.$("Atom Set Collection")));
    
    JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT, treePanel, propertiesPanel); 
    astPanel.add(splitPane, BorderLayout.CENTER);
    splitPane.setResizeWeight(0.5);

    container.add(astPanel, BorderLayout.CENTER);
    
    //////////////////////////////////////////////////////////
    // The Controller area is south of the container
    //////////////////////////////////////////////////////////
    JPanel controllerPanel = new JPanel();
    controllerPanel.setLayout(new BoxLayout(controllerPanel, BoxLayout.Y_AXIS));
    container.add(controllerPanel, BorderLayout.SOUTH);
    
    //////////////////////////////////////////////////////////
    // The collection chooser/controller/feedback area
    //////////////////////////////////////////////////////////
    JPanel collectionPanel = new JPanel();
    collectionPanel.setLayout(new BoxLayout(collectionPanel, BoxLayout.Y_AXIS));
    collectionPanel.setBorder(new TitledBorder(GT.$("Collection")));
    controllerPanel.add(collectionPanel);
    // info area
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel.setBorder(new TitledBorder(GT.$("Info")));
    infoLabel = new JLabel(" ");
    infoPanel.add(infoLabel, BorderLayout.SOUTH);
    collectionPanel.add(infoPanel);
    // select slider area
    JPanel cpsPanel = new JPanel();
    cpsPanel.setLayout(new BorderLayout());
    cpsPanel.setBorder(new TitledBorder(GT.$("Select")));
    selectSlider = new JSlider(0, 0, 0);
    selectSlider.addChangeListener(this);
    selectSlider.setMajorTickSpacing(5);
    selectSlider.setMinorTickSpacing(1);
    selectSlider.setPaintTicks(true);
    selectSlider.setSnapToTicks(true);
    selectSlider.setEnabled(false);
    cpsPanel.add(selectSlider, BorderLayout.SOUTH);
    collectionPanel.add(cpsPanel);
    // panel with controller and fps
    JPanel row = new JPanel();
    collectionPanel.add(row);
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    // repeat check box to be added to the controller
    repeatCheckBox = new JCheckBox(GT.$("Repeat"), false);
    JPanel vcrpanel = createVCRController(COLLECTION);
    vcrpanel.add(repeatCheckBox); // put the repeat text box in the vcr control
    // VCR-like play controller
    row.add(vcrpanel);
    // fps slider
    JPanel fpsPanel = new JPanel();
    row.add(fpsPanel);
    int fps = vwr.getInt(T.animationfps);
    if (fps > FPS_MAX)
      fps = FPS_MAX;
    fpsPanel.setLayout(new BorderLayout());
    fpsPanel.setBorder(new TitledBorder(GT.$("FPS")));
    fpsSlider = new JSlider(0, FPS_MAX, fps);
    fpsSlider.setMajorTickSpacing(5);
    fpsSlider.setMinorTickSpacing(1);
    fpsSlider.setPaintTicks(true);
    fpsSlider.setSnapToTicks(true);
    fpsSlider.addChangeListener(this);
    fpsPanel.add(fpsSlider, BorderLayout.SOUTH);

    //////////////////////////////////////////////////////////
    // The vector panel
    //////////////////////////////////////////////////////////
    JPanel vectorPanel = new JPanel();
    controllerPanel.add(vectorPanel);
    // fill out the contents of the vectorPanel
    vectorPanel.setLayout(new BoxLayout(vectorPanel, BoxLayout.Y_AXIS));
    vectorPanel.setBorder(new TitledBorder(GT.$("Vector")));
    // the first row in the vectoPanel: radius and scale of the vector
    JPanel row1 = new JPanel();
    row1.setLayout(new BoxLayout(row1,BoxLayout.X_AXIS));
    // controller for the vector representation
    JPanel radiusPanel = new JPanel();
    radiusPanel.setLayout(new BorderLayout());
    radiusPanel.setBorder(new TitledBorder(GT.$("Radius")));
    radiusSlider = new JSlider(0, RADIUS_MAX, RADIUS_VALUE);
    radiusSlider.setMajorTickSpacing(5);
    radiusSlider.setMinorTickSpacing(1);
    radiusSlider.setPaintTicks(true);
    radiusSlider.setSnapToTicks(true);
    radiusSlider.addChangeListener(this);
    script("vector "+ RADIUS_VALUE);
    radiusPanel.add(radiusSlider);
    row1.add(radiusPanel);
    // controller for the vector scale
    JPanel scalePanel = new JPanel();
    scalePanel.setLayout(new BorderLayout());
    scalePanel.setBorder(new TitledBorder(GT.$("Scale")));
    scaleSlider = new JSlider(0, (int)(SCALE_MAX/SCALE_PRECISION),
        (int) (SCALE_VALUE/SCALE_PRECISION));
    scaleSlider.addChangeListener(this);
    script("vector scale " + SCALE_VALUE);
    scalePanel.add(scaleSlider);
    row1.add(scalePanel);
    vectorPanel.add(row1);
    // the second row: amplitude and period of the vibration animation
    JPanel row2 = new JPanel();
    row2.setLayout(new BoxLayout(row2,BoxLayout.X_AXIS));
    // controller for vibrationScale = amplitude
    JPanel amplitudePanel = new JPanel();
    amplitudePanel.setLayout(new BorderLayout());
    amplitudePanel.setBorder(new TitledBorder(GT.$("Amplitude")));
    amplitudeSlider = new JSlider(0, (int) (AMPLITUDE_MAX/AMPLITUDE_PRECISION),
        (int)(AMPLITUDE_VALUE/AMPLITUDE_PRECISION));
    script("vibration scale " + AMPLITUDE_VALUE);
    amplitudeSlider.addChangeListener(this);
    amplitudePanel.add(amplitudeSlider);
    row2.add(amplitudePanel);
    // controller for the vibrationPeriod
    JPanel periodPanel = new JPanel();
    periodPanel.setLayout(new BorderLayout());
    periodPanel.setBorder(new TitledBorder(GT.$("Period")));
    periodSlider = new JSlider(0,
        (int)(PERIOD_MAX/PERIOD_PRECISION),
        (int)(PERIOD_VALUE/PERIOD_PRECISION));
    script("vibration " + PERIOD_VALUE + ";vibration off;");
    periodSlider.addChangeListener(this);
    periodPanel.add(periodSlider);
    row2.add(periodPanel);
    vectorPanel.add(row2);
    // finally the controller at the bottom
    vectorPanel.add(createVCRController(VECTOR));
  }
  
  /**
   * Creates a VCR type set of controller inside a JPanel.
   * 
   * <p>Uses the JmolResourceHandler to get the label for the panel,
   * the images for the buttons, and the tooltips. The button names are 
   * <code>rewind</code>, <code>prev</code>, <code>play</code>, <code>pause</code>,
   * <code>next</code>, and <code>ff</code>.
   * <p>The handler for the buttons should determine from the getActionCommand
   * which button in which section triggered the actionEvent, which is identified
   * by <code>{section}.{name}</code>.
   * @param section String of the section that the controller belongs to.
   * @return The JPanel
   */
  private JPanel createVCRController(String section) {
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
    controlPanel.setBorder(new TitledBorder((section.equals(COLLECTION) ? GT.$("Frame") : GT.$("Vibration"))));
    Insets inset = new Insets(1,1,1,1);
// take out the save functionality until the XYZ file can properly be created
//    String buttons[] = {REWIND,PREVIOUS,PLAY,PAUSE,NEXT,FF,SAVE};
    String buttons[] = {REWIND,PREVIOUS,PLAY,PAUSE,NEXT,FF};
    String tooltips[] = null;
    if (section.equals(COLLECTION)) {
      tooltips = new String[] {
          GT.$("Go to first atom set in the collection"),
          GT.$("Go to previous atom set in the collection"),
          GT.$("Play the whole collection of atom sets"),
          GT.$("Pause playing"),
          GT.$("Go to next atom set in the collection"),
          GT.$("Jump to last atom set in the collection")
      };
    } else if (section.equals(VECTOR)) {
      tooltips = new String[] {
          GT.$("Go to first atom set in the collection"),
          GT.$("Go to previous atom set in the collection"),
          GT.$("Vibration ON"),
          GT.$("Vibration OFF"),
          GT.$("Go to next atom set in the collection"),
          GT.$("Jump to last atom set in the collection")
      };
    }
    for (int i=buttons.length, idx=0; --i>=0; idx++) {
      String action = buttons[idx];
      // the icon and tool tip come from 
      JButton btn = new JButton(
          JmolResourceHandler.getIconX("AtomSetChooser."+action+"Image"));
      if ((tooltips != null) && (tooltips.length > idx)) {
        btn.setToolTipText(tooltips[idx]);
      }
      btn.setMargin(inset);
      btn.setActionCommand(section+"."+action);
      btn.addActionListener(this);
      controlPanel.add(btn);
    }
    controlPanel.add(Box.createHorizontalGlue());
    return controlPanel;
  }
  
  @Override
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
    tree.getLastSelectedPathComponent();
    if (node == null) {
      return;
    }
    try {
      int index = 0; // default for branch selection
      if (node.isLeaf()) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        setIndexes(parent); // the indexes are based what is in the parent
        index = parent.getIndex(node); // find out which index I had there
      } else { // selected branch
        setIndexes(node);
      }
      showAtomSetIndex(index, true);
    }
    catch (Exception exception) {
 //     exception.printStackTrace();
    }
  }
  
  /**
   * Show an atom set from the indexes array
   * @param index The index in the index array
   * @param bSetSelectSlider If true, updates the selectSlider
   */
  protected void showAtomSetIndex(int index, boolean bSetSelectSlider) {
    if (bSetSelectSlider) {
      selectSlider.setValue(index); // slider calls back to really set the frame
      return;
    }
    try {
      currentIndex = index;
      int atomSetIndex = indexes[index];
      script("frame " + vwr.getModelNumberDotted(atomSetIndex));
      infoLabel.setText(vwr.getModelName(atomSetIndex));
      showProperties(vwr.ms.am[atomSetIndex].properties);
      showAuxiliaryInfo(vwr.ms.getModelAuxiliaryInfo(atomSetIndex));
    } catch (Exception e) {
      // if this fails, ignore it.
    }
  }
  
  /**
   * Sets the indexes to the atomSetIndex values of each leaf of the node.
   * @param node The node whose leaf's atomSetIndex values should be used
   */
  protected void setIndexes(DefaultMutableTreeNode node) {
    int atomSetCount = node.getLeafCount();
    indexes = new int[atomSetCount];
    Enumeration<?> e = node.depthFirstEnumeration();
    int idx=0;
    while (e.hasMoreElements()) {
      node = (DefaultMutableTreeNode) e.nextElement();
      if (node.isLeaf())
        indexes[idx++]= ((AtomSet) node).getAtomSetIndex();
    }
    // now update the selectSlider (may trigger a valueChanged event...)
    selectSlider.setEnabled(atomSetCount>0);
    selectSlider.setMaximum(atomSetCount-1);
  }
  
  @Override
  public void actionPerformed (ActionEvent e) {
    String cmd = e.getActionCommand();
    String parts[]=cmd.split("\\.");
    try {
      if (parts.length==2) {
        String section = parts[0];
        cmd = parts[1];
        if (COLLECTION.equals(section)) {
         if (REWIND.equals(cmd)) {
            animThread = null;
            showAtomSetIndex(0, true);
          } else if (PREVIOUS.equals(cmd)) {
            showAtomSetIndex(currentIndex-1, true);
          } else if (PLAY.equals(cmd)) {
            if (animThread == null) {
              animThread = new Thread(this,"AtomSetChooserAnimationThread");
              animThread.start();
            }
          } else if (PAUSE.equals(cmd)) {
             animThread = null;
          } else if (NEXT.equals(cmd)) {
            showAtomSetIndex(currentIndex+1, true);
          } else if (FF.equals(cmd)) {
            animThread = null;
            showAtomSetIndex(indexes.length-1, true);
          } else if (SAVE.equals(cmd)) {
            saveXYZCollection();
          }
        } else if (VECTOR.equals(section)) {
          if (REWIND.equals(cmd)) {
            findFrequency(0,1);
          } else if (PREVIOUS.equals(cmd)) {
            findFrequency(currentIndex-1,-1);
          } else if (PLAY.equals(cmd)) {
            script("vibration on; vectors " + radiusValue);
          } else if (PAUSE.equals(cmd)) {
            script("vibration off; vectors off");
          } else if (NEXT.equals(cmd)) {
            findFrequency(currentIndex+1,1);
          } else if (FF.equals(cmd)) {
            findFrequency(indexes.length-1,-1);
          } else if (SAVE.equals(cmd)) {
            Logger.warn("Not implemented");
            // since I can not get to the vectors, I can't output this one (yet)
            // saveXYZVector();
          }
        }
      }
    } catch (Exception exception) {
      // exceptions during indexes array access: ignore it
    }
  }
  
  /**
   * Saves the currently active collection as a multistep XYZ file. 
   */
  public void saveXYZCollection() {
    int nidx = indexes.length;
    if (nidx==0) {
      Logger.warn("No collection selected.");
      return;
    }

    if (saveChooser == null)
      saveChooser = new JFileChooser();
    int retval = saveChooser.showSaveDialog(this);
    if (retval == 0) {
      File file = saveChooser.getSelectedFile();
      String fname = file.getAbsolutePath();
      try {
        PrintWriter f = new PrintWriter(new FileOutputStream(fname));
        for (int idx = 0; idx < nidx; idx++ ) {
          int modelIndex = indexes[idx];
          SB str = new SB();
          str.append(vwr.getModelName(modelIndex)).append("\n");
          int natoms=0;
          for (int i = 0, n = vwr.ms.ac; i < n;  i++) {
            if (vwr.ms.at[i].mi == modelIndex) {
              natoms++;
              P3 p = vwr.ms.at[i];
              // should really be getElementSymbol(i) in stead
              str.append(vwr.ms.at[i].getAtomName()).append("\t");
              str.appendF(p.x).append("\t").appendF(p.y).append("\t").appendF(p.z).append("\n");
              // not sure how to get the vibration vector and charge here...
            }
          }
          f.println(natoms);
          f.print(str);
        }
        f.close();
      } catch (FileNotFoundException e) {
        // e.printStackTrace();
      }
    }
  }
  
  /**
   * Have the vwr show a particular frame with frequencies
   * if it can be found.
   * @param index Starting index where to start looking for frequencies
   * @param increment Increment value for how to go through the list
   */
  public void findFrequency(int index, int increment) {
    int maxIndex = indexes.length;
    boolean foundFrequency = false;
    
    // search till get to either end of found a frequency
    while (index >= 0 && index < maxIndex 
        && !(foundFrequency=(vwr.modelHasVibrationVectors(indexes[index])))) {
      index+=increment;
    }
    
    if (foundFrequency) {
      showAtomSetIndex(index, true);      
    }
  }

  private int radiusValue = 1;
  
  @Override
  public void stateChanged(ChangeEvent e) {
    Object src = e.getSource();
    int value = ((JSlider) src).getValue();
    String cmd = null;
    if (src == selectSlider) {
      showAtomSetIndex(value, false);
    } else if (src == fpsSlider) {
      if (value == 0)
        fpsSlider.setValue(1); // make sure I never set it to 0...
      else
        cmd = "animation fps " + value;
    } else if (src == radiusSlider) {
      if (value == 0)
        radiusSlider.setValue(value = 1); // make sure I never set it to 0..
      else
        cmd = "vector " + value;
      radiusValue = value;
    } else if (src == scaleSlider) {
      cmd = "vector scale " + (value * SCALE_PRECISION);
    } else if (src == amplitudeSlider) {
      cmd = "vibration scale " + (value * AMPLITUDE_PRECISION);
    } else if (src == periodSlider) {
      cmd = "vibration " + (value * PERIOD_PRECISION);
    }
    if (cmd != null)
      script(cmd);
  }
  
  private void script(String cmd) {
    vwr.evalStringQuiet(cmd + JC.REPAINT_IGNORE);    
  }

  /**
   * Shows the properties in the propertiesPane of the
   * AtomSetChooser window
   * @param properties Properties to be shown.
   */
  protected void showProperties(Properties properties) {
    boolean needLF = false;
    propertiesTextArea.setText("");
    if (properties != null) {
      Enumeration<?> e = properties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        if (propertyName.startsWith("."))
          continue; // skip the 'hidden' ones
        propertiesTextArea.append((needLF?"\n ":" ") 
            + propertyName + "=" + properties.getProperty(propertyName));
        needLF = true;
      }
    }
  }
  
  /**
   * Shows the auxiliary information in the propertiesPane of the
   * AtomSetChooser window
   * @param auxiliaryInfo Hashtable to be shown.
   */
  protected void showAuxiliaryInfo(Map<String, Object> auxiliaryInfo) {
    String separator = " ";
    //propertiesTextArea.setText(""); AFTER properties
    if (auxiliaryInfo != null) {
      for (String keyName: auxiliaryInfo.keySet()) {
        if (keyName.startsWith("."))
          continue; // skip the 'hidden' ones
        //won't show objects properly, of course
        //equivalent in JavaScript is in Jmol-new.js
        propertiesTextArea.append(separator + keyName + "="
            + auxiliaryInfo.get(keyName));
        separator = "\n ";
      }
    }
  }
  
  /**
   * Creates the treeModel of the AtomSets available in the JmolViewer
   */
  private void createTreeModel() {
    String key = null;
    String separator = null;
    String name = vwr.ms.modelSetName;
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(
        name == null ? JC.ZAP_TITLE : name);

    // first determine whether we have a PATH_KEY in the modelSetProperties
    Properties modelSetProperties = (name == null ? null : vwr
        .getModelSetProperties());
    if (modelSetProperties != null) {
      key = modelSetProperties.getProperty("PATH_KEY");
      separator = modelSetProperties.getProperty("PATH_SEPARATOR");
    }
    if (key == null || separator == null) {
      // make a flat hierarchy if no key or separator are known
      if (name != null)
        for (int atomSetIndex = 0, count = vwr.ms.mc; atomSetIndex < count; ++atomSetIndex) {
          root.add(new AtomSet(atomSetIndex, vwr.getModelName(atomSetIndex)));
        }
    } else {
      for (int atomSetIndex = 0, count = vwr.ms.mc; atomSetIndex < count; ++atomSetIndex) {
        DefaultMutableTreeNode current = root;
        String path = vwr.ms.getModelProperty(atomSetIndex, key);
        // if the path is not null we need to find out where to add a leaf
        if (path != null) {
          DefaultMutableTreeNode child = null;
          String[] folders = path.split(separator);
          for (int i = 0, nFolders = folders.length; --nFolders >= 0; i++) {
            boolean found = false; // folder is initially not found
            String lookForFolder = folders[i];
            for (int childIndex = current.getChildCount(); --childIndex >= 0;) {
              child = (DefaultMutableTreeNode) current.getChildAt(childIndex);
              found = lookForFolder.equals(child.toString());
              if (found)
                break;
            }
            if (found) {
              current = child; // follow the found folder
            } else {
              // the 'folder' was not found: we need to add it
              DefaultMutableTreeNode newFolder = new DefaultMutableTreeNode(
                  lookForFolder);
              current.add(newFolder);
              current = newFolder; // follow the new folder
            }
          }
        }
        // current is the folder where the AtomSet is to be added
        current.add(new AtomSet(atomSetIndex, vwr.getModelName(atomSetIndex)));
      }
    }
    treeModel.setRoot(root);
    treeModel.reload();

    // en/dis able the tree based on whether the root has children
    tree.setEnabled(root.getChildCount() > 0);
    // disable the slider and set it up so that we don't have anything selected..
    indexes = null;
    currentIndex = -1;
    selectSlider.setEnabled(false);
  }
  
  /**
   * Objects in the AtomSetChooser tree
   */
  private static class AtomSet extends DefaultMutableTreeNode {
    /**
     * The index of that AtomSet
     */
    private int atomSetIndex;
    /**
     * The name of the AtomSet
     */
    private String atomSetName;
    
    public AtomSet(int atomSetIndex, String atomSetName) {
      this.atomSetIndex = atomSetIndex;
      this.atomSetName = atomSetName;
    }
    
    public int getAtomSetIndex() {
      return atomSetIndex;
    }
    
    @Override
    public String toString() {
      return atomSetName;
    }
    
  }
  
  ////////////////////////////////////////////////////////////////
  // PropertyChangeListener to receive notification that
  // the underlying AtomSetCollection has changed
  ////////////////////////////////////////////////////////////////
  
  @Override
  public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
    String eventName = propertyChangeEvent.getPropertyName();
    if (eventName.equals(JmolPanel.chemFileProperty)) {
      createTreeModel(); // all I need to do is to recreate the tree model
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    Thread myThread = Thread.currentThread();
    myThread.setPriority(Thread.MIN_PRIORITY);
    while (animThread == myThread) {
      // since user can change the tree selection, I need to treat
      // all variables as volatile.
      if (currentIndex < 0) {
        animThread = null; // kill thread if I don't have a proper index
      } else {
        ++currentIndex;
        if (currentIndex == indexes.length) {
          if (repeatCheckBox.isSelected())
            currentIndex = 0;  // repeat at 0
          else {
            currentIndex--;    // went 1 too far, step back
            animThread = null; // stop the animation thread
          }
        }
        showAtomSetIndex(currentIndex, true); // update the view
        try {
          // sleep for the amount of time required for the fps setting
          // NB the vwr's fps setting is never 0, so I could
          // set it directly, but just in case this behavior changes later...
          int fps = vwr.getInt(T.animationfps);
          Thread.sleep((int) (1000.0/(fps==0?1:fps)));
        } catch (InterruptedException e) {
          Logger.errorEx(null, e);
        }
      }
    }
  }
  
}
