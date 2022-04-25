
/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2020-12-16 08:50:41 -0600 (Wed, 16 Dec 2020) $
 * $Revision: 22052 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.script;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.AU;
import javajs.util.Lst;

import org.jmol.util.Logger;

/**
 * 
 * Script token class.
 * 
 */
public class T {
  public int tok;
  public Object value;
  public int intValue = Integer.MAX_VALUE;
  
  public final static T t(int tok) {
    T token = new T();
    token.tok = tok;
    return token;
  }
 
  public final static T tv(int tok, int intValue, Object value) {
    T token = t(tok);
    token.intValue = intValue;
    token.value = value;
    return token;
  }
 
  public final static T o(int tok, Object value) {
    T token = t(tok);
    token.value = value;
    return token;
  }

  public final static T n(int tok, int intValue) {
    T token = t(tok);
    token.intValue = intValue;
    return token;
  }

  public final static T i(int intValue) {
    T token = t(integer);
    token.intValue = intValue;
    return token;
  }

  public final static String[] astrType;
  
  static {
    astrType = "nada identifier integer decimal string inscode hash array point point4 bitset matrix3f matrix4f array hash bytearray keyword".split(" ");    
  }

  public final static int nada       =  0;
  public final static int integer    =  2;
  public final static int decimal    =  3;
  public final static int string     =  4;
  
  public final static int inscode    =  5; 
  public final static int hash       =  6;  // associative array; Hashtable
  public final static int varray     =  7;  // List<ScriptVariable>
  public final static int point3f    =  8;
  public final static int point4f    =  9;  
  public final static int bitset     =  10;
  
  public final static int matrix3f   = 11;  
  public final static int matrix4f   = 12;  
  // listf "list-float" is specifically for xxx.all.bin, 
  // but it could be developed further
  public final static int listf      = 13;     
  public final static int context    = 14;
  public final static int barray     = 15; // byte array
  final private static int keyword   = 16;
  

  public static boolean tokAttr(int a, int b) {
    return (a & b) == (b & b);
  }
  
  public static boolean tokAttrOr(int a, int b1, int b2) {
    return (a & b1) == (b1 & b1) || (a & b2) == (b2 & b2);
  }
  
 

  // TOKEN BIT FIELDS
  
  // first 9 bits are generally identifier bits
  // or bits specific to a type
  
  /* bit flags:
   * 
   * parameter bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *    |   |   |   |   |   |   |     
   *  x                  xxxxxxxxxxx setparam  "set THIS ...."
   *  x     x                        strparam
   *  x    x                         intparam
   *  x   x                          floatparam
   *  x  x                           booleanparam
   * xx                              deprecatedparam
   * x                   xxxxxxxxxxx misc
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *                   x             sciptCommand
   *                  xx             atomExpressionCommand
   *                 x x           o implicitStringCommand (parsing of @{x})
   *                 x x           x implicitStringCommand (no initial parsing of @{x})
   *                x  x             mathExpressionCommand
   *               xx  x             flowCommand
   *              x    x             shapeCommand
   *             x                   noArgs
   *            x                    defaultON
   *                     xxxxxxxxxxx uniqueID (may include math flags)
   * 
   *              
   * math bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *    FFFF    FFFF    FFFF    FFFF
   *          x                      predefined set
   * x       x                       atomproperty
   * x      xx                       strproperty
   * x     x x                       intproperty
   * x    x  x                       floatproperty
   * x   x                           mathproperty
   *    x                            mathfunc
   *        
   *        
   *                           xxxxx unique id 1 to 0x1F (31)
   *                          x      min
   *                         x       max
   *                         xx      average
   *                        x        sum
   *                        x x      sum2
   *                        xx       stddev
   *                        xxx      selectedfloat  (including just the atoms selected)
   *                       x         allfloat (including all atoms, not just selected)
   *                       x???      [available] 
   *                       xxxx      minmaxmask (all)
   *                     xx          maximum number of parameters for function
   *                    x            settable
   *                   
   * 3         2         1         0
   * 0987654321098765432109876543210
   *   x       x                     mathop
   *   x       x           x         comparator
   *                            xxxx unique id (0 to 15)
   *                        xxxx     precedence
   *
   *                        
   * 
   */
   
  //
  // parameter bit flags
  //
  
  public final static int setparam          = 1 << 29; // parameter to set command
  public final static int misc              = 1 << 30; // misc parameter
  public final static int deprecatedparam   = setparam | misc;
  
  public final static int identifier =  misc;

  public final static int scriptCommand            = 1 << 12;
  
  // the command assumes an atom expression as the first parameter
  // -- center, define, delete, display, hide, restrict, select, subset, zap
  public final static int atomExpressionCommand  = (1 << 13) | scriptCommand;
  
  // this implicitString flag indicates that then entire command is an implied quoted string  
  // -- ODD echo, hover, label, message, pause  -- do NOT parse variables the same way
  // -- EVEN help, javascript, cd, gotocmd -- allow for single starting variable
  public final static int implicitStringCommand     = (1 << 14) | scriptCommand;
  
  // this mathExpression flag indicates that 
  // the command evaluates a math expression. 
  // -- elseif, forcmd, ifcmd, print, returncmd, set, var, whilecmd
  public final static int mathExpressionCommand = (1 << 15) | scriptCommand;
  
  // program flow commands include:
  // -- breakcmd, continuecmd, elsecmd, elseif, end, endifcmd, switch, case, 
  //    forcmd, function, ifcmd, whilecmd
  public final static int flowCommand    = (1 << 16) | mathExpressionCommand;

  // these commands will be handled specially
  public final static int shapeCommand   = (1 << 17) | scriptCommand;

  // Command argument compile flags
  
  public final static int noArgs         = 1 << 18;
  public final static int defaultON      = 1 << 19;
  
  public final static int predefinedset = (1 << 21);
  
  public final static int atomproperty  = (1 << 22) | misc; 
  // all atom properties are either a member of one of the next three groups,
  // or they are a point/vector, in which case they are just atomproperty
  public final static int strproperty   = (1 << 23) | atomproperty; // string property
  public final static int intproperty   = (1 << 24) | atomproperty; // int parameter
  public final static int floatproperty = (1 << 25) | atomproperty; // float parameter

  public final static int PROPERTYFLAGS = strproperty | intproperty | floatproperty; // includes point-type

  // parameters that can be set using the SET command
  public final static int strparam   = (1 << 23) | setparam; // string parameter
  public final static int intparam   = (1 << 24) | setparam; // int parameter
  public final static int floatparam = (1 << 25) | setparam; // float parameter
  public final static int booleanparam = (1 << 26) | setparam; // boolean parameter
  public final static int paramTypes = strparam | intparam | floatparam | booleanparam;
  
  // note: the booleanparam and the mathproperty bits are the same, but there is no
  //       conflict because mathproperty is only checked in ScriptEvaluator.getBitsetProperty
  //       meaning it is coming after a "." as in {*}.min
  
  public final static int mathproperty         = (1 << 26) | misc; // {xxx}.nnnn
  public final static int mathfunc             = (1 << 27);  
  public final static int mathop               = (1 << 28);
  public final static int comparator           = mathop | (1 << 8);
  
  public final static int center       = 1 | atomExpressionCommand;
  public final static int define       = 2 | atomExpressionCommand;
  public final static int delete       = 3 | atomExpressionCommand;
  public final static int display      = 4 | atomExpressionCommand | deprecatedparam;
  public final static int fixed        = 5 | atomExpressionCommand; // Jmol 12.0.RC15
  public final static int hide         = 6 | atomExpressionCommand;
  public final static int restrict     = 7 | atomExpressionCommand;
//public final static int select       see mathfunc
  public final static int subset       = 8 | atomExpressionCommand | predefinedset;
  public final static int zap          = 9 | atomExpressionCommand;

  public final static int print        = 1 | mathExpressionCommand;
  public final static int returncmd    = 2 | mathExpressionCommand;
  public final static int set          = 3 | mathExpressionCommand;
  public final static int var          = 4 | mathExpressionCommand;
  public final static int log          = 5 | mathExpressionCommand;
  public final static int throwcmd     = 6 | mathExpressionCommand;
  //public final static int prompt     see mathfunc
  
  public final static int echo         = 1 /* must be odd */ | implicitStringCommand | shapeCommand | setparam;
  public final static int help         = 2 /* must be even */ | implicitStringCommand;
  public final static int hover        = 3 /* must be odd */ | implicitStringCommand | defaultON;
//public final static int javascript   see mathfunc
//public final static int label        see mathfunc
  public final static int message      = 5 /* must be odd */ | implicitStringCommand;
  public final static int pause        = 7 /* must be odd */ | implicitStringCommand;
  public final static int scale        = 1 | scriptCommand | setparam;

  //these commands control flow
  //sorry about GOTO!
//public final static int function     see mathfunc
//public final static int ifcmd        see mathfunc
  public final static int elseif       = 2 | flowCommand;
  public final static int elsecmd      = 3 | flowCommand | noArgs;
  public final static int endifcmd     = 4 | flowCommand | noArgs;
//public final static int forcmd       see mathfunc
  public final static int whilecmd     = 6 | flowCommand;
  public final static int breakcmd     = 7 | flowCommand;
  public final static int continuecmd  = 8 | flowCommand;
  public final static int end          = 9 | flowCommand;
  public final static int switchcmd    = 10 | flowCommand;
  public final static int casecmd      = 11 | flowCommand;
  public final static int catchcmd     = 12 | flowCommand;
  public final static int defaultcmd   = 13 | flowCommand;
  public final static int trycmd       = 14 | flowCommand | noArgs;
  
  public final static int animation    = scriptCommand | 1;
  public final static int assign       = scriptCommand | 2;
  public final static int background   = scriptCommand | 3 | deprecatedparam;
  public final static int bind         = scriptCommand | 4;
  public final static int bondorder    = scriptCommand | 5;
  public final static int calculate    = scriptCommand | 6;
//public final static int cache  see mathfunc
  public final static int capture      = scriptCommand | 7;
  public final static int cd           = scriptCommand | 8 /* must be even */| implicitStringCommand; // must be even
  public final static int centerat     = scriptCommand | 9;
//public final static int color  see intproperty
//public final static int configuration see intproperty
  public final static int connect      = scriptCommand | 10;
  public final static int console      = scriptCommand | 11 | defaultON;
//public final static int data  see mathfunc
  public final static int delay        = scriptCommand | 13 | defaultON;
  public final static int depth        = scriptCommand | 14 | intparam | defaultON;
  public final static int exit         = scriptCommand | 15 | noArgs;
  public final static int exitjmol     = scriptCommand | 16 | noArgs;
//public final static int file  see intproperty
  public final static int font         = scriptCommand | 18;
  public final static int frame        = scriptCommand | 19;
//public final static int getproperty  see mathfunc
  public final static int gotocmd      = scriptCommand | 20 /*must be even*/| implicitStringCommand;
  public final static int hbond        = scriptCommand | 22 | deprecatedparam | predefinedset | defaultON;
  public final static int history      = scriptCommand | 23 | deprecatedparam;
  public final static int image        = scriptCommand | 24;
  public final static int initialize   = scriptCommand | 25;
  public final static int invertSelected = scriptCommand | 26;
//public final static int load   see mathfunc
  public final static int loop         = scriptCommand | 27 | defaultON;
  public final static int macro        = scriptCommand | 28;
  public final static int mapproperty  = scriptCommand | 29;
  public final static int minimize     = scriptCommand | 30;
  public final static int modelkitmode = scriptCommand | booleanparam | 31;  // 12.0.RC15

//public final static int model  see mathfunc
//public final static int measure   see mathfunc
  public final static int move         = scriptCommand | 32;
  public final static int moveto       = scriptCommand | 33;
  public final static int mutate       = scriptCommand | 34;
  public final static int navigate     = scriptCommand | 35;
//public final static int quaternion   see mathfunc
  public final static int parallel     = flowCommand   | 36;
  public final static int plot         = scriptCommand | 37;
  public final static int privat       = scriptCommand | 38;
  public final static int process      = flowCommand   | 39;
//  public final static int prompt  see mathfunc
//  public final static int push  see mathfunc //internal only
  public final static int quit         = scriptCommand | 41 | noArgs;
  public final static int ramachandran = scriptCommand | 42;
  public final static int redo         = scriptCommand | 43;
  public final static int redomove     = scriptCommand | 44;
  public final static int refresh      = scriptCommand | 44 | noArgs;
  public final static int reset        = scriptCommand | 45;
  public final static int restore      = scriptCommand | 46;
  public final static int resume       = scriptCommand | 47;
  public final static int rotate       = scriptCommand | 48 | defaultON;
  public final static int rotateSelected = scriptCommand | 49;
  public final static int save           = scriptCommand | 50;
//public final static int script   see mathfunc
  public final static int selectionhalos = scriptCommand | 52 | deprecatedparam | defaultON;
// public final static int show     see mathfunc
  public final static int slab         = scriptCommand | 53 | intparam | defaultON;
  public final static int spin         = scriptCommand | 55 | deprecatedparam | defaultON;
  public final static int ssbond       = scriptCommand | 56 | deprecatedparam | defaultON;
  public final static int step         = scriptCommand | 58 | noArgs;
  public final static int stereo       = scriptCommand | 59 | defaultON;
//public final static int structure    see intproperty
  public final static int sync         = scriptCommand | 60;
  public final static int timeout      = scriptCommand | 62 | setparam;
  public final static int translate    = scriptCommand | 64;
  public final static int translateSelected   = scriptCommand | 66;
  public final static int unbind              = scriptCommand | 67;
  public final static int undomove     = scriptCommand | 69;
  public final static int vibration    = scriptCommand | 70;
  //public final static int write   see mathfunc
  public final static int zoom                = scriptCommand | 72;
  public final static int zoomTo              = scriptCommand | 74;

  // shapes:
  
  public final static int axes         = shapeCommand | 2 | deprecatedparam | defaultON;
//public final static int boundbox     see mathproperty
//public final static int contact      see mathfunc
  public final static int cgo          = shapeCommand | 6; // PyMOL Compiled Graphical Object
  public final static int dipole       = shapeCommand | 7;
  public final static int draw         = shapeCommand | 8;
  public final static int frank        = shapeCommand | 10 | deprecatedparam | defaultON;
  public final static int isosurface   = shapeCommand | 12;
  public final static int lcaocartoon  = shapeCommand | 14;
  public final static int measurements = shapeCommand | 16 | setparam;
  public final static int mo           = shapeCommand | 18 | misc;
  public final static int nbo          = shapeCommand | 19 | misc;
  public final static int pmesh        = shapeCommand | 20;
  public final static int plot3d       = shapeCommand | 22;
  // public final static int polyhedra see mathfunc 
  // public final static int spacefill see floatproperty
  public final static int struts       = shapeCommand | 26 | defaultON;
  // public final static int unitcell see mathfunc
  public final static int vector       = shapeCommand | 30;
  public final static int wireframe    = shapeCommand | 32 | defaultON;


  


  public final static int amino                = predefinedset | 2;
  public final static int bonded               = predefinedset | 3;
  public final static int dna           = predefinedset | 4;
  public final static int hetero        = predefinedset | 6 | deprecatedparam;
  public final static int helixalpha           = predefinedset | 7;   // Jmol 12.1.14
  public final static int helix310             = predefinedset | 8;   // Jmol 12.1.14
  public final static int helixpi              = predefinedset | 10; 
  public final static int hydrogen      = predefinedset | 12 | deprecatedparam;
  public final static int leadatom      = predefinedset | 13;
  public final static int nucleic       = predefinedset | 14;
  public final static int protein       = predefinedset | 16;
  public final static int purine        = predefinedset | 18;
  public final static int pyrimidine    = predefinedset | 20;
  public final static int rna           = predefinedset | 22;
  public final static int solvent       = predefinedset | 24 | deprecatedparam;
  public final static int sidechain     = predefinedset | 26;
  public final static int surface              = predefinedset | 28;
  public final static int thismodel            = predefinedset | 30;
  public final static int sheet         = predefinedset | 32;
  public final static int spine         = predefinedset | 34;  // 11.9.34
  // these next are predefined in the sense that they are known quantities
  public final static int carbohydrate    = predefinedset | 36;
  public final static int clickable              = predefinedset | 38;
  public final static int displayed              = predefinedset | 40;
  public final static int hidden                 = predefinedset | 42;
  public final static int specialposition = predefinedset | 44;
  public final static int visible                = predefinedset | 46;
  public final static int basemodel              = predefinedset | 48; // specific to JCAMP-MOL files

  
  static int getPrecedence(int tokOperator) {
    return ((tokOperator >> 4) & 0xF);  
  }


  public final static int leftparen    = 0 | mathop | 1 << 4;
  public final static int rightparen   = 1 | mathop | 1 << 4;

  public final static int opIf         = 1 | mathop | 2 << 4 | setparam;   // set ?
  public final static int colon        = 2 | mathop | 2 << 4;

  public final static int comma        = 0 | mathop | 3 << 4;

  public final static int leftsquare   = 0 | mathop | 4 << 4;
  public final static int rightsquare  = 1 | mathop | 4 << 4;

  public final static int opOr         = 0 | mathop | 5 << 4;
  public final static int opXor        = 1 | mathop | 5 << 4;
  public final static int opToggle = 2 | mathop | 5 << 4;

  public final static int opAnd        = 0 | mathop | 6 << 4;
 
  public final static int opNot        = 0 | mathop | 7 << 4;

