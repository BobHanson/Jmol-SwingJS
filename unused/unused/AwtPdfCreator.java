/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-30 18:58:33 -0500 (Tue, 30 Jun 2009) $
 * $Revision: 11158 $
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
package jspecview.unused;

//import com.lowagie.text.Document;
//import com.lowagie.text.PageSize;
//import com.lowagie.text.pdf.PdfContentByte;
//import com.lowagie.text.pdf.PdfTemplate;
//import com.lowagie.text.pdf.PdfWriter;
//
//import java.awt.Dimension;
//import java.awt.Graphics2D;
//import java.awt.print.PageFormat;
//import java.awt.print.Paper;
//import java.io.OutputStream;
//
//import javax.print.attribute.standard.MediaSizeName;
//
//import jspecview.api.JSVPanel;
//import jspecview.api.PdfCreatorInterface;
//import jspecview.common.PrintLayout;
//import jspecview.java.AwtPanel;

public class AwtPdfCreator {//implements PdfCreatorInterface {
  
  public AwtPdfCreator() {
   // for Class.forName  
  }

//	public void createPdfDocument(JSVPanel panel, PrintLayout pl, OutputStream os) {
//  	boolean isLandscape = pl.layout.equals("landscape");
//    Document document = new Document(isLandscape ? PageSize.LETTER.rotate() : PageSize.LETTER);
//    Dimension d = getDimension(pl.paper);
//    PageFormat pf = new PageFormat();
//    Paper p = new Paper();      
//    p.setImageableArea(0, 0, d.width, d.height);
//    pf.setPaper(p);
//    pf.setOrientation(pl.layout.equals("landscape") ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
//    int w = (isLandscape ? d.height : d.width);
//    int h = (isLandscape ? d.width : d.height);
//    try {    	
//      PdfWriter writer = PdfWriter.getInstance(document, os);
//      document.open();
//      PdfContentByte cb = writer.getDirectContent();
//      PdfTemplate tp = cb.createTemplate(w, h);
//      
//      Graphics2D g2 = tp.createGraphics(w, h);
//      tp.setWidth(w);
//      tp.setHeight(h);
//      ((AwtPanel) panel).print(g2, pf, 0);
//      g2.dispose();
//      cb.addTemplate(tp, 0, 0);
//    } catch (Exception e) {
//      panel.showMessage(e.getMessage(), "PDF Creation Error");
//    }
//    document.close();
//	}
//
//	private Dimension getDimension(Object opaper) {
//		MediaSizeName paper = (MediaSizeName) opaper;
//		// ftp://ftp.pwg.org/pub/pwg/media-sizes/pwg-media-size-03.pdf
//		// at 72 dpi we have...
//		if (paper == MediaSizeName.NA_LETTER) {
//			return new Dimension((int) (8.5 * 72), 11 * 72);
//		}
//		if (paper == MediaSizeName.NA_LEGAL) {
//			return new Dimension((int) (8.5 * 72), 14 * 72);
//		}
//		if (paper == MediaSizeName.ISO_A4) {
//			return new Dimension((int) (210 / 25.4 * 72), (int) (297 / 25.4 * 72));
//		}
//		// if (paper == MediaSizeName.ISO_B4) {
//		return new Dimension((int) (250 / 25.4 * 72), (int) (353 / 25.4 * 72));
//	}
  
}
