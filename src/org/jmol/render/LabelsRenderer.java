/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-09-12 22:40:29 -0500 (Mon, 12 Sep 2016) $
 * $Revision: 21241 $
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

package org.jmol.render;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.shape.Labels;
import org.jmol.util.Font;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;

import javajs.util.P3;
import javajs.util.P3i;

public class LabelsRenderer extends FontLineShapeRenderer {

  // offsets are from the font baseline

  final int[] minZ = new int[1];

  protected int ascent;
  protected int descent;
  protected float sppm;
  protected float[] xy = new float[3];
  private P3i screen = new P3i();

  int fidPrevious;

  protected P3 pTemp = new P3();

  protected short bgcolix;
  protected short labelColix;

  private int fid;

  private Atom atom;
  protected Point3fi atomPt;

  private int doPointer;

  private int offset;

  protected int textAlign;

  private int pointer;

  protected int zSlab = Integer.MIN_VALUE;

  private int zBox;

  private float[] boxXY;

  private float scalePixelsPerMicron;

  private int mode;

  @Override
  protected boolean render() {
    fidPrevious = 0;
    Labels labels = (Labels) shape;

    String[] labelStrings = labels.strings;
    int[] fids = labels.fids;
    int[] offsets = labels.offsets;
    if (labelStrings == null)
      return false;
    Atom[] atoms = ms.at;
    short backgroundColixContrast = vwr.cm.colixBackgroundContrast;
    int backgroundColor = vwr.getBackgroundArgb();
    sppm = vwr.getScalePixelsPerAngstrom(true);
    scalePixelsPerMicron = (vwr.getBoolean(T.fontscaling) ? sppm * 10000f : 0);
    imageFontScaling = vwr.imageFontScaling;
    int iGroup = -1;
    minZ[0] = Integer.MAX_VALUE;
    boolean isAntialiased = g3d.isAntialiased();
    for (int i = labelStrings.length; --i >= 0;) {
      atomPt = atom = atoms[i];
      if (!isVisibleForMe(atom))
        continue;
      String label = labelStrings[i];
      if (label == null || label.length() == 0 || labels.mads != null
          && labels.mads[i] < 0)
        continue;
      labelColix = labels.getColix2(i, atom, false);
      bgcolix = labels.getColix2(i, atom, true);
      if (bgcolix == 0
          && vwr.gdata.getColorArgbOrGray(labelColix) == backgroundColor)
        labelColix = backgroundColixContrast;
      fid = ((fids == null || i >= fids.length || fids[i] == 0) ? labels.zeroFontId
          : fids[i]);
      offset = (offsets == null || i >= offsets.length ? 0 : offsets[i]);
      boolean labelsFront = ((offset & JC.LABEL_ZPOS_FRONT) != 0);
      boolean labelsGroup = ((offset & JC.LABEL_ZPOS_GROUP) != 0);
      textAlign = JC.getAlignment(offset);
      pointer = JC.getPointer(offset);
      doPointer = (pointer & JC.LABEL_POINTER_ON);
      int isAbsolute = offset & JC.LABEL_EXPLICIT;
      mode = (doPointer | isAbsolute | (isAntialiased ? TextRenderer.MODE_IS_ANTIALIASED : 0));
      zSlab = atom.sZ - atom.sD / 2 - 3;
      if (zSlab < 1)
        zSlab = 1;
      zBox = zSlab;
      if (labelsGroup) {
        Group group = atom.group;
        int ig = group.groupIndex;
        if (ig != iGroup) {
          group.getMinZ(atoms, minZ);
          iGroup = ig;
        }
        zBox = minZ[0];
      } else if (labelsFront) {
        zBox = 1;
      }
      if (zBox < 1)
        zBox = 1;

      Text text = labels.getLabel(i);
      boxXY = (!isExport || vwr.creatingImage ? labels.getBox(i) : new float[5]);
      if (boxXY == null)
        labels.putBox(i, boxXY = new float[5]);
      text = renderLabelOrMeasure(text, label);
      if (text != null) {
        labels.putLabel(i, text);
      }
      if (isAntialiased) {
        boxXY[0] /= 2;
        boxXY[1] /= 2;
      }
      boxXY[4] = zBox;
    }
    return false;
  }

  protected Text renderLabelOrMeasure(Text text, String label) {
    boolean newText = false;
    short pointerColix = ((pointer & JC.LABEL_POINTER_BACKGROUND) != 0
        && bgcolix != 0 ? bgcolix : labelColix);
    if (text != null) {
      if (text.font == null)
        text.setFontFromFid(fid);
      text.atomX = atomPt.sX; // just for pointer
      text.atomY = atomPt.sY;
      text.atomZ = zSlab;
      if (text.pymolOffset == null) {
        text.setXYZs(atomPt.sX, atomPt.sY, zBox, zSlab);
        text.colix = labelColix;
        text.bgcolix = bgcolix;
      } else {
        text.getPymolScreenOffset(atomPt, screen, zSlab, pTemp, sppm);
      }
    } else {
      // Labels only, not measurements
      boolean isLeft = (textAlign == JC.TEXT_ALIGN_LEFT || textAlign == JC.TEXT_ALIGN_NONE);
      if (fid != fidPrevious || ascent == 0) {
        vwr.gdata.setFont(Font.getFont3D(fid));
        fidPrevious = fid;
        font3d = vwr.gdata.getFont3DCurrent();
        if (isLeft) {
          ascent = font3d.getAscent();
          descent = font3d.getDescent();
        }
      }
      boolean isSimple = isLeft
          && (imageFontScaling == 1 && scalePixelsPerMicron == 0
              && label.indexOf("|") < 0 && label.indexOf("\n") < 0 && label.indexOf("<su") < 0 && label
              .indexOf("<co") < 0);
      if (isSimple) {
        boxXY[0] = atomPt.sX;
        boxXY[1] = atomPt.sY;
        TextRenderer.renderSimpleLabel(g3d, font3d, label, labelColix, bgcolix,
            boxXY, zBox, zSlab, JC.getXOffset(offset), JC.getYOffset(offset),
            ascent, descent, pointerColix, (doPointer == 0 ? 0 : vwr.getInt(T.labelpointerwidth)), mode);
        return null;
      }
      text = Text.newLabel(vwr, font3d, label, labelColix, bgcolix, textAlign,
          0);
      text.atomX = atomPt.sX; // just for pointer
      text.atomY = atomPt.sY;
      text.atomZ = zSlab;
      text.setXYZs(atomPt.sX, atomPt.sY, zBox, zSlab);
      newText = true;
    }
    if (text.pymolOffset == null) {
      if (text.font == null)
        text.setFontFromFid(font3d.fid);
      text.setOffset(offset);
      if (textAlign != JC.TEXT_ALIGN_NONE)
        text.setAlignment(textAlign);
    }
    text.pointer = pointer;
    TextRenderer.render(text, g3d, scalePixelsPerMicron, imageFontScaling,
       boxXY, xy, pointerColix, (doPointer == 0 ? 0 : vwr.getInt(T.labelpointerwidth)), mode);
    return (newText ? text : null);
  }
}