  public final static int opAND        = 0 | mathop | 8 << 4;

  public final static int opGT         = 0 | comparator | 9 << 4;
  public final static int opGE         = 1 | comparator | 9 << 4;
  public final static int opLE         = 2 | comparator | 9 << 4;
  public final static int opLT         = 3 | comparator | 9 << 4;
  public final static int opEQ         = 4 | comparator | 9 << 4;
  public final static int opNE         = 5 | comparator | 9 << 4;
  public final static int opLIKE       = 6 | comparator | 9 << 4;
   
  public final static int minus        = 0 | mathop | 10 << 4;
  public final static int plus         = 1 | mathop | 10 << 4;
 
  public final static int divide         = 0 | mathop | 11 << 4;
  public final static int times          = 1 | mathop | 11 << 4;
  public final static int percent = 2 | mathop | 11 << 4;
  public final static int leftdivide     = 3 | mathop | 11 << 4;  //   quaternion1 \ quaternion2
  
  public final static int unaryMinus   = 0 | mathop | 12 << 4;
  public final static int minusMinus   = 1 | mathop | 12 << 4;
  public final static int plusPlus     = 2 | mathop | 12 << 4;
  public final static int timestimes   = 3 | mathop | 12 << 4;
  
  
  public final static int propselector = 1 | mathop | 13 << 4;

  public final static int andequals    = 2 | mathop | 13 << 4;

  // these atom and math properties are invoked after a ".":
  // x.atoms
  // myset.bonds
  
  // .min and .max, .average, .sum, .sum2, .stddev, and .all 
  // are bitfields added to a preceding property selector
  // for example, x.atoms.max, x.atoms.all
  // .all gets incorporated as minmaxmask
  // .selectedfloat is a special flag used by mapPropety() and plot()
  // to pass temporary float arrays to the .bin() function
  // .allfloat is a special flag for colorShape() to get a full
  // atom float array
  
  public final static int minmaxmask /*all*/ = 0xF << 5; 
  public final static int min           = 1 << 5;
  public final static int max           = 2 << 5;
  public final static int average       = 3 << 5;
  public final static int sum           = 4 << 5;
  public final static int sum2          = 5 << 5;
  public final static int stddev        = 6 << 5;
  public final static int selectedfloat = 7 << 5; //not user-selectable
  public final static int allfloat      = 8 << 5; //not user-selectable
  public final static int apivot        = 9 << 5;

  public final static int settable           = 1 << 11;
  
  // bits 0 - 4 are for an identifier -- DO NOT GO OVER 31!
  // but, note that we can have more than 1 provided other parameters differ
  
  // ___.xxx math properties and all atom properties 
    
  public final static int atoms     = 1 | mathproperty;
  public final static int bonds     = 2 | mathproperty | deprecatedparam;
  public final static int length           = 3 | mathproperty;
  public final static int lines            = 4 | mathproperty;
  public final static int reverse   = 5 | mathproperty;
  public final static int size             = 6 | mathproperty;
  public final static int type      = 8 | mathproperty;
  public final static int boundbox  = 9 | mathproperty | deprecatedparam | shapeCommand | defaultON;
  public final static int xyz       =10 | mathproperty | atomproperty | settable;
  public final static int fracxyz   =11 | mathproperty | atomproperty | settable;
  public final static int screenxyz =12 | mathproperty | atomproperty | settable;
  public final static int fuxyz     =13 | mathproperty | atomproperty | settable;
  public final static int unitxyz   =14 | mathproperty | atomproperty;
  public final static int vibxyz    =15 | mathproperty | atomproperty | settable;
  public final static int modxyz    =16 | mathproperty | atomproperty;
  public final static int w         =17 | mathproperty;
  public final static int keys      =18 | mathproperty; 
  
  // occupancy, radius, and structure are odd, because they takes different meanings when compared
  
  public final static int occupancy     = intproperty | floatproperty | 1 | settable;
  public final static int radius        = intproperty | floatproperty | 2 | deprecatedparam | settable;
  public final static int structure     = intproperty | strproperty   | 3 | setparam | scriptCommand;

  // any new int, float, or string property should be added also to LabelToken.labelTokenIds
  // and the appropriate Atom.atomPropertyXXXX() method
  
  public final static int atomtype      = strproperty | 1 | settable;
  public final static int atomname      = strproperty | 2 | settable;
  public final static int altloc        = strproperty | 3;
  public final static int chain         = strproperty | 4 | settable;
  public final static int element       = strproperty | 5 | settable;
  public final static int group         = strproperty | 6;
  public final static int group1        = strproperty | 7;
  public final static int sequence      = strproperty | 8;
  public final static int identify      = strproperty | 9;
  public final static int insertion     = strproperty |10;
  public final static int seqcode       = strproperty |11;
  public final static int shape         = strproperty |12;
  public final static int strucid       = strproperty |13;
  public final static int symbol        = strproperty |14 | settable;
  public final static int symmetry      = strproperty |15 | predefinedset;
  public final static int chirality     = strproperty |16;
  public final static int ciprule = strproperty |17;

  public final static int atomno        = intproperty | 1 | settable;
  public final static int atomid        = intproperty | 2;
  public final static int atomindex     = intproperty | 3;
  // bondcount -- see xxx(a) 
  public final static int cell          = intproperty | 5;
  public final static int centroid      = intproperty | 6;
  public final static int chainno       = intproperty | 7;
  public final static int configuration = intproperty | 8 | scriptCommand;
  //color: see xxx(a, b, c, d)
  public final static int elemisono     = intproperty | 9;
  public final static int elemno        = intproperty | 10 | settable;
  //file: see xxx(a)
  public final static int formalcharge  = intproperty | 11 | setparam | settable;
  public final static int groupid       = intproperty | 12;
  public final static int groupindex    = intproperty | 13;
  public final static int model         = intproperty | 14 | scriptCommand;
  public final static int modelindex    = intproperty | 15;
  public final static int molecule      = intproperty | 16;
  public final static int monomer       = intproperty | 17;
  public final static int polymer       = intproperty | 18;
  public final static int polymerlength = intproperty | 19;
  public final static int resno         = intproperty | 20 | settable;
  public final static int seqid         = intproperty | 21;
  public final static int site          = intproperty | 22;
  public final static int strucno       = intproperty | 23;
  public final static int subsystem     = intproperty | 24;
  public final static int valence       = intproperty | 26 | settable;

  // float values must be multiplied by 100 prior to comparing to integer values

  // max 31 here
  
  public final static int adpmax          = floatproperty | 1;
  public final static int adpmin          = floatproperty | 2;
  public final static int chemicalshift   = floatproperty | 3; // Jmol 13.1.19
  public final static int covalentradius        = floatproperty | 4;
  public final static int eta             = floatproperty | 5; // Jmol 12.0.RC23
  public final static int magneticshielding = floatproperty | 6;  // Jmol 13.1.19
  public final static int mass            = floatproperty | 7;
  public final static int omega           = floatproperty | 8;
  public final static int phi             = floatproperty | 9;
  public final static int psi             = floatproperty | 10;
  public final static int screenx         = floatproperty | 11;
  public final static int screeny         = floatproperty | 12;
  public final static int screenz         = floatproperty | 13;
  public final static int straightness    = floatproperty | 14;
  public final static int surfacedistance = floatproperty | 15;
  public final static int theta           = floatproperty | 16; // Jmol 12.0.RC23
  public final static int unitx           = floatproperty | 17;
  public final static int unity           = floatproperty | 18;
  public final static int unitz           = floatproperty | 19;
  public final static int modt1           = floatproperty | 20;
  public final static int modt2           = floatproperty | 21;
  public final static int modt3           = floatproperty | 22;
  public final static int modx            = floatproperty | 23;
  public final static int mody            = floatproperty | 24;
  public final static int modz            = floatproperty | 25;
  public final static int modo            = floatproperty | 26;
  public final static int dssr            = floatproperty | 27;
  public final static int vectorscale     = floatproperty | 1 | floatparam;
  public final static int atomx           = floatproperty | 1 | settable;
  public final static int atomy           = floatproperty | 2 | settable;
  public final static int atomz           = floatproperty | 3 | settable;
  public final static int fracx           = floatproperty | 4 | settable;
  public final static int fracy           = floatproperty | 5 | settable;
  public final static int fracz           = floatproperty | 6 | settable;
  public final static int fux             = floatproperty | 7 | settable;
  public final static int fuy             = floatproperty | 8 | settable;
  public final static int fuz             = floatproperty | 9 | settable;
  public final static int bondingradius   = floatproperty | 10 | settable;
  public final static int partialcharge   = floatproperty | 11 | settable;
  public final static int temperature     = floatproperty | 12 | settable;
  public final static int vibx            = floatproperty | 18 | settable;
  public final static int viby            = floatproperty | 19 | settable;
  public final static int vibz            = floatproperty | 20 | settable;
  public final static int x               = floatproperty | 21 | settable;
  public final static int y               = floatproperty | 22 | settable;
  public final static int z               = floatproperty | 23 | settable;
  public final static int vanderwaals     = floatproperty | 24 | settable | setparam;
  public final static int property        = floatproperty | 25 | settable | setparam | mathproperty;
  public final static int hydrophobicity  = floatproperty | 26 | settable | predefinedset;
  public final static int selected        = floatproperty | 27 | settable | predefinedset;
  
  public final static int backbone     = floatproperty | shapeCommand | 1 | predefinedset | defaultON | settable;
  public final static int cartoon      = floatproperty | shapeCommand | 2 | defaultON | settable;
  public final static int dots         = floatproperty | shapeCommand | 3 | defaultON;
  public final static int ellipsoid    = floatproperty | shapeCommand | 4 | defaultON;
  public final static int geosurface   = floatproperty | shapeCommand | 5 | defaultON;
  public final static int halo         = floatproperty | shapeCommand | 6 | defaultON | settable;
  public final static int meshRibbon   = floatproperty | shapeCommand | 7 | defaultON | settable;
  public final static int ribbon       = floatproperty | shapeCommand | 9 | defaultON | settable;
  public final static int rocket       = floatproperty | shapeCommand | 10 | defaultON | settable;
  public final static int spacefill    = floatproperty | shapeCommand | 11 | defaultON | settable;
  public final static int star         = floatproperty | shapeCommand | 12 | defaultON | settable;
  public final static int strands      = floatproperty | shapeCommand | 13 | deprecatedparam | defaultON | settable;
  public final static int trace        = floatproperty | shapeCommand | 14 | defaultON | settable;

  // mathfunc               means x = somefunc(a,b,c)
  // mathfunc|mathproperty  means x = y.somefunc(a,b,c)
  // 
  // maximum number of parameters is set by the << 9 shift
  // the min/max mask requires that the first number here must not exceed 63
  // the only other requirement is that these numbers be unique, so the initial 
  // number can be duplicated if necessary, as long as other flags are different


  static int getMaxMathParams(int tokCommand) {
    return  ((tokCommand >> 9) & 0x3);
  }

  // 0 << 9 indicates that ScriptMathProcessor 
  // will check length in second stage of compilation

  // xxx(a,b,c,d,e,...)
  
  public final static int angle            = 1 | 0 << 9 | mathfunc;
  public final static int array            = 2 | 0 << 9 | mathfunc | mathproperty;
  public final static int axisangle        = 3 | 0 << 9 | mathfunc;
  public final static int bin              = 4 | 0 << 9 | mathfunc | mathproperty;
  public final static int cache            = 5 | 0 << 9 | mathfunc | scriptCommand; // new in Jmol 13.1.2
  public final static int color            = 6 | 0 << 9 | mathfunc | intproperty | scriptCommand | deprecatedparam | settable;
  public final static int compare          = 7 | 0 << 9 | mathfunc | scriptCommand;
  public final static int connected        = 8 | 0 << 9 | mathfunc;
  public final static int count            = 9 | 0 << 9 | mathfunc | mathproperty;
  public final static int data             = 10 | 0 << 9 | mathfunc | scriptCommand;
  public final static int find             = 11 | 0 << 9 | mathfunc | mathproperty;
  public final static int format           = 12 | 0 << 9 | mathfunc | mathproperty | strproperty | settable;
  public final static int function         = 13 | 0 << 9 | mathfunc | flowCommand;
  public final static int getproperty      = 14 | 0 << 9 | mathfunc | mathproperty | scriptCommand;
  public final static int helix            = 15 | 0 << 9 | mathfunc | predefinedset;
  public final static int in               = 16 | 0 << 9 | mathfunc | mathproperty;
  public final static int inchi            = 17 | 0 << 9 | mathfunc | mathproperty;
  public final static int label            = 18 /* must NOT be odd */| 0 << 9 | mathfunc | mathproperty | strproperty | settable | implicitStringCommand | shapeCommand | defaultON | deprecatedparam; 
  public final static int measure          = 19 | 0 << 9| mathfunc | shapeCommand | deprecatedparam | defaultON;
  public final static int modulation       = 20 | 0 << 9 | mathfunc | mathproperty | scriptCommand;
  public final static int pivot            = 21 | 0 << 9 | mathfunc | mathproperty | apivot;
  public final static int pivot2           = 21 | 0 << 9 | mathfunc | mathproperty;
  public final static int plane            = 22 | 0 << 9 | mathfunc;
  public final static int point            = 23 | 0 << 9 | mathfunc;
  public final static int polyhedra        = 24 | 0 << 9 | mathfunc | mathproperty | shapeCommand;
  public final static int pop              = 25 | 0 << 9 | mathfunc | mathproperty | scriptCommand | noArgs; //internal only;
  public final static int quaternion       = 26 | 0 << 9 | mathfunc | scriptCommand;
  public final static int replace          = 27 | 0 << 9 | mathfunc | mathproperty;
  public final static int sort             = 28 | 0 << 9 | mathfunc | mathproperty;
  public final static int tensor           = 29 | 0 << 9 | mathfunc | mathproperty;
  public final static int unitcell         = 30 | 0 << 9 | mathfunc | mathproperty | shapeCommand | deprecatedparam | predefinedset | defaultON;
  public final static int __               = 30 | 0 << 9 | mathfunc | mathproperty; // same as getProperty 
  public final static int within           = 31 | 0 << 9 | mathfunc;
  public final static int pointgroup       = 31 | 0 << 9 | mathfunc | mathproperty;
  // NO mathproperty AFTER 31, as F << 5 is the minmax mask
  public final static int write            = 32 | 0 << 9 | mathfunc | scriptCommand;
  public final static int intersection     = 35 | 0 << 9 | mathfunc;
  public final static int spacegroup       = 36 | 0 << 9 | mathfunc;
  public final static int _args            = 37 | 0 << 9 | mathfunc;


    // xxx(a)
  
  public final static int acos         = 2 | 1 << 9 | mathfunc;
  public final static int bondcount    = 3 | 1 << 9 | mathfunc | intproperty;
  public final static int sin          = 4 | 1 << 9 | mathfunc;
  public final static int cos          = 5 | 1 << 9 | mathfunc;
  public final static int sqrt         = 6 | 1 << 9 | mathfunc;
  public final static int file         = 7 | 1 << 9 | mathfunc | intproperty | scriptCommand;
  public final static int forcmd       = 8 | 1 << 9 | mathfunc | flowCommand;
  public final static int ifcmd        = 9 | 1 << 9 | mathfunc | flowCommand;
  public final static int abs          = 10 | 1 << 9 | mathfunc;
  public final static int javascript   = 12 /* must be even */| 1 << 9 | mathfunc | implicitStringCommand;
  public final static int show         = 14 | 1 << 9 | mathfunc | scriptCommand;

  
  // ___.xxx(a)
  
  // a.distance(b) is in a different set -- distance(b,c) -- because it CAN take
  // two parameters and it CAN be a dot-function (but not both together)
  
  public final static int div          = 0 | 1 << 9 | mathfunc | mathproperty;
  public final static int mul          = 1 | 1 << 9 | mathfunc | mathproperty;
  public final static int mul3         = 2 | 1 << 9 | mathfunc | mathproperty;
  public final static int sub          = 3 | 1 << 9 | mathfunc | mathproperty;
  public final static int trim         = 4 | 1 << 9 | mathfunc | mathproperty;  
  public final static int volume       = 5 | 1 << 9 | mathfunc | mathproperty | floatproperty;  
  public final static int col          = 6 | 1 << 9 | mathfunc | mathproperty;
  public final static int row          = 7 | 1 << 9 | mathfunc | mathproperty;

  // xxx(a,b)
  
  public final static int load         = 1 | 2 << 9 | mathfunc | scriptCommand;
  public final static int script       = 2 | 2 << 9 | mathfunc | scriptCommand;
  public final static int substructure = 3 | 2 << 9 | mathfunc | intproperty | strproperty;
  public final static int search       = 4 | 2 << 9 | mathfunc;
  public final static int smiles       = 5 | 2 << 9 | mathfunc;
  public final static int contact      = 6 | 2 << 9 | mathfunc | shapeCommand;
  public final static int eval         = 7 | 2 << 9 | mathfunc;
  public final static int now          = 8 | 2 << 9 | mathfunc;
  

  // ___.xxx(a,b)

  // note that distance is here because it can take two forms:
  //     a.distance(b)
  // and
  //     distance(a,b)
  //so it can be a math property and it can have up to two parameters
  
