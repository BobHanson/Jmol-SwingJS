/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-09-25 15:33:17 -0500 (Wed, 25 Sep 2013) $
 * $Revision: 18695 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package jspecview.java;

import java.awt.Component;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import jspecview.api.JSVPanel;
import jspecview.app.GenericMouse;

/**
 * formerly org.jmol.viewer.MouseManager14
 * 
 * methods required by Jmol that access java.awt.event
 * 
 * private to jspecview.java
 * 
 */

class Mouse extends GenericMouse implements MouseWheelListener, MouseListener,
		MouseMotionListener, KeyListener {

	/**
	 * @param jsvp
	 */
	Mouse(JSVPanel jsvp) {
		super(jsvp);
		Component display = (Component) jsvp;
		display.addKeyListener(this);
		display.addMouseListener(this);
		display.addMouseMotionListener(this);
		display.addMouseWheelListener(this);
	}

	@Override
	public void dispose() {
		Component display = (Component) jsvp;
		display.removeMouseListener(this);
		display.removeMouseMotionListener(this);
		display.removeMouseWheelListener(this);
		display.removeKeyListener(this);
		super.dispose();
	}


}
