/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-09-20 18:53:37 -0500 (Sun, 20 Sep 2009) $
 * $Revision: 11558 $
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

package org.jmol.multitouch;



import java.util.List;
import javajs.util.P3;

import com.sparshui.GestureType;

public interface JmolMultiTouchClient {
  
  /*
   * An interface that involves only Java 1.4-compliant classes.
   * 
   * ActionManagerMT implements this interface.
   * It is connected to the SparshUI code (com.sparshui.client) 
   * within org.jmol.multitouch.sparshui.SparshClient
   * 
   */
  public int getGroupID(int x, int y);
  public List<GestureType> getAllowedGestures(int groupID);
  public void processMultitouchEvent(int groupID, int eventType, int touchID, 
                           int iData, P3 pt, long time);
}