  public final static int add          = 1 | 2 << 9 | mathfunc | mathproperty;
  public final static int cross        = 2 | 2 << 9 | mathfunc | mathproperty;
  public final static int distance     = 3 | 2 << 9 | mathfunc | mathproperty;
  public final static int dot          = 4 | 2 << 9 | mathfunc | mathproperty;
  public final static int push         = 5 | 2 << 9 | mathfunc | mathproperty | scriptCommand | noArgs; //internal only;
  public final static int join         = 6 | 2 << 9 | mathfunc | mathproperty;
  public final static int split        = 7 | 2 << 9 | mathfunc | mathproperty;
  
  // xxx(a,b,c)
  
  public final static int prompt       = 3 | 3 << 9 | mathfunc | mathExpressionCommand;
  public final static int random       = 4 | 3 << 9 | mathfunc;
  public final static int select       = 5 | 3 << 9 | mathfunc | mathproperty | atomExpressionCommand;

  // ___.xxx(a,b,c)
  
  public final static int hkl          = 1 | 4 << 9 | mathfunc;
  public final static int symop        = 2 | 4 << 9 | mathfunc | mathproperty | intproperty; 

  // set parameters 
  
  // deprecated or handled specially in ScriptEvaluator
  
  public final static int bondmode           = deprecatedparam | 1;  
  public final static int fontsize           = deprecatedparam | 2;
  public final static int measurementnumbers = deprecatedparam | 3;
  public final static int scale3d            = deprecatedparam | 4;
  public final static int togglelabel        = deprecatedparam | 5;

  // handled specially in ScriptEvaluator

  public final static int backgroundmodel  = setparam | 2;
  public final static int debug            = setparam | 4;
  public final static int debughigh        = setparam | 5;
  public final static int defaultlattice   = setparam | 6;
  public final static int highlight        = setparam | 8;// 12.0.RC14
  public final static int showscript       = setparam | 10;
  public final static int specular         = setparam | 12;
  public final static int trajectory       = setparam | 14;
  public final static int usercolorscheme  = setparam | 18;
  public final static int window           = setparam  | 20;

  // full set of all Jmol "set" parameters

  public final static int animationmode                  = strparam | 1;
  public final static int appletproxy                    = strparam | 2;
  public final static int atomtypes                      = strparam | 4;
  public final static int axescolor                      = strparam | 6;
  public final static int axis1color                     = strparam | 8;
  public final static int axis2color                     = strparam | 10;
  public final static int axis3color                     = strparam | 12;
  public final static int backgroundcolor                = strparam | 14;
  public final static int boundboxcolor                  = strparam | 16;
  public final static int currentlocalpath               = strparam | 18;
  public final static int dataseparator                  = strparam | 20;
  public final static int defaultanglelabel              = strparam | 22;
  public final static int defaultlabelpdb                = strparam | 23;
  public final static int defaultlabelxyz                = strparam | 24;
  public final static int defaultcolorscheme             = strparam | 25;
  public final static int defaultdirectory               = strparam | 26;
  public final static int defaultdistancelabel           = strparam | 27;
  public final static int defaultdropscript              = strparam | 28;
  public final static int defaultloadfilter              = strparam | 29;
  public final static int defaultloadscript              = strparam | 30;
  public final static int defaults                       = strparam | 32;
  public final static int defaulttorsionlabel            = strparam | 34;
  public final static int defaultvdw                     = strparam | 35;
  public final static int eds                            = strparam | 36;
  public final static int edsdiff                        = strparam | 37;
//  public final static int edsurlcutoff                   = strparam | 36;
//  public final static int edsurlformat                   = strparam | 37;
//  public final static int edsurlformatdiff               = strparam | 38;
  public final static int energyunits                    = strparam | 38; 
  public final static int filecachedirectory             = strparam | 39;
  public final static int forcefield                     = strparam | 40;
  public final static int helppath                       = strparam | 41;
  public final static int hoverlabel                     = strparam | 42;
  public final static int language                       = strparam | 43;
  public final static int loadformat                     = strparam | 44;
  public final static int loadligandformat               = strparam | 45;
  public final static int logfile                        = strparam | 46;
  public final static int macrodirectory                 = strparam | 47;
  public final static int measurementunits               = strparam | 48; 
  public final static int nmrpredictformat               = strparam | 49;
  public final static int nihresolverformat              = strparam | 50;
  public final static int nmrurlformat                   = strparam | 51;
  public final static int pathforallfiles                = strparam | 52;
  public final static int picking                        = strparam | 53;
  public final static int pickingstyle                   = strparam | 54;
  public final static int picklabel                      = strparam | 56;
  public final static int propertycolorscheme            = strparam | 58;
  public final static int quaternionframe                = strparam | 60;
  public final static int smilesurlformat                = strparam | 62;
  public final static int smiles2dimageformat            = strparam | 64;
  public final static int unitcellcolor                  = strparam | 66;
  
  public final static int axesoffset                     = floatparam | 1;
  public final static int axesscale                      = floatparam | 2;
  public final static int cartoonblockheight             = floatparam | 3;
  public final static int bondtolerance                  = floatparam | 4;
  public final static int cameradepth                    = floatparam | 6;
  public final static int defaultdrawarrowscale          = floatparam | 8;
  public final static int defaulttranslucent             = floatparam | 9;
  public final static int dipolescale                    = floatparam | 10;
  public final static int drawfontsize                   = floatparam | 11;
  public final static int ellipsoidaxisdiameter          = floatparam | 12;
  public final static int exportscale                    = floatparam | 13;
  public final static int gestureswipefactor             = floatparam | 14;
  public final static int hbondsangleminimum             = floatparam | 15;
  public final static int hbondnodistancemaximum          = floatparam | 16;
  public final static int hbondhxdistancemaximum         = floatparam | 17;
  public final static int hoverdelay                     = floatparam | 18;
  public final static int loadatomdatatolerance          = floatparam | 19;  
  public final static int minbonddistance                = floatparam | 20;
  public final static int minimizationcriterion          = floatparam | 21;
  public final static int modulationscale                = floatparam | 22;
  public final static int mousedragfactor                = floatparam | 23;
  public final static int mousewheelfactor               = floatparam | 24;
  public final static int multiplebondradiusfactor       = floatparam | 25;
  public final static int multiplebondspacing            = floatparam | 26;
  public final static int navfps                         = floatparam | 27;
  public final static int navigationdepth                = floatparam | 28;
  public final static int navigationslab                 = floatparam | 29;
  public final static int navigationspeed                = floatparam | 30;
  public final static int navx                           = floatparam | 32;
  public final static int navy                           = floatparam | 34;
  public final static int navz                           = floatparam | 36;
  public final static int particleradius                 = floatparam | 37;
  public final static int pointgroupdistancetolerance    = floatparam | 38;
  public final static int pointgrouplineartolerance      = floatparam | 40;
  public final static int rotationradius                 = floatparam | 44;
  public final static int scaleangstromsperinch          = floatparam | 46;
  public final static int sheetsmoothing                 = floatparam | 48;
  public final static int slabrange                      = floatparam | 49;
  public final static int solventproberadius             = floatparam | 50;
  public final static int spinfps                        = floatparam | 52;
  public final static int spinx                          = floatparam | 54;
  public final static int spiny                          = floatparam | 56;
  public final static int spinz                          = floatparam | 58;
  public final static int starwidth                     = floatparam | 59; // Jmol 13.1.15
  public final static int stereodegrees                  = floatparam | 60;
  public final static int strutdefaultradius             = floatparam | 62;
  public final static int strutlengthmaximum             = floatparam | 64;
  public final static int vibrationperiod                = floatparam | 68;
  public final static int vibrationscale                 = floatparam | 70;
  public final static int visualrange                    = floatparam | 72;

  public final static int ambientocclusion               = intparam | 1;               
  public final static int ambientpercent                 = intparam | 2;               
  public final static int animationfps                   = intparam | 4;
  public final static int axesmode                       = intparam | 5;
  public final static int bondradiusmilliangstroms       = intparam | 6;
  public final static int celshadingpower                = intparam | 7;
  public final static int bondingversion                 = intparam | 8;
  public final static int delaymaximumms                 = intparam | 9;
  public final static int diffusepercent                 = intparam | 10;
  public final static int dotdensity                     = intparam | 11;
  public final static int dotscale                       = intparam | 12;
  public final static int ellipsoiddotcount              = intparam | 13;  
  public final static int helixstep                      = intparam | 14;
  public final static int hermitelevel                   = intparam | 15;
  public final static int historylevel                   = intparam | 16;
  public final static int infofontsize                   = intparam | 17;
  public final static int isosurfacepropertysmoothingpower=intparam | 18;
  public final static int labelpointerwidth              = intparam | 19;
  public final static int loglevel                       = intparam | 21;
  public final static int meshscale                      = intparam | 22;
  public final static int minimizationsteps              = intparam | 23;
  public final static int minimizationmaxatoms           = intparam | 24;
  public final static int minpixelselradius              = intparam | 25;
  public final static int percentvdwatom                 = intparam | 26;
  public final static int perspectivemodel               = intparam | 27;
  public final static int phongexponent                  = intparam | 28;
  public final static int pickingspinrate                = intparam | 29;
  public final static int platformspeed                  = intparam | 30;
  public final static int propertyatomnumberfield        = intparam | 31;
  public final static int propertyatomnumbercolumncount  = intparam | 32;
  public final static int propertydatacolumncount        = intparam | 34;
  public final static int propertydatafield              = intparam | 36;
  public final static int repaintwaitms                  = intparam | 37;
  public final static int ribbonaspectratio              = intparam | 38;
  public final static int contextdepthmax                = intparam | 39;
  public final static int scriptreportinglevel           = intparam | 40;
  public final static int smallmoleculemaxatoms          = intparam | 42;
  public final static int specularexponent               = intparam | 44;
  public final static int specularpercent                = intparam | 46;
  public final static int specularpower                  = intparam | 48;
  public final static int strandcount                    = intparam | 50;
  public final static int strandcountformeshribbon       = intparam | 52;
  public final static int strandcountforstrands          = intparam | 54;
  public final static int strutspacing                   = intparam | 55;
  public final static int undomax                        = intparam | 56;
  public final static int vectortrail                    = intparam | 57;
  public final static int zdepth                         = intparam | 58;
  public final static int zslab                          = intparam | 60;
  public final static int zshadepower                    = intparam | 62;

  public final static int allowembeddedscripts           = booleanparam | 2;
  public final static int allowgestures                  = booleanparam | 4;
  public final static int allowkeystrokes                = booleanparam | 5;
  public static final int allowmodelkit                  = booleanparam | 6; // Jmol 12.RC15
  public final static int allowmoveatoms                 = booleanparam | 7; // Jmol 12.1.21
  public static final int allowmultitouch                = booleanparam | 8; // Jmol 11.9.24
  public final static int allowrotateselected            = booleanparam | 9;
  public final static int antialiasdisplay               = booleanparam | 10;
  public final static int antialiasimages                = booleanparam | 12;
  public final static int antialiastranslucent           = booleanparam | 14;
  public final static int appendnew                      = booleanparam | 16;
  public final static int applysymmetrytobonds           = booleanparam | 18;
  public final static int atompicking                    = booleanparam | 20;
  public final static int allowaudio                     = booleanparam | 21;
  public final static int autobond                       = booleanparam | 22;
  public final static int autofps                        = booleanparam | 24;
  public final static int autoplaymovie                  = booleanparam | 26;
//  public final static int autoloadorientation            = booleanparam | 26;
  public final static int axesmolecular                  = booleanparam | 28;
  public final static int axesorientationrasmol          = booleanparam | 30;
  public final static int axesunitcell                   = booleanparam | 32;
  public final static int axeswindow                     = booleanparam | 33;
  public final static int cartoonblocks                  = booleanparam | 34;
  public final static int cartoonsteps                   = booleanparam | 35;
  public final static int bondmodeor                     = booleanparam | 36;
  public final static int bondpicking                    = booleanparam | 38;
// set mathproperty  public final static int bonds                          = booleanparam | 40;
  public final static int cartoonbaseedges               = booleanparam | 39;
  public final static int cartoonsfancy                  = booleanparam | 40;
  public final static int cartoonladders                 = booleanparam | 41;
  public final static int cartoonribose                  = booleanparam | 42;
  public final static int cartoonrockets                 = booleanparam | 43;
  public final static int celshading                     = booleanparam | 44;
  public final static int checkcir                       = booleanparam | 45;
  public final static int chaincasesensitive             = booleanparam | 46;
  public final static int ciprule6full                   = booleanparam | 47;
  public final static int colorrasmol                    = booleanparam | 48;
  public final static int debugscript                    = booleanparam | 49;
  public final static int defaultstructuredssp           = booleanparam | 50;
  public final static int disablepopupmenu               = booleanparam | 51;
  public final static int displaycellparameters          = booleanparam | 52;
  public final static int dotsselectedonly               = booleanparam | 53;
  public final static int dotsurface                     = booleanparam | 54;
  public final static int doubleprecision                = booleanparam | 55;
  public final static int dragselected                   = booleanparam | 56;
  public final static int drawhover                      = booleanparam | 57;
  public final static int drawpicking                    = booleanparam | 58;
  public final static int dsspcalchydrogen               = booleanparam | 59;
  public final static int dynamicmeasurements            = booleanparam | 60; //DEPRECATED; not implemented; leave here to avoid SET error
  public final static int ellipsoidarcs                  = booleanparam | 61;  
  public final static int ellipsoidarrows                = booleanparam | 62;  
  public final static int ellipsoidaxes                  = booleanparam | 63;  
  public final static int ellipsoidball                  = booleanparam | 64;  
  public final static int ellipsoiddots                  = booleanparam | 65;  
  public final static int ellipsoidfill                  = booleanparam | 66;  
  public final static int filecaching                    = booleanparam | 67;
  public final static int fontcaching                    = booleanparam | 68;
  public final static int fontscaling                    = booleanparam | 69;
  public final static int forceautobond                  = booleanparam | 71;
  public final static int fractionalrelative             = booleanparam | 72;
// see shapecommand public final static int frank                          = booleanparam | 72;
  public final static int greyscalerendering             = booleanparam | 74;
  public final static int hbondsbackbone                 = booleanparam | 76;
  public final static int hbondsrasmol                   = booleanparam | 77;
  public final static int hbondssolid                    = booleanparam | 78;
// see predefinedset  public final static int hetero                         = booleanparam | 80;
  public final static int hiddenlinesdashed              = booleanparam | 80;
  public final static int hidenameinpopup                = booleanparam | 82;
  public final static int hidenavigationpoint            = booleanparam | 84;
  public final static int hidenotselected                = booleanparam | 86;
  public final static int highresolution                 = booleanparam | 88;
// see predefinedset  public final static int hydrogen                       = booleanparam | 90;
  public final static int imagestate                     = booleanparam | 89;
  public static final int iskiosk                        = booleanparam | 90; // 11.9.29
  public final static int isosurfacekey                  = booleanparam | 91;
  public final static int isosurfacepropertysmoothing    = booleanparam | 92;
  public final static int jmolinjspecview                = booleanparam | 93; // 14.13.1
  public final static int justifymeasurements            = booleanparam | 94;
  public final static int languagetranslation            = booleanparam | 95;
  public final static int legacyautobonding              = booleanparam | 96;
  public final static int legacyhaddition                = booleanparam | 97;
  public final static int legacyjavafloat                = booleanparam | 98;
  public final static int logcommands                    = booleanparam | 99;
  public final static int loggestures                    = booleanparam | 100;
  public final static int measureallmodels               = booleanparam | 101;
  public final static int measurementlabels              = booleanparam | 102;
  public final static int messagestylechime              = booleanparam | 103;
  public final static int minimizationrefresh            = booleanparam | 104;
  public final static int minimizationsilent             = booleanparam | 105;
  public final static int modulateoccupancy              = booleanparam | 108;  // 14.3.13
  public final static int monitorenergy                  = booleanparam | 109;
  public final static int multiplebondbananas            = booleanparam | 110;
  public final static int multiprocessor                 = booleanparam | 111;
  public final static int navigatesurface                = booleanparam | 112;
  public final static int navigationmode                 = booleanparam | 113;
  public final static int navigationperiodic             = booleanparam | 114;
  public final static int nbocharges                     = booleanparam | 115;
  public final static int nodelay                        = booleanparam | 116;
  public final static int partialdots                    = booleanparam | 117; // 12.1.46
  public final static int pdbaddhydrogens                = booleanparam | 118;
  public final static int pdbgetheader                   = booleanparam | 119;
  public final static int pdbsequential                  = booleanparam | 120;
  public final static int perspectivedepth               = booleanparam | 121;
  public final static int preservestate                  = booleanparam | 122;
  public final static int rangeselected                  = booleanparam | 123;
  public final static int refreshing                     = booleanparam | 124;
  public final static int ribbonborder                   = booleanparam | 125;
  public final static int rocketbarrels                  = booleanparam | 126;
  public final static int saveproteinstructurestate      = booleanparam | 127;
  public final static int scriptqueue                    = booleanparam | 128;
  public final static int selectallmodels                = booleanparam | 130;
  public final static int selecthetero                   = booleanparam | 132;
  public final static int selecthydrogen                 = booleanparam | 134;
  // see commands public final static int selectionhalo                  = booleanparam | 136;
  public final static int showaxes                       = booleanparam | 138;
  public final static int showboundbox                   = booleanparam | 140;
  public final static int showfrank                      = booleanparam | 142;
  public final static int showhiddenselectionhalos       = booleanparam | 144;
  public final static int showhydrogens                  = booleanparam | 146;
  public final static int showkeystrokes                 = booleanparam | 148;
  public final static int showmeasurements               = booleanparam | 150;
  public final static int showmodvecs                    = booleanparam | 151;
  public final static int showmultiplebonds              = booleanparam | 152;
  public final static int shownavigationpointalways      = booleanparam | 154;
// see intparam  public final static int showscript                     = booleanparam | 156;
  public final static int showtiming                     = booleanparam | 158;
  public final static int showunitcell                   = booleanparam | 160;
  public final static int showunitcelldetails            = booleanparam | 161;
  public final static int slabbyatom                     = booleanparam | 163;
  public final static int slabbymolecule                 = booleanparam | 164;
  public final static int slabenabled                    = booleanparam | 166;
  public final static int smartaromatic                  = booleanparam | 168;
// see predefinedset  public final static int solvent                        = booleanparam | 170;
  public final static int solventprobe                   = booleanparam | 172;
// see intparam  public final static int specular                       = booleanparam | 174;
  public final static int ssbondsbackbone                = booleanparam | 176;
  public final static int statusreporting                = booleanparam | 178;
  public final static int strutsmultiple                 = booleanparam | 179;
  public final static int syncmouse                      = booleanparam | 180;
  public final static int syncscript                     = booleanparam | 182;
  public final static int testflag1                      = booleanparam | 184;
  public final static int testflag2                      = booleanparam | 186;
  public final static int testflag3                      = booleanparam | 188;
  public final static int testflag4                      = booleanparam | 189;
  public final static int tracealpha                     = booleanparam | 190;
  public final static int translucent                    = booleanparam | 191;
  public final static int twistedsheets                  = booleanparam | 192;
  public final static int undo                           = booleanparam | scriptCommand | 193;
  public final static int undoauto                       = booleanparam | 193;
  //public final static int usearcball                     = booleanparam | 193;
  public final static int useminimizationthread          = booleanparam | 194;
  public final static int usenumberlocalization          = booleanparam | 195;
  public final static int vectorscentered                = booleanparam | 196;
  public final static int vectorsymmetry                 = booleanparam | 197;
  public final static int waitformoveto                  = booleanparam | 199;
  public final static int windowcentered                 = booleanparam | 200;
  public final static int wireframerotation              = booleanparam | 201;
  public final static int zerobasedxyzrasmol             = booleanparam | 202;
  public final static int zoomenabled                    = booleanparam | 204;
  public final static int zoomheight                     = booleanparam | 206;
  public final static int zoomlarge                      = booleanparam | 207;
  public final static int zshade                         = booleanparam | 208;

  
  // misc

