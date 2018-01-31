/* Copyright (c) 2002-2013 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.java;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.print.attribute.standard.MediaSizeName;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import jspecview.api.JSVPrintDialog;
import jspecview.common.PrintLayout;


/**
 * Dialog to set print preferences for JSpecview.
 * @author Bob Hanson hansonr@stolaf.edu
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AwtPrintDialog extends JDialog implements JSVPrintDialog {

	private static final long serialVersionUID = 1L;
  
  private ButtonGroup layoutButtonGroup = new ButtonGroup();
  private ButtonGroup fontButtonGroup = new ButtonGroup();
  private ButtonGroup positionButtonGroup = new ButtonGroup();

  private PrintLayout pl;
	private PrintLayout plNew;

  //private JButton layoutButton = new JButton();
  private JButton previewButton = new JButton();
  private JButton cancelButton = new JButton();
  private JButton printButton = new JButton();
  private JButton pdfButton = new JButton();
  
  private JCheckBox scaleXCheckBox = new JCheckBox();
  private JCheckBox scaleYCheckBox = new JCheckBox();
  private JCheckBox gridCheckBox = new JCheckBox();
  private JCheckBox titleCheckBox = new JCheckBox();
  
  private JRadioButton landscapeRadioButton = new JRadioButton();
  private JRadioButton topLeftRadioButton = new JRadioButton();
  private JRadioButton centerRadioButton = new JRadioButton();
  private JRadioButton portraitRadioButton = new JRadioButton();
  private JRadioButton fitToPageRadioButton = new JRadioButton();
  private JRadioButton chooseFontRadioButton = new JRadioButton();
  private JRadioButton defaultFontRadioButton = new JRadioButton();

  //private static JComboBox<String> fontComboBox = new JComboBox<String>();
  private static JComboBox<MediaSizeName> paperComboBox = new JComboBox<MediaSizeName>();
  //private JComboBox printerNameComboBox = new JComboBox();

  public AwtPrintDialog() {
  	super((Frame) null, "Print layout", true);
  }
  /**
   * Initialises a modal <code>PrintLayoutDialog</code> with a default title
   * of "Print Layout".
   * @param frame the parent frame
   * @param pl    null or previous layout
   * @param isJob 
   * @return this
   */
  @Override
	public AwtPrintDialog set(Object frame, PrintLayout pl, boolean isJob) {
    if (pl == null)
      pl = new PrintLayout(null);
    this.pl = pl;
    try {
      jbInit(isJob);
      setSize(320, 250);
      setResizable(false);
      pack();
      pdfButton.requestFocusInWindow();
      setVisible(true);
      return this;
    }
    catch(Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }
  //private static ImageIcon portraitIcon;
  //private static ImageIcon landscapeIcon;
  private static ImageIcon previewPortraitCenterIcon;
  private static ImageIcon previewPortraitDefaultIcon;
  private static ImageIcon previewPortraitFitIcon;
  private static ImageIcon previewLandscapeCenterIcon;
  private static ImageIcon previewLandscapeDefaultIcon;
  private static ImageIcon previewLandscapeFitIcon;

	private static void setStaticElements() {
		if (previewLandscapeFitIcon != null)
			return;
		
    paperComboBox.addItem(MediaSizeName.NA_LETTER);
    paperComboBox.addItem(MediaSizeName.NA_LEGAL);
    paperComboBox.addItem(MediaSizeName.ISO_A4);
    paperComboBox.addItem(MediaSizeName.ISO_B4);

    //GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    //String allFontNames[] = ge.getAvailableFontFamilyNames();
    //for(int i = 0; i < allFontNames.length; i++)
      //fontComboBox.addItem(allFontNames[i]);
		previewPortraitCenterIcon = getIcon("portraitCenter");
		previewPortraitDefaultIcon = getIcon("portraitDefault");
		previewPortraitFitIcon = getIcon("portraitFit");
		previewLandscapeCenterIcon = getIcon("landscapeCenter");
		previewLandscapeDefaultIcon = getIcon("landscapeDefault");
		previewLandscapeFitIcon = getIcon("landscapeFit");
	}

	private static ImageIcon getIcon(String name) {
		return new ImageIcon(AwtPrintDialog.class.getResource(
				"icons/" + name + ".gif"));
	}
	
	/**
   * Initalises the GUI components
	 * @param isJob 
   * @throws Exception
   */
  private void jbInit(boolean isJob) throws Exception {

    layoutButtonGroup.add(portraitRadioButton);
    layoutButtonGroup.add(landscapeRadioButton);
    positionButtonGroup.add(centerRadioButton);
    positionButtonGroup.add(fitToPageRadioButton);
    positionButtonGroup.add(topLeftRadioButton);
    fontButtonGroup.add(defaultFontRadioButton);
    fontButtonGroup.add(chooseFontRadioButton);

    setStaticElements();
    
    TitledBorder layoutBorder = new TitledBorder("Layout");
    layoutBorder.setTitleJustification(TitledBorder.TOP);
    //TitledBorder elementsBorder = new TitledBorder("");
    //elementsBorder.setTitle("Elements");
    //elementsBorder.setTitleJustification(2);
    //TitledBorder titledBorder2 = new TitledBorder("");
    //TitledBorder titledBorder4 = new TitledBorder("");
    //TitledBorder titledBorder5 = new TitledBorder("");
    //TitledBorder titledBorder6 = new TitledBorder("");
    //TitledBorder titledBorder7 = new TitledBorder("");
    //TitledBorder titledBorder8 = new TitledBorder("");
    //TitledBorder titledBorder9 = new TitledBorder("");
    //titledBorder9.setTitle("Paper");
    //titledBorder9.setTitleJustification(2);
    //titledBorder2.setTitle("Position");
    //titledBorder2.setTitleJustification(2);
    //titledBorder4.setTitle("Font");
    //titledBorder4.setTitleJustification(2);
    //titledBorder5.setTitle("Preview");
    //titledBorder5.setTitleJustification(2);
    //titledBorder6.setTitle("Printers");
    //titledBorder7.setTitle("Paper");
    //titledBorder8.setTitle("Copies");
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    printButton.setToolTipText("");
    printButton.setText("Print");
    printButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(false);
      }
    });
    pdfButton.setToolTipText("");
    pdfButton.setText("Create PDF");
    pdfButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(true);
      }
    });
    
    
    JPanel layoutContentPanel = new JPanel();
    JPanel layoutPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    //JPanel positionPanel = new JPanel();
    //JPanel previewPanel = new JPanel();
    //JPanel fontPanel = new JPanel();
    //JPanel elementsPanel = new JPanel();
    //JPanel paperPanel = new JPanel();
    //positionPanel.setBorder(titledBorder2);
    //positionPanel.setLayout(new GridBagLayout());
    //previewPanel.setBorder(titledBorder5);
    //previewPanel.setLayout(new GridBagLayout());
    //layoutButton.setBorder(null);
    //layoutButton.setIcon(portraitIcon);
    //fontPanel.setBorder(titledBorder4);
    //fontPanel.setLayout(gridBagLayout5);
    //elementsPanel.setBorder(elementsBorder);
    //elementsPanel.setLayout(new GridBagLayout());
    //paperPanel.setBorder(titledBorder9);
    //paperPanel.setLayout(new GridBagLayout());

    layoutPanel.setBorder(layoutBorder);
    layoutPanel.setLayout(new GridBagLayout());
    landscapeRadioButton.setActionCommand("Landscape");
    landscapeRadioButton.setText("Landscape");
    landscapeRadioButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        landscapeRadioButton_actionPerformed(e);
      }
    });
    layoutContentPanel.setLayout(new GridBagLayout());
    scaleXCheckBox.setText("X-Scale");
    scaleYCheckBox.setText("Y-Scale");
    previewButton.setBorder(null);
    previewButton.setIcon(previewLandscapeDefaultIcon);
    gridCheckBox.setText("Grid");
    topLeftRadioButton.setActionCommand("Default");
    topLeftRadioButton.setText("Top Left");
    topLeftRadioButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        defaultPosRadioButton_actionPerformed(e);
      }
    });
    centerRadioButton.setActionCommand("Center");
    centerRadioButton.setText("Center");
    centerRadioButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        centerRadioButton_actionPerformed(e);
      }
    });
    portraitRadioButton.setActionCommand("Portrait");
    portraitRadioButton.setText("Portrait");
    portraitRadioButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        portraitRadioButton_actionPerformed(e);
      }
    });
    fitToPageRadioButton.setActionCommand("Fit To Page");
    fitToPageRadioButton.setText("Fit to Page");
    fitToPageRadioButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        fitToPageRadioButton_actionPerformed(e);
      }
    });
    chooseFontRadioButton.setText("Choose font");
    defaultFontRadioButton.setText("Use default");
    titleCheckBox.setText("Title");
    this.getContentPane().add(buttonPanel,  BorderLayout.SOUTH);
    if (isJob)
      buttonPanel.add(printButton, null);
    buttonPanel.add(pdfButton, null);
    buttonPanel.add(cancelButton, null);
    Insets insets = new Insets(0, 0, 0, 0);
    this.getContentPane().add(layoutContentPanel,  BorderLayout.CENTER);
    
    layoutContentPanel.add(layoutPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, 
    		GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
    
    
    //layoutContentPanel.add(positionPanel,  new GridBagConstraints(1, 0, 2, 1, 0.5, 0.0
      //      ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
    //layoutContentPanel.add(paperPanel, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
    //    ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
    //layoutPanel.add(layoutButton,          new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 0, 0));
    //layoutContentPanel.add(elementsPanel, new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0
    //      ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
    //layoutContentPanel.add(fontPanel,          new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
    //        ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
    //fontPanel.add(defaultFontRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
    //        ,GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    //fontPanel.add(chooseFontRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
    //        ,GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    //fontPanel.add(fontComboBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
    //        ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    //layoutContentPanel.add(paperPanel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
    //        ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
      

    layoutPanel.add(landscapeRadioButton,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    layoutPanel.add(portraitRadioButton,   new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));

    layoutPanel.add(titleCheckBox,         new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    layoutPanel.add(gridCheckBox,          new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));

    layoutPanel.add(fitToPageRadioButton,  new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    layoutPanel.add(topLeftRadioButton,    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    layoutPanel.add(centerRadioButton,     new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    
    layoutPanel.add(scaleXCheckBox,        new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    layoutPanel.add(scaleYCheckBox,        new GridBagConstraints(1, 5, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
    
    layoutPanel.add(previewButton,         new GridBagConstraints(2, 0, 1, 5, 0.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
    layoutPanel.add(paperComboBox,         new GridBagConstraints(2, 5, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
    
    setDefaults();
  }

	/**
   * Sets the layout to portrait and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void portraitRadioButton_actionPerformed(ActionEvent e) {
    setPreview();
  }

  /**
   * Sets the layout to landscape and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void landscapeRadioButton_actionPerformed(ActionEvent e) {
    setPreview();
  }

  /**
   * Sets the position to center and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void centerRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  /**
   * Sets the position to "fit to page" and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void fitToPageRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  private void setPreview() {
    int layout = " PL".indexOf(layoutButtonGroup.getSelection().getActionCommand().charAt(0));
    //layoutButton.setIcon(layout == 1 ? portraitIcon : landscapeIcon);
    int position = " DCF".indexOf(positionButtonGroup.getSelection().getActionCommand().charAt(0));
    ImageIcon icon = null; 
    switch ((layout << 4) + position) {
    default:
    case 0x11:
    	icon = previewPortraitDefaultIcon;
    	break;
    case 0x12:
    	icon = previewPortraitCenterIcon;
    	break;
    case 0x13:
    	icon = previewPortraitFitIcon;
    	break;
    case 0x21:
    	icon = previewLandscapeDefaultIcon;
    	break;
    case 0x22:
    	icon = previewLandscapeCenterIcon;
    	break;
    case 0x23:
    	icon = previewLandscapeFitIcon;
    	break;
    }
    previewButton.setIcon(icon);
	}

	/**
   * Sets the position to default and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void defaultPosRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  private void setDefaults() {
    landscapeRadioButton.setSelected(pl.layout.equals("landscape"));
    portraitRadioButton.setSelected(!landscapeRadioButton.isSelected());
    scaleXCheckBox.setSelected(pl.showXScale);
    scaleYCheckBox.setSelected(pl.showYScale);
    gridCheckBox.setSelected(pl.showGrid);
    titleCheckBox.setSelected(pl.showTitle);    
    fitToPageRadioButton.setSelected(pl.position.equals("fit to page"));
    centerRadioButton.setSelected(pl.position.equals("center"));
    topLeftRadioButton.setSelected(pl.position.equals("default"));
    defaultFontRadioButton.setSelected(pl.font == null);
//    if (pl.font != null)
//    	for (int i = fontComboBox.getItemCount(); --i >= 0;) 
//    		if (fontComboBox.getItemAt(i).equals(pl.font)) {
//    			fontComboBox.setSelectedIndex(i);
//    			break;
//    		}
  	for (int i = 0; i < paperComboBox.getItemCount(); i++) 
  		if (pl.paper == null || paperComboBox.getItemAt(i).equals(pl.paper)) {
  		  paperComboBox.setSelectedIndex(i);
  			break;
  		}
    setPreview();
	}

	/**
	 * Stored all the layout Information the PrintLayout object and disposes the
	 * dialog
	 * @param asPDF 
	 * 
	 */
	void printButton_actionPerformed(boolean asPDF) {
		plNew = new PrintLayout(null);
		plNew.layout = layoutButtonGroup.getSelection().getActionCommand()
				.toLowerCase();
		plNew.font = null;
		//(defaultFontRadioButton.isSelected() ? null
		//		: (String) fontComboBox.getSelectedItem());
		plNew.position = positionButtonGroup.getSelection().getActionCommand()
				.toLowerCase();
		plNew.showGrid = gridCheckBox.isSelected();
		plNew.showXScale = scaleXCheckBox.isSelected();
		plNew.showYScale = scaleYCheckBox.isSelected();
		plNew.showTitle = titleCheckBox.isSelected();
		plNew.paper = paperComboBox.getSelectedItem();
		// pl.printer = services[printerNameComboBox.getSelectedIndex()];
		// pl.numCopies = ((Integer)numCopiesSpinner.getValue()).intValue();
		plNew.asPDF = asPDF;

		dispose();
	}

  /**
   * Returns the PrintLayout object
   * @return the PrintLayout object
   */
  @Override
	public PrintLayout getPrintLayout(){
    return plNew;
  }

  /**
   * set the <code>PrintLayout</code> object to null and disposes of the dialog
   * @param e the action (event)
   */
  void cancelButton_actionPerformed(ActionEvent e) {
    dispose();
  }
}
