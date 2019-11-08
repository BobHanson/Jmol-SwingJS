/* Copyright (c) 2002-2011 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.source;

import javajs.util.Lst;
import jspecview.common.Spectrum;



/**
 * <code>JDXSource</code> is representation of all the data in the JCAMP-DX file
 * or source. Note: All Jdx Source are viewed as having a set of Spectra
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */
public class JDXSource extends JDXHeader {

  public final static int TYPE_VIEW = -2;
  
  public final static int TYPE_UNKNOWN = -1;
  public final static int TYPE_SIMPLE = 0;
  public final static int TYPE_BLOCK = 1;
  public final static int TYPE_NTUPLE = 2;

  public int type = TYPE_SIMPLE;
  public boolean isCompoundSource = false;
  
  private Lst<Spectrum> jdxSpectra;
  private String errors = "";
  private String filePath;


  public void dispose() {
    headerTable = null;
    jdxSpectra = null;    
  }
  
  public int peakCount;

	public boolean isView;

  private String inlineData;

  public JDXSource(int type, String filePath) {
    this.type = type;
    setFilePath(filePath);
    headerTable = new Lst<String[]>();
    jdxSpectra = new Lst<Spectrum>();
    isCompoundSource = (type != TYPE_SIMPLE);
  }

  /**
   * Returns the Spectrum at a given index in the list
   * 
   * @param index
   *        the spectrum index
   * @return the Spectrum at a given index in the list
   */
  public Spectrum getJDXSpectrum(int index) {
    return (jdxSpectra.size() <= index ? null : jdxSpectra.get(index));
  }

  /**
   * Adds a Spectrum to the list
   * @param filePath 
   * 
   * @param spectrum
   *        the spectrum to be added
   * @param forceSub 
   */
  public void addJDXSpectrum(String filePath, Spectrum spectrum, boolean forceSub) {
    if (filePath == null)
      filePath = this.filePath;
    spectrum.setFilePath(filePath);
    if (inlineData != null)
      spectrum.setInlineData(inlineData);
    int n = jdxSpectra.size();
    if (n == 0 || !jdxSpectra.get(n - 1).addSubSpectrum(spectrum, forceSub))
      jdxSpectra.addLast(spectrum);
  }

  /**
   * Returns the number of Spectra in this Source
   * 
   * @return the number of Spectra in this Source
   */
  public int getNumberOfSpectra() {
    return jdxSpectra.size();
  }

  /**
   * Returns the Vector of Spectra
   * 
   * @return the Vector of Spectra
   */
  public Lst<Spectrum> getSpectra() {
    return jdxSpectra;
  }

  /**
   * Used in Android 
   * 
   * @return array of JDXpectrum
   */
  public Spectrum[] getSpectraAsArray() {
    return (Spectrum[]) (jdxSpectra == null ? null : jdxSpectra.toArray());
  }

  /**
   * Returns the error log for this source
   * 
   * @return the error log for this source
   */
  public String getErrorLog() {
    return errors;
  }

  /**
   * Sets the error log for this source
   * 
   * @param errors
   *        error log for this source
   */
  public void setErrorLog(String errors) {
    this.errors = errors;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
  
  public String getFilePath() {
    return filePath;
  }

  public static JDXSource createView(Lst<Spectrum> specs) {
    JDXSource source = new JDXSource(TYPE_VIEW, "view");
    source.isView = true;
    for (int i = 0; i < specs.size(); i++)
      source.addJDXSpectrum(specs.get(i).getFilePath(), specs.get(i), false);
    return source;
  }

  public String[][] getHeaderRowDataAsArray(boolean addDataClass,
                                            String[][] rowData) {
    if (rowData == null)
      rowData = new String[0][0];
    String[][] data = getHeaderRowDataAsArray(addDataClass, rowData.length);
    for (int i = rowData.length; --i >= 0; )
      data[data.length - rowData.length + i] = rowData[i];
    return data;
  }

	public void setID(String id) {
		jdxSpectra.get(0).sourceID = id;
	}

	public boolean matchesFilePath(String filePath) {
		return this.filePath.equals(filePath) 
				|| this.filePath.replace('\\', '/').equals(filePath);
	}

  public void setInlineData(String data) {
    inlineData = data;
    if (jdxSpectra != null)
      for (int i = jdxSpectra.size(); --i >= 0;)
        jdxSpectra.get(i).setInlineData(data);
  }
  
}
