/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javajs.util.BS;
import javajs.util.Lst;

import javax.swing.JCheckBoxMenuItem;

import org.jmol.util.JSONWriter;

public class LoadMeasureThreadJSON extends LoadMeasureThread {

  Map<String, Object> data;

  public LoadMeasureThreadJSON(NMR_JmolPanel nmrPanel,
      Map<String, Object> jsonData) {
    this.nmrPanel = nmrPanel;
    this.data = jsonData;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void setMore() {
    if (!data.containsKey("NamfisPopulation"))
      return;
    Lst<Object> populations = (Lst<Object>) data.get("NamfisPopulation");
    if (populations.size() > 0) {
      int nmodel = ((NMR_Viewer) nmrPanel.vwr).getModelCount();
      double[] population = new double[nmodel + 1];
      for (int i = 0; i <= nmodel; i++) {
        population[i] = 0.0;
      }
      for (int i = 0; i < populations.size(); i++) {
        Map<String, Object> p = (Map<String, Object>) populations.get(i);
        int index = getInt(p, "index");
        double pop = getDouble(p, "p");
        population[index] = pop;
      }
      nmrPanel.populationDisplay.addPopulation(population);
      JCheckBoxMenuItem mi = (JCheckBoxMenuItem) nmrPanel
          .getMenuItem("NMR.populationDisplayCheck");
      mi.setSelected(true);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void setNOEs() {
    if (!data.containsKey("NOEs")) {
      return;
    }
    Lst<Object> noes = (Lst<Object>) data.get("NOEs");
    // Noes

    for (int i = 0; i < noes.size(); i++) {
      Map<String, Object> noe = (Map<String, Object>) noes.get(i);
      int ia = getInt(noe, "a");
      int ib = getInt(noe, "b");
      String exp = (String) noe.get("exp");
      String expd = (String) noe.get("expd");
      addNOE(ia, ib, exp, expd);
    }
    if (data.containsKey("RefNOE")) {
      int[] noeNPrefIndices = new int[2];

      Map<String, Object> refNOE = (Map<String, Object>) data.get("RefNOE");
      noeNPrefIndices[0] = getInt(refNOE, "a");
      noeNPrefIndices[1] = getInt(refNOE, "b");
      nmrPanel.noeTable.setNoeNPrefIndices(noeNPrefIndices);
    }

    if (data.containsKey("ExpRefNOEValue")) {
      double expRefValue = getDouble(data, "ExpRefNOEValue");
      nmrPanel.noeTable.setNoeExprefValue(expRefValue);
    }

    if (data.containsKey("CorrelationTime")) {
      double dval = getDouble(data, "CorrelationTime");
      nmrPanel.noeTable.setCorrelationTime(dval);
      nmrPanel.noeTable.noeParameterSelectionPanel.getTauField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("MixingTime")) {
      double dval = getDouble(data, "MixingTime");
      nmrPanel.noeTable.setMixingTime(dval);
      nmrPanel.noeTable.noeParameterSelectionPanel.gettMixField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("NMRfreq")) {
      double dval = getDouble(data, "NMRfreq");
      nmrPanel.noeTable.setNMRfreq(dval);
      nmrPanel.noeTable.noeParameterSelectionPanel.getFreqField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("RhoStar")) {
      double dval = getDouble(data, "RhoStar");
      nmrPanel.noeTable.setRhoStar(dval);
      nmrPanel.noeTable.noeParameterSelectionPanel.getRhoStarField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("NoeYellowValue")) {
      double dval = getDouble(data, "NoeYellowValue");
      nmrPanel.noeTable.setYellowValue(dval);
      nmrPanel.noeTable.noeColourSelectionPanel.getYellowField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("NoeRedValue")) {
      double dval = getDouble(data, "NoeRedValue");
      nmrPanel.noeTable.setRedValue(dval);
      nmrPanel.noeTable.noeColourSelectionPanel.getRedField().setText(
          String.valueOf(dval));
    }

    if (data.containsKey("CoupleYellowValue")) {
      double dval = getDouble(data, "CoupleYellowValue");
      nmrPanel.coupleTable.setYellowValue(dval);
      nmrPanel.coupleTable.coupleColourSelectionPanel.getYellowField()
          .setText(String.valueOf(dval));
    }

    if (data.containsKey("CoupleRedValue")) {
      double dval = getDouble(data, "CoupleRedValue");
      nmrPanel.coupleTable.setRedValue(dval);
      nmrPanel.coupleTable.coupleColourSelectionPanel.getRedField().setText(
          String.valueOf(dval));
    }


  }

  @SuppressWarnings("unchecked")
  @Override
  protected void setCouples() {
    if (!data.containsKey("Couples"))
      return;
    
      Lst<Object> couples = (Lst<Object>) data.get("Couples");
      // Couples

      for (int i = 0; i < couples.size(); i++) {
        Map<String, Object> couple = (Map<String, Object>) couples.get(i);
        int ia = getInt(couple, "a");
        int ib = getInt(couple, "b");
        int ic = getInt(couple, "c");
        int id = getInt(couple, "d");
        String exp = (String) couple.get("exp");
        addCouple(ia, ib, ic, id, exp);
      }

  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean setLabels() {
    if (!data.containsKey("Labels"))
      return false;
    Lst<Object> labels = (Lst<Object>) data.get("Labels");
    // labels
    for (int i = 0; i < labels.size(); i++) {
      Map<String, Object> label = (Map<String, Object>) labels.get(i);

      int j = getInt(label, "index");
      String l = (String) label.get("label");
      addCommand(j - 1, l);
    }
    return true;
  }

  private double getDouble(Map<String, Object> p, String key) {
    return ((Number) p.get(key)).doubleValue();
  }

  private static int getInt(Map<String, Object> noe, String key) {
    return ((Number) noe.get(key)).intValue();
  }

  @SuppressWarnings("unchecked")
  public void writeNamfisFiles(String name) {

    /*
     * Namfis file .in1 contains the calculated distances and J's for each conformer
     * .in2 contains the experimental measurements and uncertainties
     */

    File namfis1 = new File(name + ".in1");
    File namfis2 = new File(name + ".in2");
    File namfis3 = new File(name + ".in3");
    File namfisout = new File(name + ".out");
    String parent = namfis1.getParent();
    File filename = new File(parent + "/filename.dat");
    File optionfile = new File(parent + "/optionfile");
    String zipFileName = name + ".zip";

    Map<String, Object> nmrdata;
    Lst<Object> noes = new Lst<Object>();
    Lst<Object> couples = new Lst<Object>();
    try {
      nmrdata = getNmrDataJSON();
      noes = (Lst<Object>) nmrdata.get("NOEs");
      couples = (Lst<Object>) nmrdata.get("Couples");
    } catch (Exception e) {
      //
      nmrdata = new Hashtable<String, Object>();
    }

    try {
      BS[] mols = nmrPanel.getAllMolecules();
      PrintWriter out1 = new PrintWriter(new FileWriter(namfis1));
      String[] labelArray = nmrPanel.labelSetter.getLabelArray();
      for (int base = 0, i = 0; i < mols.length; base += mols[i].cardinality(), i++) {
        NmrMolecule props = nmrPanel.getDistanceJMolecule(mols[i],
            labelArray, true);
        props.calcNOEs();

        for (int n = 0; n < noes.size(); n++) {
          Map<String, Object> noe = (Map<String, Object>) noes.get(n);
          String a = (String) noe.get("a");
          String b = (String) noe.get("b");

          if (noe.containsKey("expd")) {
            String exp = (String) noe.get("expd");
            if (exp != null) {
              int j = (new Integer(a)).intValue() - 1;
              int k = (new Integer(b)).intValue() - 1;
              props.addJmolDistance(j + base, k + base);
            }
          }
        }

        for (int n = 0; n < couples.size(); n++) {
          Map<String, Object> couple = (Map<String, Object>) couples.get(n);
          String a = (String) couple.get("a");
          String b = (String) couple.get("b");
          String c = (String) couple.get("c");
          String d = (String) couple.get("d");
          if (couple.containsKey("exp")) {
            String exp = (String) couple.get("exp");
            if (exp != null) {
              int j = (new Integer(a)).intValue() - 1;
              int k = (new Integer(b)).intValue() - 1;
              int l = (new Integer(c)).intValue() - 1;
              int m = (new Integer(d)).intValue() - 1;
              props.addJmolCouple(j + base, k + base, l + base, m + base);
            }
          }
        }
        // NAMFIS has problems on some systems if there are zero noes or couplings
        // Always add dummy variable
        Vector<Double> vec = props.getDistances();
        vec.add(new Double(1.0));
        writeVector(vec, out1);

        vec = props.getCouples();
        vec.add(new Double(1.0));
        writeVector(vec, out1);

      }
      out1.flush();
      out1.close();

      PrintWriter out2 = new PrintWriter(new FileWriter(namfis2));
      DecimalFormat df = new DecimalFormat("#0.00  ");
      out2.print("-1\n");

      int nnoe = 0;
      for (int i = 0; i < noes.size(); i++) {
        Map<String, Object> noe = (Map<String, Object>) noes.get(i);
        if (noe.containsKey("expd")) {
          nnoe++;
        }
      }
      int ncouple = 0;
      for (int i = 0; i < couples.size(); i++) {
        Map<String, Object> couple = (Map<String, Object>) couples.get(i);
        if (couple.containsKey("exp")) {
          ncouple++;
        }
      }

      // Add dummy variables
      nnoe++;
      ncouple++;

      // if (nnoe > 0) {
      out2.print(ncouple + " " + nnoe + " 0\n");
      for (int i = 0; i < noes.size(); i++) {
        Map<String, Object> noe = (Map<String, Object>) noes.get(i);

        if (noe.containsKey("expd")) {
          String exp = (String) noe.get("expd");
          if (exp != null) {
            out2.print(df.format(Double.valueOf(exp)) + " " + 0.4 + "\n");
          }
        }

      }
      out2.print("1.0 0.4\n"); // Dummy variable
      // }

      out2.print("\n");
      for (int i = 0; i < couples.size(); i++) {
        Map<String, Object> couple = (Map<String, Object>) couples.get(i);
        if (couple.containsKey("exp")) {
          String exp = (String) couple.get("exp");
          out2.print(exp + " ");
        }
      }
      out2.print(1.0); // Dummy variable
      out2.print("\n");
      for (int i = 0; i < couples.size(); i++) {
        Map<String, Object> couple = (Map<String, Object>) couples.get(i);
        if (couple.containsKey("exp")) {
          out2.print(2.0 + " ");
        }
      }
      out2.print(0.5); // Dummy variable

      out2.print("\n");
      out2.print("\n");
      out2.print("1.0 1.0\n");
      out2.print("5.0\n");
      out2.print("0\n");

      out2.flush();
      out2.close();

      PrintWriter out3 = new PrintWriter(new FileWriter(namfis3));
      out3.flush();
      out3.close();

      PrintWriter out4 = new PrintWriter(new FileWriter(filename));
      /*
       * String head = name.replaceFirst(parent,""); head = head.replaceFirst("/","");
       * out4.println(head + ".in1"); out4.println(head + ".in2"); out4.println(head +
       * ".out"); out4.println(head + ".in3");
       */
      out4.print(namfis1.getName() + "\n");
      out4.print(namfis2.getName() + "\n");
      out4.print(namfisout.getName() + "\n");
      out4.print(namfis3.getName() + "\n");
      out4.flush();
      out4.close();

      PrintWriter out5 = new PrintWriter(new FileWriter(optionfile));
      out5.print("  Begin\n");
      out5.print("    NoList\n");
      out5.print("    Derivative level          3\n");
      out5.print("    Verify                   No\n");
      out5.print("    Infinite step size      1.0d+20\n");
      out5.print("    step limit              1.0d-02\n");
      out5.print("    Major iterations limit    200\n");
      out5.print("    Minor iterations limit   2000\n");
      out5.print("    Major print level         10\n");
      out5.print("    Function precision        1.0d-20\n");
      out5.print("    Optimality Tolerance      1.0d-20\n");
      out5.print("    Linear Feasibility Tolerance 1.0d-2\n");
      out5.print("  end\n");
      out5.flush();
      out5.close();

      // Write zip file

      Vector<String> inputFileNames = new Vector<String>();
      inputFileNames.add(namfis1.getAbsolutePath());
      inputFileNames.add(namfis2.getAbsolutePath());
      inputFileNames.add(namfis3.getAbsolutePath());
      inputFileNames.add(filename.getAbsolutePath());
      inputFileNames.add(optionfile.getAbsolutePath());

      writeZip(inputFileNames, zipFileName);
    } catch (Exception e) {
      //
      e.printStackTrace();
    }
  }

  private void writeVector(Vector<?> vector, PrintWriter out) {
    DecimalFormat df = new DecimalFormat("0.000  ");
    int count = 0;
    for (int j = 0; j < vector.size(); j++) {
      out.print(df.format(vector.get(j)));
      if (count++ == 10) {
        out.print("\n");
        count = 0;
      }
    }
    if (count != 0) {
      out.print("\n");
    }
  }

  private void writeZip(Vector<String> v, String outFilename) {
    // Create a buffer for reading the files
    byte[] buf = new byte[2048];
    try {
      // Create the ZIP file
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
          outFilename));
      // Compress the files
      for (int i = 0; i < v.size(); i++) {
        FileInputStream in = new FileInputStream(v.get(i));

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(v.get(i)));

        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }

        // Complete the entry
        out.closeEntry();
        in.close();
      }
      // Complete the ZIP file
      out.close();
    } catch (IOException e) {
      System.out.println("Error Writing zip....");
      e.printStackTrace();
    }
  }

