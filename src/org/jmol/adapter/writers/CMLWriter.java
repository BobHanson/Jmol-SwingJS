package org.jmol.adapter.writers;

import org.jmol.api.JmolWriter;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Edge;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.SB;

/**
 * An XCrysDen XSF writer
 * 
 * see http://www.xcrysden.org/doc/XSF.html
 * 
 */
public class CMLWriter implements JmolWriter {


  protected Viewer vwr;
  protected OC oc;
  protected int atomsMax;
  protected boolean addBonds;
  protected boolean doTransform;
  protected boolean allTrajectories;

  public CMLWriter() {
    // for JavaScript dynamic loading
  }

  @Override
  public void set(Viewer viewer, OC out, Object[] data) {
    vwr = viewer;
    this.oc = (oc == null ? vwr.getOutputChannel(null,  null) : oc);
    atomsMax = ((Integer) data[0]).intValue();
    addBonds = ((Boolean) data[1]).booleanValue();
    doTransform = ((Boolean) data[2]).booleanValue();
    allTrajectories = ((Boolean) data[3]).booleanValue();
  }

  /*
   * <molecule title="acetic_acid.mol"
   * xmlns="http://www.xml-cml.org/schema/cml2/core"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.xml-cml.org/schema/cml2/core cmlAll.xsd">
   * <atomArray> <atom id="a1" elementType="C" x3="0.1853" y3="0.0096"
   * z3="0.4587"/> <atom id="a2" elementType="O" x3="0.6324" y3="1.0432"
   * z3="0.8951"/> <atom id="a3" elementType="C" x3="-1.0665" y3="-0.1512"
   * z3="-0.3758"/> <atom id="a4" elementType="O" x3="0.7893" y3="-1.1734"
   * z3="0.6766" formalCharge="-1"/> <atom id="a5" elementType="H" x3="-1.7704"
   * y3="-0.8676" z3="0.1055"/> <atom id="a6" elementType="H" x3="-0.8068"
   * y3="-0.5215" z3="-1.3935"/> <atom id="a7" elementType="H" x3="-1.5889"
   * y3="0.8259" z3="-0.4854"/> </atomArray> <bondArray> <bond atomRefs2="a1 a2"
   * order="partial12"/> <bond atomRefs2="a1 a3" order="S"/> <bond
   * atomRefs2="a1 a4" order="partial12"/> <bond atomRefs2="a3 a5" order="S"/>
   * <bond atomRefs2="a3 a6" order="S"/> <bond atomRefs2="a3 a7" order="S"/>
   * </bondArray> </molecule>
   */
  @Override
  public String write(BS bs) {

    // not allowing full trajectory business here. 
    SB sb = new SB();
    int nAtoms = bs.cardinality();
    if (nAtoms == 0)
      return "";
    openTag(sb, "molecule");
    openTag(sb, "atomArray");
    BS bsAtoms = new BS();
    Atom[] atoms = vwr.ms.at;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (--atomsMax < 0)
        break;
      Atom atom = atoms[i];
      String name = atom.getAtomName();
      PT.rep(name, "\"", "''");
      bsAtoms.set(atom.i);
      appendEmptyTag(sb, "atom", new String[] { "id",
          "a" + (atom.i + 1), "title", atom.getAtomName(), "elementType",
          atom.getElementSymbol(), "x3", "" + atom.x, "y3", "" + atom.y, "z3",
          "" + atom.z });
    }
    closeTag(sb, "atomArray");
    if (addBonds) {
      openTag(sb, "bondArray");
      int bondCount = vwr.ms.bondCount;
      Bond[] bonds = vwr.ms.bo;
      for (int i = 0; i < bondCount; i++) {
        Bond bond = bonds[i];
        if (bond == null)
          continue;
        Atom a1 = bond.atom1;
        Atom a2 = bond.atom2;
        if (!bsAtoms.get(a1.i) || !bsAtoms.get(a2.i))
          continue;
        String order = Edge.getCmlBondOrder(bond.order);
        if (order == null)
          continue;
        appendEmptyTag(sb, "bond", new String[] { "atomRefs2",
            "a" + (bond.atom1.i + 1) + " a" + (bond.atom2.i + 1),
            "order", order, });
      }
      closeTag(sb, "bondArray");
    }
    closeTag(sb, "molecule");
      oc.append(sb.toString());
    return toString();
  }

  public static void openDocument(SB sb) {
    sb.append("<?xml version=\"1.0\"?>\n");
  }
  static protected void openTag(SB sb, String name) {
    sb.append("<").append(name).append(">\n");
  }

  static protected void startOpenTag(SB sb, String name) {
    sb.append("<").append(name);
  }
  
  static protected void terminateTag(SB sb) {
    sb.append(">\n");
  }

  static protected void terminateEmptyTag(SB sb) {
    sb.append("/>\n");
  }

  static protected void appendEmptyTag(SB sb, String name, String[] attributes) {
    startOpenTag(sb, name);
    addAttributes(sb, attributes);
    terminateEmptyTag(sb);
  }

  static protected void addAttributes(SB sb, String[] attributes) {
    for (int i = 0; i < attributes.length; i++) {
      addAttribute(sb, attributes[i], attributes[++i]);
    }
  }

  static protected void addAttribute(SB sb, String key, String val) {
    //System.out.println("CMLW addAttribute " + key + "=" + val);
    sb.append(" ").append(key).append("=").append(PT.esc(val));
  }

  static protected void closeTag(SB sb, String name) {
    sb.append("</").append(name).append(">\n");
  }  

  @Override
  public String toString() {
    return (oc == null ? "" : oc.toString());
  }


}

