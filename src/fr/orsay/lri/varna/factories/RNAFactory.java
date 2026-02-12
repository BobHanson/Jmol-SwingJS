package fr.orsay.lri.varna.factories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBackboneElement;
import fr.orsay.lri.varna.models.rna.ModeleBackboneElement.BackboneType;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.utils.RNAMLParser;

/**
 * BH JAVA FIX: mostly here we are just removing a lot of unnecessary stack
 * traces when doing a drag-drop of a file BH JAVA FIX: making sure the file
 * reader is closed properly
 * 
 */
public class RNAFactory {

  public enum RNAFileType {
    FILE_TYPE_DSSR_OUTPUT, FILE_TYPE_STOCKHOLM, FILE_TYPE_TCOFFEE, FILE_TYPE_BPSEQ, FILE_TYPE_CT, FILE_TYPE_DBN, FILE_TYPE_RNAML, FILE_TYPE_UNKNOWN
  }

  private static boolean isQuiet;

  public static ArrayList<RNA> loadSecStrRNAML(Reader r)
      throws ExceptionPermissionDenied, ExceptionLoadingFailed,
      ExceptionFileFormatOrSyntax {

    ArrayList<RNA> result = new ArrayList<RNA>();
    try {
      // System.setProperty("javax.xml.parsers.SAXParserFactory",
      // "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
      SAXParserFactory saxFact = javax.xml.parsers.SAXParserFactory
          .newInstance();
      saxFact.setValidating(false);
      saxFact.setXIncludeAware(false);
      saxFact.setNamespaceAware(false);
      SAXParser sp = saxFact.newSAXParser();
      RNAMLParser RNAMLData = new RNAMLParser();
      sp.parse(new InputSource(r), RNAMLData);

      /*
       * XMLReader xr = XMLReaderFactory.createXMLReader(); RNAMLParser
       * RNAMLData = new RNAMLParser(); xr.setContentHandler(RNAMLData);
       * xr.setErrorHandler(RNAMLData); xr.setEntityResolver(RNAMLData);
       * xr.parse(new InputSource(r));
       */
      for (RNAMLParser.RNATmp rnaTmp : RNAMLData.getMolecules()) {
        RNA current = new RNA();
        // Retrieving parsed data
        List<String> seq = rnaTmp.getSequence();
        // Creating empty structure of suitable size
        int[] str = new int[seq.size()];
        for (int i = 0; i < str.length; i++) {
          str[i] = -1;
        }
        current.setRNA(seq, str);
        Vector<RNAMLParser.BPTemp> allbpsTmp = rnaTmp.getStructure();
        ArrayList<ModeleBP> allbps = new ArrayList<ModeleBP>();
        for (int i = 0; i < allbpsTmp.size(); i++) {
          RNAMLParser.BPTemp bp = allbpsTmp.get(i);
          // System.err.println(bp);
          int bp5 = bp.pos5;
          int bp3 = bp.pos3;
          ModeleBase mb = current.getBaseAt(bp5);
          ModeleBase part = current.getBaseAt(bp3);
          ModeleBP newStyle = bp.createBPStyle(mb, part);
          allbps.add(newStyle);
        }

        current.applyBPs(allbps);
        result.add(current);
      }

    } catch (IOException ioe) {
      throw new ExceptionLoadingFailed(
          "Couldn't load file due to I/O or security policy issues.", "");
    } catch (Exception ge) {
      if (!isQuiet) // BH
        ge.printStackTrace();
    }
    return result;
  }

  public static int[] parseSecStr(String _secStr)
      throws ExceptionUnmatchedClosingParentheses {
    Hashtable<Character, Stack<Integer>> stacks = new Hashtable<Character, Stack<Integer>>();
    int[] result = new int[_secStr.length()];
    int i = 0;
    try {
      for (i = 0; i < _secStr.length(); i++) {
        result[i] = -1;
        char c = _secStr.charAt(i);
        char c2 = Character.toUpperCase(c);
        if (!stacks.containsKey(c2)) {
          stacks.put(c2, new Stack<Integer>());
        }
        switch (c) {
        case '<':
        case '{':
        case '(':
        case '[':
          stacks.get(c).push(i);
          break;
        case '>': {
          int j = stacks.get('<').pop();
          result[i] = j;
          result[j] = i;
          break;
        }
        case '}': {
          int j = stacks.get('{').pop();
          result[i] = j;
          result[j] = i;
          break;
        }
        case ')': {
          int j = stacks.get('(').pop();
          result[i] = j;
          result[j] = i;
          break;
        }
        case ']': {
          int j = stacks.get('[').pop();
          result[i] = j;
          result[j] = i;
          break;
        }
        case '.':
          break;
        default: {
          if (Character.isLetter(c) && Character.isUpperCase(c)) {
            stacks.get(c).push(i);
          } else if (Character.isLetter(c) && Character.isLowerCase(c)) {
            int j = stacks.get(Character.toUpperCase(c)).pop();
            result[i] = j;
            result[j] = i;
          }
        }
        }
      }
    } catch (EmptyStackException e) {
      throw new ExceptionUnmatchedClosingParentheses(i);
    }
    return result;
  }

