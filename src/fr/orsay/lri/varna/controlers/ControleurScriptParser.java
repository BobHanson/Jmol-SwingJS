package fr.orsay.lri.varna.controlers;

import java.awt.Color;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation.ChemProbAnnotationType;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.utils.XMLUtils;

/**
 * A class to parse VARNA scripts, which are semicolon-separated strings of
 * commands of the form
 * 
 * command(arg arg arg)
 * 
 * Arguments are of the following types:
 * 
 * <code>
    BOOLEAN  true/false
    NUMBER   
    STRING   xxxx or "xxxx"
    COLOR    #00FF00
    NULL     NULL (with no quotes)
 * </code>
 *
 * NULL was added for the setSelectionColors command, introduced for
 * Jmol-SwingJS
 * 
 * I cannot find any documentation on this command scripting. But it is pretty
 * clear what most of the commands do. All are very simple syntaxes.
 * 
 * Commands include:
 * 
 * <code>
    
    addChemProb(NUMBER firstBase, NUMBER lastBase, STRING type, NUMBER intensity, COLOR color, BOOLEAN outward)
      firstBase/lastBase    range indices
      type                  one of     TRIANGLE, ARROW, PIN, DOT
      intensity             thickness of lines (relative to 1)
      color                 color for thi annotation
      outward               position of the annotation (?)

    eraseSeq()
    
    redraw()
    
    resetChemProb()
    
    setColorMap(STRING mapName) 
      mapName is either one of RED, BLUE, GREEN, HEAT, ENERGY, ROCKNROLL, VIENNA, BW,
              or a single string consisting of "v:c v:c v:c..." where v is a decimal value
              and c is a hex color starting with "#". For example, "0:#ff0000 1:#0000FF"
    
    setColorMapMaxValue(NUMBER)

    setColorMapMinValue(NUMBER)
    
    setCustomColormMp(ARRAY colorValues)
      colorVaues is an array of [value,color], such as [0.5, #FF0000]
    
    setrna(STRING sequence, STRING structure)
    
    setrnasmooth(STRING sequence, STRING structure)
      same as setrna, except the contruction is animated
    
    setselection(ARRAY selection)
      selection is either an array of index starting with 0 : [0,3,6,...]
      or an array starting with "[resno" indicating these numbers are residue numbers
    
    setselectioncolors(COLOR selected or NULL, COLOR unselected or NULL)
      selected color for selected bases; NULL to not color selected
      unselected color for unselected bases; NULL to not color unselected bases
    
    setseq(STRING)
    
    setstruct(STRING)
    
    setstructsmooth(STRING)
    
    settitle(STRING)
    
    setvalues(ARRAY values)
      values is an array of property values that will be color-mapped.
      If this is shorter than the number of bases, then missing residues will be given the value 0.

    showColorMap(BOOLEAN doShow)
      doShow true or missing to show the color map; FALSE to hide it
    
    toggleShowColormap()
      toggle the color map on or off

    write(STRING filename, NUMBER width, NUMBER height, NUMBER resolution, NUMBER quality)
      filename with extension indicating type
      width,height dimensions of the image (optional), -1 or absent for "same as current
      resolution (default 1)
      quality (?)
 </code>
 * 
 * 
 */
public class ControleurScriptParser {
  private final static String SCRIPT_ERROR_PREFIX = "VARNA Error";

  private static class Command {
    CommandFunction _f;
    Vector<Argument> _argv;

    public Command(CommandFunction f, Vector<Argument> argv) {
      _f = f;
      _argv = argv;
    }
    
    @Override
    public String toString() {
      String s = _f.toString();
      for (int i = 0; i < _argv.size(); i++)
        s += " " + _argv.get(i);
      return s;
    }

    protected Argument getArgument(int i) {
      return (i < _argv.size() ? _argv.get(i) : null);
    }
  }

  private static abstract class Argument {
    ArgumentType _t;

    public Argument(ArgumentType t) {
      _t = t;
    }

    public ArgumentType getType() {
      return _t;
    }

    @Override
    public abstract String toString();

  }

  private static class NullArgument extends Argument {

    public NullArgument() {
      super(null);
    }

    @Override
    public String toString() {
      return "NULL";
    }
  }

  protected final static NullArgument NULL = new NullArgument();

  private static class NumberArgument extends Argument {
    Number _val;

    public NumberArgument(Number val) {
      super(ArgumentType.NUMBER_TYPE);
      _val = val;
    }

