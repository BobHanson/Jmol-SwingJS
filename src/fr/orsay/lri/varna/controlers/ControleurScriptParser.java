package fr.orsay.lri.varna.controlers;

import java.awt.Color;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JOptionPane;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation.ChemProbAnnotationType;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.RNA;

public class ControleurScriptParser {
    private static String SCRIPT_ERROR_PREFIX = "Error";	


	private static class Command{
		Function _f;
		Vector<Argument> _argv; 
		public Command(Function f, Vector<Argument> argv)
		{
			_f = f;
			_argv = argv;
		}
	}; 

	private static abstract class Argument{
		ArgumentType _t;
		
		public Argument(ArgumentType t)
		{
			_t = t;
		}
		
		public ArgumentType getType()
		{
			return _t;
		}
		
		public abstract String toString();
		
	}; 
	

	private static class NumberArgument extends Argument{
		Number _val; 
		public NumberArgument(Number val)
		{
			super(ArgumentType.NUMBER_TYPE);
			_val = val;
		}
		public Number getNumber()
		{
			return _val;
		}
		public String toString()
		{
			return _val.toString();
		}
	}; 
	
	private static class ColorArgument extends Argument{
		Color _val; 
		public ColorArgument(Color val)
		{
			super(ArgumentType.COLOR_TYPE);
			_val = val;
		}
		public Color getColor()
		{
			return _val;
		}
		public String toString()
		{
			return _val.toString();
		}
	}; 

	private static class BooleanArgument extends Argument{
		boolean _val; 
		public BooleanArgument(boolean val)
		{
			super(ArgumentType.BOOLEAN_TYPE);
			_val = val;
		}
		public boolean getBoolean()
		{
			return _val;
		}
		public String toString()
		{
			return ""+_val;
		}
	}; 

	private static class StringArgument extends Argument{
		String _val; 
		public StringArgument(String val)
		{
			super(ArgumentType.STRING_TYPE);
			_val = val;
		}
		public String toString()
		{
			return _val.toString();
		}
	}; 

	private static class ArrayArgument extends Argument{
		Vector<Argument> _val; 
		public ArrayArgument(Vector<Argument> val)
		{
			super(ArgumentType.ARRAY_TYPE);
			_val = val;
		}
		public int getSize()
		{
			return _val.size();
		}
		public Argument getArgument(int i)
		{
			return _val.get(i);
		}
		public String toString()
		{
			return _val.toString();
		}
	}; 
	
	
	private enum ArgumentType{
		STRING_TYPE,
		NUMBER_TYPE,
		BOOLEAN_TYPE,
		ARRAY_TYPE,
		COLOR_TYPE
	};
    
    
    private enum Function{
		ADD_CHEM_PROB("addchemprob",new ArgumentType[] {ArgumentType.NUMBER_TYPE,ArgumentType.NUMBER_TYPE,ArgumentType.STRING_TYPE,ArgumentType.NUMBER_TYPE,ArgumentType.COLOR_TYPE,ArgumentType.BOOLEAN_TYPE}),
    	ERASE_SEQ("eraseseq",new ArgumentType[] {}),
		RESET_CHEM_PROB("resetchemprob",new ArgumentType[] {}),
		SET_COLOR_MAP_MIN("setcolormapminvalue",new ArgumentType[]{ArgumentType.NUMBER_TYPE}),
		SET_COLOR_MAP_MAX("setcolormapmaxvalue",new ArgumentType[]{ArgumentType.NUMBER_TYPE}),
		SET_COLOR_MAP("setcolormap",new ArgumentType[]{ArgumentType.STRING_TYPE}),
		SET_CUSTOM_COLOR_MAP("setcustomcolormap",new ArgumentType[]{ArgumentType.ARRAY_TYPE}),
		SET_SEQ("setseq",new ArgumentType[]{ArgumentType.STRING_TYPE}),
		SET_STRUCT("setstruct",new ArgumentType[]{ArgumentType.STRING_TYPE}),
		SET_STRUCT_SMOOTH("setstructsmooth",new ArgumentType[] {ArgumentType.STRING_TYPE}),
		SET_TITLE("settitle",new ArgumentType[] {ArgumentType.STRING_TYPE}),
		SET_RNA("setrna",new ArgumentType[]{ArgumentType.STRING_TYPE,ArgumentType.STRING_TYPE}),
		SET_RNA_SMOOTH("setrnasmooth",new ArgumentType[]{ArgumentType.STRING_TYPE,ArgumentType.STRING_TYPE}),
		SET_SELECTION("setselection",new ArgumentType[]{ArgumentType.ARRAY_TYPE}),
		SET_VALUES("setvalues",new ArgumentType[]{ArgumentType.ARRAY_TYPE}),
		TOGGLE_SHOW_COLOR_MAP("toggleshowcolormap",new ArgumentType[]{}),
		REDRAW("redraw",new ArgumentType[] {ArgumentType.STRING_TYPE}),
		UNKNOWN("N/A",new ArgumentType[] {});
		
