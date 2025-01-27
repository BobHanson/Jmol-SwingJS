package org.jmol.inchi;

import org.jmol.api.JmolInChI;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.PT;

public abstract class InchiJmol implements JmolInChI {

  protected boolean inputInChI;
  protected String inchi;
  protected String smilesOptions;
  protected boolean doGetSmiles;
  protected boolean getInchiModel;
  protected boolean getKey;
  protected boolean fixedH;

  protected String setParameters(String options, Object molData, BS atoms,
                                 Viewer vwr) {
    if (atoms == null ? molData == null : atoms.isEmpty())
      return null;
    if (options == null)
      options = "";
    String inchi = null;
    String lc = options.toLowerCase();
    boolean getSmiles = (lc.indexOf("smiles") == 0);
    boolean getInchiModel = (lc.indexOf("model") == 0);
    boolean getKey = (lc.indexOf("key") >= 0);
    String smilesOptions = (getSmiles ? options : null);
    if (lc.startsWith("model/")) {
      inchi = options.substring(10);
      options = lc = "";
    } else if (getInchiModel) {
      lc = lc.substring(5);
    }
    boolean fixedH = (options.indexOf("fixedh?") >= 0);
    boolean inputInChI = (molData instanceof String
        && ((String) molData).startsWith("InChI="));
    if (inputInChI) {
      inchi = (String) molData;
    } else {
      options = lc;
      if (getKey) {
        options = options.replace("inchikey", "");
        options = options.replace("key", "");
      }

      if (fixedH) {
        String fxd = getInchi(vwr, atoms, molData, options.replace('?', ' '));
        options = PT.rep(options, "fixedh?", "");
        String std = getInchi(vwr, atoms, molData, options);
        inchi = (fxd != null && fxd.length() <= std.length() ? std : fxd);
      }
    }
    this.fixedH = fixedH;
    this.inputInChI = inputInChI;
    this.doGetSmiles = getSmiles;
    this.inchi = inchi;
    this.getKey = getKey;
    this.smilesOptions = smilesOptions;
    this.getInchiModel = getInchiModel;
    return options;
  }

  private final static String atomicSymbols = "xx" //
      + "H He" //
      + "LiBeB C N O F Ne" //
      + "NaMgAlSiP S ClAr" //
      + "K CaScTiV CrMnFeCoNiCuZnGaGeAsSeBrKr" //
      + "RbSrY ZrNbMoTcRuRhPdAgCdInSnSbTeI Xe" //
      + "CsBaLaCePrNdPmSmEuGdTbDyHoErTmYbLuHfTaW ReOsIrPtAuHgTlPbBiPoAtRn" //
      + "FrRaAcThPaU NpPuAmCmBkCfEsFmMdNoLrRfDbSgBhHsMtDsRgCnNhFlMcLvTsOg";

  private static int getAtomicNumber(String sym) {
    // B and S are out of order, with Be before B and Si before S
    // the rest are just here for speed because they are common
    if (sym.length() == 1) {
      switch (sym.charAt(0)) {
      case 'H':
        return 1;
      case 'B':
        return 5;
      case 'C':
        return 6;
      case 'O':
        return 7;
      case 'F':
        return 8;
      case 'P':
        return 15;
      case 'S':
        return 16;
      }
    }
    return atomicSymbols.indexOf(sym) / 2;
  }

  protected static int getActualMass(String sym, int mass) {
    if (mass < 900) {
      return mass;
    }
    int atno = getAtomicNumber(sym);
    return (mass - 10000) + inchiAveAtomicMass[atno - 1];
  }

  /**
   * Extracted from util.c
   */
  private final static int[] inchiAveAtomicMass = new int[] { //
      1, //1 H
      4, //2 He
      7, //3 Li
      9, //4 Be
      11, //5 B
      12, //6 C
      14, //7 N
      16, //8 O
      19, //9 F
      20, //10 Ne
      23, //11 Na
      24, //12 Mg
      27, //13 Al
      28, //14 Si
      31, //15 P
      32, //16 S
      35, //17 Cl
      40, //18 Ar
      39, //19 K
      40, //20 Ca
      45, //21 Sc
      48, //22 Ti
      51, //23 V
      52, //24 Cr
      55, //25 Mn
      56, //26 Fe
      59, //27 Co
      59, //28 Ni
      64, //29 Cu
      65, //30 Zn
      70, //31 Ga
      73, //32 Ge
      75, //33 As
      79, //34 Se
      80, //35 Br
      84, //36 Kr
      85, //37 Rb
      88, //38 Sr
      89, //39 Y
      91, //40 Zr
      93, //41 Nb
      96, //42 Mo
      98, //43 Tc
      101, //44 Ru
      103, //45 Rh
      106, //46 Pd
      108, //47 Ag
      112, //48 Cd
      115, //49 In
      119, //50 Sn
      122, //51 Sb
      128, //52 Te
      127, //53 I
      131, //54 Xe
      133, //55 Cs
      137, //56 Ba
      139, //57 La
      140, //58 Ce
      141, //59 Pr
      144, //60 Nd
      145, //61 Pm
      150, //62 Sm
      152, //63 Eu
      157, //64 Gd
      159, //65 Tb
      163, //66 Dy
      165, //67 Ho
      167, //68 Er
      169, //69 Tm
      173, //70 Yb
      175, //71 Lu
      178, //72 Hf
      181, //73 Ta
      184, //74 W
      186, //75 Re
      190, //76 Os
      192, //77 Ir
      195, //78 Pt
      197, //79 Au
      201, //80 Hg
      204, //81 Tl
      207, //82 Pb
      209, //83 Bi
      209, //84 Po
      210, //85 At
      222, //86 Rn
      223, //87 Fr
      226, //88 Ra
      227, //89 Ac
      232, //90 Th
      231, //91 Pa
      238, //92 U
      237, //93 Np
      244, //94 Pu
      243, //95 Am
      247, //96 Cm
      247, //97 Bk
      251, //98 Cf
      252, //99 Es
      257, //100 Fm
      258, //101 Md
      259, //102 No
      260, //103 Lr
      261, //104 Rf
      270, //105 Db
      269, //106 Sg
      270, //107 Bh
      270, //108 Hs
      278, //109 Mt
      281, //110 Ds
      281, //111 Rg
      285, //112 Cn
      278, //113 Nh
      289, //114 Fl
      289, //115 Mc
      293, //116 Lv
      297, //117 Ts
      294,//118 Og
  };

}