  public void writeNmrDataJSON(File file) throws Exception {
    JSONWriter writer = new JSONWriter();
    writer.setStream(new FileOutputStream(file));
    writer.writeMap(getNmrDataJSON());
    writer.closeStream();
  }

  @SuppressWarnings("boxing")
  public Map<String, Object> getNmrDataJSON() {
    Map<String, Object> data = new Hashtable<String, Object>();
    data.put("StructureFile", nmrPanel.vwr.getModelSetPathName());

    Lst<Object> labels = new Lst<Object>();
    String[] labelArray = nmrPanel.labelSetter.getLabelArray();
    for (int i = 0; i < labelArray.length; i++) {
      if (labelArray[i] != null) {
        Map<String, Object> l = new Hashtable<String, Object>();
        l.put("index", String.valueOf(i + 1));
        l.put("label", labelArray[i]);
        labels.addLast(l);
      }
    }
    data.put("Labels", labels);

    NoeTable noeTable = nmrPanel.noeTable;

    Lst<Object> noes = new Lst<Object>();
    int noeCount = noeTable.getRowCount();
    for (int i = 0; i < noeCount; i++) {
      int[] atomIndices = noeTable.getMeasurementCountPlusIndices(i);
      Map<String, Object> n = new Hashtable<String, Object>();
      n.put("a", String.valueOf(atomIndices[1] + 1));
      n.put("b", String.valueOf(atomIndices[2] + 1));
      n.put("exp", noeTable.getExpNoe(atomIndices[1], atomIndices[2]));
      n.put("expd", noeTable.getExpDist(atomIndices[1], atomIndices[2]));

      noes.addLast(n);
    }
    data.put("NOEs", noes);

    CoupleTable coupleTable = nmrPanel.coupleTable;

    int coupleCount = coupleTable.getRowCount();
    Lst<Object> couples = new Lst<Object>();
    for (int i = 0; i < coupleCount; i++) {
      int[] atomIndices = coupleTable.getMeasurementCountPlusIndices(i);
      Map<String, Object> c = new Hashtable<String, Object>();
      c.put("a", String.valueOf(atomIndices[1] + 1));
      c.put("b", String.valueOf(atomIndices[2] + 1));
      c.put("c", String.valueOf(atomIndices[3] + 1));
      c.put("d", String.valueOf(atomIndices[4] + 1));
      c.put("exp", coupleTable.getExpCouple(atomIndices[1], atomIndices[4]));
      couples.addLast(c);
    }
    data.put("Couples", couples);

    int[] noeNPrefIndices = noeTable.getnoeNPrefIndices();
    Map<String, Object> refNOE = new Hashtable<String, Object>();
    refNOE.put("a", new Integer(noeNPrefIndices[0]));
    refNOE.put("b", new Integer(noeNPrefIndices[1]));
    data.put("RefNOE", refNOE);

    double noeExprefValue = noeTable.getNoeExprefValue();
    data.put("ExpRefNOEValue", Double.toString(noeExprefValue));

    data.put("CorrelationTime", Double.toString(noeTable.getCorrelationTime()));
    data.put("MixingTime", Double.toString(noeTable.getMixingTime()));
    data.put("NMRfreq", Double.toString(noeTable.getNMRfreq()));
    data.put("Cutoff", Double.toString(noeTable.getCutoff()));
    data.put("RhoStar", Double.toString(noeTable.getRhoStar()));

    data.put("NoeRedValue", Double.toString(noeTable.getRedValue()));
    data.put("NoeYellowValue", Double.toString(noeTable.getYellowValue()));

    data.put("CoupleRedValue", Double.toString(coupleTable.getRedValue()));
    data.put("CoupleYellowValue", Double.toString(coupleTable.getYellowValue()));

    double[] population = nmrPanel.populationDisplay.getPopulation();
    int populationLength = 0;
    if (population != null) {
      populationLength = population.length;
    }

    Lst<Object> populations = new Lst<Object>();
    for (int i = 0; i < populationLength; i++) {
      if (population[i] > 0.0) {
        Map<String, Object> p = new Hashtable<String, Object>();
        p.put("index", i);
        p.put("p", String.valueOf(population[i]));
        populations.addLast(p);
      }

    }
    data.put("NamfisPopulation", populations);

    return data;
  }

