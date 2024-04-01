/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2020-09-22 08:42:29 -0500 (Tue, 22 Sep 2020) $
 * $Revision: 22032 $
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

package org.jmol.shape;

import org.jmol.c.PAL;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Text;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Font;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3d;
import javajs.util.PT;

import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;

import java.util.Hashtable;

import java.util.Map;

public class Labels extends AtomShape {

  public String[] strings;
  public String[] formats;
  public short[] bgcolixes;
  public int[] fids;
  public int[] offsets;

  private Map<Integer, Text> atomLabels = new Hashtable<Integer, Text>();
  private Map<Integer, double[]> labelBoxes;

  public BS bsFontSet;
  public BS bsBgColixSet;

  public int defaultOffset;
  public int defaultAlignment;
  public int defaultZPos;
  public int defaultFontId;
  public short defaultColix;
  public short defaultBgcolix;
  public byte defaultPaletteID;
  public int defaultPointer;

  public int zeroFontId;

  //  private boolean defaultsOnlyForNone = true;
  /**
   * defaults are set after giving SELECT NONE;
   */
  private boolean setDefaults = false;

  //labels

  @Override
  public void initShape() {
    defaultFontId = zeroFontId = vwr.gdata.getFont3DFSS(JC.DEFAULT_FONTFACE,
        JC.DEFAULT_FONTSTYLE, JC.LABEL_DEFAULT_FONTSIZE).fid;
    defaultColix = 0; //"none" -- inherit from atom
    defaultBgcolix = 0; //"none" -- off
    defaultOffset = JC.LABEL_DEFAULT_OFFSET;
    defaultAlignment = JC.TEXT_ALIGN_LEFT;
    defaultPointer = JC.LABEL_POINTER_NONE;
    defaultZPos = 0;
    translucentAllowed = false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    isActive = true;
    Atom[] atoms = ms.at;
    int ac = ms.ac;
    if ("setDefaults" == propertyName) {
      setDefaults = ((Boolean) value).booleanValue();
      return;
    }

    if ("color" == propertyName) {
      byte pid = PAL.pidOf(value);
      short colix = C.getColixO(value);
      if (setDefaults) {// || !defaultsOnlyForNone) {
        defaultColix = colix;
        defaultPaletteID = pid;
      } else {
        int n = checkColixLength(colix, bs.length());
        for (int i = bs.nextSetBit(0); i >= 0
            && i < n; i = bs.nextSetBit(i + 1))
          setLabelColix(i, colix, pid);
      }
      return;
    }

    if ("scalereference" == propertyName) {
      if (strings == null)
        return;
      double val = ((Number) value).doubleValue();
      double scalePixelsPerMicron = (val == 0 ? 0 : 10000d / val);
      int n = Math.min(ac, strings.length);
      for (int i = bs.nextSetBit(0); i >= 0
          && i < n; i = bs.nextSetBit(i + 1)) {
        Text text = getLabel(i);
        if (text == null) {
          text = Text.newLabel(vwr, null, strings[i], C.INHERIT_ALL, (short) 0,
              0, scalePixelsPerMicron);
          putLabel(i, text);
        } else {
          text.setScalePixelsPerMicron(scalePixelsPerMicron);
        }
      }
      return;
    }

    if ("label" == propertyName) {
      boolean isPicked = (isPickingMode() && bs.cardinality() == 1
          && bs.nextSetBit(0) == lastPicked);
      setScaling();
      LabelToken[][] tokens = null;
      int nbs = checkStringLength(bs.length());
      if (defaultColix != C.INHERIT_ALL || defaultPaletteID != 0)
        checkColixLength(defaultColix, bs.length());
      if (defaultBgcolix != C.INHERIT_ALL)
        checkBgColixLength(defaultBgcolix, bs.length());
      if (value instanceof Lst) {
        Lst<SV> list = (Lst<SV>) value;
        int n = list.size();
        tokens = new LabelToken[][] { null };
        for (int pt = 0, i = bs.nextSetBit(0); i >= 0
            && i < nbs; i = bs.nextSetBit(i + 1)) {
          if (pt >= n) {
            setLabel(nullToken, "", i, !isPicked);
            continue;
          }
          tokens[0] = null;
          setLabel(tokens, SV.sValue(list.get(pt++)), i, !isPicked);
        }
      } else {
        String strLabel = (String) value;
        tokens = (strLabel == null || strLabel.length() == 0 ? nullToken
            : new LabelToken[][] { null });
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setLabel(tokens, strLabel, i, !isPicked);
      }
      return;
    }

    if (propertyName.startsWith("label:")) {
      // from @1.label = "xxx"
      setScaling();
      // in principle, we could make this more efficient,
      // it would be at the cost of general atom property setting
      checkStringLength(ac);
      String label = propertyName.substring(6);
      if (label.length() == 0)
        label = null;
      setLabel(new LabelToken[][] { null }, label, ((Integer) value).intValue(),
          false);
      return;
    }

    if ("clearBoxes" == propertyName) {
      labelBoxes = null;
      return;
    }

    if ("translucency" == propertyName || "bgtranslucency" == propertyName) {
      // no translucency
      return;
    }

    if ("bgcolor" == propertyName) {
      isActive = true;
      if (bsBgColixSet == null)
        bsBgColixSet = BS.newN(ac);
      short bgcolix = C.getColixO(value);
      if (setDefaults) { // || !defaultsOnlyForNone)
        defaultBgcolix = bgcolix;
      } else {
        int n = checkBgColixLength(bgcolix, bs.length());
        for (int i = bs.nextSetBit(0); i >= 0
            && i < n; i = bs.nextSetBit(i + 1))
          setBgcolix(i, bgcolix);
      }
      return;
    }

    // the rest require bsFontSet setting

    if (bsFontSet == null)
      bsFontSet = BS.newN(ac);

    if ("fontsize" == propertyName) {
      int fontsize = ((Number) value).intValue();
      if (fontsize < 0) {
        fids = null;
        return;
      }
      Font f;
      if (setDefaults) { // || !defaultsOnlyForNone)
        f = Font.getFont3D(defaultFontId);
        defaultFontId = vwr.getFont3D(f.fontFace, f.fontStyle, fontsize).fid;
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1)) {
          f = Font.getFont3D(
              fids == null || i >= fids.length ? fids[i] : defaultFontId);
          setFont(i, vwr.getFont3D(f.fontFace, f.fontStyle, fontsize).fid);
        }
      }
      return;
    }

    if ("font" == propertyName) {
      int fid = ((Font) value).fid;
      if (setDefaults) {// || !defaultsOnlyForNone)
        defaultFontId = fid;
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setFont(i, fid);
      }
      return;
    }

    if ("offset" == propertyName) {
      if (value instanceof Integer) {
        int offset = ((Integer) value).intValue();
        if (setDefaults) {// || !defaultsOnlyForNone)
          defaultOffset = offset;
        } else {
          for (int i = bs.nextSetBit(0); i >= 0
              && i < ac; i = bs.nextSetBit(i + 1))
            setOffsets(i, offset);
        }
      } else if (!setDefaults) {
        // pymol offset only
        checkColixLength((short) -1, ac);
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setPymolOffset(i, (double[]) value);
      }
      return;
    }

    if ("align" == propertyName) {
      // note that if the label is not offset, this centers the label with offset 0 0
      String type = (String) value;
      int hAlignment = (type.equalsIgnoreCase("right") ? JC.TEXT_ALIGN_RIGHT
          : type.equalsIgnoreCase("center") ? JC.TEXT_ALIGN_CENTER
              : JC.TEXT_ALIGN_LEFT);
      if (setDefaults) {// || !defaultsOnlyForNone)
        defaultAlignment = hAlignment;
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setHorizAlignment(i, hAlignment);
      }
      return;
    }

    if ("pointer" == propertyName) {
      int pointer = ((Integer) value).intValue();
      if (setDefaults) {// || !defaultsOnlyForNone)
        defaultPointer = pointer;
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setPointer(i, pointer);
      }
      return;
    }

    if ("front" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (setDefaults) {// || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_FRONT : 0);
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_FRONT, TF);
      }
      return;
    }

    if ("group" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (setDefaults) {// || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_GROUP : 0);
      } else {
        for (int i = bs.nextSetBit(0); i >= 0
            && i < ac; i = bs.nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_GROUP, TF);
      }
      return;
    }

    if ("display" == propertyName || "toggleLabel" == propertyName) {
      // toggle
      int mode = ("toggleLabel" == propertyName ? 0
          : ((Boolean) value).booleanValue() ? 1 : -1);
      if (mads == null)
        mads = new short[ac];
      String strLabelPDB = null;
      LabelToken[] tokensPDB = null;
      String strLabelUNK = null;
      LabelToken[] tokensUNK = null;
      String strLabel;
      LabelToken[] tokens;
      int nstr = checkStringLength(bs.length());
      short bgcolix = defaultBgcolix;
      int nbg = checkBgColixLength(bgcolix, bs.length());
      short thisMad = (short) (mode >= 0 ? 1 : -1);
      for (int i = bs.nextSetBit(0); i >= 0
          && i < ac; i = bs.nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (i < nstr && strings[i] != null) {
          // an old string -- toggle
          mads[i] = (short) (mode == 1 || mode == 0 && mads[i] < 0 ? 1 : -1);
        } else {
          // a new string -- turn on
          mads[i] = thisMad;
          if (atom.getGroup3(false).equals("UNK")) {
            if (strLabelUNK == null) {
              strLabelUNK = vwr.getStandardLabelFormat(1);
              tokensUNK = LabelToken.compile(vwr, strLabelUNK, '\0', null);
            }
            strLabel = strLabelUNK;
            tokens = tokensUNK;
          } else {
            if (strLabelPDB == null) {
              strLabelPDB = vwr.getStandardLabelFormat(2);
              tokensPDB = LabelToken.compile(vwr, strLabelPDB, '\0', null);
            }
            strLabel = strLabelPDB;
            tokens = tokensPDB;
          }
          strings[i] = LabelToken.formatLabelAtomArray(vwr, atom, tokens, '\0',
              null, ptTemp);
          formats[i] = strLabel;
          bsSizeSet.set(i);
          if (i < nbg && !bsBgColixSet.get(i))
            setBgcolix(i, defaultBgcolix);
        }
        atom.setShapeVisibility(vf, strings != null && i < strings.length
            && strings[i] != null && mads[i] >= 0);
      }
      return;
    }

    if ("pymolLabels" == propertyName) {
      setPymolLabels((Map<Integer, Text>) value, bs);
      return;
    }

    if (propertyName == JC.PROP_DELETE_MODEL_ATOMS) {
      labelBoxes = null;
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      fids = (int[]) AU.deleteElements(fids, firstAtomDeleted, nAtomsDeleted);
      bgcolixes = (short[]) AU.deleteElements(bgcolixes, firstAtomDeleted,
          nAtomsDeleted);
      offsets = (int[]) AU.deleteElements(offsets, firstAtomDeleted,
          nAtomsDeleted);
      formats = (String[]) AU.deleteElements(formats, firstAtomDeleted,
          nAtomsDeleted);
      strings = (String[]) AU.deleteElements(strings, firstAtomDeleted,
          nAtomsDeleted);
      BSUtil.deleteBits(bsFontSet, bs);
      BSUtil.deleteBits(bsBgColixSet, bs);
      // pass to super
    }

    setPropAS(propertyName, value, bs);

  }

  private boolean isPickingMode() {
    return (vwr.getPickingMode() == ActionManager.PICKING_LABEL
        && labelBoxes != null);
  }

  private int checkStringLength(int n) {
    int ac = ms.ac;
    n = Math.min(ac, n);
    if (strings == null || n > strings.length) {
      formats = AU.ensureLengthS(formats, n);
      strings = AU.ensureLengthS(strings, n);
      if (bsSizeSet == null)
        bsSizeSet = BS.newN(n);
    }
    return n;
  }

  private int checkBgColixLength(short colix, int n) {
    n = Math.min(ms.ac, n);
    if (colix == C.INHERIT_ALL)
      return (bgcolixes == null ? 0 : bgcolixes.length);
    if (bgcolixes == null || n > bgcolixes.length)
      bgcolixes = AU.ensureLengthShort(bgcolixes, n);
    return n;
  }

  private void setPymolLabels(Map<Integer, Text> labels, BS bsSelected) {
    // from PyMOL reader
    setScaling();
    int n = checkStringLength(ms.ac);
    checkColixLength((short) -1, n);
    for (int i = bsSelected.nextSetBit(0); i >= 0
        && i < n; i = bsSelected.nextSetBit(i + 1))
      setPymolLabel(i, labels.get(Integer.valueOf(i)), null);
  }

  /**
   * Sets offset using PyMOL standard array; only operates in cases where label
   * is already defined
   * 
   * @param i
   * @param value
   */
  private void setPymolOffset(int i, double[] value) {
    // from PyMOL reader or from set labeloffset [...]
    Text text = getLabel(i);
    if (text == null) {
      if (strings == null || i >= strings.length || strings[i] == null)
        return;
      int fid = (bsFontSet != null && bsFontSet.get(i) ? fids[i] : -1);
      if (fid < 0)
        setFont(i, fid = defaultFontId);
      Atom a = ms.at[i];
      text = Text.newLabel(vwr, Font.getFont3D(fid), strings[i],
          getColix2(i, a, false), getColix2(i, a, true), 0,
          scalePixelsPerMicron);
      setPymolLabel(i, text, formats[i]);
    }
    text.pymolOffset = value;
  }

  private final static LabelToken[][] nullToken = new LabelToken[][] { null };
  private boolean isScaled;
  private double scalePixelsPerMicron;
  private P3d ptTemp = new P3d();

  private void setScaling() {
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = BS.newN(ms.ac);
    isScaled = vwr.getBoolean(T.fontscaling);
    scalePixelsPerMicron = (isScaled
        ? vwr.getScalePixelsPerAngstrom(false) * 10000d
        : 0);
  }

  private void setPymolLabel(int i, Text t, String format) {
    if (t == null)
      return;
    String label = t.text;
    Atom atom = ms.at[i];
    if (atom == null)
      return;
    addString(atom, i, label,
        format == null ? PT.rep(label, "%", "%%") : format);
    atom.setShapeVisibility(vf, true);
    if (t.colix >= 0)
      setLabelColix(i, t.colix, PAL.UNKNOWN.id);
    setFont(i, t.font.fid);
    putLabel(i, t);
  }

  private void setLabel(LabelToken[][] temp, String strLabel, int i,
                        boolean doAll) {
    // checkStringLength must be first
    Atom atom = ms.at[i];
    LabelToken[] tokens = temp[0];
    if (tokens == null)
      tokens = temp[0] = LabelToken.compile(vwr, strLabel, '\0', null);
    String label = (tokens == null ? null
        : LabelToken.formatLabelAtomArray(vwr, atom, tokens, '\0', null,
            ptTemp));
    boolean isNew = addString(atom, i, label, strLabel);
    doAll |= isNew || label == null;
    Text text = getLabel(i);
    if (isScaled && doAll) {
      text = Text.newLabel(vwr, null, label, C.INHERIT_ALL, (short) 0, 0,
          scalePixelsPerMicron);
      putLabel(i, text);
    } else if (text != null) {
      if (label == null) {
        putLabel(i, null);        
      } else {
        text.setText(label);
        text.textUnformatted = strLabel;
      }
    }
    if (!doAll)
      return;
    if (defaultOffset != JC.LABEL_DEFAULT_OFFSET)
      setOffsets(i, defaultOffset);
    if (defaultAlignment != JC.TEXT_ALIGN_LEFT)
      setHorizAlignment(i, defaultAlignment);
    if ((defaultZPos & JC.LABEL_ZPOS_FRONT) != 0)
      setZPos(i, JC.LABEL_ZPOS_FRONT, true);
    else if ((defaultZPos & JC.LABEL_ZPOS_GROUP) != 0)
      setZPos(i, JC.LABEL_ZPOS_GROUP, true);
    if (defaultPointer != JC.LABEL_POINTER_NONE)
      setPointer(i, defaultPointer);
    if (defaultColix != C.INHERIT_ALL || defaultPaletteID != 0)
      setLabelColix(i, defaultColix, defaultPaletteID);
    if (defaultBgcolix != C.INHERIT_ALL)
      setBgcolix(i, defaultBgcolix);
    if (defaultFontId != zeroFontId)
      setFont(i, defaultFontId);
  }

  private boolean addString(Atom atom, int i, String label, String strLabel) {
    atom.setShapeVisibility(vf, label != null);
    boolean notNull = (strLabel != null);
    boolean isNew = (strings[i] == null);
    strings[i] = label;
    // formats are put into state, but only if we are not pulling from DATA
    formats[i] = (notNull && strLabel.indexOf("%{") >= 0 ? label : strLabel);
    bsSizeSet.setBitTo(i, notNull);
    return isNew;
  }

  @Override
  public Object getProperty(String property, int index) {
    if (property.equals("font"))
      return Font.getFont3D(defaultFontId);
    if (property.equals("offsets"))
      return offsets;
    if (property.equals("label"))
      return (strings != null && index < strings.length
          && strings[index] != null ? strings[index] : "");
    return null;
  }

  public void putLabel(int i, Text text) {
    if (text == null)
      atomLabels.remove(Integer.valueOf(i));
    else {
      atomLabels.put(Integer.valueOf(i), text);
      text.textUnformatted = formats[i];
    }
  }

  public Text getLabel(int i) {
    return atomLabels.get(Integer.valueOf(i));
  }

  public void putBox(int i, double[] boxXY) {
    if (labelBoxes == null)
      labelBoxes = new Hashtable<Integer, double[]>();
    labelBoxes.put(Integer.valueOf(i), boxXY);
  }

  public double[] getBox(int i) {
    if (labelBoxes == null)
      return null;
    return labelBoxes.get(Integer.valueOf(i));
  }

  private void setLabelColix(int i, short colix, byte pid) {
    setColixAndPalette(colix, pid, i);
    // text is only created by labelsRenderer
    Text text;
    if (colixes != null && ((text = getLabel(i)) != null))
      text.colix = colixes[i];
  }

  private void setBgcolix(int i, short bgcolix) {
    bgcolixes[i] = bgcolix;
    bsBgColixSet.setBitTo(i, bgcolix != 0);
    Text text = getLabel(i);
    if (text != null)
      text.bgcolix = bgcolix;
  }

  private void setOffsets(int i, int offset) {

    if (offsets == null || i >= offsets.length) {
      if (offset == JC.LABEL_DEFAULT_OFFSET)
        return;
      offsets = AU.ensureLengthI(offsets, ms.ac);
    }
    offsets[i] = (offsets[i] & JC.LABEL_FLAGS) | offset;

    Text text = getLabel(i);
    if (text != null)
      text.setOffset(offset);
  }

  private void setHorizAlignment(int i, int hAlign) {
    if (offsets == null || i >= offsets.length) {
      switch (hAlign) {
      case JC.TEXT_ALIGN_NONE:
      case JC.TEXT_ALIGN_LEFT:
        return;
      }
      offsets = AU.ensureLengthI(offsets, ms.ac);
    }
    if (hAlign == JC.TEXT_ALIGN_NONE)
      hAlign = JC.TEXT_ALIGN_LEFT;
    offsets[i] = JC.setHorizAlignment(offsets[i], hAlign);
    Text text = getLabel(i);
    if (text != null)
      text.setAlignment(hAlign);
  }

  private void setPointer(int i, int pointer) {
    if (offsets == null || i >= offsets.length) {
      if (pointer == JC.LABEL_POINTER_NONE)
        return;
      offsets = AU.ensureLengthI(offsets, ms.ac);
    }
    offsets[i] = JC.setPointer(offsets[i], pointer);
    Text text = getLabel(i);
    if (text != null)
      text.pointer = pointer;
  }

  private void setZPos(int i, int flag, boolean TF) {
    if (offsets == null || i >= offsets.length) {
      if (!TF)
        return;
      offsets = AU.ensureLengthI(offsets, ms.ac);
    }
    offsets[i] = JC.setZPosition(offsets[i], TF ? flag : 0);
  }

  private void setFont(int i, int fid) {
    if (fids == null || i >= fids.length) {
      if (fid == zeroFontId)
        return;
      fids = AU.ensureLengthI(fids, ms.ac);
    }
    fids[i] = fid;
    bsFontSet.set(i);
    Text text = getLabel(i);
    if (text != null) {
      text.setFontFromFid(fid);
    }
  }

  @Override
  public void setAtomClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0;) {
      String label = strings[i];
      if (label != null && ms.at.length > i && ms.at[i] != null
          && !ms.isAtomHidden(i))
        ms.at[i].setClickable(vf);
    }
  }

  //  @Override
  //  public String getShapeState() {
  //    // not implemented -- see org.jmol.viewer.StateCreator
  //    return null;
  //  }

  private int pickedAtom = -1;
  private int lastPicked = -1;
  private int pickedOffset = 0;
  private int pickedX;
  private int pickedY;

  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int modifiers,
                                                BS bsVisible,
                                                boolean drawPicking) {
    if (!isPickingMode())
      return null;
    int iAtom = findNearestLabel(x, y);
    if (iAtom < 0)
      return null;
    Map<String, Object> map = new Hashtable<String, Object>();
    map.put("type", "label");
    map.put("atomIndex", Integer.valueOf(iAtom));
    lastPicked = iAtom;
    return map;
  }

  @Override
  public synchronized boolean checkObjectDragged(int prevX, int prevY, int x,
                                                 int y, int dragAction,
                                                 BS bsVisible) {

    if (!isPickingMode())
      return false;
    // mouse down ?
    if (prevX == Integer.MIN_VALUE) {
      int iAtom = findNearestLabel(x, y);
      if (iAtom >= 0) {
        pickedAtom = iAtom;
        lastPicked = pickedAtom;
        vwr.acm.setDragAtomIndex(iAtom);
        pickedX = x;
        pickedY = y;
        pickedOffset = (offsets == null || pickedAtom >= offsets.length
            ? JC.LABEL_DEFAULT_OFFSET
            : offsets[pickedAtom]);
        return true;
      }
      return false;
    }
    // mouse up ?
    if (prevX == Integer.MAX_VALUE)
      pickedAtom = -1;
    if (pickedAtom < 0)
      return false;
    move2D(pickedAtom, x, y);
    return true;
  }

  private int findNearestLabel(int x, int y) {
    if (labelBoxes == null)
      return -1;
    double dmin = Double.MAX_VALUE;
    int imin = -1;
    double zmin = Double.MAX_VALUE;
    double afactor = (vwr.antialiased ? 2 : 1);
    Atom[] atoms = ms.at;
    for (Map.Entry<Integer, double[]> entry : labelBoxes.entrySet()) {
      if (!atoms[entry.getKey().intValue()]
          .isVisible(vf | Atom.ATOM_INFRAME_NOTHIDDEN))
        continue;
      double[] boxXY = entry.getValue();
      double dx = (x - boxXY[0]) * afactor;
      double dy = (y - boxXY[1]) * afactor;
      if (dx <= 0 || dy <= 0 || dx >= boxXY[2] || dy >= boxXY[3]
          || boxXY[4] > zmin)
        continue;
      zmin = boxXY[4];
      double d = Math.min(Math.abs(dx - boxXY[2] / 2),
          Math.abs(dy - boxXY[3] / 2));
      if (d <= dmin) {
        dmin = d;
        imin = entry.getKey().intValue();
      }
    }
    return imin;
  }

  private void move2D(int pickedAtom, int x, int y) {
    int xOffset = JC.getXOffset(pickedOffset);
    int yOffset = JC.getYOffset(pickedOffset);
    xOffset += x - pickedX;
    yOffset -= y - pickedY;
    int offset = JC.getOffset(xOffset, yOffset, true);
    setOffsets(pickedAtom, offset);
  }

  public short getColix2(int i, Atom atom, boolean isBg) {
    short colix;
    if (isBg) {
      colix = (bgcolixes == null || i >= bgcolixes.length) ? 0 : bgcolixes[i];
    } else {
      colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      colix = C.getColixInherited(colix, atom.colixAtom);
      if (C.isColixTranslucent(colix))
        colix = C.getColixTranslucent3(colix, false, 0);
    }
    return colix;
  }

}
