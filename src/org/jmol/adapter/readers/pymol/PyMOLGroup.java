package org.jmol.adapter.readers.pymol;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;

import javajs.util.Lst;

class PyMOLGroup {
  String name;
  String objectNameID;
  Map<String, PyMOLGroup> list = new Hashtable<String, PyMOLGroup>();
  Lst<Object> object;
  boolean visible = true;
  boolean occluded = false;
  BS bsAtoms = new BS();
  int firstAtom;
  int type;

  PyMOLGroup parent;
  
  PyMOLGroup(String name) {
    this.name = name;
  }

  void addList(PyMOLGroup child) {
    PyMOLGroup group = list.get(child.name);
    if (group != null)
      return;
    list.put(child.name, child);
    child.parent = this;
  }
  
  void set() {
    if (parent != null)
      return;    
  }
  
  void addGroupAtoms(BS bs) {
    bsAtoms.or(bs);
    if (parent != null)
      parent.addGroupAtoms(bsAtoms);
  }

  @Override
  public String toString() {
    return this.name;
  }

}
