/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.util;

import junit.framework.TestCase;

/**
 * JUnit tests for CommandHistory
 */
public class TestCommandHistory extends TestCase {

  /**
   * Constructor for tests.
   * 
   * @param arg0 Test name.
   */
  public TestCommandHistory(String arg0) {
    super(arg0);
  }

  /**
   * Simple test of CommandHistory.
   */
  public void testHistory() {

    // Initialisation of command history
    CommandHistory h = new CommandHistory();
    h.reset(4);
    h.addCommand("x");
    h.addCommand("y");
    h.addCommand("z");
    h.addCommand("a");
    h.addCommand("b");
    h.addCommand("c");
    h.addCommand("d");

    // Testing getCommandUp()
    assertEquals("d", h.getCommandUp());
    assertEquals("c", h.getCommandUp());
    assertEquals("b", h.getCommandUp());
    assertEquals("a", h.getCommandUp());
    assertEquals(null, h.getCommandUp());

    // Testing getCommandDown()
    assertEquals("b", h.getCommandDown());
    assertEquals("c", h.getCommandDown());
    assertEquals("d", h.getCommandDown());
    assertEquals("" , h.getCommandDown());
    assertEquals(null, h.getCommandDown());

    // Modifying history size
    h.setMaxSize(2);

    // Testing getCommandUp()
    assertEquals("d", h.getCommandUp());
    assertEquals("c", h.getCommandUp());
    assertEquals(null, h.getCommandUp());
    assertEquals(null, h.getCommandUp());
    assertEquals(null, h.getCommandUp());

    // Testing getCommandDown();
    assertEquals("d", h.getCommandDown());
    assertEquals("" , h.getCommandDown());
    assertEquals(null, h.getCommandDown());

    // Modifying history size
    h.setMaxSize(4);

    // Testing getCommandUp()
    assertEquals("d", h.getCommandUp());
    assertEquals("c", h.getCommandUp());
    assertEquals(null, h.getCommandUp());
    assertEquals(null, h.getCommandUp());
    assertEquals(null, h.getCommandUp());
    
    h.addCommand("e");
    h.addCommand("f");

    // Testing getCommandUp()
    assertEquals("f", h.getCommandUp());
    assertEquals("e", h.getCommandUp());
    assertEquals("d", h.getCommandUp());
    assertEquals("c", h.getCommandUp());
    assertEquals(null, h.getCommandUp());

    // Testing getCommandDown();
    assertEquals("d", h.getCommandDown());
    assertEquals("e", h.getCommandDown());
    assertEquals("f", h.getCommandDown());
    assertEquals("" , h.getCommandDown());
    assertEquals(null, h.getCommandDown());
  }
}