		String _funName;
		ArgumentType[] _args;
		Function(String funName, ArgumentType[] args)
		{
			_funName = funName;
			_args = args;
		}
		ArgumentType[] getPrototype()
		{
			return this._args;
		}
		String getFunName()
		{
			return this._funName;
		}
		
	};
	
	private static Hashtable<String,Function> _name2Fun = new Hashtable<String,Function>();  
	private static Hashtable<Function,ArgumentType[]> _fun2Prot = new Hashtable<Function,ArgumentType[]>();
	
	
	private static void initFunctions()
	{
		if (_name2Fun.size()>0)
		{ return; }
		Function[] funs = Function.values();
		for(int i=0;i<funs.length;i++)
		{
			Function fun = funs[i];
			_name2Fun.put(fun.getFunName(),fun);
			_fun2Prot.put(fun,fun.getPrototype());
		}			
	}
	
	private static Function getFunction(String f)
	{
		String s = f.trim().toLowerCase();
		if (_name2Fun.containsKey(s))
			return _name2Fun.get(s);
		return Function.UNKNOWN;
	}

	private static ArgumentType[] getPrototype(Function f)
	{
		if (_fun2Prot.containsKey(f))
			return _fun2Prot.get(f);
		return new ArgumentType[0];
	}	
	
    public static void executeScript(VARNAPanel vp, String cmdtxt) throws Exception
    {
    	Vector<Command> cmds = parseScript(cmdtxt);
    	for(int i=0;i<cmds.size();i++)
    	{
    		Command cmd = cmds.get(i);
    		switch(cmd._f)
    		{
    			case ADD_CHEM_PROB:
    			{
    				int from = (int)((NumberArgument) cmd._argv.get(0)).getNumber().intValue();
    				int to = (int)((NumberArgument) cmd._argv.get(1)).getNumber().intValue();
    				ChemProbAnnotationType t = ChemProbAnnotation.annotTypeFromString(((StringArgument) cmd._argv.get(2)).toString());
    				double intensity = ((NumberArgument) cmd._argv.get(3)).getNumber().doubleValue();
    				Color c = ((ColorArgument) cmd._argv.get(4)).getColor();
    				boolean out = ((BooleanArgument) cmd._argv.get(5)).getBoolean();
    				vp.getRNA().addChemProbAnnotation(new ChemProbAnnotation(
    						vp.getRNA().getBaseAt(from),
    						vp.getRNA().getBaseAt(to),
    						t,
    						intensity,
    						c,
    						out));
    			}
    			break;
    			case ERASE_SEQ:
    			{
    				vp.eraseSequence();
    			}
    			break;
    			case RESET_CHEM_PROB:
    			{
    				vp.getRNA().clearChemProbAnnotations();
    				vp.repaint();
    			}
    			break;
    			case SET_COLOR_MAP_MIN:
    			{
    				vp.setColorMapMinValue(((NumberArgument) cmd._argv.get(0)).getNumber().doubleValue());
    			}
    			break;
    			case SET_COLOR_MAP_MAX:
    			{
    				vp.setColorMapMaxValue(((NumberArgument) cmd._argv.get(0)).getNumber().doubleValue());
    			}
    			break;
    			case SET_COLOR_MAP:
    			{
    				vp.setColorMap(ModeleColorMap.parseColorMap(cmd._argv.get(0).toString()));
    			}
    			break;
    			case SET_CUSTOM_COLOR_MAP:
    			{
    				ModeleColorMap cm = new ModeleColorMap();
    				//System.out.println("a"+cmd._argv.get(0));
    				ArrayArgument arg = (ArrayArgument) cmd._argv.get(0);
    				for (int j=0;j<arg.getSize();j++)
    				{
    					Argument a = arg.getArgument(j);
    					if (a._t==ArgumentType.ARRAY_TYPE)
    					{ 
    	    				//System.out.println("%");
    						ArrayArgument aarg = (ArrayArgument) a; 
    						if (aarg.getSize()==2)
    						{
    							Argument a1 = aarg.getArgument(0);
    							Argument a2 = aarg.getArgument(1);
        	    				//System.out.println("& |"+a1+"| ["+a1.getType()+"] |"+a2+"| ["+a2.getType()+"]");
    							if ((a1.getType()==ArgumentType.NUMBER_TYPE)&&(a2.getType()==ArgumentType.COLOR_TYPE))
    							{
            	    				//System.out.println("+");
    								cm.addColor(((NumberArgument)a1).getNumber().doubleValue(),((ColorArgument)a2).getColor());
    							}
    						}
    					}
    				}    				
    				vp.setColorMap(cm);
    			}
    			break;
    			case SET_RNA:
    			{
    				String seq = cmd._argv.get(0).toString();
    				String str = cmd._argv.get(1).toString();
    				vp.drawRNA(seq, str);
    			}
    			break;
    			case SET_RNA_SMOOTH:
    			{
    				String seq = cmd._argv.get(0).toString();
    				String str = cmd._argv.get(1).toString();
    				vp.drawRNAInterpolated(seq, str);
    				vp.repaint();
    			}
    			break;
    			case SET_SELECTION:
    			{
    				ArrayArgument arg = (ArrayArgument) cmd._argv.get(0);
    				ArrayList<Integer> vals = new ArrayList<Integer>();
    				for (int j=0;j<arg.getSize();j++)
    				{
    					Argument a = arg.getArgument(j);
    					if (a._t==ArgumentType.NUMBER_TYPE)
    					{ 
    						NumberArgument narg = (NumberArgument) a; 
    						vals.add(narg.getNumber().intValue()); 
    					}
    				}    				
    				vp.setSelection(vals);
    				vp.repaint();
    			}
    			break;	
    			case SET_SEQ:
    			{
    				String seq = cmd._argv.get(0).toString();
    				vp.setSequence(seq);
    			}
    			break;
    			case SET_STRUCT:
    			{
    				String seq = vp.getRNA().getSeq();
    				String str = cmd._argv.get(0).toString();
    				vp.drawRNA(seq, str);
    			}
    			break;
    			case SET_STRUCT_SMOOTH:
    			{
    				String seq = vp.getRNA().getSeq();
    				String str = cmd._argv.get(0).toString();
    				vp.drawRNAInterpolated(seq, str);
    				vp.repaint();
    			}
    			break;
    			case SET_TITLE:
    			{
    				vp.setTitle(cmd._argv.get(0).toString());
    			}
    			break;
    			case SET_VALUES:
    			{
    				ArrayArgument arg = (ArrayArgument) cmd._argv.get(0);
    				Double[] vals = new Double[arg.getSize()];
    				for (int j=0;j<arg.getSize();j++)
    				{
    					Argument a = arg.getArgument(j);
    					if (a._t==ArgumentType.NUMBER_TYPE)
    					{ 
    						NumberArgument narg = (NumberArgument) a; 
    						vals[j] = narg.getNumber().doubleValue(); 
    					}
    				}    				
    				vp.setColorMapValues(vals);
    				vp.repaint();
    			}
    			break;
    			case REDRAW:
    			{
    				int mode = -1;
    				String modeStr = cmd._argv.get(0).toString().toLowerCase(); 
    				if (modeStr.equals("radiate"))
    					mode = RNA.DRAW_MODE_RADIATE;
    				else if (modeStr.equals("circular"))
    					mode = RNA.DRAW_MODE_CIRCULAR;
    				else if (modeStr.equals("naview"))
    					mode = RNA.DRAW_MODE_NAVIEW;
    				else if (modeStr.equals("linear"))
    					mode = RNA.DRAW_MODE_LINEAR;
    				if (mode != -1)
    				  vp.drawRNA(vp.getRNA(), mode);
    			}
    			break;
    			case TOGGLE_SHOW_COLOR_MAP:
    			{
    				vp.setColorMapVisible(!vp.getColorMapVisible());
    			}
    			break;
    			default:
    				throw new Exception(SCRIPT_ERROR_PREFIX+": Method '"+cmd._f+"' unimplemented.");
    		}
    		vp.repaint();
    	}
    }

	
	
