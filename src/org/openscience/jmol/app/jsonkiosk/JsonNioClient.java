/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-19 08:25:14 -0500 (Wed, 19 May 2010) $
 * $Revision: 13133 $
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

package org.openscience.jmol.app.jsonkiosk;


/**
 * a client of a JsonNioService -- just needs notices of the service shutting down 
 * or indicating that a banner needs to be changed.
 * 
 */
public interface JsonNioClient {

  void nioRunContent(JsonNioServer jsonNioService);
  void setBannerLabel(String label);
  void nioClosed(JsonNioServer jsonNioService);
 
}
