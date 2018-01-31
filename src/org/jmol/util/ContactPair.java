package org.jmol.util;


import javajs.util.P3;
import javajs.util.V3;

import org.jmol.modelset.Atom;
import org.jmol.script.T;

public class ContactPair {
  public float[] radii = new float[2];
  public float[] vdws = new float[2];
  public Atom[] myAtoms = new Atom[2];
  public P3 pt;
  public double volume = 0;
  public double vdwVolume = 0; 
  public float score;
  public float d;
  public float chord;
  public int contactType;
  public float xVdwClash = Float.NaN;

  public ContactPair(Atom[] atoms, int i1, int i2, float R, float r, float vdwA, float vdwB) {
    radii[0] = R;
    radii[1] = r;
    vdws[0] = vdwA;
    vdws[1] = vdwB;
    myAtoms[0] = atoms[i1];
    myAtoms[1] = atoms[i2];
    
    //    ----------d---------
    //    -----R----|->
    //    A      (  .  )     B
    //            <-|----r----
    //              pt
    // dist(A, pt) = x = R - (R + r - d)/2 = (R - r + d)/2
    // pt = A + (Vab/d)*x
    //
    // Note that R is not necessarily VDW(A), and 
    // r is not necessarily VDW(B). That's certainly true for clashes,
    // for attractive Van der Waals forces R and r will be larger
    
    V3 v = V3.newVsub(myAtoms[1], myAtoms[0]);
    d = v.length();
    
    // find center of asymmetric lens
    //NOT float f = (vdw1*vdw1 - vdw2*vdw2 + dAB*dAB) / (2 * dAB*dAB);
    // as that would be for truly planar section, but it is not quite planar

    float f = (R - r + d) / (2 * d);
    pt = new P3();
    pt.scaleAdd2(f, v, myAtoms[0]);

    // http://mathworld.wolfram.com/Sphere-SphereIntersection.html
    //  volume = pi * (R + r - d)^2 (d^2 + 2dr - 3r^2 + 2dR + 6rR - 3R^2)/(12d)

    // score > 0 if two atoms are not really touching (Van der Waals interaaction only)
    // score = 0 when just touching
    // score < 0 when two atoms are in contact (clash or hydrogen bond).
    // lower score --> more contact. 
    score = d - vdwA - vdwB;
    contactType = (score < 0 ? T.clash : T.vanderwaals);
    if (score < 0) {
      radii[0] = R = vdwA;
      radii[1] = r = vdwB;
    }
    getVolume();
    // chord check:
  }
  
  private void getVolume() {
    double R = radii[0];
    double r = radii[1];
    volume = (R + r - d);
    volume *= Math.PI * volume
        * (d * d + 2 * d * r - 3 * r * r + 2 * d * R + 6 * r * R - 3 * R * R)
        / 12 / d;
    vdwVolume = (score > 0 ? -volume : volume);
    double a = (d * d - r * r + R * R);
    chord = (float) Math.sqrt(4 * d * d * R * R - a * a) / d;
  }

  private int oldType = 0;
  public boolean setForVdwClash(boolean isVdw) {
    if (Float.isNaN(xVdwClash))
      return false;
    if (isVdw) {
      oldType  = contactType;
      contactType = T.vanderwaals;
      radii[0] = vdws[0] + xVdwClash;
      radii[1] = vdws[1] + xVdwClash;
    } else {
      contactType = oldType;
      radii[0] = vdws[0];
      radii[1] = vdws[1];
    }
    getVolume();
    return true;
  }
  
  public void switchAtoms() {
    Atom atom = myAtoms[0];
    myAtoms[0] = myAtoms[1];
    myAtoms[1] = atom;
    float r = radii[0];
    radii[0] = radii[1];
    radii[1] = r;
    r = vdws[0];
    vdws[0] = vdws[1];
    vdws[1] = r;
  }
  
  @Override
  public String toString() {
    return "type=" + T.nameOf(contactType) + " " + myAtoms[0] + " " + myAtoms[1] + " dAB=" + d + " score=" +  score + " chord=" + chord + " volume=" + volume;
  }

}
