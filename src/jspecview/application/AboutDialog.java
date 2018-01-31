/* Copyright (c) 2002-2016 The University of the West Indies
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

package jspecview.application;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import jspecview.app.JSVApp;
import jspecview.common.JSVersion;

/**
 * The <code>About Dialog</code> class is the <i>help | about</i> window for JSpecView.
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */
public class AboutDialog extends JDialog {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JPanel p = new JPanel();
  private JTextArea txt;

  /**
   * Constructor
   * @param owner the parent frame
   * @param title the title of the frame
   * @param modal true is the dialog should be modal
   */
  private AboutDialog(Frame owner, String title, boolean modal) {
    super(owner, title, modal);

    try {
      jbInit();

      // dialog properties
      this.setTitle("About JSpecView");
      this.pack();
      setResizable(false);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // Sets the location to the middle of the parent frame if it has one
    if (owner != null)
      setLocation( (owner.getLocation().x + owner.getSize().width) / 2,
                  (owner.getLocation().y + owner.getSize().height) / 2);
    setVisible(true);
  }

  /**
   * Constructor that initalises the Dialog a parent frame,
   * no title and modality to true
   * @param frame parent container for the About dialog
   */
  AboutDialog(Frame frame) {
    this(frame, "", true);
  }

  private void jbInit() throws Exception {
    JLabel lbl = new JLabel(new ImageIcon(getClass().getResource("icons/about.gif")));

    Border b1 = new BevelBorder(BevelBorder.LOWERED);

    Border b2 = new EmptyBorder(5, 5, 5, 5);
    lbl.setBorder(new CompoundBorder(b1, b2));

    p.add(lbl);

    // add image to About dialog
    getContentPane().add(p, BorderLayout.WEST);

    p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

    String message = "JSpecView Application\n" + "Version " + JSVersion.VERSION;
    txt = drawMessage(message, "Helvetica", Font.BOLD, 12);
    p.add(txt);

    message="Distributed under the GNU Lesser Public License\n";
    message+="via sourceforge at http://jspecview.sf.net";
    txt = drawMessage(message, "Arial", Font.PLAIN, 12);
    p.add(txt);

    message = JSVApp.CREDITS;
    txt = drawMessage(message, "Arial", Font.BOLD, 12);
    p.add(txt);

    message = "Copyright (c) 2002-2017, Department of Chemistry\nUniversity of the West Indies, Mona Campus\nJAMAICA";
    txt = drawMessage(message, "Arial", Font.PLAIN, 12);
    p.add(txt);

    // add text to About dialog
    getContentPane().add(p, BorderLayout.CENTER);

    JButton okButton = new JButton("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      @Override
			public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });

    p = new JPanel();
    p.add(okButton);

    // add OK button to About dialog
    getContentPane().add(p, BorderLayout.SOUTH);
  }

  private JTextArea drawMessage(String message, String fontType, int fontStyle,
                           int fontSize) {
    JTextArea text = new JTextArea(message);
    text.setBorder(new EmptyBorder(5, 10, 5, 10));
    text.setFont(new Font(fontType, fontStyle, fontSize));
    text.setEditable(false);
    text.setBackground(getBackground());
    return text;
  }
}
