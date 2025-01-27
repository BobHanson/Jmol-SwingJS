package org.jmol.ocl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;

public class OpenChemLib {
  
  public OpenChemLib() {
    
  }
  public String smilesToMolfile(String smiles) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStreamWriter writer = new OutputStreamWriter(bos);
      StereoMolecule mol = new SmilesParser().parseMolecule(smiles);
      new MolfileCreator(mol).writeMolfile(writer);
      writer.close();
      return new String(bos.toByteArray());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }
  
}