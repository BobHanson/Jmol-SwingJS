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


import java.net.URL;
import java.net.MalformedURLException;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.JButton;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;

public class HelpDialog extends JDialog implements HyperlinkListener {

  JEditorPane html;

  public HelpDialog(JFrame fr) {
      this(fr, null);
  }
  
  public HelpDialog(JFrame fr, String title, boolean modal) {
    super(fr, title, modal);
  }
  
  /**
   * If url is null, then the default help url is taken.
   * @param fr
   * @param url
   */
  public HelpDialog(JFrame fr, URL url) {
    this(fr, GT.$("Jmol Help"), false);
    init(url, "Help.helpURL");
  }

  protected void init(URL url, String resource) {
    try {
      URL myURL = url;
      if (myURL == null) {
        resource = JmolResourceHandler.getStringX(resource);
        myURL = (resource.startsWith("http") ? new URL(resource) : this
            .getClass().getClassLoader().getResource(resource));
      }
      html = (myURL == null ? new JEditorPane("text/plain", GT.o(
          GT.$("Unable to find url \"{0}\"."), resource)) : new JEditorPane(
          myURL));
      html.setEditable(false);
      html.addHyperlinkListener(this);
    } catch (MalformedURLException e) {
      Logger.errorEx("Malformed URL", e);
    } catch (IOException e) {
      Logger.errorEx("IOException", e);
    }
    JScrollPane scroller = new JScrollPane();
    scroller.setPreferredSize(new Dimension(500, 400));
    scroller.setAlignmentX(LEFT_ALIGNMENT);
    scroller.getViewport().add(html);

    JPanel htmlWrapper = new JPanel(new BorderLayout());
    htmlWrapper.setAlignmentX(LEFT_ALIGNMENT);
    htmlWrapper.add(scroller, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton ok = new JButton(GT.$("OK"));
    ok.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        OKPressed();
      }
    });
    buttonPanel.add(ok);
    getRootPane().setDefaultButton(ok);

    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

    container.add(htmlWrapper, BorderLayout.CENTER);
    container.add(buttonPanel, BorderLayout.SOUTH);

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  @Override
  public void hyperlinkUpdate(HyperlinkEvent e) {
    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
      linkActivated(e.getURL());
    }
  }

  /**
   * Follows the reference in an
   * link.  The given url is the requested reference.
   * By default this calls <a href="#setPage">setPage</a>,
   * and if an exception is thrown the original previous
   * document is restored and a beep sounded.  If an
   * attempt was made to follow a link, but it represented
   * a malformed url, this method will be called with a
   * null argument.
   *
   * @param u the URL to follow
   */
  protected void linkActivated(URL u) {
    Cursor c = html.getCursor();
    Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    html.setCursor(waitCursor);
    SwingUtilities.invokeLater(new PageLoader(u, c));
  }

  /**
   * temporary class that loads synchronously (although later than
   * the request so that a cursor change can be done).
   */
  class PageLoader implements Runnable {

    PageLoader(URL u, Cursor c) {
      url = u;
      cursor = c;
    }

    @Override
    public void run() {

      if (url == null) {

        // restore the original cursor
        html.setCursor(cursor);

        // remove this hack when automatic validation is
        // activated.
        Container parent = html.getParent();
        parent.repaint();
      } else {
        Document doc = html.getDocument();
        try {
          html.setPage(url);
        } catch (IOException ioe) {
          html.setDocument(doc);
          getToolkit().beep();
        } finally {

          // schedule the cursor to revert after the paint
          // has happended.
          url = null;
          SwingUtilities.invokeLater(this);
        }
      }
    }

    URL url;
    Cursor cursor;
  }


  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void OKPressed() {
    this.setVisible(false);
  }

}
