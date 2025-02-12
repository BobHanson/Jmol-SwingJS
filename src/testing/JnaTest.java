package testing;

import java.io.IOException;

import io.github.dan2097.jnainchi.InchiOutput;
import io.github.dan2097.jnainchi.InchiStatus;
import io.github.dan2097.jnainchi.SmilesToInchi;

public class JnaTest {
  
public static void main(String[] args) {
  String smiles = "CN(CC[C@]12c3c(C4)ccc(O)c3O[C@H]11)[C@H]4[C@@H]2C=C[C@@H]1O";
  InchiOutput output;
  try {
    output = SmilesToInchi.toInchi(smiles);
    if (output.getStatus() == InchiStatus.SUCCESS || output.getStatus() == InchiStatus.WARNING) {
      String inchi = output.getInchi();
      System.out.println(inchi);
    }
  } catch (IOException e) {
    // TODO
  }
  
}
  
}