    public double doubleValue() {
      return _val.doubleValue();
    }

    public int intValue() {
      return (int) _val.doubleValue();
    }

    @Override
    public String toString() {
      return _val.toString();
    }
  }

  private static class ColorArgument extends Argument {
    Color _val;

    public ColorArgument(Color val) {
      super(ArgumentType.COLOR_TYPE);
      _val = val;
    }

    public Color getColor() {
      return _val;
    }

    @Override
    public String toString() {
      return _val.toString();
    }
  }

  private static class BooleanArgument extends Argument {
    boolean _val;

    public BooleanArgument(boolean val) {
      super(ArgumentType.BOOLEAN_TYPE);
      _val = val;
    }

    public boolean getBoolean() {
      return _val;
    }

    @Override
    public String toString() {
      return "" + _val;
    }
  }

  private static class StringArgument extends Argument {
    String _val;

    public StringArgument(String val) {
      super(ArgumentType.STRING_TYPE);
      _val = val;
    }

    @Override
    public String toString() {
      return _val;
    }
  }

  private static class ArrayArgument extends Argument {
    Vector<Argument> _val;

    public ArrayArgument(Vector<Argument> val) {
      super(ArgumentType.ARRAY_TYPE);
      _val = val;
    }

    public int getSize() {
      return _val.size();
    }

    public Argument getArgument(int i) {
      return _val.get(i);
    }

    @Override
    public String toString() {
      return _val.toString();
    }
  }

  private enum ArgumentType {
    STRING_TYPE, NUMBER_TYPE, BOOLEAN_TYPE, ARRAY_TYPE, COLOR_TYPE
  }

