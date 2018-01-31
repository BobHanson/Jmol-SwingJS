/*
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
		

package jspecview.common;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Properties;


import javajs.J2SIgnoreImport;
import javajs.util.PT;

@J2SIgnoreImport({java.util.Properties.class, java.io.InputStream.class, java.io.BufferedInputStream.class, javajs.util.PT.class})
public class JSVersion {

  public static final String VERSION;
	public static final String VERSION_SHORT;

	static {
		String tmpVersion = null;
		String tmpDate = null;
		String tmpSVN = null;

		// Reading version from resource inside jar
		/**
		 * @j2sNative
		 * 
		 *            tmpVersion = Jmol.___JSVVersion; tmpDate = Jmol.___JSVDate; 
		 *            tmpSVN =  Jmol.___JSVSvnRev;
		 * 
		 */
		{
			BufferedInputStream bis = null;
			InputStream is = null;
			try {
				is = JSVersion.class.getClassLoader().getResourceAsStream(
						"jspecview/common/TODO.txt");
				bis = new BufferedInputStream(is);
				Properties props = new Properties();
				props.load(bis);
				tmpVersion = PT.trimQuotes(props.getProperty("Jmol.___JSVVersion", tmpVersion));
				tmpDate = PT.trimQuotes(props.getProperty("Jmol.___JSVDate", tmpDate));
				tmpSVN = PT.trimQuotes(props.getProperty("Jmol.___JSVSvnRev", tmpSVN));
			} catch (Exception e) {
				// Nothing to do
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (Exception e) {
						// Nothing to do
					}
				}
				if (is != null) {
					try {
						is.close();
					} catch (Exception e) {
						// Nothing to do
					}
				}
			}
		}
		if (tmpDate != null)
			tmpDate = tmpDate.substring(7, 23);
		tmpSVN = (tmpSVN == null ? "" : "/SVN" + tmpSVN.substring(22, 27));
		VERSION_SHORT = (tmpVersion != null ? tmpVersion : "(Unknown version)");
		VERSION = VERSION_SHORT + tmpSVN + "/"
				+ (tmpDate != null ? tmpDate : "(Unknown date)");
	}

  
}
