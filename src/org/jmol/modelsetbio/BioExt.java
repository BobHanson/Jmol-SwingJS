package org.jmol.modelsetbio;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.A4d;
import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;

public class BioExt {

  private Viewer vwr;
  private ModelSet ms;

  public BioExt() {
    // for reflection
  }

  BioExt set(Viewer vwr, ModelSet ms) {
    this.vwr = vwr;
    this.ms = ms;
    return this;
  }

  void getAllPolymerInfo(BS bs,
                         Map<String, Lst<Map<String, Object>>> fullInfo) {
    Lst<Map<String, Object>> modelVector = new Lst<Map<String, Object>>();
    int modelCount = ms.mc;
    Model[] models = ms.am;
    for (int i = 0; i < modelCount; ++i)
      if (models[i].isBioModel) {
        BioModel m = (BioModel) models[i];
        Map<String, Object> modelInfo = new Hashtable<String, Object>();
        Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
        for (int ip = 0; ip < m.bioPolymerCount; ip++) {
          BioPolymer bp = m.bioPolymers[ip];
          Map<String, Object> pInfo = new Hashtable<String, Object>();
          Lst<Map<String, Object>> mInfo = new Lst<Map<String, Object>>();
          Lst<Map<String, Object>> sInfo = null;
          ProteinStructure ps;
          ProteinStructure psLast = null;
          int n = 0;
          P3d ptTemp = new P3d();
          for (int im = 0; im < bp.monomerCount; im++) {
            if (bs.get(bp.monomers[im].leadAtomIndex)) {
              Map<String, Object> monomerInfo = bp.monomers[im]
                  .getMyInfo(ptTemp);
              monomerInfo.put("monomerIndex", Integer.valueOf(im));
              mInfo.addLast(monomerInfo);
              if ((ps = bp.getProteinStructure(im)) != null && ps != psLast) {
                Map<String, Object> psInfo = new Hashtable<String, Object>();
                psLast = ps;
                psInfo.put("type", ps.type.getBioStructureTypeName(false));
                int[] leadAtomIndices = bp.getLeadAtomIndices();
                int[] iArray = AU.arrayCopyRangeI(leadAtomIndices,
                    ps.monomerIndexFirst, ps.monomerIndexFirst + ps.nRes);
                psInfo.put("leadAtomIndices", iArray);
                ps.calcAxis();
                if (ps.axisA != null) {
                  psInfo.put("axisA", ps.axisA);
                  psInfo.put("axisB", ps.axisB);
                  psInfo.put("axisUnitVector", ps.axisUnitVector);
                }
                psInfo.put("index", Integer.valueOf(n++));
                if (sInfo == null)
                  sInfo = new Lst<Map<String, Object>>();
                sInfo.addLast(psInfo);
              }
            }
          }
          if (mInfo.size() > 0) {
            pInfo.put("sequence", bp.getSequence());
            pInfo.put("monomers", mInfo);
            if (sInfo != null)
              pInfo.put("structures", sInfo);
          }
          if (!pInfo.isEmpty())
            info.addLast(pInfo);
        }
        if (info.size() > 0) {
          modelInfo.put("modelIndex", Integer.valueOf(m.modelIndex));
          modelInfo.put("polymers", info);
          modelVector.addLast(modelInfo);
        }
      }
    fullInfo.put("models", modelVector);
  }