  public final static int absolute      = misc  | 2;
  public final static int addhydrogens  = misc  | 4;
  public final static int adjust        = misc  | 6;
  public final static int align         = misc  | 8;
  public final static int allconnected  = misc  | 10;
  public final static int angstroms     = misc  | 12;
  public final static int anisotropy    = misc  | 13;
  public final static int append        = misc  | 15;
  public final static int arc           = misc  | 16;
  public final static int area          = misc  | 18;
  public final static int aromatic      = misc  | 20 | predefinedset;
  public final static int arrow         = misc  | 22;
  public final static int as            = misc  | 24; // for LOAD and ISOSURFACE only
  public final static int async         = misc  | 25;
  public final static int atomicorbital = misc  | 26;
  public final static int audio         = misc  | 27;
  
  public final static int auto          = misc  | 28;
  public final static int axis          = misc  | 30;
  public final static int babel         = misc  | 32;
  public final static int babel21       = misc  | 33; 
  public final static int back          = misc  | 34;
  public final static int balls         = misc  | 35;
  public final static int barb          = misc  | 36;
  public final static int backlit       = misc  | 37;
  public final static int backshell     = misc  | 38;
  public final static int basepair      = misc  | 39;
  public final static int best          = misc  | 40;
  public final static int beta          = misc  | 41;
  public final static int binary        = misc  | 42;
  public final static int blockdata     = misc  | 44;
  public final static int bondset       = misc  | 46; // never used
  public final static int bottom        = misc  | 47;
  public final static int brillouin     = misc  | 48;
  public final static int cancel        = misc  | 50;
  public final static int cap           = misc  | 51;
  public final static int cavity        = misc  | 52;
  public final static int check         = misc  | 53;
  public final static int chemical      = misc  | 55;
  public final static int circle        = misc  | 56;
  public final static int clash         = misc  | 57;
  public final static int clear         = misc  | 58;
  public final static int clipboard     = misc  | 60;
  public final static int collapsed     = misc  | 62;
  public final static int colorscheme   = misc  | 64;
  public final static int command       = misc  | 66;
  public final static int commands      = misc  | 68;
  public final static int constraint    = misc  | 70;
  public final static int contour       = misc  | 72;
  public final static int contourlines  = misc  | 74;
  public final static int contours      = misc  | 76;
  public final static int corners       = misc  | 78;
  public final static int create = misc  | 80;
  public final static int criterion     = misc  | 81;
  public final static int crossed       = misc  | 82;
  public final static int curve         = misc  | 84;
  public final static int cutoff        = misc  | 86;
  public final static int cylinder      = misc  | 88;
  public final static int density        = misc  | 90;
  public final static int dssp           = misc  | 91;
  public final static int diameter       = misc  | 93;
  public final static int direction      = misc  | 94;
  public final static int discrete       = misc  | 96;
  public final static int displacement   = misc  | 98;
  public final static int distancefactor = misc  | 100;
  public final static int domains        = misc  | 101;
  public final static int dotted         = misc  | 102;
  public final static int downsample     = misc  | 104;
  public final static int drawing        = misc  | 105;
  public final static int eccentricity   = misc  | 107;
  public final static int ed             = misc  | 108;
  public final static int edges          = misc  | 109;
  public final static int edgesonly      = misc  | 110;
  public final static int energy         = misc  | 111;
  public final static int error          = misc  | 112;
  public final static int facecenteroffset = misc  | 113;
  public final static int fill    = misc  | 114;
  public final static int filter         = misc  | 116;
  public final static int first   = misc  | 118;
  public final static int fixedtemp      = misc  | 122;
  public final static int flat           = misc  | 124;
  public final static int fps            = misc  | 126;
  public final static int from           = misc  | 128;
  public final static int front   = misc  | 130;
  public final static int frontedges     = misc  | 132;
  public final static int frontlit = misc  | 134;
  public final static int frontonly = misc  | 136;
  public final static int full            = misc  | 137;
  public final static int fullplane       = misc  | 138;
  public final static int fullylit        = misc  | 140;
  public final static int functionxy     = misc  | 142;
  public final static int functionxyz    = misc  | 144;
  public final static int gridpoints     = misc  | 146;
  public final static int homo           = misc  | 149;
  public final static int id             = misc  | 150;
  public final static int ignore         = misc  | 152;
  public final static int inchikey       = misc  | 154;
  public final static int increment      = misc  | 157;
  public final static int info           = misc  | 158;
  public final static int inline         = misc  | 160;
  public final static int insideout      = misc  | 161;
  public final static int interior       = misc  | 162;
  public final static int internal       = misc  | 164;
  public final static int intramolecular = misc  | 165;
  public final static int intermolecular = misc  | 166;
  public final static int jmol           = misc  | 167;
  public final static int json           = misc  | 168;
  public final static int last    = misc  | 169;
  public final static int lattice        = misc  | 170;
  public final static int lighting       = misc  | 171;
  public final static int left    = misc  | 172;
  public final static int line           = misc  | 174;
  public final static int link           = misc  | 175;
  public final static int linedata       = misc  | 176;
  public final static int list    = misc  | 177; // just "list"
  public final static int lobe           = misc  | 178;
  public final static int lonepair       = misc  | 180;
  public final static int lp             = misc  | 182;
  public final static int lumo           = misc  | 184;
  public final static int manifest       = misc  | 186;
  public final static int maxset         = misc  | 190;
  public final static int menu           = misc  | 191;
  public final static int mep            = misc  | 192;
  public final static int mesh    = misc  | 194;
  public final static int middle         = misc  | 195;
  public final static int minset         = misc  | 196;
  public final static int mlp            = misc  | 198;
  public final static int mode           = misc  | 200;
  public final static int modify         = misc  | 201;
  public final static int modifyorcreate = misc  | 202;
  public final static int modelbased     = misc  | 203;
  public final static int molecular      = misc  | 204;
  public final static int morph          = misc  | 205;
  public final static int mouse          = misc  | 206;
  public final static int movie          = misc  | 207;
  public final static int mrc            = misc  | 208;
  public final static int msms           = misc  | 209;
  public final static int name           = misc  | 210;
  public final static int nci            = misc  | 212;
  public final static int next           = misc  | 213;
  public final static int nmr            = misc  | 214;
  public final static int nocontourlines  = misc  | 215;
  public final static int nocross        = misc  | 216;
  public final static int nodebug        = misc  | 217;
  public final static int nodots         = misc  | 218;
  public final static int noedges        = misc  | 220;
  public final static int nofill         = misc  | 222;
  public final static int nohead         = misc  | 224;
  public final static int noload         = misc  | 226;
  public final static int nomesh         = misc  | 228;
  public final static int noplane        = misc  | 230;
  public final static int normal         = misc  | 232;
  public final static int nobackshell    = misc  | 233;
  public final static int notfrontonly   = misc  | 234;
  public final static int notriangles    = misc  | 236;
  public final static int obj            = misc  | 238;
  public final static int object         = misc  | 240;
  public final static int offset         = misc  | 242;
  public final static int offsetside     = misc  | 244;
  public final static int once           = misc  | 246;
  public final static int only           = misc  | 248;
  public final static int opaque         = misc  | 250;
  public final static int options        = misc  | 251;
  public final static int orbital        = misc  | 252;
  public final static int orientation    = misc  | 253;
  public final static int origin         = misc  | 254; // 12.1.51
  public final static int out            = misc  | 255;
  public final static int packed         = misc  | 256;
  public final static int palindrome     = misc  | 258;
  public final static int parameters     = misc  | 259;
  public final static int path           = misc  | 260;
  public final static int pdb            = misc  | 262;
  public final static int pdbheader      = misc  | 264;
  public final static int period         = misc  | 266;
  public final static int perpendicular  = misc  | 268;
  public final static int phase          = misc  | 270;
  public final static int play           = misc  | 272;
  public final static int playrev        = misc  | 274;
  public final static int planarparam    = misc  | 275;
  public final static int pocket         = misc  | 276;
  public final static int pointsperangstrom = misc  | 280;
  public final static int polygon        = misc  | 282;
  public final static int prev           = misc  | 284;
  public final static int probe          = misc  | 285;
  public final static int pymol          = misc  | 286;
  public final static int rad            = misc  | 287;
  public final static int radical        = misc  | 288;
  public final static int range          = misc  | 290;
  public final static int rasmol         = misc  | 292;
  public final static int reference      = misc  | 294;
  public final static int remove         = misc  | 295;
  public final static int residue        = misc  | 296;
  public final static int resolution     = misc  | 298;
  public final static int reversecolor   = misc  | 300;
  public final static int rewind         = misc  | 301;
  public final static int right          = misc  | 302;
  public final static int rmsd           = misc  | 303;
  public final static int rna3d          = misc  | 304;
  public final static int rock           = misc  | 305;
  public final static int rotate45       = misc  | 306;
  public final static int rotation       = misc  | 308;
  public final static int rubberband     = misc  | 310;
  public final static int sasurface      = misc  | 311;
  public final static int saved          = misc  | 312;
  public final static int scene          = misc  | 315; // Jmol 12.3.32
  public final static int selection      = misc  | 316;
  public final static int shapely        = misc  | 320;
  public final static int sigma          = misc  | 322;
  public final static int sign           = misc  | 323;
  public final static int silent         = misc  | 324;
  public final static int solid          = misc  | 326;
  public final static int sphere         = misc  | 330;
  public final static int squared        = misc  | 332;
  public final static int state          = misc  | 334;
  public final static int stdinchi       = misc  | 335;
  public final static int stdinchikey    = misc  | 336;
  public final static int stop           = misc  | 338;
  public final static int supercell      = misc  | 339;//
  public final static int ticks          = misc  | 340; 
  public final static int title          = misc  | 342;
  public final static int titleformat    = misc  | 344;
  public final static int to             = misc  | 346;
  public final static int top            = misc  | 348;
  public final static int torsion        = misc  | 350;
  public final static int transform      = misc  | 352;
  public final static int translation    = misc  | 354;
  public final static int triangles      = misc  | 358;
  public final static int url            = misc  | 360;
  public final static int user           = misc  | 362;
  public final static int val            = misc  | 364;
  public final static int validation     = misc  | 365;
  public final static int variable       = misc  | 366;
  public final static int variables      = misc  | 368;
  public final static int vertices       = misc  | 370;
  public final static int spacebeforesquare = misc  | 371;
  public final static int width          = misc  | 372;
  public final static int wigner         = misc  | 373;

  // used to be "expression":
  
  public final static int expressionBegin     = misc | 501;
  public final static int expressionEnd       = misc | 502;
  public final static int all                 = misc | 503;
  public final static int branch              = misc | 504;
  public final static int coord               = misc | 505;
  public final static int dollarsign          = misc | 506;
  public final static int isaromatic          = misc | 507;
  public final static int leftbrace           = misc | 508;
  public final static int none                = misc | 509;
  public final static int off                 = misc | 510;
  public final static int on                  = misc | 511;
  public final static int per                 = misc | 512;
  public final static int perper              = misc | 513;
  public final static int rightbrace          = misc | 514;
  public final static int semicolon           = misc | 515;
  public final static int spec_alternate      = misc | 531;
  public final static int spec_atom           = misc | 532;
  public final static int spec_chain          = misc | 533;
  public final static int spec_model          = misc | 534;  // /3, /4
  public final static int spec_model2         = misc | 535;  // 1.2, 1.3
  public final static int spec_name_pattern   = misc | 536;
  public final static int spec_resid          = misc | 537;
  public final static int spec_seqcode        = misc | 538;
  public final static int spec_seqcode_range  = misc | 539;

  
  // NOTE: It is important that width is the last token. 
  //       build_13_tojs.xml needs to see that to clear out
  //       the unnecessary static defs.

  
  
  // predefined Tokens: 
  
  public final static T tokenSpaceBeforeSquare = o(spacebeforesquare, " ");
  public final static T tokenOn  = tv(on, 1, "on");
  public final static T tokenOff = tv(off, 0, "off");
  public final static T tokenAll = o(all, "all");
  public final static T tokenIf = o(ifcmd, "if");
  public final static T tokenAnd = o(opAnd, "and");
  public final static T tokenAndSpec = o(opAND, "");
  public final static T tokenOr  = o(opOr, "or");
  public final static T tokenAndFALSE = o(opAnd, "and");
  public final static T tokenOrTRUE = o(opOr, "or");
  public final static T tokenOpIf  = o(opIf, "?");
  public final static T tokenComma = o(comma, ",");
  public final static T tokenDefineString = tv(define, string, "@");
  public final static T tokenPlus = o(plus, "+");
  public final static T tokenMinus = o(minus, "-");
  public final static T tokenMul3 = o(mul3, "mul3"); // used only in internal calc.
  public final static T tokenTimes = o(times, "*");
  public final static T tokenDivide = o(divide, "/");

  public final static T tokenLeftParen = o(leftparen, "(");
  public final static T tokenRightParen = o(rightparen, ")");
  public final static T tokenArraySquare = o(array, "[");        // special operator stack flag
  public final static T tokenArrayOpen = o(leftsquare, "[");     // used also as special operand stack flag
  public final static T tokenArrayClose = o(rightsquare, "]");   
  public final static T tokenLeftBrace = o(leftbrace, "{");
 
  public final static T tokenExpressionBegin = o(expressionBegin, "expressionBegin");
  public final static T tokenExpressionEnd   = o(expressionEnd, "expressionEnd");
  public final static T tokenConnected       = o(connected, "connected");
  public final static T tokenCoordinateBegin = o(leftbrace, "{");
  public final static T tokenRightBrace = o(rightbrace, "}");
  public final static T tokenCoordinateEnd = tokenRightBrace;
  public final static T tokenColon           = o(colon, ":");
  public final static T tokenSetCmd          = o(set, "set");
  public final static T tokenSet             = tv(set, '=', "");
  public final static T tokenSetArray        = tv(set, '[', "");
  public final static T tokenSetProperty     = tv(set, '.', "");
  public final static T tokenSetVar          = tv(var, '=', "var");
  public final static T tokenEquals          = o(opEQ, "=");
  public final static T tokenScript          = o(script, "script");
  public final static T tokenSwitch          = o(switchcmd, "switch");
    
  private static Map<String, T> tokenMap = new Hashtable<String, T>();

  public static void addToken(String ident, T token) {
    tokenMap.put(ident, token);
  }
  
  public static T getTokenFromName(String name) {
    // this one needs to NOT be lower case for ScriptCompiler
    return tokenMap.get(name);
  }
  
  public static int getTokFromName(String name) {
    T token = getTokenFromName(name.toLowerCase());
    return (token == null ? nada : token.tok);
  }


  
  /**
   * note: nameOf is a very inefficient mechanism for getting 
   * the name of a token. But it is only used for error messages
   * and listings of variables and such.
   * 
   * @param tok
   * @return     the name of the token or 0xAAAAAA
   */
  public static String nameOf(int tok) {
    for (T token : tokenMap.values()) {
      if (token.tok == tok)
        return "" + token.value;
    }
    return "0x"+Integer.toHexString(tok);
   }
   
