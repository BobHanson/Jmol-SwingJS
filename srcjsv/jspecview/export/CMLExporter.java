/* Copyright (c) 2006-2008 The University of the West Indies
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
 * class <code>CMLExporter</code> contains static methods to export a Graph as
 * as CIML. <code>CMLExporter</code> uses a template file called 'cml_tmp.vm'
 *  or 'cml_nmr.vm'. So any changes in design should be done in these files.
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class CMLExporter extends XMLExporter {

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

    if (model == null || model.equals(""))
      model = "unknown";

    if (datatype.contains("MASS"))
      spectypeInitials = "massSpectrum";
    else if (datatype.contains("INFRARED")) {
      spectypeInitials = "infrared";
    } else if (datatype.contains("UV") || (datatype.contains("VIS"))) {
      spectypeInitials = "UV/VIS";
    } else if (datatype.contains("NMR")) {
      spectypeInitials = "NMR";
    }
    ident = spectypeInitials + "_"
    + title.substring(0, Math.min(10, title.length()));

    if (xUnits.toLowerCase().equals("m/z"))
      xUnits = "moverz";
    else if (xUnits.toLowerCase().equals("1/cm"))
      xUnits = "cm-1";
    else if (xUnits.toLowerCase().equals("nanometers"))
      xUnits = "nm";

    setContext();

    return writeFormType("cml");
  }
}
