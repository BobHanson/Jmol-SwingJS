/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package jspecview.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.GZIPInputStream;

import jspecview.api.JSVZipInterface;

public class JSVZipUtil implements JSVZipInterface {

	public JSVZipUtil() {
		// for reflection
	}
	@Override
	public InputStream newGZIPInputStream(InputStream bis)
			throws IOException {
		return new GZIPInputStream(bis, 512);
	}

	@SuppressWarnings("resource")
	@Override
	public BufferedReader newJSVZipFileSequentialReader(InputStream in,
			String[] subFileList, String startCode) {
		return new JSVZipFileSequentialReader().set(in, subFileList, startCode);
	}

}
