// Copyright (c) 2004 Sun Microsystems, Inc. All rights reserved. Use is
// subject to license terms.
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the Lesser GNU General Public License as
// published by the Free Software Foundation; either version 2 of the
// License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA

package org.jmol.screensaver;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.jdesktop.jdic.screensaver.ScreensaverContext;
import org.jdesktop.jdic.screensaver.SimpleScreensaver;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

/**
 * Jmol screen saver
 */
public class JmolScreensaver extends SimpleScreensaver
{
  // Jmol
  private JmolAdapter adapter = null;
  private JmolViewer viewer = null;
  private final Dimension currentSize = new Dimension();

  /**
   * Screen saver initialisation
   */
  public void init() {
    ScreensaverContext context = getContext();
    Component c = context.getComponent();
    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(c, adapter);
    viewer.evalStringQuiet(
        "load C:\\Program Files\\Folding@Home\\v1\\work\\current.xyz");
  }

  /**
   * Paint the next frame
   * @param g Graphic object
   */
  public void paint( Graphics g ) {
    Component c = getContext().getComponent();
    viewer.setScreenDimension(c.getSize(currentSize));
    Rectangle rectClip = new Rectangle();
    g.getClipBounds(rectClip);
    viewer.renderScreenImage(g, currentSize, rectClip);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      //
    }
  }
}