  void calculateStraightnessAll() {
    char qtype = vwr.getQuaternionFrame();
    int mStep = vwr.getInt(T.helixstep);
    // testflag3 ON  --> preliminary: Hanson's original normal-based straightness
    // testflag3 OFF --> final: Kohler's new quaternion-based straightness
    for (int i = ms.mc; --i >= 0;)
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel) ms.am[i];
        P3d ptTemp = new P3d();
        for (int p = 0; p < m.bioPolymerCount; p++)
          getPdbData(m.bioPolymers[p], 'S', qtype, mStep, 2, null, null, false,
              false, false, null, null, null, new BS(), ptTemp);
      }
    ms.haveStraightness = true;
  }

  final private static String[] qColor = { "yellow", "orange", "purple" };

  private void getPdbData(BioPolymer bp, char ctype, char qtype, int mStep,
                          int derivType, BS bsAtoms, BS bsSelected,
                          boolean bothEnds, boolean isDraw, boolean addHeader,
                          LabelToken[] tokens, OC pdbATOM, SB pdbCONECT,
                          BS bsWritten, P3d ptTemp) {
    boolean calcRamachandranStraightness = (qtype == 'C' || qtype == 'P');
    boolean isRamachandran = (ctype == 'R'
        || ctype == 'S' && calcRamachandranStraightness);
    if (isRamachandran && !bp.calcPhiPsiAngles())
      return;
    /*
     * A quaternion visualization involves assigning a frame to each amino acid
     * residue or nucleic acid base. This frame is an orthonormal x-y-z axis
     * system, which can be defined any number of ways.
     * 
     * 'c' C-alpha, as defined by Andy Hanson, U. of Indiana (unpublished
     * results)
     * 
     * X: CA->C (carbonyl carbon) Z: X x (CA->N) Y: Z x X
     * 
     * 'p' Peptide plane as defined by Bob Hanson, St. Olaf College (unpublished
     * results)
     * 
     * X: C->CA Z: X x (C->N') Y: Z x X
     * 
     * 'n' NMR frame using Beta = 17 degrees (Quine, Cross, et al.)
     * 
     * Y: (N->H) x (N->CA) X: R[Y,-17](N->H) Z: X x Y
     * 
     * quaternion types:
     * 
     * w, x, y, z : which of the q-terms to expunge in order to display the
     * other three.
     * 
     * 
     * a : absolute (standard) derivative r : relative (commuted) derivative s :
     * same as w but for calculating straightness
     */

    boolean isAmino = (bp.type == BioPolymer.TYPE_AMINO);
    boolean isRelativeAlias = (ctype == 'r');
    boolean quaternionStraightness = (!isRamachandran && ctype == 'S');
    if (derivType == 2 && isRelativeAlias)
      ctype = 'w';
    if (quaternionStraightness)
      derivType = 2;
    boolean useQuaternionStraightness = (ctype == 'S');
    boolean writeRamachandranStraightness = ("rcpCP".indexOf(qtype) >= 0);
    if (Logger.debugging
        && (quaternionStraightness || calcRamachandranStraightness)) {
      Logger.debug("For straightness calculation: useQuaternionStraightness = "
          + useQuaternionStraightness + " and quaternionFrame = " + qtype);
    }
    if (addHeader && !isDraw) {
      pdbATOM.append("REMARK   6    AT GRP CH RESNO  ");
      switch (ctype) {
      default:
      case 'w':
        pdbATOM.append("x*10___ y*10___ z*10___      w*10__       ");
        break;
      case 'x':
        pdbATOM.append("y*10___ z*10___ w*10___      x*10__       ");
        break;
      case 'y':
        pdbATOM.append("z*10___ w*10___ x*10___      y*10__       ");
        break;
      case 'z':
        pdbATOM.append("w*10___ x*10___ y*10___      z*10__       ");
        break;
      case 'R':
        if (writeRamachandranStraightness)
          pdbATOM.append("phi____ psi____ theta         Straightness");
        else
          pdbATOM.append("phi____ psi____ omega-180    PartialCharge");
        break;
      }
      pdbATOM.append("    Sym   q0_______ q1_______ q2_______ q3_______");
      pdbATOM.append("  theta_  aaX_______ aaY_______ aaZ_______");
      if (ctype != 'R')
        pdbATOM.append("  centerX___ centerY___ centerZ___");
      if (qtype == 'n')
        pdbATOM.append("  NHX_______ NHY_______ NHZ_______");
      pdbATOM.append("\n\n");
    }
    double factor = (ctype == 'R' ? 1d : 10d);
    bothEnds = false;//&= !isDraw && !isRamachandran;
    for (int j = 0; j < (bothEnds ? 2 : 1); j++, factor *= -1)
      for (int i = 0; i < (mStep < 1 ? 1 : mStep); i++)
        if (bp.hasStructure)
          getData(i, mStep, bp, ctype, qtype, derivType, bsAtoms, bsSelected,
              isDraw, isRamachandran, calcRamachandranStraightness,
              useQuaternionStraightness, writeRamachandranStraightness,
              quaternionStraightness, factor, isAmino, isRelativeAlias, tokens,
              pdbATOM, pdbCONECT, bsWritten, ptTemp);
  }

  /**
   * 
   * @param m0
   * @param mStep
   * @param p
   * @param ctype
   * @param qtype
   * @param derivType
   * @param bsAtoms
   * @param bsSelected
   * @param isDraw
   * @param isRamachandran
   * @param calcRamachandranStraightness
   * @param useQuaternionStraightness
   * @param writeRamachandranStraightness
   * @param quaternionStraightness
   *        NOT USED
   * @param factor
   * @param isAmino
   * @param isRelativeAlias
   * @param tokens
   * @param pdbATOM
   * @param pdbCONECT
   * @param bsWritten
   * @param ptTemp
   */
  @SuppressWarnings("static-access")
  private void getData(int m0, int mStep, BioPolymer p, char ctype, char qtype,
                       int derivType, BS bsAtoms, BS bsSelected, boolean isDraw,
                       boolean isRamachandran,
                       boolean calcRamachandranStraightness,
                       boolean useQuaternionStraightness,
                       boolean writeRamachandranStraightness,
                       boolean quaternionStraightness, double factor,
                       boolean isAmino, boolean isRelativeAlias,
                       LabelToken[] tokens, OC pdbATOM, SB pdbCONECT,
                       BS bsWritten, P3d ptTemp) {
    String prefix = (derivType > 0 ? "dq" + (derivType == 2 ? "2" : "") : "q");
    Qd q;
    Atom aprev = null;
    Qd qprev = null;
    Qd dq = null;
    Qd dqprev = null;
    Qd qref = null;
    Atom atomLast = null;
    double x = 0, y = 0, z = 0, w = 0;
    String strExtra = "";
    double val1 = Double.NaN;
    double val2 = Double.NaN;
    P3d pt = (isDraw ? new P3d() : null);

    int dm = (mStep <= 1 ? 1 : mStep);
    for (int m = m0; m < p.monomerCount; m += dm) {
      Monomer monomer = p.monomers[m];
      if (bsAtoms == null || bsAtoms.get(monomer.leadAtomIndex)) {
        Atom a = monomer.getLeadAtom();
        String id = monomer.getUniqueID();
        if (isRamachandran) {
          if (ctype == 'S')
            monomer.setGroupParameter(T.straightness, Double.NaN);
          x = monomer.getGroupParameter(T.phi);
          y = monomer.getGroupParameter(T.psi);
          z = monomer.getGroupParameter(T.omega);
          if (z < -90)
            z += 360;
          z -= 180; // center on 0
          if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            if (bsAtoms != null)
              bsAtoms.clear(a.i);
            continue;
          }
          double angledeg = (writeRamachandranStraightness
              ? p.calculateRamachandranHelixAngle(m, qtype)
              : 0);
          double straightness = (calcRamachandranStraightness
              || writeRamachandranStraightness
                  ? getStraightness(
                      Math.cos(angledeg / 2 / 180 * Math.PI))
                  : 0);
          if (ctype == 'S') {
            monomer.setGroupParameter(T.straightness, straightness);
            continue;
          }
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getIndex()))
              continue;
            // draw arrow arc {3.N} {3.ca} {3.C} {131 -131 0.5} "phi -131"
            // draw arrow arc {3.CA} {3.C} {3.N} {0 133 0.5} "psi 133"
            // as looked DOWN the bond, with {pt1} in the back, using
            // standard dihedral/Jmol definitions for anticlockwise positive
            // angles
            AminoMonomer aa = (AminoMonomer) monomer;
            pt.set(-x, x, 0.5d);
            pdbATOM.append("draw ID \"phi").append(id).append("\" ARROW ARC ")
                .append(Escape.eP(aa.getNitrogenAtom())).append(Escape.eP(a))
                .append(Escape.eP(aa.getCarbonylCarbonAtom()))
                .append(Escape.eP(pt)).append(" \"phi = ")
                .append(String.valueOf(Math.round(x))).append("\" color ")
                .append(qColor[2]).append("\n");
            pt.set(0, y, 0.5d);
            pdbATOM.append("draw ID \"psi").append(id).append("\" ARROW ARC ")
                .append(Escape.eP(a))
                .append(Escape.eP(aa.getCarbonylCarbonAtom()))
                .append(Escape.eP(aa.getNitrogenAtom())).append(Escape.eP(pt))
                .append(" \"psi = ").append(String.valueOf(Math.round(y)))
                .append("\" color ").append(qColor[1]).append("\n");
            pdbATOM.append("draw ID \"planeNCC").append(id).append("\" ")
                .append(Escape.eP(aa.getNitrogenAtom())).append(Escape.eP(a))
                .append(Escape.eP(aa.getCarbonylCarbonAtom())).append(" color ")
                .append(qColor[0]).append("\n");
            pdbATOM.append("draw ID \"planeCNC").append(id).append("\" ")
                .append(Escape.eP(
                    ((AminoMonomer) p.monomers[m - 1]).getCarbonylCarbonAtom()))
                .append(Escape.eP(aa.getNitrogenAtom())).append(Escape.eP(a))
                .append(" color ").append(qColor[1]).append("\n");
            pdbATOM.append("draw ID \"planeCCN").append(id).append("\" ")
                .append(Escape.eP(a))
                .append(Escape.eP(aa.getCarbonylCarbonAtom()))
                .append(Escape
                    .eP(((AminoMonomer) p.monomers[m + 1]).getNitrogenAtom()))
                .append(" color ").append(qColor[2]).append("\n");
            continue;
          }
          if (Double.isNaN(angledeg)) {
            strExtra = "";
            if (writeRamachandranStraightness)
              continue;
          } else {
            q = Qd.newVA(P3d.new3(1, 0, 0), angledeg);
            strExtra = getQInfo(q);
            if (writeRamachandranStraightness) {
              z = angledeg;
              w = straightness;
            } else {
              w = a.getPartialCharge();
            }

          }
        } else {
          // quaternion
          q = monomer.getQuaternion(qtype);
          if (q != null) {
            q.setRef(qref);
            qref = Qd.newQ(q);
          }
          if (derivType == 2)
            monomer.setGroupParameter(T.straightness, Double.NaN);
          if (q == null) {
            qprev = null;
            qref = null;
          } else if (derivType > 0) {
            Atom anext = a;
            Qd qnext = q;
            if (qprev == null) {
              q = null;
              dqprev = null;
            } else {

              // we now have two quaternionions, q and qprev
              // the revised procedure assigns dq for these to group a, not aprev
              // a/anext: q
              // aprev: qprev

              // back up to previous frame pointer - no longer
              //a = aprev;
              //q = qprev;
              //monomer = (Monomer) a.getGroup();
              // get dq or dq* for PREVIOUS atom
              if (isRelativeAlias) {
                // ctype = 'r';
                // dq*[i] = q[i-1] \ q[i]
                // R(v) = q[i-1] \ q(i) * (0, v) * q[i] \ q[i-1]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                //PRE 11.7.47:
                // dq*[i] = q[i] \ q[i+1]
                // R(v) = q[i] \ q(i+1) * (0, v) * q[i+1] \ q[i]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                dq = qprev.leftDifference(q);// qprev.inv().mul(q) = qprev \ q
              } else {
                // ctype = 'a' or 'w' or 's'

                // OLD:
                // the standard "absolute" difference dq
                // dq[i] = q[i+1] / q[i]
                // R(v) = q[i+1] / q[i] * (0, v) * q[i] / q[i+1]
                // used for definition of the local helical axis

                // NEW:
                // the standard "absolute" difference dq
                // dq[i] = q[i] / q[i-1]
                // R(v) = q[i] / q[i-1] * (0, v) * q[i-1] / q[i]
                // used for definition of the local helical axis

                dq = q.rightDifference(qprev);// q.mul(qprev.inv());
              }
              if (derivType == 1) {
                // first deriv:
                q = dq;
              } else if (dqprev == null) {
                q = null;
              } else {
                /*
                 * standard second deriv.
                 * 
                 * OLD:
                 * 
                 * ddq[i] =defined= (q[i+1] \/ q[i]) / (q[i] \/ q[i-1])
                 * 
                 * Relative to the previous atom as "i" (which is now "a"), we
                 * have:
                 * 
                 * dqprev = q[i] \/ q[i-1] dq = q[i+1] \/ q[i]
                 * 
                 * and so
                 * 
                 * ddq[i] = dq / dqprev
                 * 
                
                 * NEW: dq = q[i] \/ q[i-1]    dqprev = q[i-1] \/ q[i-2]
                 * 
                 * and so
                 * 
                 * ddq[iprev] = dq / dqprev
                 * 
                 * 
                 * 
                 * Looks odd, perhaps, because it is written "dq[i] / dq[i-1]"
                 * but this is correct; we are assigning ddq to the correct
                 * atom.
                 * 
                 * 5.8.2009 -- bh -- changing quaternion straightness to be
                 * assigned to PREVIOUS aa
                 * 
                 */
                q = dq.rightDifference(dqprev); // q = dq.mul(dqprev.inv());
                val1 = getQuaternionStraightness(id, dqprev, dq);
                val2 = get3DStraightness(id, dqprev, dq);
                ((Monomer) aprev.group).setGroupParameter(T.straightness,
                    useQuaternionStraightness ? val1 : val2);
              }
              dqprev = dq;
            }
            aprev = anext; //(a)
            qprev = qnext; //(q)
          }
          if (q == null) {
            atomLast = null;
            continue;
          }
          switch (ctype) {
          default:
            x = q.q1;
            y = q.q2;
            z = q.q3;
            w = q.q0;
            break;
          case 'x':
            x = q.q0;
            y = q.q1;
            z = q.q2;
            w = q.q3;
            break;
          case 'y':
            x = q.q3;
            y = q.q0;
            z = q.q1;
            w = q.q2;
            break;
          case 'z':
            x = q.q2;
            y = q.q3;
            z = q.q0;
            w = q.q1;
            break;
          }
          P3d ptCenter = monomer.getQuaternionFrameCenter(qtype);
          if (ptCenter == null)
            ptCenter = new P3d();
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getIndex()))
              continue;
            int deg = (int) Math.floor(Math.acos(w) * 360 / Math.PI);
            if (derivType == 0) {
              pdbATOM.append(Escape.drawQuat(q, prefix, id, ptCenter, 1d));
              if (qtype == 'n' && isAmino) {
                P3d ptH = ((AminoMonomer) monomer).getNitrogenHydrogenPoint();
                if (ptH != null)
                  pdbATOM.append("draw ID \"").append(prefix).append("nh")
                      .append(id).append("\" width 0.1 ").append(Escape.eP(ptH))
                      .append("\n");
              }
            }
            if (derivType == 1) {
              pdbATOM
                  .append((String) monomer.getHelixData(T.draw, qtype, mStep))
                  .append("\n");
              continue;
            }
            pt.set(x * 2, y * 2, z * 2);
            pdbATOM.append("draw ID \"").append(prefix).append("a").append(id)
                .append("\" VECTOR ").append(Escape.eP(ptCenter))
                .append(Escape.eP(pt)).append(" \">")
                .append(String.valueOf(deg)).append("\" color ")
                .append(qColor[derivType]).append("\n");
            continue;
          }
          strExtra = getQInfo(q) + PT.sprintf("  %10.5p %10.5p %10.5p", "p",
              new Object[] { ptCenter });
          if (qtype == 'n' && isAmino) {
            strExtra += PT.sprintf("  %10.5p %10.5p %10.5p", "p", new Object[] {
                ((AminoMonomer) monomer).getNitrogenHydrogenPoint() });
          } else if (derivType == 2 && !Double.isNaN(val1)) {
            strExtra += PT.sprintf(" %10.5f %10.5f", "F",
                new Object[] { new double[] { val1, val2 } });
          }
        }
        if (pdbATOM == null)// || bsSelected != null && !bsSelected.get(a.getIndex()))
          continue;
        bsWritten.set(((Monomer) a.group).leadAtomIndex);
        pdbATOM.append(ms.getLabeler().formatLabelAtomArray(vwr, a, tokens,
            '\0', null, ptTemp));
        pdbATOM.append(
            PT.sprintf("%8.2f%8.2f%8.2f      %6.3f          %2s    %s\n", "ssF",
                new Object[] { a.getElementSymbolIso(false).toUpperCase(),
                    strExtra, new double[] { x * factor, y * factor, z * factor,
                        w * factor } }));
        if (atomLast != null
            && atomLast.group.getBioPolymerIndexInModel() == a.group
                .getBioPolymerIndexInModel()) {
          pdbCONECT.append("CONECT")
              .append(PT.formatStringI("%5i", "i", atomLast.getAtomNumber()))
              .append(PT.formatStringI("%5i", "i", a.getAtomNumber()))
              .appendC('\n');
        }
        atomLast = a;
      }
    }
  }

  private static String getQInfo(Qd q) {
    A4d axis = q.toA4d();
    return PT.sprintf("%10.6f%10.6f%10.6f%10.6f  %6.2f  %10.5f %10.5f %10.5f",
        "F", new Object[] { new double[] { q.q0, q.q1, q.q2, q.q3,
            (axis.angle * 180 / Math.PI), axis.x, axis.y, axis.z } });
  }

  static String drawQuat(Qd q, String prefix, String id, P3d ptCenter,
                         double scale) {
    String strV = " VECTOR " + Escape.eP(ptCenter) + " ";
    if (scale == 0)
      scale = 1d;
    return "draw " + prefix + "x" + id + strV
        + Escape.eP(q.getVectorScaled(0, scale)) + " color red\n" + "draw "
        + prefix + "y" + id + strV + Escape.eP(q.getVectorScaled(1, scale))
        + " color green\n" + "draw " + prefix + "z" + id + strV
        + Escape.eP(q.getVectorScaled(2, scale)) + " color blue\n";
  }

  // starting with Jmol 11.7.47, dq is defined so as to LEAD TO
  // the target atom, not LEAD FROM it. 
  /**
   * @param id
   *        for debugging only
   * @param dq
   * @param dqnext
   * @return calculated straightness
   * 
   */
  private static double get3DStraightness(String id, Qd dq, Qd dqnext) {
    // 
    // Normal-only simple dot-product straightness = dq1.normal.DOT.dq2.normal
    //
    return dq.getNormal().dot(dqnext.getNormal());
  }

  /**
   * 
   * @param id
   *        for debugging only
   * @param dq
   * @param dqnext
   * @return straightness
   */
  private static double getQuaternionStraightness(String id, Qd dq,
                                                 Qd dqnext) {
    // 
    // Dan Kohler's quaternion straightness = 1 - acos(|dq1.dq2|)/(PI/2)
    //
    // alignment = near 0 or near 180 --> same - just different rotations.
    // It's a 90-degree change in direction that corresponds to 0.
    //
    return getStraightness(dq.dot(dqnext));
  }

  private static double getStraightness(double cosHalfTheta) {
    return (1 - 2 * Math.acos(Math.abs(cosHalfTheta)) / Math.PI);
  }

  void getPdbDataM(BioModel m, Viewer vwr, String type, char ctype,
                   boolean isDraw, BS bsSelected, OC out, LabelToken[] tokens,
                   SB pdbCONECT, BS bsWritten) {
    boolean bothEnds = false;
    char qtype = (ctype != 'R' ? 'r'
        : type.length() > 13 && type.indexOf("ramachandran ") >= 0
            ? type.charAt(13)
            : 'R');
    if (qtype == 'r')
      qtype = vwr.getQuaternionFrame();
    int mStep = vwr.getInt(T.helixstep);
    int derivType = (type.indexOf("diff") < 0 ? 0
        : type.indexOf("2") < 0 ? 1 : 2);
    if (!isDraw) {
      out.append("REMARK   6 Jmol PDB-encoded data: " + type + ";");
      if (ctype != 'R') {
        out.append("  quaternionFrame = \"" + qtype + "\"");
        bothEnds = true; //???
      }
      out.append("\nREMARK   6 Jmol Version ").append(Viewer.getJmolVersion())
          .append("\n");
      if (ctype == 'R')
        out.append(
            "REMARK   6 Jmol data min = {-180 -180 -180} max = {180 180 180} "
                + "unScaledXyz = xyz * {1 1 1} + {0 0 0} plotScale = {100 100 100}\n");
      else
        out.append("REMARK   6 Jmol data min = {-1 -1 -1} max = {1 1 1} "
            + "unScaledXyz = xyz * {0.1 0.1 0.1} + {0 0 0} plotScale = {100 100 100}\n");
    }

    P3d ptTemp = new P3d();
    for (int p = 0; p < m.bioPolymerCount; p++)
      getPdbData(m.bioPolymers[p], ctype, qtype, mStep, derivType, m.bsAtoms,
          bsSelected, bothEnds, isDraw, p == 0, tokens, out, pdbCONECT,
          bsWritten, ptTemp);
  }

  ///////////////////////////////////////////////////////////
  //
  // Struts calculation (for rapid prototyping)
  //
  ///////////////////////////////////////////////////////////

  int calculateAllstruts(Viewer vwr, ModelSet ms, BS bs1, BS bs2) {
    vwr.setModelVisibility();
    // select only ONE model
    ms.makeConnections2(0, Double.MAX_VALUE, Edge.BOND_STRUT, T.delete, bs1, bs2,
        null, false, false, 0, null);
    int iAtom = bs1.nextSetBit(0);
    if (iAtom < 0)
      return 0;
    Model m = ms.am[ms.at[iAtom].mi];
    if (!m.isBioModel)
      return 0;
    // only check the atoms in THIS model
    Lst<Atom> vCA = new Lst<Atom>();
    BS bsCheck;
    if (bs1.equals(bs2)) {
      bsCheck = bs1;
    } else {
      bsCheck = BSUtil.copy(bs1);
      bsCheck.or(bs2);
    }
    Atom[] atoms = ms.at;
    bsCheck.and(vwr.getModelUndeletedAtomsBitSet(m.modelIndex));
    for (int i = bsCheck.nextSetBit(0); i >= 0; i = bsCheck.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      if (a.checkVisible() && a.atomID == JC.ATOMID_ALPHA_CARBON
          && a.group.groupID != JC.GROUPID_CYSTEINE
          && atoms[i].group.leadAtomIndex >= 0)
        vCA.addLast(atoms[i]);
    }
    if (vCA.size() == 0)
      return 0;
    Lst<Atom[]> struts = calculateStruts(ms, bs1, bs2, vCA,
        vwr.getDouble(T.strutlengthmaximum), vwr.getInt(T.strutspacing),
        vwr.getBoolean(T.strutsmultiple));
    short mad = (short) (vwr.getDouble(T.strutdefaultradius) * 2000);
    for (int i = 0; i < struts.size(); i++) {
      Atom[] o = struts.get(i);
      ms.bondAtoms(o[0], o[1], Edge.BOND_STRUT, mad, null, 0, false, true);
    }
    return struts.size();
  }

  /**
   * 
   * Algorithm of George Phillips phillips@biochem.wisc.edu
   * 
   * originally a contribution to pyMol as struts.py; adapted here by Bob Hanson
   * for Jmol 1/2010
   * 
   * Return a vector of support posts for rapid prototyping models along the
   * lines of George Phillips for Pymol except on actual molecular segments
   * (biopolymers), not PDB chains (which may or may not be continuous).
   * 
   * Like George, we go from thresh-4 to thresh in units of 1 Angstrom, but we
   * do not require this threshold to be an integer. In addition, we prevent
   * double-creation of struts by tracking where struts are, and we do not look
   * for any addtional end struts if there is a strut already to an atom at a
   * particular biopolymer end. The three parameters are:
   * 
   * set strutDefaultRadius 0.3 set strutSpacingMinimum 6 set strutLengthMaximum
   * 7.0
   * 
   * Struts will be introduced by:
   * 
   * calculate struts {atom set A} {atom set B}
   * 
   * where the two atom sets are optional and default to the currently selected
   * set.
   * 
   * They can be manipulated using the STRUTS command much like any "bond"
   * 
   * struts 0.3 color struts opaque pink connect {atomno=3} {atomno=4} strut
   * 
   * struts only
   * 
   * command
   * 
   * @param modelSet
   * @param bs1
   * @param bs2
   * @param vCA
   * @param thresh
   * @param delta
   * @param allowMultiple
   * @return vector of pairs of atoms
   * 
   */
  private static Lst<Atom[]> calculateStruts(ModelSet modelSet, BS bs1, BS bs2,
                                             Lst<Atom> vCA, double thresh,
                                             int delta, boolean allowMultiple) {
    Lst<Atom[]> vStruts = new Lst<Atom[]>(); // the output vector
    double thresh2 = thresh * thresh; // use distance squared for speed

    int n = vCA.size(); // the set of alpha carbons
    int nEndMin = 3;

    // We set bitsets that indicate that there is no longer any need to
    // check for a strut. We are tracking both individual atoms (bsStruts) and
    // pairs of atoms (bsNotAvailable and bsNearbyResidues)

    BS bsStruts = new BS(); // [i]
    BS bsNotAvailable = new BS(); // [ipt]
    BS bsNearbyResidues = new BS(); // [ipt]

    // check for a strut. We are going to set struts within 3 residues
    // of the ends of biopolymers, so we track those positions as well.

    Atom a1 = vCA.get(0);
    Atom a2;
    int nBiopolymers = modelSet.getBioPolymerCountInModel(a1.mi);
    int[][] biopolymerStartsEnds = new int[nBiopolymers][nEndMin * 2];
    for (int i = 0; i < n; i++) {
      a1 = vCA.get(i);
      int polymerIndex = a1.group.getBioPolymerIndexInModel();
      int monomerIndex = a1.group.getMonomerIndex();
      int bpt = monomerIndex;
      if (bpt < nEndMin)
        biopolymerStartsEnds[polymerIndex][bpt] = i + 1;
      bpt = ((Monomer) a1.group).getBioPolymerLength() - monomerIndex - 1;
      if (bpt < nEndMin)
        biopolymerStartsEnds[polymerIndex][nEndMin + bpt] = i + 1;
    }

    // Get all distances.
    // For n CA positions, there will be n(n-1)/2 distances needed.
    // There is no need for a full matrix X[i][j]. Instead, we just count
    // carefully using the variable ipt:
    //
    // ipt = i * (2 * n - i - 1) / 2 + j - i - 1

    double[] d2 = new double[n * (n - 1) / 2];
    for (int i = 0; i < n; i++) {
      a1 = vCA.get(i);
      for (int j = i + 1; j < n; j++) {
        int ipt = strutPoint(i, j, n);
        a2 = vCA.get(j);
        int resno1 = a1.getResno();
        int polymerIndex1 = a1.group.getBioPolymerIndexInModel();
        int resno2 = a2.getResno();
        int polymerIndex2 = a2.group.getBioPolymerIndexInModel();
        if (polymerIndex1 == polymerIndex2 && Math.abs(resno2 - resno1) < delta)
          bsNearbyResidues.set(ipt);
        double d = d2[ipt] = a1.distanceSquared(a2);
        if (d >= thresh2)
          bsNotAvailable.set(ipt);
      }
    }

    // Now go through 5 spheres leading up to the threshold
    // in 1-Angstrom increments, picking up the shortest distances first

    for (int t = 5; --t >= 0;) { // loop starts with 4
      thresh2 = (thresh - t) * (thresh - t);
      for (int i = 0; i < n; i++)
        if (allowMultiple || !bsStruts.get(i))
          for (int j = i + 1; j < n; j++) {
            int ipt = strutPoint(i, j, n);
            if (!bsNotAvailable.get(ipt) && !bsNearbyResidues.get(ipt)
                && (allowMultiple || !bsStruts.get(j)) && d2[ipt] <= thresh2)
              setStrut(i, j, n, vCA, bs1, bs2, vStruts, bsStruts,
                  bsNotAvailable, bsNearbyResidues, delta);
          }
    }

    // Now find a strut within nEndMin (3) residues of the end in each
    // biopolymer, but only if it is within one of the "not allowed"
    // regions - this is to prevent dangling ends to be connected by a
    // very long connection

    for (int b = 0; b < nBiopolymers; b++) {
      // if there are struts already in this area, skip this part
      for (int k = 0; k < nEndMin * 2; k++) {
        int i = biopolymerStartsEnds[b][k] - 1;
        if (i >= 0 && bsStruts.get(i)) {
          for (int j = 0; j < nEndMin; j++) {
            int pt = (k / nEndMin) * nEndMin + j;
            if ((i = biopolymerStartsEnds[b][pt] - 1) >= 0)
              bsStruts.set(i);
            biopolymerStartsEnds[b][pt] = -1;
          }
        }
      }
      if (biopolymerStartsEnds[b][0] == -1
          && biopolymerStartsEnds[b][nEndMin] == -1)
        continue;
      boolean okN = false;
      boolean okC = false;
      int iN = 0;
      int jN = 0;
      int iC = 0;
      int jC = 0;
      double minN = Double.MAX_VALUE;
      double minC = Double.MAX_VALUE;
      for (int j = 0; j < n; j++)
        for (int k = 0; k < nEndMin * 2; k++) {
          int i = biopolymerStartsEnds[b][k] - 1;
          if (i == -2) {
            // skip all
            k = (k / nEndMin + 1) * nEndMin - 1;
            continue;
          }
          if (j == i || i == -1)
            continue;
          int ipt = strutPoint(i, j, n);
          if (bsNearbyResidues.get(ipt)
              || d2[ipt] > (k < nEndMin ? minN : minC))
            continue;
          if (k < nEndMin) {
            if (bsNotAvailable.get(ipt))
              okN = true;
            jN = j;
            iN = i;
            minN = d2[ipt];
          } else {
            if (bsNotAvailable.get(ipt))
              okC = true;
            jC = j;
            iC = i;
            minC = d2[ipt];
          }
        }
      if (okN)
        setStrut(iN, jN, n, vCA, bs1, bs2, vStruts, bsStruts, bsNotAvailable,
            bsNearbyResidues, delta);
      if (okC)
        setStrut(iC, jC, n, vCA, bs1, bs2, vStruts, bsStruts, bsNotAvailable,
            bsNearbyResidues, delta);
    }
    return vStruts;
  }

  private static int strutPoint(int i, int j, int n) {
    return (j < i ? j * (2 * n - j - 1) / 2 + i - j - 1
        : i * (2 * n - i - 1) / 2 + j - i - 1);
  }

  private static void setStrut(int i, int j, int n, Lst<Atom> vCA, BS bs1,
                               BS bs2, Lst<Atom[]> vStruts, BS bsStruts,
                               BS bsNotAvailable, BS bsNearbyResidues,
                               int delta) {
    Atom a1 = vCA.get(i);
    Atom a2 = vCA.get(j);
    if (!bs1.get(a1.i) || !bs2.get(a2.i))
      return;
    vStruts.addLast(new Atom[] { a1, a2 });
    bsStruts.set(i);
    bsStruts.set(j);
    for (int k1 = Math.max(0, i - delta); k1 <= i + delta && k1 < n; k1++) {
      for (int k2 = Math.max(0, j - delta); k2 <= j + delta && k2 < n; k2++) {
        if (k1 == k2) {
          continue;
        }
        int ipt = strutPoint(k1, k2, n);
        if (!bsNearbyResidues.get(ipt)) {
          bsNotAvailable.set(ipt);
        }
      }
    }
  }

  /**
   * mutate the given group
   * 
   * @param vwr
   * @param bs
   * @param group
   * @param sequence
   * @param phipsi
   * @param helixType
   * @return true if even partially successful
   */
  boolean mutate(Viewer vwr, BS bs, String group, String[] sequence,
                 String helixType, double[] phipsi) {
    boolean addH = vwr.getBoolean(T.pdbaddhydrogens);
    if (sequence == null)
      return mutateAtom(vwr, bs.nextSetBit(0), group, addH);
    boolean haveHelixType = (helixType != null);
    boolean isCreate = (haveHelixType || phipsi != null);
    if (isCreate) {
      boolean isTurn = false;
      if (haveHelixType) {
        helixType = helixType.toLowerCase();
        isTurn = (helixType.startsWith("turn"));
        phipsi = getPhiPsiForHelixType(helixType);
        if (phipsi == null)
          return false;
      }
      BS bs0 = vwr.getAllAtoms();
      // The number of residues is determined by whether we have given a helix type or not.
      // When we have a turn, we add two, one for each end, but not more than the sequence length.
      // Otherwise, we take the max of the half the length of phipsi and the sequence length
      // 
      int nseq = sequence.length;
      int nres = (haveHelixType && phipsi.length == 2 ? nseq
          : Math.max((haveHelixType ? 2 : 0) + phipsi.length / 2,
              nseq));
      // we can handle GLY and ALA without mutation
      int[] gly = new int[nres];
      for (int i = 0;i < nres; i++) {
        gly[i] = (sequence[i % nseq].equals("GLY") ? 1 : 0);
      }
      createHelix(vwr, nres, gly, phipsi, isTurn);
      bs = BSUtil.andNot(vwr.getAllAtoms(), bs0);
    }
    boolean isFile = (group == null);
    if (isFile)
      group = sequence[0];
    Group[] groups = vwr.ms.getGroups();
    boolean isOK = false;
    for (int i = 0, pt = 0; i < groups.length; i++) {
      Group g = (groups[i].isProtein() ? groups[i] : null);
      if (g == null || !g.isSelected(bs))
        continue;
      if (!isFile) {
        group = sequence[pt++ % sequence.length];
        if (group.equals("UNK") || isCreate && (group.equals("ALA") || group.equals("GLY"))) {
          isOK = true;
          continue;
        }
        group = "==" + group;
      }
      if (mutateAtom(vwr, g.firstAtomIndex, group, addH)) {
        isOK = true;
      }
    }
    return isOK;
  }

  static String helixScript = 
      "var nRes = $NRES; var a = $PHIPSI; var isTurn = $ISTURN;var gly=$GLY\n" + 
      "var phi = a[1];\n" + 
      "var psi = a[2];\n" + 
      "if (nRes == 0) nRes = 10;\n" + 
      "var isRandomPhi = (phi == 999)\n" + 
      "var isRandomPsi = (psi == 999)\n" + 
      "var a0 = {*};\n" + 
      "var pdbadd = pdbAddHydrogens;pdbAddHydrogens = false\n" +
      "data \"append mydata\"\n" +
      "ATOM      1  N   ALA     0      -0.499   1.323   0.000  1.00 13.99           N  \n" + 
      "ATOM      2  CA  ALA     0       0.000   0.000   0.000  1.00 20.10           C  \n" + 
      "ATOM      3  C   ALA     0       1.461   0.000   0.000  1.00 17.07           C  \n" + 
      "ATOM      4  O   ALA     0       2.110  -1.123   0.000  1.00 17.78           O  \n" + 
      "ATOM      5  CB  ALA     0      -0.582  -0.697  -1.291  1.00 13.05           C  \n" + 
      "ATOM      6  OXT ALA     0       2.046   1.197   0.000  1.00 13.05           O  \n" + 
      "end \"append mydata\"\n" + 
      "set appendnew false\n" + 
      "data \"mydata\"\n" + 
      "ATOM   0001  N   ALA     0      -0.499   1.323   0.000  1.00 13.99           N  \n" + 
      "ATOM   0002  CA  ALA     0       0.000   0.000   0.000  1.00 20.10           C  \n" + 
      "ATOM   0003  C   ALA     0       1.461   0.000   0.000  1.00 17.07           C  \n" + 
      "ATOM   0004  O   ALA     0       2.110  -1.123   0.000  1.00 17.78           O  \n" + 
      "ATOM   0005  OXT ALA     0       2.046   1.197   0.000  1.00 13.05           O  \n" + 
      "ATOM   0006  CB  ALA     0      -0.582  -0.697  -1.291  1.00 13.05           C  \n" + 
      "end \"mydata\"\n" + 
      // loop through and build structure having all phi = 180 and all psi = 0 
      "var C2 = null;\n" +
      "var apt = -2\n" +
      "var nangle = a.length\n" + 
      "for (var i = 1; i <= nRes; i++) {\n" + 
      "  if (isTurn && (i == 2 || i == nRes))apt -= 2\n" +
      "  apt = (apt + 2)%nangle\n" +
      "  phi = a[apt + 1]\n" +
      "  psi = a[apt + 2]\n" +
      "  if (isRandomPhi){\n" + 
      "     phi = random(360) - 180\n" + 
      "  }\n" + 
      "  if (isRandomPsi){\n" + 
      "     psi = random(360) - 180\n" + 
      "  }\n" + 
      // rotate what we have now 
      "  select !a0;\n" + 
      "  rotateselected molecular {0 0 0} {0 0 1} -69\n" + 
      // prepare for connection C to N' 
      "  var C = (C2 ? C2 : {!a0 & *.C}[0]);\n" + 
      "  translateselected @{-C.xyz}\n" + 
      "  rotateselected molecular {0 0 0} {0 0 1} 9\n" + 
      "  translateselected {-1.461 0 0}\n" + 
      "  rotateselected molecular {0 0 0} {0 0 1} -9\n" + 
      "  var a1 = {*};\n" + 
      // add the new coordinates 
      "  var sdata = data(\"mydata\")\n" + 
      "  sdata = sdata.replace('  0   ',('  '+i)[-2][0] + '   ')\n" + 
      "  if (gly[((i-1)%nRes) + 1]) { sdata = sdata.replace('ALA','GLY').split('ATOM   0006')[1]}\n" +
      "  var n = a1.size\n" + 
      "  sdata = sdata.replace('0001',('   '+(n+1))[-3][0])\n" + 
      "  sdata = sdata.replace('0002',('   '+(n+2))[-3][0])\n" + 
      "  sdata = sdata.replace('0003',('   '+(n+3))[-3][0])\n" + 
      "  sdata = sdata.replace('0004',('   '+(n+4))[-3][0])\n" + 
      "  sdata = sdata.replace('0005',('   '+(n+5))[-3][0])\n" + 
      "  sdata = sdata.replace('0006',('   '+(n+6))[-3][0])\n" + 
      "  data \"append @sdata\"\n" + 
      // translate to fit 
      "  var N = {!a1 && *.N}[0]\n" + 
      "  connect @N @C\n" + 
      "  select !a1\n" + 
      "  translateselected @{-N.xyz}\n" + 
      // set psi  C-CA dihedral 
      "  C2  = {!a1 && *.C}[0]\n" + 
      "  var CA = {!a1 && *.CA}[0]\n" + 
      "  select @{within('branch',C2,CA)}\n" + 
      "  rotateSelected @C2 @CA @psi\n" + 
      // set phi CA-N dihedral
      "  select @{within('branch',CA,N)}\n" + 
      "  rotateSelected @CA @N @{180 + phi}\n" +
      // iterate
      "}\n" + 
      // remove the starting template
      "  var xt = {*.OXT}[1][-1];\n" + 
      "  delete 0 or *.HXT or xt;\n" + 
      "  pdbAddHydrogens = pdbadd;\n" +
      // save the structure and reload it to create the polymers
      
      "  select !a0;var a=-{selected}.xyz;\n" + 
      "  translateSelected @a\n" +
      "  var pdbdata = data(!a0, 'PDB');\n" + 
      "  if (a0) {\n" + 
      "    delete !a0;\n" + 
      "    data \"append model @pdbdata\";\n" + 
      "    set appendnew true\n" + 
      "  } else {\n" + 
      "    data \"model @pdbdata\";\n" + 
      "  }\n" + 
      // orient along axis
      " var vr = {resno=2}[0]\n"+
      "  var v = helix(within(group,vr),'axis')\n" + 
      "  var vrot = cross(v, {0 0 1})\n" + 
      "  rotateselected molecular {0 0 0} @vrot @{angle(v, {0 0 0}, {0 0 1})}\n" + 
      "  set appendnew true\n" +
      // reset globals 
      "  pdbSequential = ispdbseq;\n";
  
  /**
   * @param vwr 
   * @param nRes 
   * @param gly  
   * @param phipsi 
   * @param isTurn 
   */
  private static void createHelix(Viewer vwr, int nRes, int[] gly, double[] phipsi, boolean isTurn) {
    String script = PT.rep(PT.rep(PT.rep(PT.rep(helixScript, 
        "$NRES", "" + nRes), 
        "$PHIPSI", PT.toJSON(null, phipsi)), 
        "$GLY", PT.toJSON(null, gly)), 
        "$ISTURN", "" + isTurn);
    try {
      if (Logger.debugging)
        Logger.debug(script);
      vwr.eval.runScript(script);
      vwr.calculateStructures(vwr.getFrameAtoms(), true, true, -1);
    } catch (Exception e) {
      // serious Jmol bug here!
      e.printStackTrace();
      System.out.println(e);
    }
  }

  // unfortunately, I did not record the sources for these.
  
  final static Object[] alphaTypes = { 
      "alpha", new double[] { -65, -40 },
      "3-10", new double[] { -74, -4 }, 
      "pi", new double[] { -57.1F, -69.7F },
      "alpha-l", new double[] { 57.1F, 4 }, 
      "helix-ii", new double[] { -79, 150 },
      "collagen", new double[] { -51, 153 }, 
      "beta", new double[] { -140, 130 }, // http://www.cryst.bbk.ac.uk/PPS95/course/9_quaternary/3_geometry/torsion.html
      "beta-120", new double[] { -120, 120 }, 
      "beta-135", new double[] { -135, 135 }, 
      "extended", new double[] { 180, 180 }, 
      "turn-i", new double[] { -60, -30, -90, 0 }, 
      "turn-ii", new double[] { -60, 120, 80, 0 }, 
      "turn-iii", new double[] { -60, -30, -60, -30 }, 
      "turn-i'", new double[] { 60, 30, 90, 0 }, 
      "turn-ii'", new double[] { 60, -120, -80, 0 }, 
      "turn-iii'", new double[] { 60, 30, 60, 30 }
  };

  public double[] getPhiPsiForHelixType(String t) {
    t = t.toLowerCase().trim().replace(' ', '-');
    for (int i = alphaTypes.length - 2; i >= 0; i -= 2) {
      if (t.equals(alphaTypes[i]))
        return (double[]) alphaTypes[i + 1];
    }
    return null;
  }

  private static String mutateScript = "" + "try{\n" + "  var a0 = {*}\n"
      + "  var res0 = $RES0\n" + "  load mutate $FNAME\n"
      + "  var res1 = {!a0};var r1 = res1[1];var r0 = res1[0]\n"
      + "  if ({r1 & within(group, r0)}){\n"
      + "    var haveHs = ({_H & connected(res0)} != 0)\n"
      + "    var rh = {_H} & res1;\n"
      + "    if (!haveHs) {delete rh; res1 = res1 & !rh}\n"
      + "    var sm = '[*.N][*.CA][*.C][*.O]'\n"
      + "    var keyatoms = res1.find(sm)\n"
      + "    var x = compare(res1,res0,sm,'BONDS')\n" 
      + "    if(x){\n"
      + "      var allN = {*.N};var allCA = {*.cA};var allC = {*.C}; var allO = {*.O}; var allH = {*.H}\n"
      + "      print 'mutating ' + res0[1].label('%n%r') + ' to ' + $FNAME.trim('=')\n"
      + "      rotate branch @x\n;"
      + "      compare @res1 @res0 SMARTS '[*.N][*.CA][*.C]' rotate translate 0\n"
      + "      var c = {!res0 & connected(res0)}\n"
      + "      var N2 = {allN & c}\n" 
      + "      var C0 = {allC & c}\n"
      + "      {allN & res1}.xyz = {allN & res0}.xyz\n"
      + "      {allCA & res1}.xyz = {allCA & res0}.xyz\n"
      + "      {allC & res1}.xyz = {allC & res0}.xyz\n"
      + "      if (N2) {allO & res1}.xyz = {allO & res0}.xyz\n"
      + "      var NH = {_H & res0 && connected(allN)}\n"
      + "      var angleH = ({*.H and res0} ? angle({allC and res0},{allCA and res0},{allN and res0},NH) : 1000)\n"      
      + "      delete res0\n" 
      + "      if (N2) {\n" // not last
      + "        delete (*.OXT,*.HXT) and res1\n"
      + "        connect {N2} {keyatoms & *.C}\n" 
      + "      } else {\n"
      + "        delete (*.HXT) and res1\n"
      + "        {*.OXT & res1}.formalCharge = -1\n"
      + "      }\n"
      + "      var N1 = {allN & res1}\n"
      + "      var H1 = {allH and res1}\n"
      + "      var NH = {H1 & connected(N1)}\n"
      + "      if (C0) {\n" // not first
      + "        switch (0 + NH) {\n" 
      + "        case 0:\n" // no hydrogens
      + "          break\n" 
      + "        case 1:\n" // proline or proline-like
      + "          delete H1\n" 
      + "          break\n" 
      + "        default:\n"
      + "          var CA1 = {allCA & res1}\n"
      + "          x = angle({allC and res1},CA1,N1,NH)\n"
      + "          rotate branch @CA1 @N1 @{angleH-x}\n"
      + "          delete *.H2 and res1\n" 
      + "          delete *.H3 and res1\n"
      + "          break\n" + "        }\n"
      + "        connect {C0} {keyatoms & allN}\n" 
      + "      }\n"
      + "    }\n"
      + "  }\n" + "}catch(e){print e}\n";

  private static boolean mutateAtom(Viewer vwr, int iatom, String fileName, boolean addH) {
    // no mutating a trajectory. What would that mean???
    ModelSet ms = vwr.ms;
    int iModel = ms.at[iatom].mi;
    Model m = ms.am[iModel];
    if (ms.isTrajectory(iModel))
      return false;

    String[] info = vwr.fm.getFileInfo();
    Group g = ms.at[iatom].group;
    //try {
    // get the initial group -- protein for now
    if (!(g instanceof AminoMonomer))
      return false;
    ((BioModel) m).isMutated = true;
    AminoMonomer res0 = (AminoMonomer) g;
    int ac = ms.ac;
    BS bsRes0 = new BS();
    res0.setAtomBits(bsRes0);
    bsRes0.and(vwr.getModelUndeletedAtomsBitSet(iModel));
    Atom[] backbone = getMutationBackbone(res0, null);

    // just use a script -- it is much easier!

    fileName = PT.esc(fileName);
    String script = PT.rep(PT.rep(mutateScript, "$RES0", BS.escape(bsRes0, '(', ')')), "$FNAME", fileName);
    try {
      if (Logger.debugging)
        Logger.debug(script);
      vwr.eval.runScript(script);
    } catch (Exception e) {
      // serious Jmol bug here!
      e.printStackTrace();
      System.out.println(e);
    }
    ms = vwr.ms; // has been replaced
    if (ms.ac == ac)
      return false;
    m = ms.am[iModel];
    SB sb = m.loadScript;
    String s = PT.rep(sb.toString(), "load mutate ",
        "mutate ({" + iatom + "})");
    sb.setLength(0);
    sb.append(s);
    // check for protein monomer
    BS bs = vwr.getModelUndeletedAtomsBitSet(iModel);
    int ia = bs.length() - 1;
    if (ms.at[ia] == null) {
      System.out.println("BioExt fail ");
      return false;
    }
    g = ms.at[ia].group;
    if (g != ms.at[ac + 1].group || !(g instanceof AminoMonomer)) {
      BS bsAtoms = new BS();
      g.setAtomBits(bsAtoms);
      vwr.deleteAtoms(bsAtoms, false);
      return false;
    }
    AminoMonomer res1 = (AminoMonomer) g;

    // fix h position as same as previous group
    getMutationBackbone(res1, backbone);
    // must get new group into old chain

    // note that the terminal N if replacing the N-terminus will only have two H atoms

    replaceMutatedMonomer(vwr, res0, res1, addH);
    //res1.chain.model.freeze();
    //ms.recalculatePolymers(bsExclude);      
    //} catch (Exception e) {
    //System.out.println("" + e);
    // }
    vwr.fm.setFileInfo(info);
    return true;

  }

  private static void replaceMutatedMonomer(Viewer vwr, AminoMonomer res0,
                                            AminoMonomer res1, boolean addH) {
    res1.setResno(res0.getResno());
    res1.monomerIndex = res0.monomerIndex;
    res1.seqcode = res0.seqcode;
    res1.chain.groupCount = 0;
    res1.chain = res0.chain;
    res1.chain.model.groupCount = -1;
    // problem here is that res0 will be deleted, and its structure will
    // be changed
    res1.proteinStructure = res0.proteinStructure;
    //BS bsExclude = BSUtil.newBitSet2(0, ms.mc);
    //bsExclude.clear(res1.chain.model.modelIndex);

    vwr.shm.replaceGroup(res0, res1);
    Group[] groups = res0.chain.groups;
    for (int i = groups.length; --i >= 0;)
      if (groups[i] == res0) {
        groups[i] = res1;
        break;
      }
    res1.bioPolymer = res0.bioPolymer;
    if (res1.bioPolymer != null) {
      Monomer[] m = res1.bioPolymer.monomers;
      for (int j = m.length; --j >= 0;)
        if (m[j] == res0) {
          m[j] = res1;
          break;
        }
    }
    res1.bioPolymer.recalculateLeadMidpointsAndWingVectors();
    if (addH) {
      fixHydrogens(vwr, res1);
    }
  }

  private static void fixHydrogens(Viewer vwr, AminoMonomer res1) {
    Atom a = res1.getNitrogenAtom();
    switch (a.getBondCount() * 10 + a.getCovalentHydrogenCount()) {
      case 32: // terminal
        a.setFormalCharge(1);
        P3d[] p = vwr.getAdditionalHydrogens(BSUtil.newAndSetBit(a.i), null,
            AtomCollection.CALC_H_ALLOW_H);
        if (p.length == 1) {
          Lst<Atom> vConnections = new Lst<Atom>();
          vConnections.add(a);
          Atom b = vwr.ms.addAtom(a.mi, a.group, 1, "H3", null, 0, a.getSeqID(), 0, p[0],
              Double.NaN, null, 0, 0, 1, 0, null, a.isHetero(), (byte) 0, null, Double.NaN);
          vwr.ms.bondAtoms(a, b, Edge.BOND_COVALENT_SINGLE, 
              vwr.ms.getDefaultMadFromOrder(Edge.BOND_COVALENT_SINGLE), null, 0, true, false);
          b.setMadAtom(vwr,  vwr.rd);
        }
        break;
      case 3:
        break;

      }
  }

  /**
   * @param res
   * @param backbone
   * @return [C O CA N H]
   */
  private static Atom[] getMutationBackbone(AminoMonomer res,
                                            Atom[] backbone) {
    Atom[] b = new Atom[] { res.getCarbonylCarbonAtom(),
        res.getCarbonylOxygenAtom(), res.getLeadAtom(),
        res.getNitrogenAtom(), res.getExplicitNH() };
    if (backbone == null) {
      // don't place H if there is more than one covalent H on res0.N
      if (b[3].getCovalentHydrogenCount() > 1)
        b[4] = null;
    } else {
      for (int i = 0; i < 5; i++) {
        Atom a0 = backbone[i];
        Atom a1 = b[i];
        if (a0 != null && a1 != null)
          a1.setT(a0);
      }
    }
    return b;
  }

  private final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  String getFullPDBHeader(Map<String, Object> auxiliaryInfo) {
    String info = vwr.getCurrentFileAsString("biomodel");
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0
          : info.indexOf("\n" + strRecord))) {
      case -1:
        continue;
      case 0:
        auxiliaryInfo.put("fileHeader", "");
        return "";
      default:
        if (ichFound < ichMin)
          ichMin = ++ichFound;
      }
    }
    info = info.substring(0, ichMin);
    auxiliaryInfo.put("fileHeader", info);
    return info;
  }

  /**
   * returns an array if we have special hybridization or charge
   * 
   * @param res
   * @param name
   * @param ret
   *        [0] (target valence) may be reduced by one for sp2 for C or O only
   *        [1] will be set to 1 if positive (lysine or terminal N) or -1 if
   *        negative (OXT) [2] will be set to 2 if sp2 [3] is supplied covalent
   *        bond count
   * @return true for special; false if not
   */
  boolean getAminoAcidValenceAndCharge(String res, String name, int[] ret) {
    int valence = ret[4];
    ret[4] = 0;
    if (res == null || res.length() == 0 || res.length() > 3
        || name.equals("CA") || name.equals("CB"))
      return false;
    char ch0 = name.charAt(0);
    char ch1 = (name.length() == 1 ? '\0' : name.charAt(1));
    boolean isSp2 = false;
    int bondCount = ret[3];
    switch (res.length()) {
    case 3:
      // protein, but also carbohydrate?
      if (name.length() == 1) {
        switch (ch0) {
        case 'N':
          // terminal N?
          if (bondCount > 1)
            return false;
          ret[1] = 1;
          break;
        case 'O':
          if (valence == 1) {
            return true;
          }
          isSp2 = ("HOH;DOD;WAT".indexOf(res) < 0);
          break;
        default:
          isSp2 = true;
        }
      } else {
        String id = res + ch0;
        isSp2 = (aaSp2.indexOf(id) >= 0);
        if (aaPlus.indexOf(id) >= 0) {
          // LYS N is 1+
          ret[1] = 1;
        } else if (ch0 == 'O' && ch1 == 'X') {
          // terminal O is 1-
          ret[1] = -1;
        }
      }
      break;
    case 1:
    case 2:
      // dna/rna
      if (name.length() > 2 && name.charAt(2) == '\'')
        return false;
      switch (ch0) {
      case 'C':
        if (ch1 == '7') // T CH3
          return false;
        break;
      case 'N':
        switch (ch1) {
        case '1':
        case '3':
          if (naNoH.indexOf("" + res.charAt(res.length() - 1) + ch1) >= 0)
            ret[0]--;
          break;
        case '7':
          ret[0]--;
          break;
        }
        break;
      }
      isSp2 = true;
    }
    if (isSp2) {
      ret[4] = (aaSp21.indexOf(res + name) >= 0 ? 0 : 1);
      switch (ch0) {
      case 'N':
        ret[2] = 2;
        if (valence == 2 && bondCount == 1) // already double bonded
          ret[4]++;
        break;
      case 'C':
        ret[2] = 2;
        ret[0]--;
        break;
      case 'O':
        if (valence == 2 && bondCount == 1) // already double bonded
          ret[4]--;
        ret[0]--;
        break;
      }
    }
    return true;
  }

  private final static String naNoH = "A3;A1;C3;G3;I3";
  private final static String aaSp2 = "ARGN;ASNN;ASNO;ASPO;" + "GLNN;GLNO;GLUO;"
      + "HISN;HISC;PHEC" + "TRPC;TRPN;TYRC";
  private final static String aaSp21 = "ARGNE;ARGNH1;ASNNH2;GLNNE2;TRPNE1;HISNE2";

  private final static String aaPlus = "LYSN";

}
