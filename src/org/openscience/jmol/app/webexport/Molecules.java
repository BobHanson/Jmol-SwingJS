/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
package org.openscience.jmol.app.webexport;

//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.PrintStream;
//
//import javax.swing.BorderFactory;
//import javax.swing.DefaultListModel;
//import javax.swing.JButton;
//import javax.swing.JComboBox;
//import javax.swing.JComponent;
//import javax.swing.JFileChooser;
//import javax.swing.JLabel;
//import javax.swing.JList;
import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTextField;
//import javax.swing.ListSelectionModel;

class Molecules extends JPanel {//implements ActionListener {
//
//  /*
//   * old code -- not implemented in Jmol 11.3 -- needs clean-up
//   */
//
//  private static final long serialVersionUID = 1L;
//  //The constants used to generate panels, etc.
//  JButton saveButton, MolecopenButton, MolecdeleteButton;
//  JTextField appletPath;
//  JFileChooser fc;
//  JList MolecList;
//  JComboBox RenderMode, FormatBox;
//
//  //set some constants for page formats
//  private static final int FromLinks = 1;
//  private static final int FromMenu = 2;
//
//  //set some constants for the rendering mode
//  private static final int Wireframe = 1;
//  private static final int BallandStick = 2;
//  private static final int Spacefilling = 3;
//
//  //Need the panel maker and the action listener.
//  JComponent getPanel() {
//
//    //Create the brief discription text
//    JLabel Description = new JLabel(
//        "Create a web page with one Jmol Applet to display molecules chosen by user.");
//
//    //Create the text field for the path to the JMol applet
//    appletPath = new JTextField(20);
//    appletPath.addActionListener(this);
//    appletPath.setText("../../Applets/Java/Jmol");
//
//    //Path to applet panel
//    JPanel pathPanel = new JPanel();
//    pathPanel.setLayout(new BorderLayout());
//    //		JLabel pathLabel = new JLabel("Relative Path to Jmol Applet:");
//    //		pathPanel.add(pathLabel, BorderLayout.PAGE_START);
//    pathPanel.add(appletPath, BorderLayout.PAGE_END);
//    pathPanel.setBorder(BorderFactory
//        .createTitledBorder("Relative Path to Jmol Applet:"));
//
//    //For layout purposes, put things in separate panels
//
//    //Create the ComboBox (popup menu) for the Rendering Mode
//    JLabel RenderModeLabel = new JLabel("Rendering Mode:");
//    String[] RenderModes = { "Wireframe", "BallandStick", "Spacefilling" };
//    RenderMode = new JComboBox(RenderModes);
//    RenderMode.setSelectedIndex(2);
//    //Attached no action listener.  Will just read for selection.
//    //Put in panel with label
//    JPanel RenderPanel = new JPanel();
//    RenderPanel.add(RenderModeLabel);
//    RenderPanel.add(RenderMode);
//
//    //Combine applet path, coordinate file and Rendering mode panels
//    JPanel PathCoorRendPanel = new JPanel();
//    PathCoorRendPanel.setLayout(new BorderLayout());
//    PathCoorRendPanel.add(pathPanel, BorderLayout.PAGE_START);
//    //		PathCoorRendPanel.add(CoorPanel,BorderLayout.CENTER);
//    PathCoorRendPanel.add(RenderPanel, BorderLayout.PAGE_END);
//
//    //Create the ComboBox (popup menu) for the Page Format
//    JLabel PageFormatLabel = new JLabel("Page Format:");
//    String[] PageFormats = { "Molecules from links (best with 4 or less)",
//        "Molecules from popup menu" };
//    FormatBox = new JComboBox(PageFormats);
//    FormatBox.setSelectedIndex(0);
//    //Put in panel with a label
//    JPanel FormatPanel = new JPanel();
//    FormatPanel.add(PageFormatLabel);
//    FormatPanel.add(FormatBox);
//
//    //Create the save button. 
//    saveButton = new JButton("Save .html as...");
//    saveButton.addActionListener(this);
//
//    //save file selection panel
//    JPanel savePanel = new JPanel();
//    savePanel.add(saveButton);
//
//    //Combine previous three panels into one
//    JPanel leftpanel = new JPanel();
//    leftpanel.setLayout(new BorderLayout());
//    leftpanel.add(PathCoorRendPanel, BorderLayout.PAGE_START);
//    leftpanel.add(FormatPanel, BorderLayout.CENTER);
//    leftpanel.add(savePanel, BorderLayout.PAGE_END);
//
//    //Create file chooser
//    fc = new JFileChooser();
//
//    //Create the list and list view to handle the list of 
//    //orbital files.
//    ArrayListTransferHandler arrayListHandler = new ListTransferHandler(
//        null);
//    DefaultListModel Molecfilelist = new DefaultListModel();
//    MolecList = new JList(Molecfilelist);
//    MolecList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//    MolecList.setTransferHandler(arrayListHandler);
//    MolecList.setDragEnabled(true);
//    JScrollPane MolecListView = new JScrollPane(MolecList);
//    MolecListView.setPreferredSize(new Dimension(300, 200));
//
//    //Create the label for the orbital file area
//    //JLabel MolecLabel = new JLabel("Molecule Files (Drag to Preferred Order):");
//
//    //Create the Molecule file add button.
//    MolecopenButton = new JButton("Add File(s)...");
//    MolecopenButton.addActionListener(this);
//
//    //Create the delete file button
//    MolecdeleteButton = new JButton("Delete Selected");
//    MolecdeleteButton.addActionListener(this);
//
//    //Molecule file selection
//    JPanel MolecButtonsPanel = new JPanel();
//    MolecButtonsPanel.add(MolecopenButton);
//    MolecButtonsPanel.add(MolecdeleteButton);
//
//    //Title and border for the Molecule file selection
//    JPanel MolecPanel = new JPanel();
//    MolecPanel.setLayout(new BorderLayout());
//    MolecPanel.add(MolecButtonsPanel, BorderLayout.PAGE_START);
//    MolecPanel.add(MolecListView, BorderLayout.PAGE_END);
//    MolecPanel.setBorder(BorderFactory
//        .createTitledBorder("Molecule Files (Drag to Preferred Order):"));
//
//    //Create the overall panel
//    JPanel MoleculePanel = new JPanel();
//    MoleculePanel.setLayout(new BorderLayout());
//
//    //Add everything to this panel.
//    MoleculePanel.add(Description, BorderLayout.PAGE_START);
//    MoleculePanel.add(leftpanel, BorderLayout.CENTER);
//    MoleculePanel.add(MolecPanel, BorderLayout.LINE_END);
//
//    return (MoleculePanel);
//  }
//
//  public void actionPerformed(ActionEvent e) {
//
//    //Handle open button action.
//    if (e.getSource() == MolecopenButton) {
//      //enable this to allow multiple file selection
//      fc.setMultiSelectionEnabled(true);
//      fc.setDialogTitle("Choose the Molecule Files:");
//      int returnVal = fc.showOpenDialog(Molecules.this);
//      if (returnVal == JFileChooser.APPROVE_OPTION) {
//        File[] files = fc.getSelectedFiles();
//        DefaultListModel listModel = (DefaultListModel) MolecList.getModel();
//        //This is where a real application would open the file.
//        //Were're just making a list since we only use the file names.
//        for (int i = 0; i < files.length; i++) {
//          //LogPanel.Log("Molecule file "+i+" selected: " + files[i].getName());
//          String str = files[i].getName();
//          listModel.addElement(str);
//        }
//
//      } else {
//        //LogPanel.Log("Molecule file selection cancelled by user.");
//      }
//      //Handle Delete button
//    } else if (e.getSource() == MolecdeleteButton) {
//      DefaultListModel listModel = (DefaultListModel) MolecList.getModel();
//      //find out which are selected and remove them.
//      int[] todelete = MolecList.getSelectedIndices();
//      for (int i = 0; i < todelete.length; i++) {
//        listModel.remove(todelete[i]);
//      }
//      //Handle save button action.
//    } else if (e.getSource() == saveButton) {
//      fc.setDialogTitle("Save .html file as:");
//      int returnVal = fc.showSaveDialog(Molecules.this);
//      if (returnVal == JFileChooser.APPROVE_OPTION) {
//        File file = fc.getSelectedFile();
//        //This is where a real application would save the file.
//        DefaultListModel listModel = (DefaultListModel) MolecList.getModel();
//        LogPanel.log("Saving: " + file.getName() + ".\n");
//        for (int i = 0; i < listModel.getSize(); i++) {
//          LogPanel.log("  Molecule file #" + i + " is "
//              + listModel.getElementAt(i) + ".");
//        }
//        boolean retVal = true;
//        try {
//          retVal = molectohtml((FormatBox.getSelectedIndex() + 1), (RenderMode
//              .getSelectedIndex() + 1), file, MolecList, appletPath.getText());
//        } catch (IOException IOe) {
//          LogPanel.log(IOe.getMessage());
//        }
//        if (!retVal) {
//          LogPanel.log("Call to molectohtml unsuccessful.");
//        }
//      } else {
//        LogPanel.log("Save command cancelled by \"user\".");
//      }
//      //Handle choose Coordinate file button
//    }
//  }
//
//  private boolean checkformat(int FormatChoice) throws IOException {
//    //Error checking for allowed format choices. Returns true if OK.
//    switch (FormatChoice) {
//    case FromLinks: //Allow picking molecules by clicking a link
//      return true;
//    case FromMenu: //Allow picking moleclules from a menu
//      return true;
//    default: //we should not get here so return error
//      throw new IOException("Unacceptable format choice for web page.");
//    }
//  }
//
//  public boolean molectohtml(int FormatChoice, int Rendering, File outfile,
//                             JList MolecList, String appletPath)
//      throws IOException { //returns true if successful.
//    boolean formatOK = false;
//    try {
//      formatOK = checkformat(FormatChoice);
//    } catch (IOException IOe) {
//      throw IOe;
//    }
//    //If we get here things should be OK, but just in case check the formatOK boolean
//    if (formatOK) {
//      //open the printstream for outfile
//      PrintStream out = null;
//      try {
//        out = new PrintStream(new FileOutputStream(outfile));
//      } catch (FileNotFoundException e) {
//        throw e; //Pass the error up the line so it can go in the log window.
//      }
//      //html output.
//      out
//          .println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
//      out.println("<html>");
//      out.println("<head>");
//      out.println("  <meta content=\"text/html; charset=ISO-8859-1\"");
//      out.println(" http-equiv=\"content-type\">");
//      out.println("  <title>Molecules using jmol</title>");
//      out.println("  <meta content=\"???\" name=\"author\">");
//      out
//          .println("  <meta content=\"chemistry, Jmol, image, animation, rotatable, live display\" name=\"keywords\">");
//      out
//          .println("  <meta content=\"Molecule display using jmol\" name=\"description\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"Page design and layout by J. Gutow 7-2006, page written by Orbitals to Web.jar\"");
//      out.println(" name=\"details\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"This page is designed to have the text and normal html edited in a standard web design\"");
//      out.println(" name=\"instr1\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"program.  The javascript is to control the jmol applet (see below).  It is best to use\"");
//      out.println(" name=\"instr2\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"the java program to generate the proper java script to allow orbital display.  It is\"");
//      out.println(" name=\"instr3\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"important to have all the gzipped files in the same directory as the .html file.  The\"");
//      out.println(" name=\"instr4\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"files needed are: 1) a gzipped files with the atom coordinates, in any of the formats Jmol\"");
//      out.println(" name=\"instr5\">");
//      out.println("  <meta");
//      out.println(" content=\"can read.\"");
//      out.println(" name=\"instr6\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"It is also recommended that the file names for the molecules correspond to\"");
//      out.println(" name=\"instr8\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"reasonable names for the molecules as the file names are used to generate the menu items\"");
//      out.println(" name=\"instr9\">");
//      out
//          .println("  <meta content=\"that appear in the popup menus or as links for selecting molecules.\"");
//      out.println(" name=\"instr10\">");
//      out.println("  <meta");
//      out
//          .println(" content=\"The relative path to Jmol on your server must be correct below!!\"");
//      out.println(" name=\"instr11\">");
//      out.println("  <script src=\"" + appletPath + "/Jmol.js\"></script>");
//      out.println("</head>");
//      out.println("<body>");
//      out.println("<div style=\"text-align: center;\"><big><big><span");
//      out
//          .println(" style=\"font-weight: bold;\">&lt;Replace this text with your title&gt;<br>");
//      out.println("</span></big></big>");
//      out.println("<div style=\"text-align: left;\"><big><big><span");
//      out
//          .println(" style=\"font-weight: bold;\"></span></big></big>&lt;Describe your");
//      out.println("molecules here. Don't forget to mention that there is a");
//      out
//          .println("live display below.&nbsp; The user's browser window may be too small to");
//      out
//          .println("display your text and the table containing the Jmol applet without");
//      out.println("scrolling.&gt;<br>");
//      out.println("<br>");
//      out
//          .println("<big><big><span style=\"font-weight: bold;\"></span></big></big>");
//      out
//          .println("<table style=\"width: 100%; text-align: left;\" border=\"1\" cellpadding=\"2\"");
//      out.println(" cellspacing=\"2\">");
//      out.println("  <tbody>");
//      out.println("    <tr>");
//      out.println("      <td style=\"vertical-align: top;\">");
//      out.println("      <script>");
//      out.println("jmolInitialize(\"" + appletPath + "\");");
//      //load the applet without a molecule displayed, but set the default format
//      out.println("jmolApplet(300);");
//      out.println("jmolBr();");
//      out
//          .println("jmolHtml(\"This image may be rotated and zoomed.  See below for more instructions.\");");
//      out.println("        </script>");
//      out.println("      <br>");
//      out.println("      </td>");
//      out.println("      <td style=\"vertical-align: top;\">");
//      out.println("      <div style=\"text-align: right;\"> </div>");
//      out.println("      <form name=\"appletcontrol\">");
//      out.println("        <div style=\"text-align: right;\"> </div>");
//      out
//          .println("        <table style=\"width: 100%; text-align: left;\" border=\"0\"");
//      out.println(" cellpadding=\"2\" cellspacing=\"2\">");
//      out.println("          <tbody>");
//      switch (FormatChoice) {
//      case FromLinks: //make a table with links for each molecule
//      {//loop through the molecule files to make the links.
//        for (int i = 0; i < MolecList.getModel().getSize(); i++) {
//          out.println("            <tr>");
//          out.println("              <td colspan=\"1\" rowspan=\"1\"");
//          out
//              .println(" style=\"vertical-align: top; white-space: nowrap; text-align: right;\"><span");
//          out.println(" style=\"font-weight: bold;\">Molecule Name" + i
//              + ":</span><br>");
//          out.println("              </td> <td>");
//          int dotIndex = MolecList.getModel().getElementAt(i).toString()
//              .indexOf(".");
//          String Itemname = MolecList.getModel().getElementAt(i).toString()
//              .substring(0, dotIndex);
//          out.println("                <script>");
//          switch (Rendering) {
//          case Wireframe: {
//            //Appropriate for displaying orbitals on a molecule or just displaying a large molecule
//            out
//                .println("       jmolLink('load "
//                    + MolecList.getModel().getElementAt(i)
//                    + "; spacefill 0%; wireframe; labels %e; set labeloffset 0 0; background black;',\""
//                    + Itemname + "\");");
//            break;
//          }
//          case BallandStick: {
//            //Appropriate for displaying small molecules
//            out.println("       jmolLink('load "
//                + MolecList.getModel().getElementAt(i)
//                + "; spacefill 20%; wireframe 0.15; background black; ',\""
//                + Itemname + "\");");
//            break;
//          }
//          case Spacefilling: {
//            //Appropriate for displaying small molecules
//            out.println("       jmolLink('load "
//                + MolecList.getModel().getElementAt(i)
//                + "; spacefill 100%; wireframe; background black; ',\""
//                + Itemname + "\");");
//            break;
//          }
//
//          }
//          //add a blank box at the end of the row in case someone wants to add something
//          out.println("              </script></td><td> place holder text");
//          out.println("             </td></tr>");
//        }
//        break;
//      }
//      case FromMenu: //Make a popup menu with the molecules in it
//      {
//        out.println("            <tr>");
//        out.println("              <td colspan=\"1\" rowspan=\"1\"");
//        out
//            .println(" style=\"vertical-align: top; white-space: nowrap; text-align: right;\"><span");
//        out
//            .println(" style=\"font-weight: bold;\">Choose a Molecule:</span><br>");
//        out.println("              </td>");
//        out.println("              <td style=\"vertical-align: top;\">");
//        out.println("              <script>");
//        out.println("		jmolMenu([['load empty;','none'],");
//        //Loop the following line to make an entry for each molecule.
//        for (int i = 0; i < MolecList.getModel().getSize(); i++) {
//          int dotIndex = MolecList.getModel().getElementAt(i).toString()
//              .indexOf(".");
//          String Itemname = MolecList.getModel().getElementAt(i).toString()
//              .substring(0, dotIndex);
//          switch (Rendering) {
//          case Wireframe: {
//            //Appropriate for displaying orbitals on a molecule or just displaying a large molecule
//            out
//                .println("       ['load "
//                    + MolecList.getModel().getElementAt(i)
//                    + "; spacefill 0%; wireframe; labels %e; set labeloffset 0 0; background black;',\""
//                    + Itemname + "\"],");
//            break;
//          }
//          case BallandStick: {
//            //Appropriate for displaying small molecules
//            out.println("       ['load " + MolecList.getModel().getElementAt(i)
//                + "; spacefill 20%; wireframe 0.15; background black; ',\""
//                + Itemname + "\"],");
//            break;
//          }
//          case Spacefilling: {
//            //Appropriate for displaying small molecules
//            out.println("       ['load " + MolecList.getModel().getElementAt(i)
//                + "; spacefill 100%; wireframe; background black; ',\""
//                + Itemname + "\"],");
//            break;
//          }
//
//          }
//        }
//        out.println("		]);");
//        out.println("		</script>");
//        out.println("              <br>");
//        out.println("              </td>");
//        out.println("            </tr>");
//        // July 16, 2006 droping the background color and atoms size radio controls because they're avialable in
//        // Jmol popup.
//        //					out.println ("            <tr>");
//        //					out.println ("              <td");
//        //					out.println (" style=\"vertical-align: top; text-align: right; white-space: nowrap;\">Background Color:");
//        //					out.println ("              </td>");
//        //					out.println ("              <td");
//        //					out.println (" style=\"vertical-align: top; text-align: left; white-space: nowrap;\">");
//        //					out.println ("              <script>");
//        //					out.println ("		jmolRadioGroup([['background black;','Black','true'],");
//        //					out.println ("		['background blue;','Blue'],");
//        //					out.println ("		['background pink;','Pink'],");
//        //					out.println ("		['background white;','White'],");
//        //					out.println ("		 ]);");
//        //					out.println ("		</script><br>");
//        //					out.println ("              </td>");
//        //					out.println ("            </tr>");
//        //					out.println ("            <tr>");
//        //					out.println ("              <td");
//        //					out.println (" style=\"vertical-align: top; text-align: right; white-space: nowrap;\">Atom Size (% vanderWaals):");
//        //					out.println ("              </td>");
//        //					out.println ("              <td");
//        //					out.println (" style=\"vertical-align: top; text-align: left; white-space: nowrap;\">");
//        //					out.println ("              <script>");
//        //					out.println ("     jmolRadioGroup([");
//        //					switch (Rendering){
//        //						case Wireframe: {
//        //							//Appropriate for displaying orbitals on a molecule or just displaying a large molecule
//        //							out.println ("         ['spacefill 0%;','0%','true'],");
//        //							out.println ("         ['spacefill 20%;','20%'],");
//        //							out.println ("         ['spacefill 35%;','25%'],");
//        //							out.println ("         ['spacefill 50%;','50%'],");
//        //							out.println ("         ['spacefill 75%;','75%'],");
//        //							out.println ("         ['spacefill 100%;','100%']");
//        //							break;
//        //						}
//        //						case BallandStick: {
//        //							//Appropriate for displaying small molecules
//        //							out.println ("         ['spacefill 0%;','0%'],");
//        //							out.println ("         ['spacefill 20%;','20%','true'],");
//        //							out.println ("         ['spacefill 35%;','25%'],");
//        //							out.println ("         ['spacefill 50%;','50%'],");
//        //							out.println ("         ['spacefill 75%;','75%'],");
//        //							out.println ("         ['spacefill 100%;','100%']");
//        //							break;
//        //						}
//        //						case Spacefilling: {
//        //							//Appropriate for small molecules
//        //							out.println ("         ['spacefill 0%;','0%'],");
//        //							out.println ("         ['spacefill 20%;','20%'],");
//        //							out.println ("         ['spacefill 35%;','25%'],");
//        //							out.println ("         ['spacefill 50%;','50%'],");
//        //							out.println ("         ['spacefill 75%;','75%'],");
//        //							out.println ("         ['spacefill 100%;','100%','true']");
//        //						}
//        //							
//        //					}
//        //					out.println ("		]);");
//        //					out.println ("		</script>");
//        //					out.println ("              <br>");
//        //					out.println ("              </td>");
//        //					out.println ("            </tr>");
//
//      }
//      }
//
//      out.println("          </tbody>");
//      out.println("        </table>");
//      out.println("      </form>");
//      out
//          .println("&lt;This space can be used as a short caption for the applet at left.&gt;<br><br>");
//      String Stamp = "";
//      Stamp = WebExport.TimeStamp_WebLink();
//      out.println(Stamp);
//      out.println("Original");
//      out
//          .println("page composed by <a href=\"http://www.uwosh.edu/faculty_staff/gutow/\">J.");
//      out.println("Gutow 7/2006</a>. </small> </td>");
//      out.println("    </tr>");
//      out.println("    <tr>");
//      out
//          .println("      <td colspan=\"2\" rowspan=\"1\" style=\"vertical-align: top;\">");
//      out
//          .println("      <div style=\"text-align: center;\"><span style=\"font-weight: bold;\">Instructions");
//      out.println("for using Jmol to display");
//      out.println("molecules</span>: <br>");
//      out.println("      </div>");
//      out.println("      <ol>");
//      out
//          .println("        <li>Choose which molecules to display by selecting them using the");
//      out.println("popup menus or clicking on the appropriate link.");
//      out.println("        </li>");
//      out.println("        <li>ROTATE the image by");
//      out.println("holding");
//      out
//          .println("down the mouse button while moving the cursor over the image.&nbsp; </li>");
//      out
//          .println("        <li>ZOOM by holding down the shift key while moving the cursor");
//      out.println("up");
//      out
//          .println("(decrease magnification) or down (increase magnification) on top of the");
//      out.println("image.&nbsp; </li>");
//      out
//          .println("        <li>Other options are available in the control menu accessible by");
//      out.println("holding");
//      out
//          .println("the mouse button down while the cursor is over \"Jmol\" in the lower");
//      out
//          .println("right corner (right click also works on a multibutton mouse).&nbsp; </li>");
//      out
//          .println("        <li>For more info about Jmol go to <a target=\"_blank\"");
//      out.println(" href=\"http://www.jmol.org\">www.jmol.org.</a></li>");
//      out.println("      </ol>");
//      out.println("      </td>");
//      out.println("    </tr>");
//      out.println("  </tbody>");
//      out.println("</table>");
//      out.println("</div>");
//      out.println("</div>");
//      out.println("</body>");
//      out.println("</html>");
//      out.close();
//    }
//    return true;
//  }

}
