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

package org.jmol.api;

import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementData;

public interface JmolMeasurementClient {

  public abstract void processNextMeasure(MeasurementData measurementData, Measurement m);
  
}