  private enum CommandFunction {
    WRITE("write", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE, // file
            ArgumentType.NUMBER_TYPE, // width
            ArgumentType.NUMBER_TYPE, // height
            ArgumentType.NUMBER_TYPE, // resolution
            ArgumentType.NUMBER_TYPE, // quality
        }, 1), //
    ADD_CHEM_PROB("addchemprob", //
        new ArgumentType[] { //
            ArgumentType.NUMBER_TYPE, //
            ArgumentType.NUMBER_TYPE, //
            ArgumentType.STRING_TYPE, //
            ArgumentType.NUMBER_TYPE, // 
            ArgumentType.COLOR_TYPE, //
            ArgumentType.BOOLEAN_TYPE //
        }, 2), //
    ERASE_SEQ("eraseseq", //
        new ArgumentType[] {}, 0), //
    RESET_CHEM_PROB("resetchemprob", //
        new ArgumentType[] {}, 0), //
    REDRAW("redraw", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_COLOR_MAP_MIN("setcolormapminvalue", //
        new ArgumentType[] { //
            ArgumentType.NUMBER_TYPE //
        }, 1), //
    SET_COLOR_MAP_MAX("setcolormapmaxvalue", new ArgumentType[] { //
        ArgumentType.NUMBER_TYPE //
    }, 1), //
    SET_COLOR_MAP("setcolormap", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_CUSTOM_COLOR_MAP("setcustomcolormap", //
        new ArgumentType[] { //
            ArgumentType.ARRAY_TYPE //
        }, 1), //
    SET_SEQ("setseq", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_STRUCT("setstruct", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_STRUCT_SMOOTH("setstructsmooth", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_TITLE("settitle", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE //
        }, 1), //
    SET_RNA("setrna", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE, //
            ArgumentType.STRING_TYPE //
        }, 2), //
    SET_RNA_SMOOTH("setrnasmooth", //
        new ArgumentType[] { //
            ArgumentType.STRING_TYPE, //
            ArgumentType.STRING_TYPE //
        }, 2), //
    SET_SELECTION("setselection", //
        new ArgumentType[] { //
            ArgumentType.ARRAY_TYPE //
        }, 1), //
    SET_SELECTION_COLORS("setselectioncolors", //
        new ArgumentType[] { //
            ArgumentType.COLOR_TYPE, //
            ArgumentType.COLOR_TYPE //
        }, 0), //
    SET_VALUES("setvalues", //
        new ArgumentType[] { //
            ArgumentType.ARRAY_TYPE //
        }, 1), //
    SHOW_COLOR_MAP("showcolormap", //
        new ArgumentType[] { //
            ArgumentType.BOOLEAN_TYPE //
        }, 0), //
    TOGGLE_SHOW_COLOR_MAP("toggleshowcolormap", //
        new ArgumentType[] {}, 1), //
    UNKNOWN("N/A", new ArgumentType[] {}, 0);

    protected String _funName;
    protected ArgumentType[] _args;
    protected int minArgCount;

    private static Hashtable<String, CommandFunction> _name2Fun = new Hashtable<String, CommandFunction>();

    CommandFunction(String funName, ArgumentType[] args, int minArgCount) {
      _funName = funName;
      _args = args;
      this.minArgCount = minArgCount;
    }

    public static CommandFunction fromString(String cmd) {
      CommandFunction func = _name2Fun.get(cmd.trim().toLowerCase());
      return (func == null ? CommandFunction.UNKNOWN : func);
    }

    public static void initFunctions() {
      if (_name2Fun.size() > 0) {
        return;
      }
      CommandFunction[] funs = CommandFunction.values();
      for (int i = 0; i < funs.length; i++) {
        CommandFunction fun = funs[i];
        _name2Fun.put(fun._funName, fun);
      }
    }

    public void checkArgs(Vector<Argument> givenArguments) throws Exception {
      int nArgs = givenArguments.size();
      if (nArgs < minArgCount)
        throw new Exception(SCRIPT_ERROR_PREFIX
            + ": Wrong number of argument for function \"" + _funName + "\"." + givenArguments);
      int i = 0;
      for (; i < nArgs; i++) {
        Argument given = givenArguments.get(i);
        if (given != NULL && _args[i] != given._t) {
          throw new Exception(SCRIPT_ERROR_PREFIX + ": Bad type (" + _args[i]
              + "!=" + givenArguments.get(i)._t + ") for argument #" + (i + 1)
              + " in function \"" + _funName + "\".");
        }
      }
      for (; i < _args.length; i++) {
        givenArguments.add(NULL);
      }
    }

  }

  public static void executeScript(VARNAPanel vp, String cmdtxt)
      throws Exception {
    Vector<Command> cmds = parseScript(cmdtxt);
    for (int i = 0; i < cmds.size(); i++) {
      Command cmd = cmds.get(i);

      switch (cmd._f) {
      case ADD_CHEM_PROB: {
        int firstBase = getInt(cmd.getArgument(0));
        int lastBase = getInt(cmd.getArgument(1));
        ChemProbAnnotationType t = ChemProbAnnotationType
            .annotTypeFromString(cmd.getArgument(2).toString());
        double intensity = getDouble(cmd.getArgument(3),
            ChemProbAnnotation.DEFAULT_INTENSITY);
        Color c = getColor(cmd.getArgument(4),
            ChemProbAnnotation.DEFAULT_COLOR);
        boolean outward = getBoolean(cmd.getArgument(5), true);
        vp.getRNA().addChemProbAnnotation(
            new ChemProbAnnotation(vp.getRNA().getBaseAt(firstBase),
                vp.getRNA().getBaseAt(lastBase), t, intensity, c, outward));
      }
        break;
      case ERASE_SEQ: {
        vp.eraseSequence();
      }
        break;
      case RESET_CHEM_PROB: {
        vp.getRNA().clearChemProbAnnotations();
        vp.repaint();
      }
        break;
      case SET_COLOR_MAP_MIN: {
        vp.setColorMapMinValue(getDouble(cmd.getArgument(0), 0));
      }
        break;
      case SET_COLOR_MAP_MAX: {
        vp.setColorMapMaxValue(getDouble(cmd.getArgument(0), 0));
      }
        break;
      case SET_COLOR_MAP: {
        vp.setColorMap(
            ModeleColorMap.parseColorMap(getString(cmd.getArgument(0))));
      }
        break;
      case SET_CUSTOM_COLOR_MAP: {
        ModeleColorMap cm = new ModeleColorMap();
        //System.out.println("a"+cmd.getArgument(0));
        ArrayArgument arg = (ArrayArgument) cmd.getArgument(0);
        for (int j = 0; j < arg.getSize(); j++) {
          Argument a = arg.getArgument(j);
          if (a._t == ArgumentType.ARRAY_TYPE) {
            //System.out.println("%");
            ArrayArgument aarg = (ArrayArgument) a;
            if (aarg.getSize() == 2) {
              Argument a1 = aarg.getArgument(0);
              Argument a2 = aarg.getArgument(1);
              //System.out.println("& |"+a1+"| ["+a1.getType()+"] |"+a2+"| ["+a2.getType()+"]");
              if ((a1.getType() == ArgumentType.NUMBER_TYPE)
                  && (a2.getType() == ArgumentType.COLOR_TYPE)) {
                //System.out.println("+");
                cm.addColor(getDouble(a1, 0), getColor(a2, null));
              }
            }
          }
        }
        vp.setColorMap(cm);
      }
        break;
      case SET_RNA: {
        String seq = cmd.getArgument(0).toString();
        String str = cmd.getArgument(1).toString();
        vp.setRNA(seq, str);
      }
        break;
      case SET_RNA_SMOOTH: {
        // do the interpolation
        String seq = cmd.getArgument(0).toString();
        String str = cmd.getArgument(1).toString();
        vp.drawRNAInterpolated(seq, str);
        vp.repaint();
      }
        break;
      case SET_SELECTION: {
        ArrayArgument arg = (ArrayArgument) cmd.getArgument(0);
        ArrayList<Integer> vals = new ArrayList<Integer>();
        boolean byResidueNumber = false;
        // accepts setSelection([resno 2 3 4])
        // to indicate that these are PDB residue numbers
        for (int j = 0; j < arg.getSize(); j++) {
          Argument a = arg.getArgument(j);
          switch (a._t) {
          case NUMBER_TYPE:
            NumberArgument narg = (NumberArgument) a;
            vals.add(Integer.valueOf(narg.intValue()));
            break;
          case STRING_TYPE:
            if ("resno".equals(getString(a)))
              byResidueNumber = true;
            break;
          case ARRAY_TYPE:
          case BOOLEAN_TYPE:
          case COLOR_TYPE:
            // ignore or error?
            break;
          }
        }
        if (byResidueNumber)
          vp.doSelectBasesByResno(vals);
        else
          vp.setSelection(vals);
        vp.repaint();
      }
        break;
      case SET_SELECTION_COLORS: {
        Color colorSelected = getColor(cmd.getArgument(0), null);
        Color colorUnselected = getColor(cmd.getArgument(1), null);
        vp.setSelectionColors(colorSelected, colorUnselected);
      }
        break;
      case SET_SEQ: {
        String seq = cmd.getArgument(0).toString();
        vp.setSequence(seq);
      }
        break;
      case SET_STRUCT: {
        String seq = vp.getRNA().getSeq();
        String str = cmd.getArgument(0).toString();
        vp.setRNA(seq, str);
      }
        break;
      case SET_STRUCT_SMOOTH: {
        String seq = vp.getRNA().getSeq();
        String str = cmd.getArgument(0).toString();
        vp.drawRNAInterpolated(seq, str);
        vp.repaint();
      }
        break;
      case SET_TITLE: {
        vp.setTitle(cmd.getArgument(0).toString());
      }
        break;
      case SET_VALUES: {
        ArrayArgument arg = (ArrayArgument) cmd.getArgument(0);
        double[] vals = new double[arg.getSize()];
        for (int j = 0; j < arg.getSize(); j++) {
          Argument a = arg.getArgument(j);
          if (a._t == ArgumentType.NUMBER_TYPE) {
            vals[j] = getDouble(a, 0);
          }
        }
        vp.setColorMapValues(vals);
        vp.repaint();
      }
        break;
      case REDRAW: {
        int mode = -1;
        String modeStr = cmd.getArgument(0).toString().toLowerCase();
        if (modeStr.equals("radiate"))
          mode = RNA.DRAW_MODE_RADIATE;
        else if (modeStr.equals("circular"))
          mode = RNA.DRAW_MODE_CIRCULAR;
        else if (modeStr.equals("naview"))
          mode = RNA.DRAW_MODE_NAVIEW;
        else if (modeStr.equals("linear"))
          mode = RNA.DRAW_MODE_LINEAR;
        if (mode != -1)
          vp.setRNA(vp.getRNA(), mode);
      }
        break;
      case SHOW_COLOR_MAP:
        boolean doShow = getBoolean(cmd.getArgument(0), true);
        vp.setColorMapVisible(doShow);
      break;
      case TOGGLE_SHOW_COLOR_MAP:
        vp.setColorMapVisible(!vp.getColorMapVisible());
        break;
      default:
        throw new Exception(
            SCRIPT_ERROR_PREFIX + ": Method '" + cmd._f + "' unimplemented.");
      }
      vp.repaint();
    }
  }

  private static int getInt(Argument a) {
    return (a == NULL ? 0 : ((NumberArgument) a).intValue());
  }

  private static boolean getBoolean(Argument a, boolean def) {
    return (a == NULL ? def : ((BooleanArgument) a).getBoolean());
  }

  private static double getDouble(Argument a,
                                  double defaultIntensity) {
    return (a == NULL ? defaultIntensity : ((NumberArgument) a).doubleValue());
  }

  private static Color getColor(Argument a, Color defaultColor) {
    return (a == NULL ? defaultColor : ((ColorArgument) a).getColor());
  }

  private static String getString(Argument a) {
    return (a == NULL ? null : ((StringArgument) a).toString());
  }

  private static Boolean parseBoolean(String s) {
    switch (s.toLowerCase()) {
    case "true":
      return Boolean.TRUE;
    case "false":
      return Boolean.FALSE;
    default:
      return null;
    }
  }

  private static Vector<Argument> parseArguments(StreamTokenizer st,
                                                 boolean parType)
      throws Exception {
    Vector<Argument> result = new Vector<Argument>();
    while ((st.ttype != ')' && parType) || (st.ttype != ']' && !parType)) {
      st.nextToken();
      //System.out.println(""+ (parType?"Par.":"Bra.")+" "+(char)st.ttype);
      switch (st.ttype) {
      case (StreamTokenizer.TT_NUMBER):
        result.add(new NumberArgument(Double.valueOf(st.nval)));
        break;
      case (StreamTokenizer.TT_WORD):
        if (st.sval.equals("NULL")) {
          result.add(NULL);
        } else {
          // note that "0x....." is not valid here, as it is broken into "0" and "x...."
          // only "#" (parser would also take "0..." for an octal, but...)
          Color c = (st.sval.startsWith("#") ? XMLUtils.colorFromHTML(st.sval) : null);
          if (c != null) {
            result.add(new ColorArgument(c));
          } else {
            Boolean b = parseBoolean(st.sval);
            if (b != null) {
              result.add(new BooleanArgument(b.booleanValue()));
            } else {
              result.add(new StringArgument(st.sval));
            }
          }
        }
        break;
      case ('"'):
        result.add(new StringArgument(st.sval));
        break;
      case ('['):
        result.add(new ArrayArgument(parseArguments(st, false)));
        break;
      case ('('):
        result.add(new ArrayArgument(parseArguments(st, true)));
        break;
      case (')'):
        if (!parType)
          parenthesisError("bracket");
        return result;
      case (']'):
        if (parType)
          parenthesisError("parenthesis");
        return result;
      case (','):
        break;
      case (StreamTokenizer.TT_EOF):
        throw new Exception(SCRIPT_ERROR_PREFIX + ": Unmatched opening "
            + (parType ? "parenthesis" : "bracket"));
      }
    }
    return result;
  }

  private static void parenthesisError(String type) throws Exception {
    throw new Exception(SCRIPT_ERROR_PREFIX + ": Opening " + type
        + " matched with a closing " + type);
  }

  private static Command parseCommand(String cmd) throws Exception {
    int cut = cmd.indexOf("(");
    if (cut == -1) {
      throw new Exception(SCRIPT_ERROR_PREFIX + ": Syntax error for '" + cmd + "'");
    }
    String fun = cmd.substring(0, cut);
    CommandFunction f = CommandFunction.fromString(fun);
    if (f == CommandFunction.UNKNOWN) {
      throw new Exception(
          SCRIPT_ERROR_PREFIX + ": Unknown function \"" + fun + "\"");
    }
    StreamTokenizer st = new StreamTokenizer(
        new StringReader(cmd.substring(cut + 1)));
    st.eolIsSignificant(false);
    st.parseNumbers();
    st.quoteChar('\"');
    st.ordinaryChar('=');
    st.ordinaryChar(',');
    st.ordinaryChar('[');
    st.ordinaryChar(']');
    st.ordinaryChar('(');
    st.ordinaryChar(')');
    st.wordChars('#', '#');
    Vector<Argument> argv = parseArguments(st, true);
    f.checkArgs(argv);
    Command result = new Command(f, argv);
    return result;
  }

  private static Vector<Command> parseScript(String cmd) throws Exception {
    CommandFunction.initFunctions();
    Vector<Command> cmds = new Vector<Command>();
    String[] data = cmd.split(";");
    for (int i = 0; i < data.length; i++) {
      cmds.add(parseCommand(data[i].trim()));
    }
    return cmds;
  }

  public static void main(String[] args) {
    try {
      Vector<Command> c = parseScript("setSelectionColors(#0000FF,NULL)");
      System.out.println(c);
      // [SET_SELECTION_COLORS java.awt.Color[r=0,g=0,b=255] NULL]
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