  public static ArrayList<RNA> loadSecStrDBN(Reader r)
      throws ExceptionLoadingFailed, ExceptionPermissionDenied,
      ExceptionUnmatchedClosingParentheses, ExceptionFileFormatOrSyntax {
    boolean loadOk = false;
    ArrayList<RNA> result = new ArrayList<RNA>();
    RNA current = new RNA();
    try {
      BufferedReader fr = new BufferedReader(r);
      String line = fr.readLine();
      String title = "";
      String seqTmp = "";
      String strTmp = "";
      while ((line != null) && (strTmp.equals(""))) {
        line = line.trim();
        if (!line.startsWith(">")) {
          if (seqTmp.equals("")) {
            seqTmp = line;
          } else {
            strTmp = line;
          }
        } else {
          title = line.substring(1).trim();
        }
        line = fr.readLine();
      }
      if (strTmp.length() != 0) {
        current.setRNA(seqTmp, strTmp);
        current.setName(title);
        loadOk = true;
      }
    } catch (IOException e) {
      throw new ExceptionLoadingFailed(e.getMessage(), "");
    }
    if (loadOk) {
      result.add(current);
    }
    return result;
  }

  public static ArrayList<RNA> loadSecStr(File f)
      throws ExceptionFileFormatOrSyntax {
    try {
      return loadSecStr(new BufferedReader(new FileReader(f)),
          RNAFileType.FILE_TYPE_UNKNOWN);
    } catch (FileNotFoundException e) {
      throw new ExceptionFileFormatOrSyntax(f.toString());
    }
  }

  public static ArrayList<RNA> loadSecStr(Reader r)
      throws ExceptionFileFormatOrSyntax {
    return loadSecStr(new BufferedReader(r), RNAFileType.FILE_TYPE_UNKNOWN);
  }

