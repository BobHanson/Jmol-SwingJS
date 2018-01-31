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

import org.jmol.util.Logger;

import jspecview.api.JSVExporter;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;

/**
 * The XMLExporter should be a totally generic exporter
 *
 * no longer uses Velocity.
 * 
 * Implemented as AML, CML, and SVG
 *
 * @author Bob Hanson, hansonr@stolaf.edu
 *
 */
abstract class FormExporter implements JSVExporter {

  FormContext context = new FormContext();
  String errMsg;
  String currentTime;
  protected OC out;
  protected JSViewer vwr;


  protected void initForm(JSViewer viewer, OC out) {
  	this.vwr = viewer;
  	this.out = out;
    currentTime = viewer.apiPlatform.getDateFormat(null);
  }

  protected String writeForm(String templateFile) throws IOException {
    String[] error = new String[1];
    String template = JSVFileManager.getResourceString(this, "resources/" + templateFile, error);
    if (template == null) {
      Logger.error(error[0]);
      return error[0];
    }
    errMsg = context.setTemplate(template);
    if (errMsg != null) {
      Logger.error(errMsg);
      return errMsg;
    }

    errMsg = context.merge(out);
    if (out == null)
      return errMsg;
    if (errMsg != null) {
      Logger.error(errMsg);
      throw new IOException(errMsg);
    }

    out.closeChannel();
    return "OK " + out.getByteCount() + " bytes";
  }
}
