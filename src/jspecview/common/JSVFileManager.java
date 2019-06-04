/* Copyright (c) 2002-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 * Author: Bob Hanson (hansonr@stolaf.edu) and Jmol developers -- 2008
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

package jspecview.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Encoding;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;

import jspecview.api.JSVZipInterface;
import jspecview.exception.JSVException;

public class JSVFileManager {

	// ALL STATIC METHODS

	public final static String SIMULATION_PROTOCOL = "http://SIMULATION/";
	// possibly http://SIMULATION/H1/MOL=...\n....\n....\n....

	public static URL appletDocumentBase;

	private static JSViewer viewer;

	public boolean isApplet() {
		return (appletDocumentBase != null);
	}

	public static String jsDocumentBase = "";

	
	/**
	 * @param name
	 * @return file as string
	 * 
	 */

	public static String getFileAsString(String name) {
		if (name == null)
			return null;
		BufferedReader br;
		SB sb = new SB();
		try {
			br = getBufferedReaderFromName(name, null);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.appendC('\n');
			}
			br.close();
		} catch (Exception e) {
			return null;
		}
		return sb.toString();
	}

	public static BufferedReader getBufferedReaderForInputStream(InputStream in) {
		try {
			return new BufferedReader(new InputStreamReader(in, "UTF-8"));
		} catch (Exception e) {
			return null;
		}
	}

  public static BufferedReader getBufferedReaderForStringOrBytes(Object stringOrBytes) {
    return (stringOrBytes == null ? null : new BufferedReader(new StringReader(
        stringOrBytes instanceof String ? (String) stringOrBytes : new String((byte[]) stringOrBytes))));
  }

	public static BufferedReader getBufferedReaderFromName(String name,
			String startCode) throws JSVException {
		if (name == null)
			throw new JSVException("Cannot find " + name);
		Logger.info("JSVFileManager getBufferedReaderFromName " + name);
		String path = getFullPathName(name);
		if (!path.equals(name))
			Logger.info("JSVFileManager getBufferedReaderFromName " + path);
		return getUnzippedBufferedReaderFromName(path, startCode);
	}

	/**
	 * 
	 * FileManager.classifyName
	 * 
	 * follow this with .replace('\\','/') and Escape.escape() to match Jmol's
	 * file name in <PeakData file="...">
	 * 
	 * @param name
	 * @return name
	 * @throws JSVException
	 */
	public static String getFullPathName(String name) throws JSVException {
		try {
			if (appletDocumentBase == null) {
				// This code is for the app
				if (isURL(name)) {
					URL url = new URL((URL) null, name, null);
					return url.toString();
				}
				return viewer.apiPlatform.newFile(name).getFullPath();
			}
			// This code is only for the applet
			if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
				name = "file:///" + name;
			else if (name.startsWith("cache://"))
				return name;
			URL url = new URL(appletDocumentBase, name, null);
			return url.toString();
		} catch (Exception e) {
			throw new JSVException("Cannot create path for " + name);
		}
	}

	private final static String[] urlPrefixes = { "http:", "https:", "ftp:",
			SIMULATION_PROTOCOL, "file:" };

	public final static int URL_LOCAL = 4;

	public static boolean isURL(String name) {
		for (int i = urlPrefixes.length; --i >= 0;)
			if (name.startsWith(urlPrefixes[i]))
				return true;
		return false;
	}

	public static int urlTypeIndex(String name) {
		for (int i = 0; i < urlPrefixes.length; ++i) {
			if (name.startsWith(urlPrefixes[i])) {
				return i;
			}
		}
		return -1;
	}

	public static boolean isLocal(String fileName) {
		if (fileName == null)
			return false;
		int itype = urlTypeIndex(fileName);
		return (itype < 0 || itype == URL_LOCAL);
	}

	private static BufferedReader getUnzippedBufferedReaderFromName(String name,
			String startCode) throws JSVException {
		String[] subFileList = null;
		if (name.indexOf("|") >= 0) {
			subFileList = PT.split(name, "|");
			if (subFileList != null && subFileList.length > 0)
				name = subFileList[0];
		}
		if (name.startsWith(SIMULATION_PROTOCOL))
			return getSimulationReader(name);
		try {
			Object ret = getInputStream(name, true, null);
			if (ret instanceof SB || ret instanceof String)
				return new BufferedReader(new StringReader(ret.toString()));			
			if (isAB(ret))
				return new BufferedReader(new StringReader(new String((byte[]) ret)));
			BufferedInputStream bis = new BufferedInputStream((InputStream) ret);
			InputStream in = bis;
			if (isZipFile(bis))
				return ((JSVZipInterface) JSViewer
						.getInterface("jspecview.common.JSVZipUtil"))
						.newJSVZipFileSequentialReader(in, subFileList, startCode);
			if (isGzip(bis))
				in = ((JSVZipInterface) JSViewer
						.getInterface("jspecview.common.JSVZipUtil")).newGZIPInputStream(in);
			return new BufferedReader(new InputStreamReader(in, "UTF-8"));
		} catch (Exception e) {
			throw new JSVException("Cannot read file " + name + " " + e);
		}
	}

	/**
	 * In the case of applet-based simulations with file names that 
	 * involve mol=..., we want to abbreviate those names for display
	 * 
	 * @param name  actual path name to simulation
	 * @return actual name or hashed name
	 */
	public static String getAbbrSimulationFileName(String name) {
		String type = getSimulationType(name);
		String filename = getAbbreviatedSimulationName(name, type, true);
//		if (name.indexOf("MOL=") >= 0)
//			cachePut(name, cacheGet(type + name));
		return filename;
	}
	
	static String getAbbreviatedSimulationName(String name, String type, boolean addProtocol) {
		return (name.indexOf("MOL=") >= 0 ? (addProtocol ? SIMULATION_PROTOCOL : "") + "MOL=" 
				+ getSimulationHash(name, type) : name);
	}

	private static String getSimulationHash(String name, String type) {
		String code = type + Math.abs(name.substring(name.indexOf("V2000") + 1).hashCode());
		if (Logger.debugging)
			System.out.println("JSVFileManager hash for " + name + " = " + code);
		return code;
	}
	
	public static String getSimulationFileData(String name, String type) {
    return cacheGet(name.startsWith("MOL=") ? name.substring(4) : 
    	  getAbbreviatedSimulationName(name, type, false));
	}

	private static Map<String, String> htCorrelationCache = new Hashtable<String, String>();
	
	public static void cachePut(String name, String data) {
		if (Logger.debugging)
			Logger.debug("JSVFileManager cachePut " + data + " for " + name);
		if (data != null)
			htCorrelationCache.put(name,  data);
	}

	public static String cacheGet(String key) {
		String data = htCorrelationCache.get(key);
		if (Logger.debugging)
			Logger.info("JSVFileManager cacheGet " + data +  " for " + key);
		return data;
	}

  private static BufferedReader getSimulationReader(String name) {
		String data = cacheGet(name);
		if (data == null)
			cachePut(name, data = getNMRSimulationJCampDX(name.substring(SIMULATION_PROTOCOL.length())));
		return getBufferedReaderForStringOrBytes(data);
	}

	public static boolean isAB(Object x) {
		/**
		 * @j2sNative return Clazz.isAI(x);
		 */
		{
			return x instanceof byte[];
		}
	}

	public static boolean isZipFile(InputStream is) throws JSVException {
		try {
			byte[] abMagic = new byte[4];
			is.mark(5);
			int countRead = is.read(abMagic, 0, 4);
			is.reset();
			return (countRead == 4 && abMagic[0] == (byte) 0x50
					&& abMagic[1] == (byte) 0x4B && abMagic[2] == (byte) 0x03 && abMagic[3] == (byte) 0x04);
		} catch (Exception e) {
			throw new JSVException(e.toString());
		}
	}

	private static boolean isGzip(InputStream is) throws JSVException {
		try {
			byte[] abMagic = new byte[4];
			is.mark(5);
			int countRead = is.read(abMagic, 0, 4);
			is.reset();
			return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
		} catch (Exception e) {
			throw new JSVException(e.toString());
		}
	}

	public static Object getStreamAsBytes(BufferedInputStream bis, OC out)
			throws JSVException {
		try {
		byte[] buf = new byte[1024];
		byte[] bytes = (out == null ? new byte[4096] : null);
		int len = 0;
		int totalLen = 0;
		while ((len = bis.read(buf, 0, 1024)) > 0) {
			totalLen += len;
			if (out == null) {
				if (totalLen >= bytes.length)
					bytes = AU.ensureLengthByte(bytes, totalLen * 2);
				System.arraycopy(buf, 0, bytes, totalLen - len, len);
			} else {
				out.write(buf, 0, len);
			}
		}
		bis.close();
		if (out == null) {
			return AU.arrayCopyByte(bytes, totalLen);
		}
		return totalLen + " bytes";
		} catch (Exception e) {
			throw new JSVException(e.toString());
		}

	}

	public static String postByteArray(String fileName, byte[] bytes) {
		Object ret = null;
		try {
			ret = getInputStream(fileName, false, bytes);
		} catch (Exception e) {
			ret = e.toString();
		}
		if (ret instanceof String)
			return (String) ret;
		try {
			ret = getStreamAsBytes((BufferedInputStream) ret, null);
		} catch (JSVException e) {
			try {
				((BufferedInputStream) ret).close();
			} catch (Exception e1) {
				// ignore
			}
		}
		return (ret == null ? "" : fixUTF((byte[]) ret));
	}

	private static Encoding getUTFEncoding(byte[] bytes) {
		if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB
				&& bytes[2] == (byte) 0xBF)
			return Encoding.UTF8;
		if (bytes.length >= 4 && bytes[0] == (byte) 0 && bytes[1] == (byte) 0
				&& bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF)
			return Encoding.UTF_32BE;
		if (bytes.length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE
				&& bytes[2] == (byte) 0 && bytes[3] == (byte) 0)
			return Encoding.UTF_32LE;
		if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
			return Encoding.UTF_16LE;
		if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)
			return Encoding.UTF_16BE;
		return Encoding.NONE;

	}

	public static String fixUTF(byte[] bytes) {

		Encoding encoding = getUTFEncoding(bytes);
		if (encoding != Encoding.NONE)
			try {
				String s = new String(bytes, encoding.name().replace('_', '-'));
				switch (encoding) {
				case UTF8:
				case UTF_16BE:
				case UTF_16LE:
					// extra byte at beginning removed
					s = s.substring(1);
					break;
				default:
					break;
				}
				return s;
			} catch (IOException e) {
				Logger.error("fixUTF error " + e);
			}
		return new String(bytes);
	}

	public static InputStream getInputStream(String name, boolean showMsg,
			byte[] postBytes) throws JSVException {
		boolean isURL = isURL(name);
		boolean isApplet = (appletDocumentBase != null);
		Object in = null;
		// int length;
		String post = null;
		int iurl;
		if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
			post = name.substring(iurl + 6);
			name = name.substring(0, iurl);
		}
		if (isApplet || isURL) {
			URL url;
			try {
				url = new URL(appletDocumentBase, name, null);
			} catch (Exception e) {
				throw new JSVException("Cannot read " + name);
			}
			Logger.info("JSVFileManager opening URL " + url
					+ (post == null ? "" : " with POST of " + post.length() + " bytes"));
			in = viewer.apiPlatform.getURLContents(url, postBytes, post, false);
		} else {
			if (showMsg)
				Logger.info("JSVFileManager opening file " + name);
			in = viewer.apiPlatform.getBufferedFileInputStream(name);
		}
		if (in instanceof String)
			throw new JSVException((String) in);
		return (InputStream) in;

	}

	private static String nciResolver = "https://cactus.nci.nih.gov/chemical/structure/%FILE/file?format=sdf&get3d=True";
	private static String nmrdbServerH1 = "http://www.nmrdb.org/tools/jmol/predict.php?POST?molfile=";
  private static String nmrdbServerC13 = "http://www.nmrdb.org/service/jsmol13c?POST?molfile=";

	/**
	 * Accepts either $chemicalname or MOL=molfiledata Queries NMRDB or NIH+NMRDB
	 * to get predicted spectrum
	 * 
	 * TODO: how about adding spectrometer frequency? TODO: options for other data
	 * types? 2D? IR?
	 * 
	 * @param name
	 * @return jcamp data
	 */
	@SuppressWarnings({ "unchecked" })
	private static String getNMRSimulationJCampDX(String name) {
		int pt = 0;
		String molFile = null;
		String type = getSimulationType(name);
		if (name.startsWith(type))
			name = name.substring(type.length() + 1);
		boolean isInline = name.startsWith("MOL=");
		if (isInline) {
			name = name.substring(4);
			pt = name.indexOf("/n__Jmol");
			if (pt > 0)
				name = name.substring(0, pt) + PT.rep(name.substring(pt), "/n", "\n");
			molFile = name = PT.rep(name, "\\n", "\n");
		}
		String key = "" + getSimulationHash(name, type);
		if (Logger.debugging)
			Logger.info("JSVFileManager type=" + type + " key=" + key + " name="
					+ name);
		String jcamp = cacheGet(key);
		if (jcamp != null)
			return jcamp;
		String src = (isInline ? null : PT.rep(nciResolver, "%FILE",
				PT.escapeUrl(name)));
		if (!isInline && (molFile = getFileAsString(src)) == null
				|| molFile.indexOf("<html") >= 0) {
			Logger.error("no MOL data returned by NCI");
			return null;
		}
		boolean is13C = type.equals("C13");
		String url = (is13C ? nmrdbServerC13 : nmrdbServerH1);
		String json = getFileAsString(url + molFile);
		Map<String, Object> map = (new javajs.util.JSJSONParser()).parseMap(json,
				true);
		cachePut("json", json);
		// json = PT.rep(json, "\\r\\n", "\n");
		// json = PT.rep(json, "\\t", "\t");
		// json = PT.rep(json, "\\n", "\n");
		if (is13C)
			map = (Map<String, Object>) map.get("result");
		String jsonMolFile = (String) map.get("molfile");
		if (jsonMolFile == null) {
			System.out.println("JSVFileManager: no MOL file returned from EPFL");
			jsonMolFile = molFile;
		} else {
			System.out.println("JSVFileManager: MOL file hash="
					+ jsonMolFile.hashCode());
		}
		// atomMap added in Jmol 14.5.5
		int[] atomMap = getAtomMap(jsonMolFile, molFile);
		cachePut("mol", molFile);
		/**
		 * @j2sNative
		 * 
		 *            if (!isInline) Jmol.Cache.put("http://SIMULATION/" + type +
		 *            "/" + name + "#molfile", molFile.getBytes());
		 * 
		 */
		{
			// JAVA only
			System.out.println("type/name: " + type + "/" + name);
			// System.out.println("molFile is \n" + molFile);
			System.out.println("jsonMolFile is \n" + jsonMolFile);
			viewer.syncScript("JSVSTR:" + molFile);
		}
		String xml = "<Signals src="
				+ PT.esc(PT.rep(is13C ? nmrdbServerC13 : nmrdbServerH1,
						"?POST?molfile=", "")) + ">\n";
		if (is13C) {
			// 13C data -- no XML
			Map<String, Object> spec = (Map<String, Object>) map.get("spectrum13C");
			jcamp = (String) ((Map<String, Object>) spec.get("jcamp")).get("value");
			Lst<Object> lst = (Lst<Object>) spec.get("predCSNuc");
			SB sb = new SB();
			for (int i = lst.size(); --i >= 0;) {
				map = (Map<String, Object>) lst.get(i);
				sb.append("<Signal ");
				setAttr(sb, "type", "nucleus", map);
				if (atomMap == null)
					setAttr(sb, "atoms", "assignment", map);
				else
					sb.append("atoms=\"")
							.appendI(atomMap[PT.parseInt((String) map.get("assignment"))])
							.append("\" ");
				setAttr(sb, "multiplicity", "multiplicity", map);
				map = (Map<String, Object>) map.get("integralData");
				setAttr(sb, "xMin", "from", map);
				setAttr(sb, "xMax", "to", map);
				setAttr(sb, "integral", "value", map);
				sb.append("></Signal>\n");
			}
			sb.append("</Signals>");
			xml += sb.toString();
		} else {
			// <Signals><Signal type="1HNMR" xMin="7.2226225" xMax="7.295377500000001"
			// atoms="13" multiplicity="dd" integral="1"
			// diaID="dcND`BePfTfYUYa``bX@GzP`HeT"><Couplings><Coupling atoms="16"
			// value="4.752"/><Coupling atoms="15"
			// value="8.799"/></Couplings></Signal><Signal type="1HNMR"
			// xMin="7.222512500000001" xMax="7.2954875" atoms="14" multiplicity="dd"
			// integral="1" diaID="dcND`BePfTfYUYa``bX@GzP`HeT"><Couplings><Coupling
			// atoms="15" value="4.797"/><Coupling atoms="16"
			// value="8.798"/></Couplings></Signal><Signal type="1HNMR" xMin="6.63251"
			// xMax="6.705489999999999" atoms="15" multiplicity="dd" integral="1"
			// diaID="dcND`AIPfTfYwYn``JX@GzP`HeT"><Couplings><Coupling atoms="13"
			// value="8.799"/><Coupling atoms="14"
			// value="4.797"/></Couplings></Signal><Signal type="1HNMR"
			// xMin="6.632625" xMax="6.705374999999999" atoms="16" multiplicity="dd"
			// integral="1" diaID="dcND`AIPfTfYwYn``JX@GzP`HeT"><Couplings><Coupling
			// atoms="13" value="4.752"/><Coupling atoms="14"
			// value="8.798"/></Couplings></Signal><Signal type="1HNMR" xMin="2.0125"
			// xMax="2.0175" atoms="17" multiplicity="" integral="1"
			// diaID="dcND`BsPfTeme^Uih@H@GzP`HeT"><Couplings></Couplings></Signal><Signal
			// type="1HNMR" xMin="2.0125" xMax="2.0175" atoms="18" multiplicity=""
			// integral="1"
			// diaID="dcND`BsPfTeme^Uih@H@GzP`HeT"><Couplings></Couplings></Signal><Signal
			// type="1HNMR" xMin="2.0125" xMax="2.0175" atoms="19" multiplicity=""
			// integral="1"
			// diaID="dcND`BsPfTeme^Uih@H@GzP`HeT"><Couplings></Couplings></Signal></Signals>
			xml = PT.rep((String) map.get("xml"), "<Signals>", xml);
			if (atomMap != null) {
				SB sb = new SB();
				String[] signals = PT.split(xml, " atoms=\"");
				sb.append(signals[0]);
				for (int i = 1; i < signals.length; i++) {
					String s = signals[i];
					int a = PT.parseInt(s);
					sb.append(" atoms=\"").appendI(atomMap[a])
							.append(s.substring(s.indexOf("\"")));
				}
				xml = sb.toString();
			}
			xml = PT.rep(xml, "</", "\n</");
			xml = PT.rep(xml, "><", ">\n<");
			xml = PT.rep(xml, "\\\"", "\"");
			jcamp = (String) map.get("jcamp");
		}
		if (Logger.debugging)
			Logger.info(xml);
		cachePut("xml", xml);
		jcamp = "##TITLE=" + (isInline ? "JMOL SIMULATION/" + type : name) + "\n"
				+ jcamp.substring(jcamp.indexOf("\n##") + 1);
		pt = molFile.indexOf("\n");
		pt = molFile.indexOf("\n", pt + 1);
		if (pt > 0 && pt == molFile.indexOf("\n \n"))
			molFile = molFile.substring(0, pt + 1) + "Created "
					+ viewer.apiPlatform.getDateFormat("8824") + " by JSpecView "
					+ JSVersion.VERSION + molFile.substring(pt + 1);
		pt = 0;
		pt = jcamp.indexOf("##.");
		String id = getAbbreviatedSimulationName(name, type, false);
		int pt1 = id.indexOf("id='");
		if (isInline && pt1 > 0)
			id = id.substring(pt1 + 4, (id + "'").indexOf("'", pt1 + 4));
		jcamp = jcamp.substring(0, pt) + "##$MODELS=\n<Models>\n"
				+ "<ModelData id=" + PT.esc(id) + " type=\"MOL\" src=" + PT.esc(src)
				+ ">\n" + molFile + "</ModelData>\n</Models>\n" + "##$SIGNALS=\n" + xml
				+ "\n" + jcamp.substring(pt);
		cachePut("jcamp", jcamp);
		cachePut(key, jcamp);
		return jcamp;
	}

	/**
	 * create a map from JSON to Jmol
	 * 
	 * @param jsonMolFile
	 * @param jmolMolFile
	 * @return int[]
	 */
	private static int[] getAtomMap(String jsonMolFile, String jmolMolFile) {
		P3[] acJson = getCoord(jsonMolFile);
		P3[] acJmol = getCoord(jmolMolFile);
		int n = acJson.length; 
		if (n != acJmol.length)
			return null;
		int[] map = new int[n];
		BS bs = new BS();
		bs.setBits(0, n);
		boolean haveMap = false;
		for (int i = 0; i < n; i++) {
			P3 a = acJson[i];
			for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
				if (a.distanceSquared(acJmol[j]) < 0.1f) {
					bs.clear(j);
					map[i] = j;
					if (i != j)
						haveMap = true;
					break;
				}
			}			
		}
		return (haveMap ? map : null);
	}

	/**
	 * just extract coordinates from MOL file
	 * 
	 * @param mol
	 * @return P3[]
	 */
	private static P3[] getCoord(String mol) {
		String[] lines = PT.split(mol,  "\n");
		float[] data = new float[3];
		int n = Integer.parseInt(lines[3].substring(0, 3).trim());
		P3[] pts = new P3[n];
		for (int i = 0; i < n; i++) {
			String line = lines[4 + i];
			PT.parseFloatArrayInfested(PT.getTokens(line.substring(0, 31)), data);
			pts[i] = P3.new3(data[0], data[1], data[2]); 
		}
		return pts;
	}

	private static void setAttr(SB sb, String mykey, String lucsKey,
			Map<String, Object> map) {
		sb.append(mykey + "=\"").appendO(map.get(lucsKey)).append("\" ");
	}

	private static URL getResource(Object object, String fileName, String[] error) {
		URL url = null;
		try {
			if ((url = object.getClass().getResource(fileName)) == null)
				error[0] = "Couldn't find file: " + fileName;
		} catch (Exception e) {

			error[0] = "Exception " + e + " in getResource " + fileName;
		}
		return url;
	}

	public static String getResourceString(Object object, String name,
			String[] error) {
		Object url = getResource(object, name, error);
		if (url == null) {
			error[0] = "Error loading resource " + name;
			return null;
		}
		if (url instanceof String) {
			// JavaScript does this -- all resources are just files on the site
			// somewhere
			return getFileAsString((String) url);
		}
		SB sb = new SB();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(InputStream) ((URL) url).getContent(), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line).append("\n");
			br.close();
		} catch (Exception e) {
			error[0] = e.toString();
		}
		return sb.toString();
	}

	public static String getJmolFilePath(String filePath) {
		try {
			filePath = getFullPathName(filePath);
		} catch (JSVException e) {
			return null;
		}
		return (appletDocumentBase == null ? filePath.replace('\\', '/') : filePath);
	}

	private static int stringCount;

	/**
	 * Returns a name that can be used as a tag, possibly
	 * abbreviated. 
	 * 
	 * @param fileName
	 * @return actual or abbreviated file name
	 */
	public static String getTagName(String fileName) {
		if (fileName == null)
			return "String" + (++stringCount);
		if (isURL(fileName)) {
			try {
				if (fileName.startsWith(SIMULATION_PROTOCOL))
					return getAbbrSimulationFileName(fileName);
				String name = (new URL((URL) null, fileName, null)).getFile();
				return name.substring(name.lastIndexOf('/') + 1);
			} catch (IOException e) {
				return null;
			}
		}
		return viewer.apiPlatform.newFile(fileName).getName();
	}