	private static Color parseColor(String s)
	{
		Color result = null;
		try {result = Color.decode(s); }
		catch (Exception e) {}
		return result;
	}

	private static Boolean parseBoolean(String s)
	{
		Boolean result = null;
		if (s.toLowerCase().equals("true"))
			result = new Boolean(true);
		if (s.toLowerCase().equals("false"))
			result = new Boolean(false);
		return result;
	}

	
	private static Vector<Argument> parseArguments(StreamTokenizer st, boolean parType) throws Exception
	{
		Vector<Argument> result = new Vector<Argument>();
		while((st.ttype!=')' && parType) || (st.ttype!=']' && !parType))
		{
			st.nextToken();
			  //System.out.println(""+ (parType?"Par.":"Bra.")+" "+(char)st.ttype);
			switch(st.ttype)
			{
			  case(StreamTokenizer.TT_NUMBER):
			  {
				  result.add(new NumberArgument(st.nval));
			  }
			  break;
			  case(StreamTokenizer.TT_WORD):
			  {
				  Color c = parseColor(st.sval);
				  if (c!=null)
				  {
					 result.add(new ColorArgument(c));
				  }
				  else
				  {
					  Boolean b = parseBoolean(st.sval);
					  if (b!=null)
					  {
						 result.add(new BooleanArgument(b));
					  }
					  else
					  {
						 result.add(new StringArgument(st.sval));					  
					  }
				  }
			  }
			  break;
			  case('"'):
			  {
				  result.add(new StringArgument(st.sval));
			  }
			  break;
			  case('['):
			  {
				  result.add(new ArrayArgument(parseArguments(st, false)));
			  }
			  break;
			  case('('):
			  {
				  result.add(new ArrayArgument(parseArguments(st, true)));
			  }
			  break;
			  case(')'):
			  {
				  if (parType)
				    return result;
				  else
					throw new Exception(SCRIPT_ERROR_PREFIX+": Opening "+(parType?"parenthesis":"bracket")+" matched with a closing "+(!parType?"parenthesis":"bracket"));					  
			  }
			  case(']'):
			  {
				  if (!parType)
				    return result;
				  else
					throw new Exception(SCRIPT_ERROR_PREFIX+": Opening "+(parType?"parenthesis":"bracket")+" matched with a closing "+(!parType?"parenthesis":"bracket"));					  
			  }
			  case(','):
				  break;
			  case(StreamTokenizer.TT_EOF):
			  {
				  throw new Exception(SCRIPT_ERROR_PREFIX+": Unmatched opening "+(parType?"parenthesis":"bracket"));
			  }
			  
			}
		}
		return result;
	}
	
	
	private static Command parseCommand(String cmd) throws Exception
	{
		int cut = cmd.indexOf("(");
		if (cut==-1)
		{
			throw new Exception(SCRIPT_ERROR_PREFIX+": Syntax error");
		}
		String fun = cmd.substring(0,cut);
		Function f  = getFunction(fun);
		if (f==Function.UNKNOWN)
		{ throw new Exception(SCRIPT_ERROR_PREFIX+": Unknown function \""+fun+"\""); }
		StreamTokenizer st = new StreamTokenizer(new StringReader(cmd.substring(cut+1)));
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
		Vector<Argument> argv = parseArguments(st,true);
		checkArgs(f,argv);
		Command result = new Command(f,argv); 
		return result;
	}
	
	private static boolean checkArgs(Function f, Vector<Argument> argv) throws Exception
	{
		ArgumentType[] argtypes = getPrototype(f);
		if (argtypes.length!=argv.size())
			throw new Exception(SCRIPT_ERROR_PREFIX+": Wrong number of argument for function \""+f+"\".");
		for (int i=0;i<argtypes.length;i++)
		{
			if (argtypes[i] != argv.get(i)._t)
			{
				throw new Exception(SCRIPT_ERROR_PREFIX+": Bad type ("+argtypes[i]+"!="+argv.get(i)._t+") for argument #"+(i+1)+" in function \""+f+"\".");
			}
		}
		return true;
	}
	
	private static Vector<Command> parseScript(String cmd) throws Exception
	{
		initFunctions();
		Vector<Command> cmds = new Vector<Command>();
		String[] data = cmd.split(";");
		for (int i=0;i<data.length;i++)
		{
			cmds.add(parseCommand(data[i].trim()));
		}
		return cmds;
	}
	
	
}