  public static ArrayList<RNA> loadSecStr(BufferedReader r,
                                          RNAFileType fileType)
      throws ExceptionFileFormatOrSyntax {
    try {
      switch (fileType) {
      case FILE_TYPE_DBN: {
        try {
          ArrayList<RNA> result = loadSecStrDBN(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
        }
      }
        break;
      case FILE_TYPE_CT: {
        try {
          ArrayList<RNA> result = loadSecStrCT(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;
      case FILE_TYPE_BPSEQ: {
        try {
          ArrayList<RNA> result = loadSecStrBPSEQ(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;
      case FILE_TYPE_TCOFFEE: {
        try {
          ArrayList<RNA> result = loadSecStrTCoffee(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;
      case FILE_TYPE_STOCKHOLM: {
        try {
          ArrayList<RNA> result = loadSecStrStockholm(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;
      case FILE_TYPE_RNAML: {
        try {
          ArrayList<RNA> result = loadSecStrRNAML(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;
      case FILE_TYPE_DSSR_OUTPUT: {
        try {
          ArrayList<RNA> result = loadSecStrDSSR(r);
          if (result.size() != 0)
            return result;
        } catch (Exception e) {
          if (!isQuiet) // BH
            e.printStackTrace();
        }
      }
        break;

      case FILE_TYPE_UNKNOWN: {
        try {
          r.mark(1000000);
          RNAFactory.RNAFileType[] types = RNAFactory.RNAFileType.values();
          isQuiet = true; // BH to not report errors when
          // drag-dropping
          ArrayList<RNA> result = null;
          RNAFactory.RNAFileType t = null;
          for (int i = 0; i < types.length; i++) {
            r.reset();
            t = types[i];
            if (t != RNAFactory.RNAFileType.FILE_TYPE_UNKNOWN) {
              try {
                result = loadSecStr(r, t);
                if (result.size() != 0) {
                  break;
                }
              } catch (Exception e) {
                if (!isQuiet) // BH
                  System.err.println(e.toString());
              }
            }
          }
          System.out.println(t); // BH
          isQuiet = false; // BH
          return result;
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
      }
      throw new ExceptionFileFormatOrSyntax(
          "Couldn't parse this file as " + fileType + ".");
    } finally { // BH !!
      try {
        if (!isQuiet)
          r.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  private static ArrayList<RNA> loadSecStrDSSR(BufferedReader r)
      throws ExceptionLoadingFailed, ExceptionPermissionDenied,
      ExceptionUnmatchedClosingParentheses, ExceptionFileFormatOrSyntax {
    boolean loadOk = false;
    ArrayList<RNA> result = new ArrayList<RNA>();
    RNA current = new RNA();
    try {
      BufferedReader fr = new BufferedReader(r);
      String line = fr.readLine();
      String title = "";
      String seqTmp = "";
      String strTmp = "";
      while ((line != null) && !line.startsWith(">")) {
        line = fr.readLine();
      }
      while ((line != null) && (strTmp.equals(""))) {
        line = line.trim();
        if (!line.startsWith(">")) {
          if (seqTmp.equals("")) {
            seqTmp = line;
          } else {
            strTmp = line;
          }
        } else {
          title = line.substring(1).trim();
        }
        line = fr.readLine();
      }
      if (strTmp.length() != 0) {
        current.setRNA(seqTmp, strTmp);
        current.setName(title);
        loadOk = true;
      }

    } catch (IOException e) {
      throw new ExceptionLoadingFailed(e.getMessage(), "");
    }
    if (loadOk) {
      result.add(current);
    }
    return result;
  }

  public static RNAFileType guessFileTypeFromExtension(String path) {
    if (path.toLowerCase().endsWith("ml")) {
      return RNAFileType.FILE_TYPE_RNAML;
    }
    if (path.toLowerCase().endsWith("dbn")
        || path.toLowerCase().endsWith("faa")) {
      return RNAFileType.FILE_TYPE_DBN;
    }
    if (path.toLowerCase().endsWith("ct")) {
      return RNAFileType.FILE_TYPE_CT;
    }
    if (path.toLowerCase().endsWith("bpseq")) {
      return RNAFileType.FILE_TYPE_BPSEQ;
    }
    if (path.toLowerCase().endsWith("rfold")) {
      return RNAFileType.FILE_TYPE_TCOFFEE;
    }
    if (path.toLowerCase().endsWith("stockholm")
        || path.toLowerCase().endsWith("stk")) {
      return RNAFileType.FILE_TYPE_STOCKHOLM;
    }
    if (path.toLowerCase().indexOf("dssr") >= 0)
      return RNAFileType.FILE_TYPE_DSSR_OUTPUT;

    return RNAFileType.FILE_TYPE_UNKNOWN;

  }

  public static ArrayList<RNA> loadSecStr(String path)
      throws ExceptionExportFailed, ExceptionPermissionDenied,
      ExceptionLoadingFailed, ExceptionFileFormatOrSyntax,
      ExceptionUnmatchedClosingParentheses, FileNotFoundException {
    FileReader fr = null;
    try {
      fr = new FileReader(path);
      RNAFileType type = guessFileTypeFromExtension(path);
      ArrayList<RNA> ret = loadSecStr(new BufferedReader(fr), type);
      return ret;
    } catch (ExceptionFileFormatOrSyntax e) {
      e.setPath(path);
      throw e;
    } finally {
      if (fr != null)
        try {
          fr.close();
        } catch (IOException e2) {
        }

    }
  }

  public static ArrayList<RNA> loadSecStrStockholm(BufferedReader r)
      throws IOException, ExceptionUnmatchedClosingParentheses {
    RNAAlignment a = StockholmIO.readAlignement(r);
    return a.getRNAs();
  }

  public static ArrayList<RNA> loadSecStrBPSEQ(Reader r)
      throws ExceptionPermissionDenied, ExceptionLoadingFailed,
      ExceptionFileFormatOrSyntax {
    boolean loadOk = false;
    ArrayList<RNA> result = new ArrayList<RNA>();
    RNA current = new RNA();
    try {
      BufferedReader fr = new BufferedReader(r);
      String line = fr.readLine();
      ArrayList<String> seqTmp = new ArrayList<String>();
      Hashtable<Integer, Vector<Integer>> strTmp = new Hashtable<Integer, Vector<Integer>>();

      int bpFrom;
      String base;
      int bpTo;
      int minIndex = -1;
      boolean noWarningYet = true;
      String title = "";
      String id = "";
      String filenameStr = "Filename:";
      String organismStr = "Organism:";
      String ANStr = "Accession Number:";
      while (line != null) {
        line = line.trim();
        String[] tokens = line.split("\\s+");
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        Hashtable<Integer, Integer> numberToIndex = new Hashtable<Integer, Integer>();
        if ((tokens.length >= 3) && !tokens[0].contains("#")
            && !line.startsWith("Organism:") && !line.startsWith("Citation")
            && !line.startsWith("Filename:")
            && !line.startsWith("Accession Number:")) {
          base = tokens[1];
          seqTmp.add(base);
          bpFrom = (Integer.parseInt(tokens[0]));
          numbers.add(bpFrom);
          if (minIndex < 0)
            minIndex = bpFrom;

          if (seqTmp.size() < (bpFrom - minIndex + 1)) {
            if (noWarningYet) {
              noWarningYet = false;
              /*
               * warningEmition( "Discontinuity detected between nucleotides " +
               * (seqTmp.size()) + " and " + (bpFrom + 1) +
               * "!\nFilling in missing portions with unpaired unknown 'X' nucleotides ..."
               * );
               */
            }
            while (seqTmp.size() < (bpFrom - minIndex + 1)) {
              // System.err.println(".");
              seqTmp.add("X");
            }
          }
          for (int i = 2; i < tokens.length; i++) {
            bpTo = (Integer.parseInt(tokens[i]));
            if ((bpTo != 0) || (i != tokens.length - 1)) {
              if (!strTmp.containsKey(bpFrom))
                strTmp.put(bpFrom, new Vector<Integer>());
              strTmp.get(bpFrom).add(bpTo);
            }
          }
        } else if (tokens[0].startsWith("#")) {
          int occur = line.indexOf("#");
          String tmp = line.substring(occur + 1);
          title += tmp.trim() + " ";
        } else if (tokens[0].startsWith(filenameStr)) {
          int occur = line.indexOf(filenameStr);
          String tmp = line.substring(occur + filenameStr.length());
          title += tmp.trim();
        } else if (tokens[0].startsWith(organismStr)) {
          int occur = line.indexOf(organismStr);
          String tmp = line.substring(occur + organismStr.length());
          if (title.length() != 0) {
            title = "/" + title;
          }
          title = tmp.trim() + title;
        } else if (line.contains(ANStr)) {
          int occur = line.indexOf(ANStr);
          String tmp = line.substring(occur + ANStr.length());
          id = tmp.trim();
        }
        line = fr.readLine();
      }
      if (strTmp.size() != 0) {
        ArrayList<String> seq = seqTmp;
        int[] str = new int[seq.size()];
        for (int i = 0; i < seq.size(); i++) {
          str[i] = -1;
        }
        current.setRNA(seq, str, minIndex);
        ArrayList<ModeleBP> allbps = new ArrayList<ModeleBP>();
        for (int i : strTmp.keySet()) {
          for (int j : strTmp.get(i)) {
            if (i <= j) {
              ModeleBase mb = current.getBaseAt(i - minIndex);
              ModeleBase part = current.getBaseAt(j - minIndex);
              ModeleBP newStyle = new ModeleBP(mb, part);
              allbps.add(newStyle);
            }
          }
        }
        current.applyBPs(allbps);
        current.setName(title);
        current.setID(id);
        loadOk = true;
      }
    } catch (NumberFormatException e) {
      if (!isQuiet) // BH SwingJS
        e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      throw new ExceptionLoadingFailed(e.getMessage(), "");
    }
    if (loadOk)
      result.add(current);
    return result;
  }

  public static ArrayList<RNA> loadSecStrTCoffee(Reader r)
      throws ExceptionPermissionDenied, ExceptionLoadingFailed,
      ExceptionFileFormatOrSyntax {
    boolean loadOk = false;
    ArrayList<RNA> result = new ArrayList<RNA>();
    try {
      BufferedReader fr = new BufferedReader(r);
      String line = fr.readLine();
      ArrayList<String> seqs = new ArrayList<String>();
      ArrayList<String> ids = new ArrayList<String>();
      int numSeqs = -1;
      int currSeq = -1;
      RNA current = null;
      while (line != null) {
        if (!line.startsWith("!")) {
          String[] tokens = line.split("\\s+");
          // This may indicate new secondary structure
          if (line.startsWith("#")) {
            currSeq = Integer.parseInt(tokens[0].substring(1));
            int currSeq2 = Integer.parseInt(tokens[1]);
            // For TCoffee, a sec str is a matching between a seq and itself
            // => Disregard any alignment by filtering on the equality of sequence indices.
            if (currSeq == currSeq2) {
              current = new RNA();
              current.setName(ids.get(currSeq - 1));
              current.setSequence(seqs.get(currSeq - 1));
              result.add(current);
            } else {
              current = null;
            }
          }
          // Beginning of the file... 
          else if (current == null) {
            //... either this is the number of sequences...
            if (numSeqs < 0) {
              numSeqs = Integer.parseInt(tokens[0]);
            }
            //... or this is a sequence definition...
            else {
              String id = tokens[0];
              String seq = tokens[2];
              seqs.add(seq);
              ids.add(id);
            }
          }
          //Otherwise, this is a base-pair definition, related to the currently selected sequence
          else if (tokens.length == 3) {
            int from = Integer.parseInt(tokens[0]) - 1;
            int to = Integer.parseInt(tokens[1]) - 1;
            current.addBP(from, to);
          }
        }
        line = fr.readLine();
      }
      loadOk = true;
    } catch (NumberFormatException e) {
      if (!isQuiet) // BH SwingJS
        e.printStackTrace();
    } catch (IOException e) {
      if (!isQuiet) // BH SwingJS
        e.printStackTrace();
    }
    if (!loadOk) {
      throw new ExceptionLoadingFailed("Parse Error", "");
    }
    return result;
  }

  public static ArrayList<RNA> loadSecStrCT(Reader r)
      throws ExceptionPermissionDenied, ExceptionLoadingFailed,
      ExceptionFileFormatOrSyntax {
    boolean loadOk = false;
    ArrayList<RNA> result = new ArrayList<RNA>();
    RNA current = new RNA();
    try {
      BufferedReader fr = new BufferedReader(r);
      String line = fr.readLine();
      ArrayList<String> seq = new ArrayList<String>();
      ArrayList<String> lbls = new ArrayList<String>();
      Vector<Integer> strTmp = new Vector<Integer>();
      Vector<Integer> newStrands = new Vector<Integer>();
      int bpFrom;
      String base;
      String lbl;
      int bpTo;
      boolean noWarningYet = true;
      int minIndex = -1;
      String title = "";
      while (line != null) {
        line = line.trim();
        String[] tokens = line.split("\\s+");
        if (tokens.length >= 6) {
          try {
            bpFrom = (Integer.parseInt(tokens[0]));
            bpTo = (Integer.parseInt(tokens[4]));
            if (minIndex == -1)
              minIndex = bpFrom;
            bpFrom -= minIndex;
            if (bpTo != 0)
              bpTo -= minIndex;
            else
              bpTo = -1;
            base = tokens[1];
            lbl = tokens[5];
            int before = Integer.parseInt(tokens[2]);
            int after = Integer.parseInt(tokens[3]);

            if (before == 0 && !seq.isEmpty()) {
              newStrands.add(strTmp.size() - 1);
            }
            if (bpFrom != seq.size()) {
              if (noWarningYet) {
                noWarningYet = false;
                /*
                 * warningEmition( "Discontinuity detected between nucleotides "
                 * + (seq.size()) + " and " + (bpFrom + 1) +
                 * "!\nFilling in missing portions with unpaired unknown 'X' nucleotides ..."
                 * );
                 */
              }
              while (bpFrom > seq.size()) {
                seq.add("X");
                strTmp.add(-1);
                lbls.add("");
              }
            }
            seq.add(base);
            strTmp.add(bpTo);
            lbls.add(lbl);
          } catch (NumberFormatException e) {
            if (strTmp.size() != 0)
              e.printStackTrace();
          }
        }
        if ((line.contains("ENERGY = ")) || line.contains("dG = ")) {
          String[] ntokens = line.split("\\s+");
          if (ntokens.length >= 4) {
            String energy = ntokens[3];
            for (int i = 4; i < ntokens.length; i++) {
              title += ntokens[i] + " ";
            }
            title += "(E=" + energy + " kcal/mol)";
          }
        }
        line = fr.readLine();
      }
      if (strTmp.size() != 0) {
        int[] str = new int[strTmp.size()];
        for (int i = 0; i < strTmp.size(); i++) {
          str[i] = strTmp.elementAt(i).intValue();
        }
        current.setRNA(seq, str, minIndex);
        current.setName(title);
        for (int i = 0; i < current.getSize(); i++) {
          current.getBaseAt(i).setLabel(lbls.get(i));
        }
        for (int i : newStrands) {
          current.getBackbone().addElement(
              new ModeleBackboneElement(i, BackboneType.DISCONTINUOUS_TYPE));
        }

        loadOk = true;
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new ExceptionLoadingFailed(e.getMessage(), "");
    } catch (NumberFormatException e) {
      if (!isQuiet) // BH SwingJS
        e.printStackTrace();
      throw new ExceptionFileFormatOrSyntax(e.getMessage(), "");
    }
    if (loadOk)
      result.add(current);
    return result;
  }

}
