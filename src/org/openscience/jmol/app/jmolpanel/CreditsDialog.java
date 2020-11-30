/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-11-30 22:32:36 -0600 (Sat, 30 Nov 2013) $
 * $Revision: 19042 $
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


import javax.swing.JFrame;

import org.jmol.i18n.GT;

public class CreditsDialog extends HelpDialog {

  public CreditsDialog(JFrame fr) {

    super(fr, GT.$("Credits"), true);

    init(null, "Credits.creditsURL");
  }

}
