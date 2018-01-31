/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.export;

import java.io.IOException;

import javajs.util.OC;


import jspecview.common.ExportType;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;

/**
 * class <code>AnIMLExporter</code> contains static methods to export a Graph as
 * as AnIML. <code>AnIMLExporter</code> uses a template file called 'animl_tmp.vm'
 * or 'animl_nmr.vm'. So any changes in design should be done in these files.
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class AMLExporter extends XMLExporter {

	/**
   * Exports the Spectrum that is displayed by JSVPanel to a file given by fileName
   * If display is zoomed then export the current view
	 * @param out
	 * @param spec the spectrum to export
	 * @param startIndex the starting point of the spectrum
	 * @param endIndex the end point
	 * @param mode TODO
   * @return data if fileName is null
   * @throws IOException
   */
  @Override
	public String exportTheSpectrum(JSViewer viewer, ExportType mode, OC out, Spectrum spec,
                     int startIndex, int endIndex, PanelData pd, boolean asBase64) throws IOException {
    if (!setup(viewer, spec, out, startIndex, endIndex))
      return null;

    if (solvName == null || solvName.equals(""))
      solvName = "unknown";

    if (datatype.contains("MASS")) {
      spectypeInitials = "MS";
    } else if (datatype.contains("INFRARED")) {
      spectypeInitials = "IR";
    } else if (datatype.contains("UV") || (datatype.contains("VIS"))) {
      spectypeInitials = "UV";
    } else if (datatype.contains("NMR")) {
      spectypeInitials = "NMR";
    }
    // QUESTION: Why is the file's pathlength data totally ignored?

    pathlength = (pathlength.equals("") && spectypeInitials.equals("UV") ? "1.0"
        : "-1");

    if (vendor == null || vendor.equals(""))
      vendor = "not available from JCAMP-DX file";
    if (model == null || model.equals(""))
      model = "not available from JCAMP-DX file";
    if (resolution == null || resolution.equals(""))
      resolution="not available in JCAMP-DX file";

    setContext();
    return writeFormType("animl");
  }

}
