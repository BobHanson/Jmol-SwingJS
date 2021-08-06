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

import org.jmol.viewer.JC;

import javajs.util.PT;

public class JSVersion {

  public static final String VERSION_SHORT;
  public static final String VERSION;
	public static final String date;
	public static final int versionInt;
	public static final String majorVersion;

	static {
		String tmpVersion = null;
		String tmpDate = null;

		// Reading version from resource inside jar
		{
			BufferedInputStream bis = null;
			InputStream is = null;
			try {
        // Reading version from resource   inside jar
        String s = null;
        /**
         * @j2sNative s = "core/Jmol.properties";
         * 
         */
        {
          s = "org/jmol/viewer/Jmol.properties";
        }
        is = JSVersion.class.getClassLoader().getResourceAsStream(s);
        bis = new BufferedInputStream(is);
        Properties props = new Properties();
        props.load(bis);
        s = props.getProperty("Jmol.___JmolVersion",
            tmpVersion);
        if (s != null && s.lastIndexOf("\"") > 0)
          s = s.substring(0, s.lastIndexOf("\"") + 1);
        tmpVersion = PT.trimQuotes(s);
        tmpDate = PT.trimQuotes(props.getProperty("Jmol.___JmolDate", tmpDate));
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
    if (tmpDate != null) {
      tmpDate = tmpDate.substring(7, 23);
      // NOTE : date is updated in the properties by SVN, and is in the format
      // "$Date: 2018-01-25 01:10:13 -0600 (Thu, 25 Jan 2018) $"
      //  0         1         2
      //  012345678901234567890123456789
    }
    VERSION_SHORT = (tmpVersion != null ? tmpVersion : "(Unknown_version)");
    String mv = (tmpVersion != null ? tmpVersion : "(Unknown_version)");
    date = (tmpDate != null ? tmpDate : "");
    VERSION = VERSION_SHORT + (date == null ? "" : " " + date);
    // 11.9.999 --> 1109999
    int v = -1;
    if (tmpVersion != null)
    try {
      String s = VERSION_SHORT;
      String major = "";
      // Major number
      int i = s.indexOf(".");
      if (i < 0) {
        v = 100000 * Integer.parseInt(s);
        s = null;
      }
      if (s != null) {
        v = 100000 * Integer.parseInt(major = s.substring(0, i));

        // Minor number
        s = s.substring(i + 1);
        i = s.indexOf(".");
        if (i < 0) {
          v += 1000 * Integer.parseInt(s);
          s = null;
        }
        if (s != null) {
          String m = s.substring(0, i);
          major += "." + m;
          mv = major;
          v += 1000 * Integer.parseInt(m);

          // Revision number
          s = s.substring(i + 1);
          i = s.indexOf("_");
          if (i >= 0)
            s = s.substring(0, i);
          i = s.indexOf(" ");
          if (i >= 0)
            s = s.substring(0, i);
          v += Integer.parseInt(s);
        }
      }
    } catch (NumberFormatException e) {
      // We simply keep the version currently found
    }
    majorVersion = mv;
    versionInt = v;
  }

  
}