//	public static String getQuotedJSONAttribute(String json, String key1,
//			String key2) {
//		if (key2 == null)
//			key2 = key1;
//		key1 = "\"" + key1 + "\":";
//		key2 = "\"" + key2 + "\":";
//		int pt1 = json.indexOf(key1);
//		int pt2 = json.indexOf(key2, pt1);
//		return (pt1 < 0 || pt2 < 0 ? null : PT.getQuotedStringAt(json,
//				pt2 + key2.length()));
//	}

	public static void setDocumentBase(JSViewer v, URL documentBase) {
		viewer = v;
		appletDocumentBase = documentBase;
	}

	public static String getSimulationType(String filePath) {
	  return (filePath.indexOf("C13/") >= 0 ? "C13" : "H1");
	}

}

// a nice idea, but never implemented; not relevant to JavaScript
//
// class JSVMonitorInputStream extends FilterInputStream {
// int length;
// int position;
// int markPosition;
// int readEventCount;
//
// JSVMonitorInputStream(InputStream in, int length) {
// super(in);
// this.length = length;
// this.position = 0;
// }
//
// /**
// * purposely leaving off "Override" here for JavaScript
// *
// * @j2sIgnore
// */
// public int read() throws IOException {
// ++readEventCount;
// int nextByte = super.read();
// if (nextByte >= 0)
// ++position;
// return nextByte;
// }
// /**
// * purposely leaving off "Override" here for JavaScript
// *
// * @j2sIgnore
// */
// public int read(byte[] b) throws IOException {
// ++readEventCount;
// int cb = super.read(b);
// if (cb > 0)
// position += cb;
// return cb;
// }
//
// @Override
// public int read(byte[] b, int off, int len) throws IOException {
// ++readEventCount;
// int cb = super.read(b, off, len);
// if (cb > 0)
// position += cb;
// return cb;
// }
//
// @Override
// public long skip(long n) throws IOException {
// long cb = super.skip(n);
// // this will only work in relatively small files ... 2Gb
// position = (int) (position + cb);
// return cb;
// }
//
// @Override
// public synchronized void mark(int readlimit) {
// super.mark(readlimit);
// markPosition = position;
// }
//
// @Override
// public synchronized void reset() throws IOException {
// position = markPosition;
// super.reset();
// }
//
// int getPosition() {
// return position;
// }
//
// int getLength() {
// return length;
// }
//
// int getPercentageRead() {
// return position * 100 / length;
// }
// }