  @Override
  public String toString() {
    return toString2();
  }
  
  ////////command sets ///////

  protected String toString2() {
    return "Token["
    + astrType[tok < keyword ? tok : keyword]
    + "("+(tok%(1<<9))+"/0x" + Integer.toHexString(tok) + ")"
    + ((intValue == Integer.MAX_VALUE) ? "" : " intValue=" + intValue
        + "(0x" + Integer.toHexString(intValue) + ")")
    + ((value == null) ? "" : value instanceof String ? " value=\"" + value
        + "\"" : " value=" + value) + "]";
  }

  /**
   * retrieves an unsorted list of viable commands that could be
   * completed by this initial set of characters. If fewer than
   * two characters are given, then only the "preferred" command
   * is given (measure, not monitor, for example), and in all cases
   * if both a singular and a plural might be returned, only the
   * singular is returned.
   * 
   * @param strBegin initial characters of the command, or null
   * @return UNSORTED semicolon-separated string of viable commands
   */
  public static String getCommandSet(String strBegin) {
    String cmds = "";
    Map<String, Boolean> htSet = new Hashtable<String, Boolean>();
    int nCmds = 0;
    String s = (strBegin == null || strBegin.length() == 0 ? null : strBegin
        .toLowerCase());
    boolean isMultiCharacter = (s != null && s.length() > 1);
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      T token = entry.getValue();
      if ((token.tok & scriptCommand) != 0
          && (s == null || name.indexOf(s) == 0)
          && (isMultiCharacter || ((String) token.value).equals(name)))
        htSet.put(name, Boolean.TRUE);
    }
    for (Map.Entry<String, Boolean> entry : htSet.entrySet()) {
      String name = entry.getKey();
      if (name.charAt(name.length() - 1) != 's'
          || !htSet.containsKey(name.substring(0, name.length() - 1)))
        cmds += (nCmds++ == 0 ? "" : ";") + name;
    }
    return cmds;
  }
  
  public static Lst<T> getAtomPropertiesLike(String type) {
    type = type.toLowerCase();
    Lst<T> v = new  Lst<T>();
    boolean isAll = (type.length() == 0);
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      if (name.charAt(0) == '_')
        continue;
      T token = entry.getValue();
      if (tokAttr(token.tok, atomproperty) && (isAll || name.toLowerCase().startsWith(type))) {
        if (isAll || !((String) token.value).toLowerCase().startsWith(type))
          token = o(token.tok, name);
        v.addLast(token);
      }
    }
    return (v.size() == 0 ? null : v);
  }

  public static String[] getTokensLike(String type) {
    int attr = (type.equals("setparam") ? setparam 
        : type.equals("misc") ? misc 
        : type.equals("mathfunc") ? mathfunc : scriptCommand);
    int notattr = (attr == setparam ? deprecatedparam : nada);
    Lst<String> v = new  Lst<String>();
    for (Map.Entry<String, T> entry : tokenMap.entrySet()) {
      String name = entry.getKey();
      T token = entry.getValue();
      if (tokAttr(token.tok, attr) && (notattr == nada || !tokAttr(token.tok, notattr)))
        v.addLast(name);
    }
    String[] a = v.toArray(new String[v.size()]);
    Arrays.sort(a);
    return a;
  }

  public static int getSettableTokFromString(String s) {
    int tok = getTokFromName(s);
    return (tok != nada && tokAttr(tok, settable) 
          && !tokAttr(tok, mathproperty) ? tok : nada);
  }

  public static String completeCommand(Map<String, ?> map, boolean isSet, 
                                       boolean asCommand, 
                                       String str, int n) {
    if (map == null)
      map = tokenMap;
    else
      asCommand = false;
    Lst<String> v = new  Lst<String>();
    str = str.toLowerCase();
    for (String name : map.keySet()) {
      if (!name.startsWith(str))
        continue;
      int tok = getTokFromName(name);
      if (asCommand ? tokAttr(tok, scriptCommand) 
          : isSet ? tokAttr(tok, setparam) && !tokAttr(tok, deprecatedparam) 
          : true)
        v.addLast(name);
    }
    return AU.sortedItem(v, n);
  }

  static {

    // OK for J2S compiler even though T is not final because 
    // tokenMap is private

    Object[] arrayPairs  = {

    // atom expressions

      "(",            tokenLeftParen,
      ")",            tokenRightParen,
      "and",          tokenAnd,
      "&",            null,
      "&&",           null,
      "or",           tokenOr,
      "|",            null,
      "||",           null,
      "?",            tokenOpIf,
      ",",            tokenComma,
      "=",            tokenEquals,
      "==",           null,
      ":",            tokenColon,
      "+",            tokenPlus,
      "-",            tokenMinus,
      "*",            tokenTimes,
      "/",            tokenDivide,
    
    // commands
        
      "script",       tokenScript,
      "source",       null,
      "set",          tokenSetCmd,
      "switch",       tokenSwitch,

      // misc
      
      "all",          tokenAll,
      "off",          tokenOff, 
      "false",        null, 
      "on",           tokenOn,
      "true",         null, 
    };

    T tokenThis, tokenLast = null;
    String sTok, lcase;
    int n = arrayPairs.length - 1;
    for (int i = 0; i < n; i += 2) {
      sTok = (String) arrayPairs[i];
      lcase = sTok.toLowerCase();
      tokenThis = (T) arrayPairs[i + 1];
      if (tokenThis == null)
        tokenThis = tokenLast;
      else if (tokenThis.value == null)
        tokenThis.value = sTok;
      tokenMap.put(lcase, tokenThis);
      tokenLast = tokenThis;
    }
    
    arrayPairs = null;    

    // This two-array system is risky, but I think worth the risk in
    // terms of how much it saves ( KB).

    String[] sTokens = {
        "+=",
        "-=",
        "*=",
        "/=",
        "\\=",
        "&=",
        "|=",
        "not",
        "!",
        "xor",
        //no-- don't do this; it interferes with define
        // "~"
        "tog",
        "<",
        "<=",
        ">=",
        ">",
        "!=",
        "<>",
        "LIKE",
        "within",
        ".",
        "..",
        "[",
        "]",
        "{",
        "}",
        "$",
        "%",
        ";",
        "++",
        "--",
        "**",
        "\\",
        
        // commands
        
        "animation",
        "anim",
        "assign",
        "axes",
        "backbone",
        "background",
        "bind",
        "bondorder",
        "boundbox",
        "boundingBox",
        "break",
        "calculate",
        "capture",
        "cartoon",
        "cartoons",
        "case",
        "catch",
        "cd",
        "center",
        "centre",
        "centerat",
        "cgo",
        "color",
        "colour",
        "compare",
        "configuration",
        "conformation",
        "config",
        "connect",
        "console",
        "contact",
        "contacts",
        "continue",
        "data",
        "default",
        "define",
        "@",
        "delay",
        "delete",
        "density",
        "depth",
        "dipole",
        "dipoles",
        "display",
        "dot",
        "dots",
        "draw",
        "echo",
        "ellipsoid",
        "ellipsoids",
        "else",
        "elseif",
        "end",
        "endif",
        "exit",
        "eval",
        "file",
        "files",
        "font",
        "for",
        "format",
        "frame",
        "frames",
        "frank",
        "function",
        "functions",
        "geosurface",
        "getProperty",
        "goto",
        "halo",
        "halos",
        "helix",
        "helixalpha",
        "helix310",
        "helixpi",
        "hbond",
        "hbonds",
        "help",
        "hide",
        "history",
        "hover",
        "if",
        "in",
        "initialize",
        "invertSelected",
        "isosurface",
        "javascript",
        "label",
        "labels",
        "lcaoCartoon",
        "lcaoCartoons",
        "load",
        "log",
        "loop",
        "measure",
        "measures",
        "monitor",
        "monitors",
        "meshribbon",
        "meshribbons",
        "message",
        "minimize",
        "minimization",
        "mo",
        "model",
        "models",
        "modulation",
        "move",
        "moveTo",
        "mutate",
        "navigate",
        "navigation",
        "nbo",
        "origin",
        "out",
        "parallel",
        "pause",
        "wait",
        "plot",
        "private",
        "plot3d",
        "pmesh",
        "polygon",
        "polyhedra",
        "polyhedron",
        "print",
        "process",
        "prompt",
        "quaternion",
        "quaternions",
        "quit",
        "ramachandran",
        "rama",
        "refresh",
        "reset",
        "unset",
        "restore",
        "restrict",
        "return",
        "ribbon",
        "ribbons",
        "rocket",
        "rockets",
        "rotate",
        "rotateSelected",
        "save",
        "select",
        "selectionHalos",
        "selectionHalo",
        "showSelections",
        "sheet",
        "show",
        "slab",
        "spacefill",
        "cpk",
        "spin",
        "ssbond",
        "ssbonds",
        "star",
        "stars",
        "step",
        "steps",
        "stereo",
        "strand",
        "strands",
        "structure",
        "_structure",
        "strucNo",
        "struts",
        "strut",
        "subset",
        "subsystem",
        "synchronize",
        "sync",
        "trace",
        "translate",
        "translateSelected",
        "try",
        "unbind",
        "unitcell",
        "var",
        "vector",
        "vectors",
        "vibration",
        "while",
        "wireframe",
        "write",
        "zap",
        "zoom",
        "zoomTo",
        
        // show parameters
        
        "atom",
        "atoms",
        "axisangle",
        "basepair",
        "basepairs",
        "orientation",
        "orientations",
        "pdbheader",
        "polymer",
        "polymers",
        "residue",
        "residues",
        "rotation",
        "row",
        "sequence",
        "seqcode",
        "shape",
        "state",
        "symbol",
        "symmetry",
        "spaceGroup",
        "transform",
        "translation",
        "url",
        
        // misc
        
        "_",
        "abs",
        "absolute",
        "_args",
        "acos",
        "add",
        "adpmax",
        "adpmin",
        "align",
        "altloc",
        "altlocs",
        "ambientOcclusion",
        "amino",
        "angle",
        "array",
        "as",
        "atomID",
        "_atomID",
        "_a",
        "atomIndex",
        "atomName",
        "atomno",
        "atomType",
        "atomX",
        "atomY",
        "atomZ",
        "average",
        "babel",
        "babel21",
        "back",
        "backlit",
        "backshell",
        "balls",
        "baseModel",
        "best",
        "beta",
        "bin",
        "bondCount",
        "bonded",
        "bottom",
        "branch",
        "brillouin",
        "bzone",
        "wignerSeitz",
        "cache",
        "carbohydrate",
        "cell",
        "chain",
        "chains",
        "chainNo",
        "chemicalShift",
        "cs",
        "clash",
        "clear",
        "clickable",
        "clipboard",
        "connected",
        "context",
        "constraint",
        "contourLines",
        "coord",
        "coordinates",
        "coords",
        "cos",
        "cross",
        "covalentRadius",
        "covalent",
        "direction",
        "displacement",
        "displayed",
        "distance",
        "div",
        "DNA",
        "domains",
        "dotted",
        "DSSP",
        "DSSR",
        "element",
        "elemno",
        "_e",
        "error",
        "exportScale",
        "fill",
        "find",
        "fixedTemperature",
        "forcefield",
        "formalCharge",
        "charge",
        "eta",
        "front",
        "frontlit",
        "frontOnly",
        "fullylit",
        "fx",
        "fy",
        "fz",
        "fxyz",
        "fux",
        "fuy",
        "fuz",
        "fuxyz",
        "group",
        "groups",
        "group1",
        "groupID",
        "_groupID",
        "_g",
        "groupIndex",
        "hidden",
        "highlight",
        "hkl",
        "hydrophobicity",
        "hydrophobic",
        "hydro",
        "id",
        "identify",
        "ident",
        "image",
        "info",
        "infoFontSize",
        "inline",
        "insertion",
        "insertions",
        "intramolecular",
        "intra",
        "intermolecular",
        "inter",
        "bondingRadius",
        "ionicRadius",
        "ionic",
        "isAromatic",
        "Jmol",
        "JSON",
        "join",
        "keys",
        "last",
        "left",
        "length",
        "lines",
        "list",
        "magneticShielding",
        "ms",
        "mass",
        "max",
        "mep",
        "mesh",
        "middle",
        "min",
        "mlp",
        "mode",
        "modify",
        "modifyOrCreate",
        "modt",
        "modt1",
        "modt2",
        "modt3",
        "modx",
        "mody",
        "modz",
        "modo",
        "modxyz",
        "molecule",
        "molecules",
        "modelIndex",
        "monomer",
        "morph",
        "movie",
        "mouse",
        "mul",
        "mul3",
        "nboCharges",
        "nci",
        "next",
        "noDelay",
        "noDots",
        "noFill",
        "noMesh",
        "none",
        "null",
        "inherit",
        "normal",
        "noBackshell",
        "noContourLines",
        "notFrontOnly",
        "noTriangles",
        "now",
        "nucleic",
        "occupancy",
        "omega",
        "only",
        "opaque",
        "options",
        "partialCharge",
        "phi",
        "pivot",
        "plane",
        "planar",
        "play",
        "playRev",
        "point",
        "points",
        "pointGroup",
        "polymerLength",
        "pop",
        "previous",
        "prev",
        "probe",
        "property",
        "properties",
        "protein",
        "psi",
        "purine",
        "push",
        "PyMOL",
        "pyrimidine",
        "random",
        "range",
        "rasmol",
        "replace",
        "resno",
        "resume",
        "rewind",
        "reverse",
        "right",
        "rmsd",
        "RNA",
        "rna3d",
        "rock",
        "rubberband",
        "saSurface",
        "saved",
        "scale",
        "scene",
        "search",
        "smarts",
        "selected",
        "seqid",
        "shapely",
        "sidechain",
        "sin",
        "site",
        "size",
        "smiles",
        "substructure",
        "solid",
        "sort",
        "specialPosition",
        "sqrt",
        "split",
        "starWidth",
        "starScale",
        "stddev",
        "straightness",
        "structureId",
        "supercell",
        "sub",
        "sum",
        "sum2",
        "surface",
        "surfaceDistance",
        "symop",
        "symops",
        "sx",
        "sy",
        "sz",
        "sxyz",
        "temperature",
        "relativeTemperature",
        "tensor",
        "theta",
        "thisModel",
        "ticks",
        "top",
        "torsion",
        "trajectory",
        "trajectories",
        "translucent",
        "transparent",
        "triangles",
        "trim",
        "type",
        "ux",
        "uy",
        "uz",
        "uxyz",
        "user",
        "valence",
        "vanderWaals",
        "vdw",
        "vdwRadius",
        "visible",
        "volume",
        "vx",
        "vy",
        "vz",
        "vxyz",
        "xyz",
        "w",
        "x",
        "y",
        "z",
  
       // more misc parameters
       "addHydrogens",
       "allConnected",
       "angstroms",
       "anisotropy",
       "append",
       "arc",
       "area",
       "aromatic",
       "arrow",
       "async",
       "audio",
       "auto",
       "axis",
       "barb",
       "binary",
       "blockData",
       "cancel",
       "cap",
       "cavity",
       "centroid",
       "check",
       "checkCIR",
       "chemical",
       "circle",
       "collapsed",
       "col",
       "colorScheme",
       "command",
       "commands",
       "contour",
       "contours",
       "corners",
       "count",
       "criterion",
       "create",
       "crossed",
       "curve",
       "cutoff",
       "cylinder",
       "diameter",
       "discrete",
       "distanceFactor",
       "downsample",
       "drawing",
       "dynamicMeasurements",
       "eccentricity",
       "ed",
       "edges",
       "edgesOnly",
       "energy",
       "exitJmol",
       "faceCenterOffset",
       "filter",
       "first",
       "fixed",
       "fix",
       "flat",
       "fps",
       "from",
       "frontEdges",
       "full",
       "fullPlane",
       "functionXY",
       "functionXYZ",
       "gridPoints",
       "hiddenLinesDashed",
       "homo",
       "ignore",
       "InChI",
       "InChIKey",
       "increment",
       "insideout",
       "interior",
       "intersection",
       "intersect",
       "internal",
       "lattice",
       "line",
       "lineData",
       "link",
       "lobe",
       "lonePair",
       "lp",
       "lumo",
       "macro",
       "manifest",
       "mapProperty",
       "maxSet",
       "menu",
       "minSet",
       "modelBased",
       "molecular",
       "mrc",
       "msms",
       "name",
       "nmr",
       "noCross",
       "noDebug",
       "noEdges",
       "noHead",
       "noLoad",
       "noPlane",
       "object",
       "obj",
       "offset",
       "offsetSide",
       "once",
       "orbital",
       "atomicOrbital",
       "packed",
       "palindrome",
       "parameters",
       "path",
       "pdb",
       "period",
       "periodic",
       "perpendicular",
       "perp",
       "phase",
       "planarParam",
       "pocket",
       "pointsPerAngstrom",
       "radical",
       "rad",
       "reference",
       "remove",
       "resolution",
       "reverseColor",
       "rotate45",
       "selection",
       "sigma",
       "sign",
       "silent",
       "sphere",
       "squared",
       "stdInChI",
       "stdInChIKey",
       "stop",
       "title",
       "titleFormat",
       "to",
       "validation",
       "value",
       "variable",
       "variables",
       "vertices",
       "width",
       "wigner",
  
       // set params
  
       "backgroundModel",
       "celShading",
       "celShadingPower",
       "debug",
       "debugHigh",
       "defaultLattice",
       "measurements",
       "measurement",
       "scale3D",
       "toggleLabel",
       "userColorScheme",
       "throw",
       "timeout",
       "timeouts",
       "window",
       
       // string
       
       "animationMode",
       "appletProxy",
       "atomTypes",
       "axesColor",
       "axis1Color",
       "axis2Color",
       "axis3Color",
       "backgroundColor",
       "bondmode",
       "boundBoxColor",
       "boundingBoxColor",
       "chirality",
       "cipRule",
       "currentLocalPath",
       "dataSeparator",
       "defaultAngleLabel",
       "defaultColorScheme",
       "defaultColors",
       "defaultDirectory",
       "defaultDistanceLabel",
       "defaultDropScript",
       "defaultLabelPDB",
       "defaultLabelXYZ",
       "defaultLoadFilter",
       "defaultLoadScript",
       "defaults",
       "defaultTorsionLabel",
       "defaultVDW",
       "drawFontSize",
       "eds",
       "edsDiff",
//       "edsUrlCutoff",
//       "edsUrlFormat",
//       "edsUrlFormatDiff",
       "energyUnits",
       "fileCacheDirectory",
       "fontsize",
       "helpPath",
       "hoverLabel",
       "language",
       "loadFormat",
       "loadLigandFormat",
       "logFile",
       "measurementUnits",
       "nihResolverFormat",
       "nmrPredictFormat",
       "nmrUrlFormat",
       "pathForAllFiles",
       "picking",
       "pickingStyle",
       "pickLabel",
       "platformSpeed",
       "propertyColorScheme",
       "quaternionFrame",
       "smilesUrlFormat",
       "smiles2dImageFormat",
       "unitCellColor",
  
       // float
       
       "axesOffset",
       "axisOffset",
       "axesScale",
       "axisScale",
       "bondTolerance",
       "cameraDepth",
       "defaultDrawArrowScale",
       "defaultTranslucent",
       "dipoleScale",
       "ellipsoidAxisDiameter",
       "gestureSwipeFactor",
       "hbondsAngleMinimum",
       "hbondHXDistanceMaximum",
       "hbondsDistanceMaximum",
       "hbondNODistanceMaximum",
       "hoverDelay",
       "loadAtomDataTolerance",
       "minBondDistance",
       "minimizationCriterion",
       "minimizationMaxAtoms",
       "modulationScale",
       "mouseDragFactor",
       "mouseWheelFactor",
       "navFPS",
       "navigationDepth",
       "navigationSlab",
       "navigationSpeed",
       "navX",
       "navY",
       "navZ",
       "particleRadius",
       "pointGroupDistanceTolerance",
       "pointGroupLinearTolerance",
       "radius",
       "rotationRadius",
       "scaleAngstromsPerInch",
       "sheetSmoothing",
       "slabRange",
       "solventProbeRadius",
       "spinFPS",
       "spinX",
       "spinY",
       "spinZ",
       "stereoDegrees",
       "strutDefaultRadius",
       "strutLengthMaximum",
       "vectorScale",
       "vectorsCentered",
       "vectorSymmetry",
       "vectorTrail",
       "vibrationPeriod",
       "vibrationScale",
       "visualRange",
  
       // int
  
       "ambientPercent",
       "ambient",
       "animationFps",
       "axesMode",
       "bondRadiusMilliAngstroms",
       "bondingVersion",
       "delayMaximumMs",
       "diffusePercent",
       "diffuse",
       "dotDensity",
       "dotScale",
       "ellipsoidDotCount",
       "helixStep",
       "hermiteLevel",
       "historyLevel",
       "labelpointerwidth",
       "lighting",
       "logLevel",
       "meshScale",
       "minimizationSteps",
       "minPixelSelRadius",
       "percentVdwAtom",
       "perspectiveModel",
       "phongExponent",
       "pickingSpinRate",
       "propertyAtomNumberField",
       "propertyAtomNumberColumnCount",
       "propertyDataColumnCount",
       "propertyDataField",
       "repaintWaitMs",
       "ribbonAspectRatio",
       "contextDepthMax",
       "scriptReportingLevel",
       "showScript",
       "smallMoleculeMaxAtoms",
       "specular",
       "specularExponent",
       "specularPercent",
       "specPercent",
       "specularPower",
       "specpower",
       "strandCount",
       "strandCountForMeshRibbon",
       "strandCountForStrands",
       "strutSpacing",
       "zDepth",
       "zSlab",
       "zshadePower",
  
       // boolean
  
       "allowEmbeddedScripts",
       "allowGestures",
       "allowKeyStrokes",
       "allowModelKit",
       "allowMoveAtoms",
       "allowMultiTouch",
       "allowRotateSelected",
       "antialiasDisplay",
       "antialiasImages",
       "antialiasTranslucent",
       "appendNew",
       "applySymmetryToBonds",
       "atomPicking",
       "allowAudio",
       "autobond",
       "autoFPS",
       "autoplayMovie",
  //               "autoLoadOrientation",
       "axesMolecular",
       "axesOrientationRasmol",
       "axesUnitCell",
       "axesWindow",
       "bondModeOr",
       "bondPicking",
       "bonds",
       "bond",
       "cartoonBaseEdges",
       "cartoonBlocks",
       "cartoonBlockHeight",
       "cartoonsFancy",
       "cartoonFancy",
       "cartoonLadders",
       "cartoonRibose",
       "cartoonRockets",
       "cartoonSteps",
       "chainCaseSensitive",
       "cipRule6Full",
       "colorRasmol",
       "debugScript",
       "defaultStructureDssp",
       "disablePopupMenu",
       "displayCellParameters",
       "showUnitcellInfo", // oops
       "dotsSelectedOnly",
       "dotSurface",
       "doublePrecision",
       "dragSelected",
       "drawHover",
       "drawPicking",
       "dsspCalculateHydrogenAlways",
       "ellipsoidArcs",
       "ellipsoidArrows",
       "ellipsoidAxes",
       "ellipsoidBall",
       "ellipsoidDots",
       "ellipsoidFill",
       "fileCaching",
       "fontCaching",
       "fontScaling",
       "forceAutoBond",
       "fractionalRelative",
   // see commands     "frank",
       "greyscaleRendering",
       "hbondsBackbone",
       "hbondsRasmol",
       "hbondsSolid",
       "hetero",
       "hideNameInPopup",
       "hideNavigationPoint",
       "hideNotSelected",
       "highResolution",
       "hydrogen",
       "hydrogens",
       "imageState",
       "isKiosk",
       "isosurfaceKey",
       "isosurfacePropertySmoothing",
       "isosurfacePropertySmoothingPower",
       "jmolInJSpecView",
       "justifyMeasurements",
       "languageTranslation",
       "leadAtom",
       "leadAtoms",
       "legacyAutoBonding",
       "legacyHAddition",
       "legacyJavaFloat",
       "logCommands",
       "logGestures",
       "macroDirectory",
       "measureAllModels",
       "measurementLabels",
       "measurementNumbers",
       "messageStyleChime",
       "minimizationRefresh",
       "minimizationSilent",
       "modelkitMode",
       "modelkit",
       "modulateOccupancy",
       "monitorEnergy",
       "multiplebondbananas",
       "multipleBondRadiusFactor",
       "multipleBondSpacing",
       "multiProcessor",
       "navigateSurface",
       "navigationMode",
       "navigationPeriodic",
       "partialDots",
       "pdbAddHydrogens",
       "pdbGetHeader",
       "pdbSequential",
       "perspectiveDepth",
       "preserveState",
       "rangeSelected",
       "redo",
       "redoMove",
       "refreshing",
       "ribbonBorder",
       "rocketBarrels",
       "saveProteinStructureState",
       "scriptQueue",
       "selectAllModels",
       "selectHetero",
       "selectHydrogen",
   // see commands     "selectionHalos",
       "showAxes",
       "showBoundBox",
       "showBoundingBox",
       "showFrank",
       "showHiddenSelectionHalos",
       "showHydrogens",
       "showKeyStrokes",
       "showMeasurements",
       "showModulationVectors",
       "showMultipleBonds",
       "showNavigationPointAlways",
   // see intparam      "showScript",
       "showTiming",
       "showUnitcell",
       "showUnitcellDetails",
       "slabByAtom",
       "slabByMolecule",
       "slabEnabled",
       "smartAromatic",
       "solvent",
       "solventProbe",
   // see intparam     "specular",
       "ssBondsBackbone",
       "statusReporting",
       "strutsMultiple",
       "syncMouse",
       "syncScript",
       "testFlag1",
       "testFlag2",
       "testFlag3",
       "testFlag4",
       "traceAlpha",
       "twistedSheets",
       "undoAuto",
       "undo",
       "undoMax",
       "undoMove",
//       "useArcBall",
       "useMinimizationThread",
       "useNumberLocalization",
       "waitForMoveTo",
       "windowCentered",
       "wireframeRotation",
       "zeroBasedXyzRasmol",
       "zoomEnabled",
       "zoomHeight",
       "zoomLarge",
       "zShade",
        
    };
    
    int[] iTokens = {
        andequals,                          // "+="
        -1,                                 // "-="
        -1,                                 // "*="
        -1,                                 // "/="
        -1,                                 // "\\="
        -1,                                 // "&="
        -1,                                 // "|="
        opNot,                              // "not"
        -1,                                 // "!"
        opXor,                              // "xor"
         //no-- don't do this; it interferes with define
         // "~"
        opToggle,                           // "tog"
        opLT,                               // "<"
        opLE,                               // "<="
        opGE,                               // ">="
        opGT,                               // ">"
        opNE,                               // "!="
        -1,                                 // "<>"
        opLIKE,                             // "like"
        within,                             // "within"
        per,                                // "."
        perper,                             // ".."
        leftsquare,                         // "["
        rightsquare,                        // "]"
        leftbrace,                          // "{"
        rightbrace,                         // "}"
        dollarsign,                         // "$"
        percent,                            // "%"
        semicolon,                          // ";"
        plusPlus,                           // "++"
        minusMinus,                         // "--"
        timestimes,                         // "**"
        leftdivide,                         // "\\"
         
         // commands
         
        animation,                          // "animation"
        -1,                                 // "anim"
        assign,                             // "assign"
        axes,                               // "axes"
        backbone,                           // "backbone"
        background,                         // "background"
        bind,                               // "bind"
        bondorder,                          // "bondorder"
        boundbox,                           // "boundbox"
        -1,                                 // "boundingBox"
        breakcmd,                           // "break"
        calculate,                          // "calculate"
        capture,                            // "capture"
        cartoon,                            // "cartoon"
        -1,                                 // "cartoons"
        casecmd,                            // "case"
        catchcmd,                           // "catch"
        cd,                                 // "cd"
        center,                             // "center"
        -1,                                 // "centre"
        centerat,                           // "centerat"
        cgo,                                // "cgo"
        color,                              // "color"
        -1,                                 // "colour"
        compare,                            // "compare"
        configuration,                      // "configuration"
        -1,                                 // "conformation"
        -1,                                 // "config"
        connect,                            // "connect"
        console,                            // "console"
        contact,                            // "contact"
        -1,                                 // "contacts"
        continuecmd,                        // "continue"
        data,                               // "data"
        defaultcmd,                         // "default"
        define,                             // "define"
        -1,                                 // "@"
        delay,                              // "delay"
        delete,                             // "delete"
        density,                            // "density"
        depth,                              // "depth"
        dipole,                             // "dipole"
        -1,                                 // "dipoles"
        display,                            // "display"
        dot,                                // "dot"
        dots,                               // "dots"
        draw,                               // "draw"
        echo,                               // "echo"
        ellipsoid,                          // "ellipsoid"
        -1,                                 // "ellipsoids"
        elsecmd,                            // "else"
        elseif,                             // "elseif"
        end,                                // "end"
        endifcmd,                           // "endif"
        exit,                               // "exit"
        eval,                               // "eval"
        file,                               // "file"
        -1,                                 // "files"
        font,                               // "font"
        forcmd,                             // "for"
        format,                             // "format"
        frame,                              // "frame"
        -1,                                 // "frames"
        frank,                              // "frank"
        function,                           // "function"
        -1,                                 // "functions"
        geosurface,                         // "geosurface"
        getproperty,                        // "getProperty"
        gotocmd,                            // "goto"
        halo,                               // "halo"
        -1,                                 // "halos"
        helix,                              // "helix"
        helixalpha,                         // "helixalpha"
        helix310,                           // "helix310"
        helixpi,                            // "helixpi"
        hbond,                              // "hbond"
        -1,                                 // "hbonds"
        help,                               // "help"
        hide,                               // "hide"
        history,                            // "history"
        hover,                              // "hover"
        ifcmd,                              // "if"
        in,                                 // "in"
        initialize,                         // "initialize"
        invertSelected,                     // "invertSelected"
        isosurface,                         // "isosurface"
        javascript,                         // "javascript"
        label,                              // "label"
        -1,                                 // "labels"
        lcaocartoon,                        // "lcaoCartoon"
        -1,                                 // "lcaoCartoons"
        load,                               // "load"
        log,                                // "log"
        loop,                               // "loop"
        measure,                            // "measure"
        -1,                                 // "measures"
        -1,                                 // "monitor"
        -1,                                 // "monitors"
        meshRibbon,                         // "meshribbon"
        -1,                                 // "meshribbons"
        message,                            // "message"
        minimize,                           // "minimize"
        -1,                                 // "minimization"
        mo,                                 // "mo"
        model,                              // "model"
        -1,                                 // "models"
        modulation,                         // "modulation"
        move,                               // "move"
        moveto,                             // "moveTo"
        mutate,                             // "mutate"
        navigate,                           // "navigate"
        -1,                                 // "navigation"
        nbo,                                // "nbo"
        origin,                             // "origin"
        out,                                // "out"
        parallel,                           // "parallel"
        pause,                              // "pause"
        -1,                                 // "wait"
        plot,                               // "plot"
        privat,                             // "private"
        plot3d,                             // "plot3d"
        pmesh,                              // "pmesh"
        polygon,                            // "polygon"
        polyhedra,                          // "polyhedra"
        -1,                                 // "polyhedron"
        print,                              // "print"
        process,                            // "process"
        prompt,                             // "prompt"
        quaternion,                         // "quaternion"
        -1,                                 // "quaternions"
        quit,                               // "quit"
        ramachandran,                       // "ramachandran"
        -1,                                 // "rama"
        refresh,                            // "refresh"
        reset,                              // "reset"
        -1,                                 // "unset"
        restore,                            // "restore"
        restrict,                           // "restrict"
        returncmd,                          // "return"
        ribbon,                             // "ribbon"
        -1,                                 // "ribbons"
        rocket,                             // "rocket"
        -1,                                 // "rockets"
        rotate,                             // "rotate"
        rotateSelected,                     // "rotateSelected"
        save,                               // "save"
        select,                             // "select"
        selectionhalos,                     // "selectionHalos"
        -1,                                 // "selectionHalo"
        -1,                                 // "showSelections"
        sheet,                              // "sheet"
        show,                               // "show"
        slab,                               // "slab"
        spacefill,                          // "spacefill"
        -1,                                 // "cpk"
        spin,                               // "spin"
        ssbond,                             // "ssbond"
        -1,                                 // "ssbonds"
        star,                               // "star"
        -1,                                 // "stars"
        step,                               // "step"
        -1,                                 // "steps"
        stereo,                             // "stereo"
        strands,                            // "strand"
        -1,                                 // "strands"
        structure,                          // "structure"
        -1,                                 // "_structure"
        strucno,                            // "strucNo"
        struts,                             // "struts"
        -1,                                 // "strut"
        subset,                             // "subset"
        subsystem,                          // "subsystem"
        sync,                               // "synchronize"
        -1,                                 // "sync"
        trace,                              // "trace"
        translate,                          // "translate"
        translateSelected,                  // "translateSelected"
        trycmd,                             // "try"
        unbind,                             // "unbind"
        unitcell,                           // "unitcell"
        var,                                // "var"
        vector,                             // "vector"
        -1,                                 // "vectors"
        vibration,                          // "vibration"
        whilecmd,                           // "while"
        wireframe,                          // "wireframe"
        write,                              // "write"
        zap,                                // "zap"
        zoom,                               // "zoom"
        zoomTo,                             // "zoomTo"
         
         // show parameters
         
        atoms,                              // "atom"
        -1,                                 // "atoms"
        axisangle,                          // "axisangle"
        basepair,                           // "basepair"
        -1,                                 // "basepairs"
        orientation,                        // "orientation"
        -1,                                 // "orientations"
        pdbheader,                          // "pdbheader"
        polymer,                            // "polymer"
        -1,                                 // "polymers"
        residue,                            // "residue"
        -1,                                 // "residues"
        rotation,                           // "rotation"
        row,                                // "row"
        sequence,                           // "sequence"
        seqcode,                            // "seqcode"
        shape,                              // "shape"
        state,                              // "state"
        symbol,                             // "symbol"
        symmetry,                           // "symmetry"
        spacegroup,                         // "spaceGroup"
        transform,                          // "transform"
        translation,                        // "translation"
        url,                                // "url"
         
         // misc
         
        __,                                  // "__" (getProperty function)
        abs,                                // "abs"
        absolute,                           // "absolute"
        _args,                              // "_args"
        acos,                               // "acos"
        add,                                // "add"
        adpmax,                             // "adpmax"
        adpmin,                             // "adpmin"
        align,                              // "align"
        altloc,                             // "altloc"
        -1,                                 // "altlocs"
        ambientocclusion,                   // "ambientOcclusion"
        amino,                              // "amino"
        angle,                              // "angle"
        array,                              // "array"
        as,                                 // "as"
        atomid,                             // "atomID"
        -1,                                 // "_atomID"
        -1,                                 // "_a"
        atomindex,                          // "atomIndex"
        atomname,                           // "atomName"
        atomno,                             // "atomno"
        atomtype,                           // "atomType"
        atomx,                              // "atomX"
        atomy,                              // "atomY"
        atomz,                              // "atomZ"
        average,                            // "average"
        babel,                              // "babel"
        babel21,                            // "babel21"
        back,                               // "back"
        backlit,                            // "backlit"
        backshell,                          // "backshell"
        balls,                              // "balls"
        basemodel,                          // "baseModel"
        best,                               // "best"
        beta,
        bin,                                // "bin"
        bondcount,                          // "bondCount"
        bonded,                             // "bonded"
        bottom,                             // "bottom"
        branch,                             // "branch"
        brillouin,                          // "brillouin"
        -1,                                 // "bzone"
        -1,                                 // "wignerSeitz"
        cache,                              // "cache"
        carbohydrate,                       // "carbohydrate"
        cell,                               // "cell"
        chain,                              // "chain"
        -1,                                 // "chains"
        chainno,                            // "chainNo"
        chemicalshift,                      // "chemicalShift"
        -1,                                 // "cs"
        clash,                              // "clash"
        clear,                              // "clear"
        clickable,                          // "clickable"
        clipboard,                          // "clipboard"
        connected,                          // "connected"
        context,                            // "context"
        constraint,                         // "constraint"
        contourlines,                       // "contourLines"
        coord,                              // "coord"
        -1,                                 // "coordinates"
        -1,                                 // "coords"
        cos,                                // "cos"
        cross,                              // "cross"
        covalentradius,                     // "covalentRadius"
        -1,                                 // "covalent"
        direction,                          // "direction"
        displacement,                       // "displacement"
        displayed,                          // "displayed"
        distance,                           // "distance"
        div,                                // "div"
        dna,                                // "DNA"
        domains,                            // "domains"
        dotted,                             // "dotted"
        dssp,                               // "DSSP"
        dssr,                               // "DSSR"
        element,                            // "element"
        elemno,                             // "elemno"
        elemisono,                          // "_e"
        error,                              // "error"
        exportscale,                        // "exportScale"
        fill,                               // "fill"
        find,                               // "find"
        fixedtemp,                          // "fixedTemperature"
        forcefield,                         // "forcefield"
        formalcharge,                       // "formalCharge"
        -1,                                 // "charge"
        eta,                                // "eta"
        front,                              // "front"
        frontlit,                           // "frontlit"
        frontonly,                          // "frontOnly"
        fullylit,                           // "fullylit"
        fracx,                              // "fx"
        fracy,                              // "fy"
        fracz,                              // "fz"
        fracxyz,                            // "fxyz"
        fux,                                // "fux"
        fuy,                                // "fuy"
        fuz,                                // "fuz"
        fuxyz,                              // "fuxyz"
        group,                              // "group"
        -1,                                 // "groups"
        group1,                             // "group1"
        groupid,                            // "groupID"
        -1,                                 // "_groupID"
        -1,                                 // "_g"
        groupindex,                         // "groupIndex"
        hidden,                             // "hidden"
        highlight,                          // "highlight"
        hkl,                                // "hkl"
        hydrophobicity,                     // "hydrophobicity"
        -1,                                 // "hydrophobic"
        -1,                                 // "hydro"
        id,                                 // "id"
        identify,                           // "identify"
        -1,                                 // "ident"
        image,                              // "image"
        info,                               // "info"
        infofontsize,                       // "infofontsize"
        inline,                             // "inline"
        insertion,                          // "insertion"
        -1,                                 // "insertions"
        intramolecular,                     // "intramolecular"
        -1,                                 // "intra"
        intermolecular,                     // "intermolecular"
        -1,                                 // "inter"
        bondingradius,                      // "bondingRadius"
        -1,                                 // "ionicRadius"
        -1,                                 // "ionic"
        isaromatic,                         // "isAromatic"
        jmol,                               // "Jmol"
        json,                               // "JSON"
        join,                               // "join"
        keys,                               // "keys"
        last,                               // "last"
        left,                               // "left"
        length,                             // "length"
        lines,                              // "lines"
        list,                               // "list"
        magneticshielding,                  // "magneticShielding"
        -1,                                 // "ms"
        mass,                               // "mass"
        max,                                // "max"
        mep,                                // "mep"
        mesh,                               // "mesh"
        middle,                             // "middle"
        min,                                // "min"
        mlp,                                // "mlp"
        mode,                               // "mode"
        modify,                             // "modify"
        modifyorcreate,                     // "modifyOrCreate"
        modt1,                              // "modt"
        -1,                                 // "modt1"
        modt2,
        modt3,
        modx,                               // "modx"
        mody,                               // "mody"
        modz,                               // "modz"
        modo,                               // "modo"
        modxyz,                             // "vxyz"
        molecule,                           // "molecule"
        -1,                                 // "molecules"
        modelindex,                         // "modelIndex"
        monomer,                            // "monomer"
        morph,                              // "morph"
        movie,                              // "movie"
        mouse,                              // "mouse"
        mul,                                // "mul"
        mul3,                               // "mul3"
        nbocharges,                         // "nbocharges"
        nci,                                // "nci"
        next,                               // "next"
        nodelay,
        nodots,                             // "noDots"
        nofill,                             // "noFill"
        nomesh,                             // "noMesh"
        none,                               // "none"
        -1,                                 // "null"
        -1,                                 // "inherit"
        normal,                             // "normal"
        nobackshell,                        // "nobackshell"
        nocontourlines,                     // "noContourLines"
        notfrontonly,                       // "notFrontOnly"
        notriangles,                        // "noTriangles"
        now,                                // "now"
        nucleic,                            // "nucleic"
        occupancy,                          // "occupancy"
        omega,                              // "omega"
        only,                               // "only"
        opaque,                             // "opaque"
        options,                            // "options"
        partialcharge,                      // "partialCharge"
        phi,                                // "phi"
        pivot,                              // "pivot"
        plane,                              // "plane"
        -1,                                 // "planar"
        play,                               // "play"
        playrev,                            // "playRev"
        point,                              // "point"
        -1,                                 // "points"
        pointgroup,                         // "pointGroup"
        polymerlength,                      // "polymerLength"
        pop,                                // "pop"
        prev,                               // "previous"
        -1,                                 // "prev"
        probe,                              // "probe"
        property,                           // "property"
        -1,                                 // "properties"
        protein,                            // "protein"
        psi,                                // "psi"
        purine,                             // "purine"
        push,                               // "push"
        pymol,                              // "PyMOL"
        pyrimidine,                         // "pyrimidine"
        random,                             // "random"
        range,                              // "range"
        rasmol,                             // "rasmol"
        replace,                            // "replace"
        resno,                              // "resno"
        resume,                             // "resume"
        rewind,                             // "rewind"
        reverse,                            // "reverse"
        right,                              // "right"
        rmsd,                               // "rmsd"
        rna,                                // "RNA"
        rna3d,                              // "rna3d"
        rock,                               // "rock"
        rubberband,                         // "rubberband"
        sasurface,                          // "saSurface"
        saved,                              // "saved"
        scale,                              // "scale"
        scene,                              // "scene"
        search,                             // "search"
        -1,                                 // "smarts"
        selected,                           // "selected"
        seqid,                              // "seqid"
        shapely,                            // "shapely"
        sidechain,                          // "sidechain"
        sin,                                // "sin"
        site,                               // "site"
        size,                               // "size"
        smiles,                             // "smiles"
        substructure,                       // "substructure"
        solid,                              // "solid"
        sort,                               // "sort"
        specialposition,                    // "specialPosition"
        sqrt,                               // "sqrt"
        split,                              // "split"
        starwidth,                         // "starWidth"
        -1,                                 // "starScale" // too confusing
        stddev,                             // "stddev"
        straightness,                       // "straightness"
        strucid,                            // "structureId"
        supercell,                          // "supercell"
        sub,                                // "sub"
        sum,                                // "sum"
        sum2,                               // "sum2"
        surface,                            // "surface"
        surfacedistance,                    // "surfaceDistance"
        symop,                              // "symop"
        -1,
        screenx,                            // "sx"
        screeny,                            // "sy"
        screenz,                            // "sz"
        screenxyz,                          // "sxyz"
        temperature,                        // "temperature"
        -1,                                 // "relativeTemperature"
        tensor,                             // "tensor"
        theta,                              // "theta"
        thismodel,                          // "thisModel"
        ticks,                              // "ticks"
        top,                                // "top"
        torsion,                            // "torsion"
        trajectory,                         // "trajectory"
        -1,                                 // "trajectories"
        translucent,                        // "translucent"
        -1,                                 // "transparent"
        triangles,                          // "triangles"
        trim,                               // "trim"
        type,                               // "type"
        unitx,                              // "ux"
        unity,                              // "uy"
        unitz,                              // "uz"
        unitxyz,                            // "uxyz"
        user,                               // "user"
        valence,                            // "valence"
        vanderwaals,                        // "vanderWaals"
        -1,                                 // "vdw"
        -1,                                 // "vdwRadius"
        visible,                            // "visible"
        volume,                             // "volume"
        vibx,                               // "vx"
        viby,                               // "vy"
        vibz,                               // "vz"
        vibxyz,                             // "vxyz"
        xyz,                                // "xyz"
        w,                                  // "w"
        x,                                  // "x"
        y,                                  // "y"
        z,                                  // "z"

                // more misc parameters
        addhydrogens,                       //        "addHydrogens"
        allconnected,                       //        "allConnected"
        angstroms,                          //        "angstroms"
        anisotropy,                         //        "anisotropy"
        append,                             //        "append"
        arc,                                //        "arc"
        area,                               //        "area"
        aromatic,                           //        "aromatic"
        arrow,                              //        "arrow"
        async,                              //        "async"
        audio,                              //        "audio"
        auto,                               //        "auto"
        axis,                               //        "axis"
        barb,                               //        "barb"
        binary,                             //        "binary"
        blockdata,                          //        "blockData"
        cancel,                             //        "cancel"
        cap,                                //        "cap"
        cavity,                             //        "cavity"
        centroid,                           //        "centroid"
        check,                              //        "check"
        checkcir,
        chemical,                           //        "chemical"
        circle,                             //        "circle"
        collapsed,                          //        "collapsed"
        col,                                //        "col"
        colorscheme,                        //        "colorScheme"
        command,                            //        "command"
        commands,                           //        "commands"
        contour,                            //        "contour"
        contours,                           //        "contours"
        corners,                            //        "corners"
        count,                              //        "count"
        criterion,                          //        "criterion"
        create,                             //        "create"
        crossed,                            //        "crossed"
        curve,                              //        "curve"
        cutoff,                             //        "cutoff"
        cylinder,                           //        "cylinder"
        diameter,                           //        "diameter"
        discrete,                           //        "discrete"
        distancefactor,                     //        "distanceFactor"
        downsample,                         //        "downsample"
        drawing,                            //        "drawing"
        dynamicmeasurements,                //        "dynamicMeasurements"
        eccentricity,                       //        "eccentricity"
        ed,                                 //        "ed"
        edges,                              //        "edges"
        edgesonly,                          //        "edgesonly" - Polyhedra
        energy,                             //        "energy"
        exitjmol,                           //        "exitJmol"
        facecenteroffset,                   //        "faceCenterOffset"
        filter,                             //        "filter"
        first,                              //        "first"
        fixed,                              //        "fixed"
        -1,                                 //        "fix"
        flat,                               //        "flat"
        fps,                                //        "fps"
        from,                               //        "from"
        frontedges,                         //        "frontEdges"
        full,                               //        "full"
        fullplane,                          //        "fullPlane"
        functionxy,                         //        "functionXY"
        functionxyz,                        //        "functionXYZ"
        gridpoints,                         //        "gridPoints"
        hiddenlinesdashed,                  //        "hiddenLinesDashed"
        homo,                               //        "homo"
        ignore,                             //        "ignore"
        inchi,                              //        "InChI"
        inchikey,                           //        "InChIKey"
        increment,                          //        "increment"
        insideout,                          //        "insideout"
        interior,                           //        "interior"
        intersection,                       //        "intersection"
        -1,                                 //        "intersect"
        internal,                           //        "internal"
        lattice,                            //        "lattice"
        line,                               //        "line"
        linedata,                           //        "lineData"
        link,                               //        "link"
        lobe,                               //        "lobe"
        lonepair,                           //        "lonePair"
        lp,                                 //        "lp"
        lumo,                               //        "lumo"
        macro,                              //        "macro"  // added in Jmol 14.3.15
        manifest,                           //        "manifest"
        mapproperty,                        //        "mapProperty"
        maxset,                             //        "maxSet"
        menu,                               //        "menu"
        minset,                             //        "minSet"
        modelbased,                         //        "modelBased"
        molecular,                          //        "molecular"
        mrc,                                //        "mrc"
        msms,                               //        "msms"
        name,                               //        "name"
        nmr,                                //        "nmr"
        nocross,                            //        "noCross"
        nodebug,                            //        "noDebug"
        noedges,                            //        "noEdges"
        nohead,                             //        "noHead"
        noload,                             //        "noLoad"
        noplane,                            //        "noPlane"
        object,                             //        "object"
        obj,                                //        "obj"
        offset,                             //        "offset"
        offsetside,                         //        "offsetSide"
        once,                               //        "once"
        orbital,                            //        "orbital"
        atomicorbital,                      //        "atomicOrbital"
        packed,                             //        "packed"
        palindrome,                         //        "palindrome"
        parameters,                         //        "parameters"
        path,                               //        "path"
        pdb,                                //        "pdb"
        period,                             //        "period"
        -1,                                 //        "periodic"
        perpendicular,                      //        "perpendicular"
        -1,                                 //        "perp"
        phase,                              //        "phase"
        planarparam,                        //        "planarparam"
        pocket,                             //        "pocket"
        pointsperangstrom,                  //        "pointsPerAngstrom"
        radical,                            //        "radical"
        rad,                                //        "rad"
        reference,                          //        "reference"
        remove,                             //        "remove"
        resolution,                         //        "resolution"
        reversecolor,                       //        "reverseColor"
        rotate45,                           //        "rotate45"
        selection,                          //        "selection"
        sigma,                              //        "sigma"
        sign,                               //        "sign"
        silent,                             //        "silent"
        sphere,                             //        "sphere"
        squared,                            //        "squared"
        stdinchi,                           //        "stdInChI"
        stdinchikey,                        //        "stdInChIKey"
        stop,                               //        "stop"
        title,                              //        "title"
        titleformat,                        //        "titleFormat"
        to,                                 //        "to"
        validation,                         //        "validation"
        val,                                //        "value"
        variable,                           //        "variable"
        variables,                          //        "variables"
        vertices,                           //        "vertices"
        width,                              //        "width"
        wigner,                             //        "wigner"

                // set params

        backgroundmodel,                    //        "backgroundModel"
        celshading,                         //        "celShading"
        celshadingpower,                    //        "celShadingPower"
        debug,                              //        "debug"
        debughigh,                          //        "debugHigh"
        defaultlattice,                     //        "defaultLattice"
        measurements,                       //        "measurements"
        -1,                                 //        "measurement"
        scale3d,                            //        "scale3D"
        togglelabel,                        //        "toggleLabel"
        usercolorscheme,                    //        "userColorScheme"
        throwcmd,                           //        "throw"
        timeout,                            //        "timeout"
        -1,                                 //        "timeouts"
        window,                             //        "window"
                
                // string
                
        animationmode,                      //        "animationMode"
        appletproxy,                        //        "appletProxy"
        atomtypes,                          //        "atomTypes"
        axescolor,                          //        "axesColor"
        axis1color,                         //        "axis1Color"
        axis2color,                         //        "axis2Color"
        axis3color,                         //        "axis3Color"
        backgroundcolor,                    //        "backgroundColor"
        bondmode,                           //        "bondmode"
        boundboxcolor,                      //        "boundBoxColor"
        -1,                                 //        "boundingBoxColor"
        chirality,
        ciprule,
        currentlocalpath,                   //        "currentLocalPath"
        dataseparator,                      //        "dataSeparator"
        defaultanglelabel,                  //        "defaultAngleLabel"
        defaultcolorscheme,                 //        "defaultColorScheme"
        -1,                                 //        "defaultColors"
        defaultdirectory,                   //        "defaultDirectory"
        defaultdistancelabel,               //        "defaultDistanceLabel"
        defaultdropscript,                  //        "defaultDropScript"
        defaultlabelpdb,                    //        "defaultLabelPDB"
        defaultlabelxyz,                    //        "defaultLabelXYZ"
        defaultloadfilter,                  //        "defaultLoadFilter"
        defaultloadscript,                  //        "defaultLoadScript"
        defaults,                           //        "defaults"
        defaulttorsionlabel,                //        "defaultTorsionLabel"
        defaultvdw,                         //        "defaultVDW"
        drawfontsize,                       //        "drawFontSize"
        eds,                                //        "eds"
        edsdiff,                            //        "edsDiff"
//        edsurlcutoff,                       //        "edsUrlCutoff"
//        edsurlformat,                       //        "edsUrlFormat"
//        edsurlformatdiff,                   //        "edsUrlFormatDiff"
        energyunits,                        //        "energyUnits"
        filecachedirectory,                 //        "fileCacheDirectory"
        fontsize,                           //        "fontsize"
        helppath,                           //        "helpPath"
        hoverlabel,                         //        "hoverLabel"
        language,                           //        "language"
        loadformat,                         //        "loadFormat"
        loadligandformat,                   //        "loadLigandFormat"
        logfile,                            //        "logFile"
        measurementunits,                   //        "measurementUnits"
        nihresolverformat,                  //        "nihResolverFormat"
        nmrpredictformat,                   //        "nmrPredictFormat"
        nmrurlformat,                       //        "nmrUrlFormat"
        pathforallfiles,                    //        "pathForAllFiles"
        picking,                            //        "picking"
        pickingstyle,                       //        "pickingStyle"
        picklabel,                          //        "pickLabel"
        platformspeed,                      //        "platformSpeed"
        propertycolorscheme,                //        "propertyColorScheme"
        quaternionframe,                    //        "quaternionFrame"
        smilesurlformat,                    //        "smilesUrlFormat"
        smiles2dimageformat,                //        "smiles2dImageFormat"
        unitcellcolor,                      //        "unitCellColor"

                // float
                
        axesoffset,                          //       "axesOffset"
        -1,                                 //        "axisOffset"
        axesscale,                          //        "axesScale"
        -1,                                 //        "axisScale"
        bondtolerance,                      //        "bondTolerance"
        cameradepth,                        //        "cameraDepth"
        defaultdrawarrowscale,              //        "defaultDrawArrowScale"
        defaulttranslucent,                 //        "defaultTranslucent"
        dipolescale,                        //        "dipoleScale"
        ellipsoidaxisdiameter,              //        "ellipsoidAxisDiameter"
        gestureswipefactor,                 //        "gestureSwipeFactor"
        hbondsangleminimum,                 //        "hbondsAngleMinimum"
        hbondhxdistancemaximum,             //        "hbondHDistanceMaximum"
        hbondnodistancemaximum,              //        "hbondsDistanceMaximum"
        -1,
        hoverdelay,                         //        "hoverDelay"
        loadatomdatatolerance,              //        "loadAtomDataTolerance"
        minbonddistance,                    //        "minBondDistance"
        minimizationcriterion,              //        "minimizationCriterion"
        minimizationmaxatoms,               //        "minimizationMaxAtom"
        modulationscale,                    //        "modulationScale"
        mousedragfactor,                    //        "mouseDragFactor"
        mousewheelfactor,                   //        "mouseWheelFactor"
        navfps,                             //        "navFPS"
        navigationdepth,                    //        "navigationDepth"
        navigationslab,                     //        "navigationSlab"
        navigationspeed,                    //        "navigationSpeed"
        navx,                               //        "navX"
        navy,                               //        "navY"
        navz,                               //        "navZ"
        particleradius,                     //        "particleRadius"
        pointgroupdistancetolerance,        //        "pointGroupDistanceTolerance"
        pointgrouplineartolerance,          //        "pointGroupLinearTolerance"
        radius,                             //        "radius"
        rotationradius,                     //        "rotationRadius"
        scaleangstromsperinch,              //        "scaleAngstromsPerInch"
        sheetsmoothing,                     //        "sheetSmoothing"
        slabrange,                          //        "slabRange"
        solventproberadius,                 //        "solventProbeRadius"
        spinfps,                            //        "spinFPS"
        spinx,                              //        "spinX"
        spiny,                              //        "spinY"
        spinz,                              //        "spinZ"
        stereodegrees,                      //        "stereoDegrees"
        strutdefaultradius,                 //        "strutDefaultRadius"
        strutlengthmaximum,                 //        "strutLengthMaximum"
        vectorscale,                        //        "vectorScale"
        vectorscentered,                    //        "vectorsCenered"
        vectorsymmetry,                     //        "vectorSymmetry"
        vectortrail,                        //        "vectorTrail"
        vibrationperiod,                    //        "vibrationPeriod"
        vibrationscale,                     //        "vibrationScale"
        visualrange,                        //        "visualRange"

                // int

        ambientpercent,                     //        "ambientPercent"
        -1,                                 //        "ambient"
        animationfps,                       //        "animationFps"
        axesmode,                           //        "axesMode"
        bondradiusmilliangstroms,           //        "bondRadiusMilliAngstroms"
        bondingversion,                     //        "bondingVersion"
        delaymaximumms,                     //        "delayMaximumMs"
        diffusepercent,                     //        "diffusePercent"
        -1,                                 //        "diffuse"
        dotdensity,                         //        "dotDensity"
        dotscale,                           //        "dotScale"
        ellipsoiddotcount,                  //        "ellipsoidDotCount"
        helixstep,                          //        "helixStep"
        hermitelevel,                       //        "hermiteLevel"
        historylevel,                       //        "historyLevel"
        labelpointerwidth,                  //        "labelpointerwidth"
        lighting,                           //        "lighting"
        loglevel,                           //        "logLevel"
        meshscale,                          //        "meshScale"
        minimizationsteps,                  //        "minimizationSteps"
        minpixelselradius,                  //        "minPixelSelRadius"
        percentvdwatom,                     //        "percentVdwAtom"
        perspectivemodel,                   //        "perspectiveModel"
        phongexponent,                      //        "phongExponent"
        pickingspinrate,                    //        "pickingSpinRate"
        propertyatomnumberfield,            //        "propertyAtomNumberField"
        propertyatomnumbercolumncount,      //        "propertyAtomNumberColumnCount"
        propertydatacolumncount,            //        "propertyDataColumnCount"
        propertydatafield,                  //        "propertyDataField"
        repaintwaitms,                      //        "repaintWaitMs"
        ribbonaspectratio,                  //        "ribbonAspectRatio"
        contextdepthmax,                     //        "scriptLevelMax"
        scriptreportinglevel,               //        "scriptReportingLevel"
        showscript,                         //        "showScript"
        smallmoleculemaxatoms,              //        "smallMoleculeMaxAtoms"
        specular,                           //        "specular"
        specularexponent,                   //        "specularExponent"
        specularpercent,                    //        "specularPercent"
        -1,                                 //        "specPercent"
        specularpower,                      //        "specularPower"
        -1,                                 //        "specpower"
        strandcount,                        //        "strandCount"
        strandcountformeshribbon,           //        "strandCountForMeshRibbon"
        strandcountforstrands,              //        "strandCountForStrands"
        strutspacing,                       //        "strutSpacing"
        zdepth,                             //        "zDepth"
        zslab,                              //        "zSlab"
        zshadepower,                        //        "zshadePower"

                // boolean

        allowembeddedscripts,               //        "allowEmbeddedScripts"
        allowgestures,                      //        "allowGestures"
        allowkeystrokes,                    //        "allowKeyStrokes"
        allowmodelkit,                      //        "allowModelKit"
        allowmoveatoms,                     //        "allowMoveAtoms"
        allowmultitouch,                    //        "allowMultiTouch"
        allowrotateselected,                //        "allowRotateSelected"
        antialiasdisplay,                   //        "antialiasDisplay"
        antialiasimages,                    //        "antialiasImages"
        antialiastranslucent,               //        "antialiasTranslucent"
        appendnew,                          //        "appendNew"
        applysymmetrytobonds,               //        "applySymmetryToBonds"
        atompicking,                        //        "atomPicking"
        allowaudio,                         //        "allowAudio"
        autobond,                           //        "autobond"
        autofps,                            //        "autoFPS"
        autoplaymovie,                      //        "autoplayMovie"
//                "autoLoadOrientation"
        axesmolecular,                      //        "axesMolecular"
        axesorientationrasmol,              //        "axesOrientationRasmol"
        axesunitcell,                       //        "axesUnitCell"
        axeswindow,                         //        "axesWindow"
        bondmodeor,                         //        "bondModeOr"
        bondpicking,                        //        "bondPicking"
        bonds,                              //        "bonds"
        -1,                                 //        "bond"
        cartoonbaseedges,                   //        "cartoonBaseEdges"
        cartoonblocks,
        cartoonblockheight,
        cartoonsfancy,                      //        "cartoonsFancy"
        -1,                                 //        "cartoonFancy"
        cartoonladders,                     //        "cartoonLadders"
        cartoonribose,                      //        "cartoonRibose"
        cartoonrockets,                     //        "cartoonRockets"
        cartoonsteps,                       //        
        chaincasesensitive,                 //        "chainCaseSensitive"
        ciprule6full,
        colorrasmol,                        //        "colorRasmol"
        debugscript,                        //        "debugScript"
        defaultstructuredssp,               //        "defaultStructureDssp"
        disablepopupmenu,                   //        "disablePopupMenu"
        displaycellparameters,              //        "displayCellParameters"
        -1,                                 //        "showUnitCellInfo"
        dotsselectedonly,                   //        "dotsSelectedOnly"
        dotsurface,                         //        "dotSurface"
        doubleprecision,
        dragselected,                       //        "dragSelected"
        drawhover,                          //        "drawHover"
        drawpicking,                        //        "drawPicking"
        dsspcalchydrogen,                   //        "dsspCalculateHydrogenAlways"
        ellipsoidarcs,                      //        "ellipsoidArcs"
        ellipsoidarrows,                    //        "ellipsoidArrows"
        ellipsoidaxes,                      //        "ellipsoidAxes"
        ellipsoidball,                      //        "ellipsoidBall"
        ellipsoiddots,                      //        "ellipsoidDots"
        ellipsoidfill,                      //        "ellipsoidFill"
        filecaching,                        //        "fileCaching"
        fontcaching,                        //        "fontCaching"
        fontscaling,                        //        "fontScaling"
        forceautobond,                      //        "forceAutoBond"
        fractionalrelative,                 //        "fractionalRelative"
          // see commands     "frank"
        greyscalerendering,                 //        "greyscaleRendering"
        hbondsbackbone,                     //        "hbondsBackbone"
        hbondsrasmol,                       //        "hbondsRasmol"
        hbondssolid,                        //        "hbondsSolid"
        hetero,                             //        "hetero"
        hidenameinpopup,                    //        "hideNameInPopup"
        hidenavigationpoint,                //        "hideNavigationPoint"
        hidenotselected,                    //        "hideNotSelected"
        highresolution,                     //        "highResolution"
        hydrogen,                           //        "hydrogen"
        -1,                                 //        "hydrogens"
        imagestate,                         //        "imageState"
        iskiosk,                            //        "isKiosk"
        isosurfacekey,                      //        "isosurfaceKey"
        isosurfacepropertysmoothing,        //        "isosurfacePropertySmoothing"
        isosurfacepropertysmoothingpower,   //        "isosurfacePropertySmoothingPower"
        jmolinjspecview,
        justifymeasurements,                //        "justifyMeasurements"
        languagetranslation,                //        "languageTranslation"
        leadatom,                           //        "leadAtom"
        -1,                                 //        "leadAtoms"
        legacyautobonding,                  //        "legacyAutoBonding"
        legacyhaddition,                    //        "legacyHAddition"
        legacyjavafloat,                   //        "legacyRangeCheck"
        logcommands,                        //        "logCommands"
        loggestures,                        //        "logGestures"
        macrodirectory,
        measureallmodels,                   //        "measureAllModels"
        measurementlabels,                  //        "measurementLabels"
        measurementnumbers,                 //        "measurementNumbers"
        messagestylechime,                  //        "messageStyleChime"
        minimizationrefresh,                //        "minimizationRefresh"
        minimizationsilent,                 //        "minimizationSilent"
        modelkitmode,                       //        "modelkitMode"
        -1,
        modulateoccupancy,
        monitorenergy,                      //        "monitorEnergy"
        multiplebondbananas,
        multiplebondradiusfactor,           //        "multipleBondRadiusFactor"
        multiplebondspacing,                //        "multipleBondSpacing"
        multiprocessor,                     //        "multiProcessor"
        navigatesurface,                    //        "navigateSurface"
        navigationmode,                     //        "navigationMode"
        navigationperiodic,                 //        "navigationPeriodic"
        partialdots,                        //        "partialDots"
        pdbaddhydrogens,                    //        "pdbAddHydrogens"
        pdbgetheader,                       //        "pdbGetHeader"
        pdbsequential,                      //        "pdbSequential"
        perspectivedepth,                   //        "perspectiveDepth"
        preservestate,                      //        "preserveState"
        rangeselected,                      //        "rangeSelected"
        redo,                               //        "redo"
        redomove,                           //        "redoMove"
        refreshing,                         //        "refreshing"
        ribbonborder,                       //        "ribbonBorder"
        rocketbarrels,                      //        "rocketBarrels"
        saveproteinstructurestate,          //        "saveProteinStructureState"
        scriptqueue,                        //        "scriptQueue"
        selectallmodels,                    //        "selectAllModels"
        selecthetero,                       //        "selectHetero"
        selecthydrogen,                     //        "selectHydrogen"
          // see commands     "selectionHalos"
        showaxes,                           //        "showAxes"
        showboundbox,                       //        "showBoundBox"
        -1,                                 //        "showBoundingBox"
        showfrank,                          //        "showFrank"
        showhiddenselectionhalos,           //        "showHiddenSelectionHalos"
        showhydrogens,                      //        "showHydrogens"
        showkeystrokes,                     //        "showKeyStrokes"
        showmeasurements,                   //        "showMeasurements"
        showmodvecs,                        //        "showModulationVectors"
        showmultiplebonds,                  //        "showMultipleBonds"
        shownavigationpointalways,          //        "showNavigationPointAlways"
          // see intparam      "showScript"
        showtiming,                         //        "showTiming"
        showunitcell,                       //        "showUnitcell"
        showunitcelldetails,                //        "showUnitcellDetails"
        slabbyatom,                         //        "slabByAtom"
        slabbymolecule,                     //        "slabByMolecule"
        slabenabled,                        //        "slabEnabled"
        smartaromatic,                      //        "smartAromatic"
        solvent,                            //        "solvent"
        solventprobe,                       //        "solventProbe"
          // see intparam     "specular"
        ssbondsbackbone,                    //        "ssBondsBackbone"
        statusreporting,                    //        "statusReporting"
        strutsmultiple,                     //        "strutsMultiple"
        syncmouse,                          //        "syncMouse"
        syncscript,                         //        "syncScript"
        testflag1,                          //        "testFlag1"
        testflag2,                          //        "testFlag2"
        testflag3,                          //        "testFlag3"
        testflag4,                          //        "testFlag4"
        tracealpha,                         //        "traceAlpha"
        twistedsheets,                      //        "twistedSheets"
        undoauto,
        undo,                               //        "undo"
        undomax,                            //        "undoMax"
        undomove,                           //        "undoMove"
//        usearcball,                         //        "useArcBall"
        useminimizationthread,              //        "useMinimizationThread"
        usenumberlocalization,              //        "useNumberLocalization"
        waitformoveto,                      //        "waitForMoveTo"
        windowcentered,                     //        "windowCentered"
        wireframerotation,                  //        "wireframeRotation"
        zerobasedxyzrasmol,                 //        "zeroBasedXyzRasmol"
        zoomenabled,                        //        "zoomEnabled"
        zoomheight,                         //        "zoomHeight"
        zoomlarge,                          //        "zoomLarge"
        zshade,                             //        "zShade"
        
    };
    

    if (sTokens.length != iTokens.length) {
      Logger.error("sTokens.length ("+sTokens.length+") != iTokens.length! ("+iTokens.length+")");
      System.exit(1);
    }

    n = sTokens.length;
    for (int i = 0; i < n; i++) {
      sTok = sTokens[i];
      lcase = sTok.toLowerCase();
      int t = iTokens[i];
      tokenThis = tokenLast = (t == -1 ? tokenLast : o(t, sTok));
      if (tokenMap.get(lcase) != null)
        Logger.error("duplicate token definition:" + lcase);
      tokenMap.put(lcase, tokenThis);
    }
    
    sTokens = null;
    iTokens = null;
  }

  public static int getParamType(int tok) {
    if (!tokAttr(tok, setparam))
      return nada;
    return tok & paramTypes;
  }
  
  public static void getTokensType(Map<String, Object> map, int attr) {
    for (Entry<String, T> e: tokenMap.entrySet()) {
      T t = e.getValue();
      if (tokAttr(t.tok, attr))
        map.put(e.getKey(), e.getValue());
    }
  }

  /**
   * commands that allow implicit ID as first parameter
   * 
   * @param cmdtok
   * @return true or false 
   */
  public static boolean isIDcmd(int cmdtok) {
    switch (cmdtok) {
    case isosurface:
    case draw:
    case cgo:
    case pmesh:
    case contact:
      return true;
    default:
      return false;
    }
  }

  @Override
  public boolean equals(Object o) {
    // only used for arrays.
    if (!(o instanceof T))
      return false;
    T t = (T) o;
    if (tok == t.tok)
      return (t.intValue == intValue && (tok == integer || tok == on || tok == off || t.value
          .equals(value)));
    switch (tok) {
    case integer:
      return (t.tok == decimal && ((Number) t.value).doubleValue() == intValue);
    case decimal:
      return (t.tok == integer && ((Number) value).doubleValue() == t.intValue);
    default:
      return false;
    }
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