  @SuppressWarnings("unchecked")
  public int jumpBestFrame() {

    Map<String, Object> nmrdata = new LoadMeasureThreadJSON(nmrPanel, null)
        .getNmrDataJSON();

    Lst<Object> noes = (Lst<Object>) nmrdata.get("NOEs");
    Lst<Object> couples = (Lst<Object>) nmrdata.get("Couples");
    try {
      BS[] mols = nmrPanel.getAllMolecules();
      String[] labelArray = nmrPanel.labelSetter.getLabelArray();
      double noeWeight = nmrPanel.frameDeltaDisplay.getNoeWeight();
      double coupleWeight = nmrPanel.frameDeltaDisplay.getCoupleWeight();
      boolean lexpNoes = nmrPanel.noeTable.getlexpNoes();
      double minDiff = Double.MAX_VALUE;
      int minFrame = -1;
      for (int base = 0, i = 0; i < mols.length; base += mols[i].cardinality(), i++) {
        NmrMolecule props = nmrPanel.getDistanceJMolecule(mols[i],
            labelArray, true);
        props.calcNOEs();
        double diffDist = 0.0;
        double diffNoe = 0.0;
        double diffCouple = 0.0;
        for (int n = 0; n < noes.size(); n++) {
          Map<String, Object> noe = (Map<String, Object>) noes.get(n);
          String a = (String) noe.get("a");
          String b = (String) noe.get("b");

          if (noe.containsKey("expd")) {
            String exp = (String) noe.get("expd");
            if (exp != null) {
              int j = (new Integer(a)).intValue() - 1;
              int k = (new Integer(b)).intValue() - 1;
              double cDist = props.getJmolDistance(j + base, k + base);
              MeasureDist measure = new MeasureDist(exp, cDist);
              diffDist += measure.getDiff();
            }
          }
          if (noe.containsKey("exp")) {
            String exp = (String) noe.get("exp");
            if (exp != null) {
              int j = (new Integer(a)).intValue() - 1;
              int k = (new Integer(b)).intValue() - 1;
              double cNoe = props.getJmolNoe(j + base, k + base);
              MeasureNoe measure = new MeasureNoe(exp, cNoe);
              diffNoe += measure.getDiff();
            }
          }
        }

        for (int n = 0; n < couples.size(); n++) {
          Map<String, Object> couple = (Map<String, Object>) couples.get(n);
          String a = (String) couple.get("a");
          String b = (String) couple.get("b");
          String c = (String) couple.get("c");
          String d = (String) couple.get("d");
          if (couple.containsKey("exp")) {
            String exp = (String) couple.get("exp");
            if (exp != null) {
              int j = (new Integer(a)).intValue() - 1;
              int k = (new Integer(b)).intValue() - 1;
              int l = (new Integer(c)).intValue() - 1;
              int m = (new Integer(d)).intValue() - 1;
              double[] cCouple = props.calcJmolCouple(j + base, k + base, l + base, m + base);
              MeasureCouple measure = new MeasureCouple(exp, cCouple[1]);
              diffCouple += measure.getDiff();
            }
          }
        }

        double diff = diffCouple * coupleWeight;
        if (lexpNoes) {
          diff += diffNoe * noeWeight;

        } else {
          diff += diffDist * noeWeight;
        }
        if (diff < minDiff) {
          minDiff = diff;
          minFrame = i;
        }
      }
      return minFrame + 1;
    } catch (Exception e) {
      //
      e.printStackTrace();
      return -1;
    }
  }

}
