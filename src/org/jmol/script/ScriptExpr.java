package org.jmol.script;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BArray;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.M34;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;
import org.jmol.scriptext.CmdExt;
import org.jmol.scriptext.IsoExt;
import org.jmol.scriptext.MathExt;
import org.jmol.scriptext.ScriptExt;
import org.jmol.scriptext.SmilesExt;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.viewer.JC;

/**
 * The ScriptExpr class holds the main functions for 
 * processing mathematical and atom selection expressions.
 * 
 * The two methods, parameterExpression and atomExpression
 * are the key starting points for this processing.
 * 
 */
abstract class ScriptExpr extends ScriptParam {

  public boolean debugHigh;

  private CmdExt cmdExt;
  public CmdExt getCmdExt() {
    return (cmdExt == null ? cmdExt = (CmdExt) ((ScriptExt) getExt("Cmd")).init(this) : cmdExt);
  }

  private IsoExt isoExt;
  public IsoExt getIsoExt() {
    return (isoExt == null ? isoExt = (IsoExt) ((ScriptExt) getExt("Iso")).init(this) : isoExt);
  }

  private MathExt mathExt;
  public MathExt getMathExt() {
    return (mathExt == null ? (mathExt = (MathExt) getExt("Math")).init(this) : mathExt);
  }

  private SmilesExt smilesExt;
  public SmilesExt getSmilesExt() {
    return (smilesExt == null ? (smilesExt = (SmilesExt) getExt("Smiles")).init(this) : smilesExt);
  }

  private Object getExt(String type) {
    return Interface.getInterface("org.jmol.scriptext." + type + "Ext", vwr, "script");
  }

  @SuppressWarnings("unchecked")
  protected Lst<SV> parameterExpressionList(int pt, int ptAtom,
                                           boolean isArrayItem)
      throws ScriptException {
    
    // isArrayItem will be true for centerParameter with $id[n]
    // in which case pt will be negative 
    
    return (Lst<SV>) parameterExpression(pt, -1, null, true, true, ptAtom,
        isArrayItem, null, null, false);
  }

  protected String parameterExpressionString(int pt, int ptMax)
      throws ScriptException {
    return (String) parameterExpression(pt, ptMax, "", true, false, -1, false,
        null, null, false);
  }

  protected boolean parameterExpressionBoolean(int pt, int ptMax)
      throws ScriptException {
    return ((Boolean) parameterExpression(pt, ptMax, null, true, false, -1,
        false, null, null, false)).booleanValue();
  }

  protected SV parameterExpressionToken(int pt) throws ScriptException {
    Lst<SV> result = parameterExpressionList(pt, -1, false);
    return (result.size() > 0 ? result.get(0) : SV.newS(""));
  }

  protected boolean parameterExpressionSelect(Map<String, SV> h, T[] where) throws ScriptException {
    st = where;
    slen = st.length;
    return ((Boolean) parameterExpression(2, Integer.MIN_VALUE, null, true, false, -1,
        false, h, null, false)).booleanValue();
  }


  /**
   * This is the primary driver of the RPN (reverse Polish notation) expression
   * processor. It handles all math outside of a "traditional" Jmol
   * SELECT/RESTRICT context. [Object atomExpression() takes care of that, and
   * also uses the RPN class.]
   * 
   * @param pt
   *        token index in statement start of expression or negative for one
   *        expression only.
   * @param ptMax
   *        token index in statement end of expression
   * @param key
   *        variable name for debugging reference only -- null indicates return
   *        Boolean -- "" indicates return String
   * @param ignoreComma
   * @param asVector
   *        a flag passed on to RPN;
   * @param ptAtom
   *        this is a for() or select() function with a specific atom selected
   * @param isArrayItem
   *        we are storing A[x] = ... so we need to deliver "x" as well
   * @param localVars
   *        see below -- lists all nested for(x, {exp}, select(y, {ex},...))
   *        variables
   * @param localVar
   *        x or y in above for(), select() examples
   * @param isSpecialAssignment
   *        x[n] = ....
   * @return either a vector or a value, caller's choice.
   * @throws ScriptException
   *         errors are thrown directly to the Eval error system.
   */
  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean ignoreComma, boolean asVector,
                                     int ptAtom, boolean isArrayItem,
                                     Map<String, SV> localVars,
                                     String localVar,
                                     boolean isSpecialAssignment)
      throws ScriptException {

    /*
     * localVar is a variable designated at the beginning of the select(x,...)
     * or for(x,...) construct that will be implicitly used for properties. So,
     * for example, "atomno" will become "x.atomno". That's all it is for.
     * localVars provides a localized context variable set for a given nested
     * set of for/select.
     * 
     * Note that localVars has nothing to do standard if/for/while flow
     * contexts, just these specialized functions. Any variable defined in for
     * or while is simply added to the context for a given script or function.
     * These assignments are made by the compiler when seeing a VAR keyword.
     */
    boolean isImplicitAtomProperty = (localVar != null);
    boolean isWhere = (localVar == null && localVars != null);
    boolean isOneExpressionOnly = (pt < 0);
    boolean returnBoolean = (!asVector && key == null);
    boolean returnString = (!asVector && key != null && key.length() == 0);
    if (isOneExpressionOnly)
      pt = -pt;
    boolean allContext = (localVars == null || ptMax != Integer.MIN_VALUE);
    if (ptMax < pt)
      ptMax = slen;
    int ptEq = (isSpecialAssignment ? 0 : 1);
    int ptWithin = -1;
    ScriptMathProcessor rpn = new ScriptMathProcessor(this,
        isSpecialAssignment, isArrayItem, asVector, false, false, key);
    Object v, res;
    int nSquare = 0;
    int nParen = 0;
    boolean topLevel = true;
    out: for (int i = pt; i < ptMax; i++) {
      v = null;
      int tok = getToken(i).tok;
      if (isImplicitAtomProperty && tokAt(i + 1) != T.per) {
        // local variable definition
        SV token = (localVars != null && localVars.containsKey(theToken.value) ? null
            : getBitsetPropertySelector(i, T.bitset));
        if (token != null) {
          rpn.addX(localVars.get(localVar));
          if (!rpn.addOpAllowMath(token, (tokAt(i + 1) == T.leftparen), T.nada))
            invArg();
          if ((token.intValue == T.function || token.intValue == T.parallel)
              && tokAt(iToken + 1) != T.leftparen) {
            rpn.addOp(T.tokenLeftParen);
            rpn.addOp(T.tokenRightParen);
          }
          i = iToken;
          continue;
        }
      }
      switch (tok) {
      case T.rightsquare:
      case T.rightbrace:
        if (!ignoreComma && topLevel)
          // end of an associative array
          break out;
        if (tok == T.rightbrace)
          invArg();
        if (isSpecialAssignment && nSquare == 1 && tokAt(i + 1) == T.opEQ)
          isSpecialAssignment = rpn.endAssignment();
      }

      switch (tok) {
      case T.define:
        if ((tok = tokAt(++i)) == T.expressionBegin) {
          // @{@x} or @{@{x}} or @{@1} -- also userFunction(@1)
          v = parameterExpressionToken(++i);
          i = iToken;
        } else if (tok == T.integer) {
          // @3
          v = vwr.ms.getAtoms(T.atomno, Integer.valueOf(st[i].intValue));
          break;
        } else if (tok == T.define && tokAt(i + 1) == T.integer) {
          // @@3
          v = vwr.ms.getAtomsFromAtomNumberInFrame(st[++i].intValue);
          break;
        } else {
          // @@x
          v = getParameter(SV.sValue(st[i]), T.variable, true);
        }
        v = getParameter(((SV) v).asString(), T.variable, true);
        break;
      case T.ifcmd:
        if (getToken(++i).tok != T.leftparen)
          invArg();
        if (localVars == null)
          localVars = new Hashtable<String, SV>();
        res = parameterExpression(++i, -1, null, ignoreComma, false, -1, false,
            localVars, localVar, false);
        boolean TF = ((Boolean) res).booleanValue();
        int iT = iToken;
        if (getToken(iT++).tok != T.semicolon)
          invArg();
        parameterExpressionBoolean(iT, -1);
        int iF = iToken;
        if (tokAt(iF++) != T.semicolon)
          invArg();
        parameterExpression(-iF, -1, null, ignoreComma, false, 1, false,
            localVars, localVar, false);
        int iEnd = iToken;
        if (tokAt(iEnd) != T.rightparen)
          invArg();
        v = parameterExpression(TF ? iT : iF, TF ? iF : iEnd, "XXX",
            ignoreComma, false, 1, false, localVars, localVar, false);
        i = iToken = iEnd;
        break;
      case T.forcmd:
      case T.select:
        boolean isFunctionOfX = (pt > 0);
        boolean isFor = (isFunctionOfX && tok == T.forcmd);
        // it is important to distinguish between the select command:
        // select {atomExpression} (mathExpression)
        // and the select(dummy;{atomExpression};mathExpression) function:
        // select {*.ca} (phi < select(y; {*.ca}; y.resno = _x.resno + 1).phi)
        String dummy;
        // for(dummy;...
        // select(dummy;...
        if (isFunctionOfX) {
          if (getToken(++i).tok != T.leftparen
              || !T.tokAttr(getToken(++i).tok, T.identifier))
            invArg();
          dummy = paramAsStr(i);
          if (getToken(++i).tok != T.semicolon)
            invArg();
        } else {
          dummy = "_x";
        }
        // for(dummy;{atom expr};...
        // select(dummy;{atom expr};...
        v = parameterExpressionToken(-(++i)).value;
        if (!(v instanceof BS))
          invArg();
        BS bsAtoms = (BS) v;
        i = iToken;
        if (isFunctionOfX && getToken(i++).tok != T.semicolon)
          invArg();
        // for(dummy;{atom expr};math expr)
        // select(dummy;{atom expr};math expr)
        // bsX is necessary because there are a few operations that still
        // are there for now that require it; could go, though.
        BS bsSelect = new BS();
        BS bsX = new BS();
        String[] sout = (isFor ? new String[bsAtoms.cardinality()] : null);
        if (localVars == null)
          localVars = new Hashtable<String, SV>();
        bsX.set(0);
        SV t = SV.newV(T.bitset, bsX);
        t.index = 0;
        localVars.put(dummy, t.setName(dummy));
        // one test just to check for errors and get iToken
        int pt2 = -1;
        if (isFunctionOfX) {
          pt2 = i - 1;
          int np = 0;
          int tok2;
          while (np >= 0 && ++pt2 < ptMax) {
            if ((tok2 = tokAt(pt2)) == T.rightparen)
              np--;
            else if (tok2 == T.leftparen)
              np++;
          }
        }
        int p = 0;
        int jlast = 0;
        int j = bsAtoms.nextSetBit(0);
        if (j < 0 || chk) {
          iToken = pt2 - 1;
        } else {
          for (; j >= 0; j = bsAtoms.nextSetBit(j + 1)) {
            if (jlast >= 0)
              bsX.clear(jlast);
            jlast = j;
            bsX.set(j);
            t.index = j;
            res = parameterExpression(i, pt2, (isFor ? "XXX" : null),
                ignoreComma, isFor, j, false, localVars, isFunctionOfX ? null
                    : dummy, false);
            if (isFor) {
              if (res == null || ((Lst<?>) res).size() == 0)
                invArg();
              sout[p++] = ((SV) ((Lst<?>) res).get(0)).asString();
            } else if (((Boolean) res).booleanValue()) {
              bsSelect.set(j);
            }
          }
        }
        if (isFor) {
          v = sout;
        } else if (isFunctionOfX) {
          v = bsSelect;
        } else {
          return listBS(bsSelect);
        }
        i = iToken + 1;
        break;
      case T.semicolon: // for (i = 1; i < 3; i=i+1)
        break out;
      case T.integer:
        // checking here for nnn.? or nnn.*, which is not treated as "float" in the compiler
        if (tokAt(iToken + 1) == T.per && ((tok = tokAt(iToken + 2)) == T.opIf || tok == T.times)) {
          theToken.value = Float.valueOf(theToken.intValue);
          theToken.tok = T.decimal;
          theToken.intValue = Integer.MAX_VALUE;
          i++;
        }
        //$FALL-THROUGH$
      case T.decimal:
      case T.spec_seqcode:
        rpn.addXNum(theToken);
        break;
      // these next are for the within() command
      case T.plane:
        if (tokAt(iToken + 1) == T.leftparen) {
          if (!rpn.addOpAllowMath(theToken, true, T.nada))
            invArg();
          break;
        }
        // for within:
        //$FALL-THROUGH$
      case T.on:
      case T.off:
      case T.string:
      case T.point3f:
      case T.point4f:
      case T.matrix3f:
      case T.matrix4f:
      case T.varray:
      case T.bitset:
      case T.hash:
      case T.context:
        rpn.addX(SV.newT(theToken));
        break;
      case T.dollarsign:
        if (tokAt(i + 2) == T.per && tokAt(i + 3) == T.getproperty) {
          rpn.addXStr("$" + paramAsStr(++i));
        } else {
          ignoreError = true;
          P3 ptc;
          try {
            ptc = centerParameter(i, null);
            rpn.addX(SV.newV(T.point3f, ptc));
          } catch (Exception e) {
            rpn.addXStr("");
          }
          ignoreError = false;
          i = iToken;
        }
        break;
      case T.leftbrace:
        if (tokAt(i + 1) == T.string) {
          if (tokAt(i + 2) == T.rightbrace) {
            v = (chk ? new BS() : getAtomBitSet(stringParameter(i + 1)));
            i += 2;
            break;
          }
          v = getAssocArray(i);
        } else {
          v = getPointOrPlane(i, false, true, true, false, 3, 4, true);
        }
        i = iToken;
        break;
      case T.expressionBegin:
        if (tokAt(i + 1) == T.expressionEnd) {
          v = new Hashtable<String, Object>();
          i++;
          break;
        } else if (tokAt(i + 1) == T.all && tokAt(i + 2) == T.expressionEnd) {
          tok = T.all;
          iToken += 2;
        }
        //$FALL-THROUGH$
      case T.all:
        if (tok == T.all)
          v = vwr.getAllAtoms();
        else
          v = atomExpression(st, i, 0, true, true, null, true);
        i = iToken;
        if (nParen == 0 && isOneExpressionOnly) {
          iToken++;
          return listBS((BS) v);
        }
        break;
      case T.spacebeforesquare:
        rpn.addOp(theToken);
        continue;
      case T.expressionEnd:
        i++;
        break out;
      case T.comma: // ignore commas
        if (!ignoreComma && topLevel)
          break out;
        if (!rpn.addOp(theToken))
          invArg();
        break;
      case T.perper:
      case T.per:
        if (isSpecialAssignment && topLevel && tokAt(i + 2) == T.opEQ)
          isSpecialAssignment = rpn.endAssignment();
        if (ptEq == 0 && topLevel) {
          switch (tokAt(i + 1)) {
          case T.nada:
            break; //?? or invArg??
          case T.push:
          case T.pop:
          case T.size:
          case T.keys:
          case T.type:
          case T.array:
            if (tok == T.per)
              break;
            //$FALL-THROUGH$
          default:
            // turn x.y into x[y] prior to equal sign
            rpn.addOp(T.tokenArrayOpen);
            rpn.addXStr(optParameterAsString(++i));
            rpn.addOp(T.tokenArrayClose);
            continue;
          }
        }
        SV var = getBitsetPropertySelector(i + 1, rpn.getXTok());
        // check for added min/max modifier
        boolean isUserFunction = (var.intValue == T.function);
        boolean allowMathFunc = true;
        int tok2 = tokAt(iToken + 2);
        if (tokAt(iToken + 1) == T.per) {
          switch (tok2) {
          case T.all:
            tok2 = T.minmaxmask;
            if (tokAt(iToken + 3) == T.per && tokAt(iToken + 4) == T.bin)
              tok2 = T.selectedfloat;
            //$FALL-THROUGH$
          case T.min:
          case T.max:
          case T.stddev:
          case T.sum:
          case T.sum2:
          case T.average:
            allowMathFunc = (isUserFunction || var.intValue == T.distance
                || tok2 == T.minmaxmask || tok2 == T.selectedfloat);
            var.intValue |= tok2;
            getToken(iToken + 2);
          }
        }
        int tokNext = tokAt(iToken + 1);
        allowMathFunc &= (tokNext == T.leftparen || isUserFunction);
        if (!rpn.addOpAllowMath(var, allowMathFunc, isUserFunction ? tokNext
            : T.nada))
          invArg();
        i = iToken;
        if (var.intValue == T.function && tokNext != T.leftparen) {
          rpn.addOp(T.tokenLeftParen);
          rpn.addOp(T.tokenRightParen);
        }
        break;
      case T.atomname:
      case T.atomtype:
      case T.branch:
      case T.boundbox:
      case T.chain:
      case T.coord:
      case T.dssr:
      case T.element:
      case T.group:
      case T.model:
      case T.molecule:
      case T.rna3d:
      case T.search:
      case T.sequence:
      case T.site:
      case T.smiles:
      case T.substructure:
      case T.structure:
        // these are "within" phrases ?
        if (!isWhere && i == ptWithin && tokAt(i + 1) == T.comma) {
          rpn.addX(SV.newT(theToken));
          break;
        }//$FALL-THROUGH$
      default:
        if (theTok == T.leftsquare && tokAt(i + 2) == T.colon) {
          v = getAssocArray(i);
          i = iToken;
          break;
        }
        if (T.tokAttr(theTok, T.mathop) || T.tokAttr(theTok, T.mathfunc)
            && tokAt(iToken + 1) == T.leftparen) {
          if (!rpn.addOp(theToken)) {
            if (ptAtom >= 0) {
              // this is expected -- the right parenthesis
              break out;
            }
            invArg();
          }
          switch (theTok) {
          case T.within:
            ptWithin = i + 2;
            break;
          case T.opEQ:
            if (topLevel)
              ptEq = i;
            break;
          case T.leftparen:
            nParen++;
            topLevel = false;
            break;
          case T.rightparen:
            if (--nParen <= 0 && nSquare == 0) {
              if (isOneExpressionOnly) {
                iToken++;
                break out;
              }
              topLevel = true;
            }
            break;
          case T.leftsquare:
            nSquare++;
            topLevel = false;
            break;
          case T.rightsquare:
            if (--nSquare == 0 && nParen == 0) {
              if (isOneExpressionOnly) {
                iToken++;
                break out;
              }
              topLevel = true;
            }
            break;
          }
        } else {
          // must be a variable name
          // first check to see if the variable has been defined already
          String name = paramAsStr(i).toLowerCase();
          boolean haveParens = (tokAt(i + 1) == T.leftparen);
          if (!haveParens)
            if (chk) {
              v = name;
            } else if (localVars == null
                || (v = PT.getMapValueNoCase(localVars, name)) == null
                && allContext) {
              if (name.startsWith("_")) {
                v = (name.equals("_") ? vwr.ms.getAuxiliaryInfo(null) : name
                    .equals("_m") ? vwr.getCurrentModelAuxInfo() : null);
              }
              if (v == null)
                v = getContextVariableAsVariable(name, false);
              else if (ptEq == 0)
                invArg();
            }
          if (v == null) {

            if (T.tokAttr(theTok, T.identifier) && vwr.isFunction(name)) {
              if (!rpn.addOp(SV.newV(T.function, theToken.value)))
                invArg();
              if (!haveParens) {
                rpn.addOp(T.tokenLeftParen);
                rpn.addOp(T.tokenRightParen);
              }
            } else {
              var = vwr.g.getAndSetNewVariable(name, false);
              switch (var.tok) {
              case T.integer:
              case T.decimal:
                // looking for ++ or --, in which case we don't copy
                if (noCopy(i, -1) || noCopy(i, 1))
                  break;
                rpn.addXCopy(var);
                continue;
              default:
              }
              rpn.addX(var);
            }
          }
        }
      }
      if (v != null) {
        if (v instanceof BS)
          rpn.addXBs((BS) v);
        else
          rpn.addXObj(v);
      }
    }
    SV result = rpn.getResult();
    if (result == null) {
      if (!chk)
        rpn.dumpStacks("null result");
      error(ERROR_endOfStatementUnexpected);
    }
    if (result.tok == T.vector) {
      if (isSpecialAssignment && ptEq == 0) {
        // no equal sign found! xxxxx.pop() for example
        Lst<SV> rv = new Lst<SV>();
        rv.addLast(new SV());
        return rv;
      }
      return result.value;
    }
    if (chk) {
      if (returnBoolean)
        return Boolean.TRUE;
      if (returnString)
        return "";
    } else {
      if (returnBoolean)
        return Boolean.valueOf(result.asBoolean());
      if (returnString) {
        if (result.tok == T.string)
          result.intValue = Integer.MAX_VALUE;
        return result.asString();
      }
    }
    switch (result.tok) {
    case T.on:
    case T.off:
      return Boolean.valueOf(result.intValue == 1);
    case T.integer:
      return Integer.valueOf(result.intValue);
    case T.bitset:
    case T.decimal:
    case T.string:
    case T.point3f:
    default:
      return result.value;
    }
  }

  protected T[] tempStatement;

  public BS atomExpressionAt(int index) throws ScriptException {
    if (!checkToken(index)) {
      iToken = index;
      bad();
    }
    return atomExpression(st, index, 0, true, false, null, true);
  }

  /**
   * @param code
   * @param pcStart
   * @param pcStop
   * @param allowRefresh
   * @param allowUnderflow
   * @param ret -- true return value; may not be a BS
   * @param andNotDeleted
   *        IGNORED
   * @return atom bitset
   * @throws ScriptException
   */
  public BS atomExpression(T[] code, int pcStart, int pcStop,
                           boolean allowRefresh, boolean allowUnderflow,
                           Object[] ret, boolean andNotDeleted)
      throws ScriptException {
    // note that this is general -- NOT just statement[]
    // errors reported would improperly access statement/line context
    // there should be no errors anyway, because this is for
    // predefined variables, but it is conceivable that one could
    // have a problem.

    isBondSet = false;
    if (code != st) {
      tempStatement = st;
      st = code;
    }
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, false, false,
        false, ret == null, allowUnderflow, null);
    Object val;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    BS bs;
    int nExpress = 0;
    int ac = vwr.ms.ac;
    int ptWithin = -10;
    if (ignoreSubset)
      pcStart = -pcStart;
    ignoreSubset |= chk;
    if (pcStop == 0 && code.length > pcStart)
      pcStop = pcStart + 1;
    // if (logMessages)
    // vwr.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      T instruction = code[pc];
      if (instruction == null)
        break;
      Object value = instruction.value;
      // if (logMessages)
      // vwr.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case T.expressionBegin:
        pcStart = pc;
        pcStop = code.length;
        nExpress++;
        break;
      case T.expressionEnd:
        nExpress--;
        if (nExpress > 0)
          continue;
        break expression_loop;
      case T.leftbrace:
        if (isPoint3f(pc)) {
          P3 pt = getPoint3f(pc, true, true);
          if (pt != null) {
            rpn.addXPt(pt);
            pc = iToken;
            break;
          }
        }
        break; // ignore otherwise
      case T.rightbrace:
        if (pc > 0 && code[pc - 1].tok == T.leftbrace)
          rpn.addXBs(new BS());
        break;
      case T.leftsquare:
        isInMath = true;
        rpn.addOp(instruction);
        break;
      case T.rightsquare:
        isInMath = false;
        rpn.addOp(instruction);
        break;
      case T.define:
        rpn.addXBs(getAtomBitSet(value));
        break;
      case T.varray:
        bs = SV.getBitSet((SV)instruction, false);
        if (bs != null) {
          rpn.addXBs(bs);
          break;
        }
       //$FALL-THROUGH$
     case T.hash:
        // unit ids
        rpn.addXBs(vwr.ms.getAtoms(T.sequence, ((SV) instruction).asString()));
        break;
      case T.hkl:
        rpn.addX(SV.newT(instruction));
        rpn.addX(SV.newV(T.point4f, hklParameter(pc + 2)));
        pc = iToken;
        break;
      case T.plane:
        rpn.addX(SV.newT(instruction));
        rpn.addX(SV.newV(T.point4f, planeParameter(pc + 2)));
        pc = iToken;
        break;
      case T.coord:
        rpn.addX(SV.newT(instruction));
        rpn.addXPt(getPoint3f(pc + 2, true, true));
        pc = iToken;
        break;
      case T.string:
        String s = (String) value;
        if (s.indexOf("({") == 0) {
          bs = BS.unescape(s);
          if (bs != null) {
            rpn.addXBs(bs);
            break;
          }
        } else if (s.indexOf("|") >= 0 && ptWithin != pc - 4) {
          // looking for unit IDs;
          // of course this means that no string can have "|" in it
          // other than unit ids. 
          // avoiding within ( dssr , "pairs where xxxx | xxx")
          rpn.addXBs(vwr.ms.getAtoms(T.sequence, s));
          break;
        }
        rpn.addX(SV.newT(instruction));
        // note that the compiler has changed all within() types to strings.
        if (s.equals("hkl")) {
          rpn.addX(SV.newV(T.point4f, hklParameter(pc + 2)));
          pc = iToken;
        }
        break;
      case T.within:
        ptWithin = pc;
        //$FALL-THROUGH$
      case T.smiles:
      case T.search:
      case T.substructure:
      case T.contact:
      case T.connected:
      case T.comma:
      case T.polyhedra:
        rpn.addOp(instruction);
        break;
      case T.all:
        rpn.addXBs(vwr.getAllAtoms());
        break;
      case T.none:
        rpn.addXBs(new BS());
        break;
      case T.on:
      case T.off:
        rpn.addX(SV.newT(instruction));
        break;
      case T.selected:
        rpn.addXBs(BSUtil.copy(vwr.bsA()));
        break;
      //removed in 13.1.17. Undocumented; unneccessary (same as "all")

      //case T.subset:
      //BS bsSubset = vwr.getSelectionSubset();
      //rpn.addXBs(bsSubset == null ? vwr.getModelUndeletedAtomsBitSet(-1)
      //    : BSUtil.copy(bsSubset));
      //break;
      case T.hidden:
        rpn.addXBs(BSUtil.copy(vwr.slm.getHiddenSet()));
        break;
      case T.fixed:
        rpn.addXBs(BSUtil.copy(vwr.getMotionFixedAtoms()));
        break;
      case T.displayed:
        rpn.addXBs(BSUtil.copyInvert(vwr.slm.getHiddenSet(), ac));
        break;
      case T.basemodel:
        rpn.addXBs(vwr.getBaseModelBitSet());
        break;
      case T.visible:
        rpn.addXBs(chk ? new BS() : BSUtil.copy(vwr.ms.getVisibleSet(!refreshed)));
        refreshed = true;
        break;
      case T.clickable:
        // a bit different, because it requires knowing what got slabbed
        if (!chk && allowRefresh)
          ((ScriptEval) this).refresh(false);
        rpn.addXBs(chk ? new BS() : vwr.ms.getClickableSet(!allowRefresh));
        allowRefresh = false;
        break;
      case T.spec_atom:
        if (vwr.ms.mc != 1 || vwr.ms.haveBioModels) {
          // old Chime scripts use *.C for _C
          int atomID = instruction.intValue;
          if (atomID > 0) {
            bs = compareInt(T.atomid, T.opEQ, atomID);
            if (atomID == 2) // *.CA (alpha carbon only)
              bs.or(compareInt(T.element, T.opEQ, 20)); // CA.CA
            rpn.addXBs(bs);            
          } else {
            rpn.addXBs(getAtomBits(instruction.tok, value));
          }
        } else {
          // Chime legacy hack.  *.C for _C
          rpn.addXBs(((ScriptEval) this).lookupIdentifierValue("_" + value));
        }
        break;
      case T.bonded:
      case T.carbohydrate:
      case T.dna:
      case T.hetero:
      case T.isaromatic:
      case T.nucleic:
      case T.leadatom:
      case T.protein:
      case T.purine:
      case T.pyrimidine:
      case T.rna:
      case T.spec_name_pattern:
      case T.spec_alternate:
      case T.specialposition:
      case T.symmetry:
      case T.unitcell:
        rpn.addXBs(getAtomBits(instruction.tok, value));
        break;
      case T.spec_model:
        // from select */1002 or */1000002 or */1.2
        // */1002 is equivalent to 1.2 when more than one file is present
      case T.spec_model2:
        // from just using the number 1.2
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE && value instanceof Integer) {
          // from select */n
          iModel = ((Integer) value).intValue();
          if (!vwr.haveFileSet()) {
            rpn.addXBs(getAtomBits(T.spec_model, Integer.valueOf(iModel)));
            break;
          }
          if (iModel <= 2147) // file number
            iModel = iModel * 1000000;
        }
        rpn.addXBs(bitSetForModelFileNumber(iModel));
        break;
      case T.spec_resid:
      case T.spec_chain:
        rpn.addXBs(getAtomBits(instruction.tok,
            Integer.valueOf(instruction.intValue)));
        break;
      case T.spec_seqcode:
        if (isInMath)
          rpn.addXNum(instruction);
        else
          rpn.addXBs(getAtomBits(T.spec_seqcode,
              Integer.valueOf(getSeqCode(instruction))));
        break;
      case T.spec_seqcode_range:
        if (isInMath) {
          rpn.addXNum(instruction);
          rpn.addOp(T.tokenMinus);
          rpn.addXNum(code[++pc]);
          break;
        }
        int chainID = (pc + 3 < code.length && code[pc + 2].tok == T.opAND
            && code[pc + 3].tok == T.spec_chain ? code[pc + 3].intValue : -1);
        rpn.addXBs(getAtomBits(T.spec_seqcode_range, new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]), chainID }));
        if (chainID != -1)
          pc += 2;
        break;
      case T.centroid:
      case T.cell:
        P3 pt = (P3) value;
        rpn.addXBs(getAtomBits(
            instruction.tok,
            new int[] { (int) Math.floor(pt.x * 1000),
                (int) Math.floor(pt.y * 1000), (int) Math.floor(pt.z * 1000) }));
        break;
      case T.thismodel:
        rpn.addXBs(vwr.am.cmi < 0 ? vwr.getFrameAtoms() : vwr.getModelUndeletedAtomsBitSet(vwr.am.cmi));
        break;
      case T.hydrogen:
      case T.amino:
      case T.backbone:
      case T.solvent:
      case T.helix:
      case T.helixalpha:
      case T.helix310:
      case T.helixpi:
      case T.sidechain:
      case T.surface:
        rpn.addXBs(((ScriptEval) this).lookupIdentifierValue((String) value));
        break;
      case T.opLT:
      case T.opLE:
      case T.opGE:
      case T.opGT:
      case T.opEQ:
      case T.opNE:
      case T.opLIKE:
        int tok = instruction.tok;
        int tokWhat = instruction.intValue;
        if ((tokWhat == T.configuration) && tok != T.opEQ)
          invArg();
        float[] data = null;
        if (tokWhat == T.property) {
          if (pc + 2 == code.length)
            invArg();
          if (!chk)
            data = (float[]) vwr.getDataObj((String) code[++pc].value, null, JmolDataManager.DATA_TYPE_AF);
        }
        if (++pc == code.length)
          invArg(); // compiler would not let this happen, actually
        rpn.addXBs(chk ? new BS() : getComparison(code[pc], tokWhat, tok, (String) value, data));
        break;
      case T.decimal:
      case T.integer:
        rpn.addXNum(instruction);
        break;
      case T.bitset:
        BS bs1 = BSUtil.copy((BS) value);
        rpn.addXBs(bs1);
        break;
      case T.point3f:
        rpn.addXPt((P3) value);
        break;
      default:
        if (T.tokAttr(instruction.tok, T.mathop)) {
          if (!rpn.addOp(instruction))
            invArg();
          break;
        }
        if (!(value instanceof String)) {
          // catch-all: point4f, hash, list, etc.
          rpn.addXObj(value);
          break;
        }
        val = getParameter((String) value, T.nada, true);
        if (isInMath) {
          rpn.addXObj(val);
          break;
        }
        // check for string-version of bitsets ({....})
        // or maybe a list of bitsets
        if (val instanceof String || val instanceof Lst<?>)
          val = getStringObjectAsVariable(val);
        // otherwise, this is a new atom expression
        if (val instanceof String)
          val = ((ScriptEval) this).lookupIdentifierValue((String) value);
        rpn.addXObj(val);
        break;
      }
    }
    SV expressionResult = rpn.getResult();
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!chk)
        rpn.dumpStacks("after getResult");
      error(ERROR_endOfStatementUnexpected);
    }
    Object exp = expressionResult.value;
    if (exp instanceof String
        && (ret == null || ((String) exp).startsWith("({"))) {
      // allow for select @{x} where x is a string that can evaluate to a bitset
      exp = (chk ? new BS() : getAtomBitSet(exp));
    }
    if (ret != null && !(exp instanceof BS)) {
      ret[0] = exp;
      return null;
    }
    bs = (exp instanceof BS ? (BS) exp : new BS());
    isBondSet = (exp instanceof BondSet);
    if (!isBondSet
        && (bs = vwr.slm.excludeAtoms(bs, ignoreSubset)).length() > vwr.ms.ac)
      bs.clearAll();
    if (tempStatement != null) {
      st = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  private BS getComparison(T t, int tokWhat, int tokOp, String strOp,
                           float[] data) throws ScriptException {
    int tokValue = t.tok;
    if (tokValue == T.varray) {
      BS bs = new BS();
      if (tokOp != T.opEQ)
        bs.setBits(0, vwr.ms.ac);
      Lst<SV> lst = ((SV) t).getList();
      for (int i = lst.size(); --i >= 0;) {
        BS res = getComparison(lst.get(i), tokWhat, tokOp, strOp, data);
        if (tokOp == T.opEQ)
          bs.or(res);
        else
          bs.and(res);
      }
      return bs;
    }
    
    int comparisonInt = t.intValue;
    float comparisonFloat = Float.NaN;

    boolean isModel = (tokWhat == T.model);
    boolean isIntProperty = T.tokAttr(tokWhat, T.intproperty);
    boolean isFloatProperty = (T.tokAttr(tokWhat, T.floatproperty)
        || (tokWhat & T.PROPERTYFLAGS) == T.atomproperty); // point
    boolean isIntOrFloat = isIntProperty && isFloatProperty;
    boolean isStringProperty = !isIntProperty
        && T.tokAttr(tokWhat, T.strproperty);
    // element comparisons must be numerical
    if (tokWhat == T.element)
      isIntProperty = !(isStringProperty = false);

    Object val = t.value;
    if (T.tokAttr(tokValue, T.identifier)) {
      if ("_modelNumber".equalsIgnoreCase((String) val)) {
        int modelIndex = vwr.am.cmi;
        val = Integer.valueOf(comparisonInt = (modelIndex < 0 ? 0 : vwr
            .getModelFileNumber(modelIndex)));
      } else {
        SV v = (SV) getParameter((String) val, T.variable, false);
        if (v != null) {
          if (v.tok == T.varray)
            return getComparison(v, tokWhat, tokOp, strOp, data);
          comparisonInt = v.intValue;
          val = (isStringProperty ? SV.sValue(v) : SV.nValue(v));
        }          
      }
    }

    if (val instanceof P3) {
      if (tokWhat == T.color) {
        comparisonInt = CU.colorPtToFFRGB((P3) val);
        tokValue = T.integer;
        isIntProperty = true;
      }
    } else if (val instanceof String) {
      if (tokWhat == T.color) {
        comparisonInt = CU.getArgbFromString((String) val);
        if (comparisonInt == 0 && T.tokAttr(tokValue, T.identifier)) {
          val = getVarParameter((String) val, true);
          if (((String) val).startsWith("{")) {
            val = Escape.uP((String) val);
            if (val instanceof P3)
              comparisonInt = CU.colorPtToFFRGB((P3) val);
            else
              comparisonInt = 0;
          } else {
            comparisonInt = CU.getArgbFromString((String) val);
          }
        }
        tokValue = T.integer;
        isIntProperty = true;
      } else if (!isStringProperty) {
        if (tokWhat == T.structure || tokWhat == T.substructure
            || tokWhat == T.element)
          isStringProperty = !(isIntProperty = (comparisonInt != Integer.MAX_VALUE));
        else
          val = SV.nValue(t);
        if (val instanceof Integer)
          comparisonFloat = comparisonInt = ((Integer) val).intValue();
        else if (val instanceof Float && isModel)
          comparisonInt = ModelSet
              .modelFileNumberFromFloat(((Float) val).floatValue());
      }
    }
    if (isStringProperty && !(val instanceof String)) {
      val = "" + val;
    } 
    if (val instanceof Integer || tokValue == T.integer) {
      if (isModel) {
        if (comparisonInt >= 1000000)
          tokWhat = -T.model;
      } else if (isIntOrFloat) {
        isFloatProperty = false;
      } else if (isFloatProperty) {
        comparisonFloat = comparisonInt;
      }        
    } else if (val instanceof Float) {
      if (isModel) {
        tokWhat = -T.model;
      } else {
        comparisonFloat = ((Float) val).floatValue();
        if (isIntOrFloat) {
          isIntProperty = false;
        } else if (isIntProperty) {
          comparisonInt = (int) (comparisonFloat);
        }
      }
    } else if (!isStringProperty) {
      iToken++;
      invArg();
    }
    if (isModel && comparisonInt >= 1000000 && comparisonInt % 1000000 == 0) {
      comparisonInt /= 1000000;
      tokWhat = T.file;
      isModel = false;
    }
    if (tokWhat == -T.model && tokOp == T.opEQ) {
      return bitSetForModelFileNumber(comparisonInt);
    }
    if (strOp != null && strOp.indexOf("-") >= 0) {
      if (isIntProperty)
        comparisonInt = -comparisonInt;
      else if (!Float.isNaN(comparisonFloat))
        comparisonFloat = -comparisonFloat;
    }
    return (isIntProperty ? compareInt(tokWhat, tokOp, comparisonInt)
        : isStringProperty ? compareString(tokWhat, tokOp, (String) val)
            : compareFloatData(tokWhat, data, tokOp, comparisonFloat));

  }
  protected boolean noCopy(int i, int dir) {
    // when there is a ++ or -- before or after
    // an integer or decimal variable by name we must 
    // NOT COPY the variable otherwise it will not be 
    // updated. But generally
    // we need to copy variables
    switch (tokAt(i + dir)) {
      case T.plusPlus:
      case T.minusMinus:
        // relative to x:
        //     ++x ++y       x++
        // dir  -1  +1        +1
        // ival -1  -1        >0
        return ((st[i+dir].intValue == -1) == (dir == -1));
      default:
        return false;
    }
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Object> getAssocArray(int i) throws ScriptException {
    Map<String, Object> ht = new Hashtable<String, Object>();
    int closer = (tokAt(i) == T.leftbrace ? T.rightbrace : T.rightsquare);
    for (i = i + 1; i < slen; i++) {
      int tok = tokAt(i);
      if (tok == closer)
        break;
      // myName is from a user-defined variable. 
      String key = null;
      if (st[i] instanceof SV)
        key = ((SV) st[i]).myName;
      if (key == null)
        key = SV.sValue(st[i]);
      i++;
      if (tokAt(i++) != T.colon)
        invArg();
      // look to end of array or next comma
      Lst<SV> v = (Lst<SV>) parameterExpression(i, 0, null, false, true, -1,
          false, null, null, false);
      if (v.size() == 0)
        invArg();
      ht.put(key, v.get(0));
      i = iToken;
      if (tokAt(i) != T.comma)
        break;
    }
    iToken = i;
    if (tokAt(i) != closer)
      invArg();
    return ht;
  }

  protected Lst<SV> listBS(BS bs) {
    Lst<SV> l = new Lst<SV>();
    l.addLast(SV.newV(T.bitset, bs));
    return l;
  }

  private P3 ptTemp;

  /**
   * 
   * @param tokWhat
   * @param data
   * @param tokOperator
   * @param comparisonFloat
   * @return BitSet
   */
  protected BS compareFloatData(int tokWhat, float[] data, int tokOperator,
                                float comparisonFloat) {
    BS bs = new BS();
    int ac = vwr.ms.ac;
    ModelSet modelSet = vwr.ms;
    Atom[] atoms = modelSet.at;
    float propertyFloat = 0;
    vwr.autoCalculate(tokWhat, null);
    boolean isProp = (tokWhat == T.property);
    if (!isProp && ptTemp == null)
      ptTemp = new P3();
    for (int i = ac; --i >= 0;) {
      boolean match = false;
      Atom atom = atoms[i];
      if (isProp) {
        if (data == null || data.length <= i)
          continue;
        propertyFloat = data[i];
      } else {
        propertyFloat = atom.atomPropertyFloat(vwr, tokWhat, ptTemp);
      }
      match = compareFloat(tokOperator, propertyFloat, comparisonFloat);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  protected boolean compareFloat(int tokOperator, float a, float b) {
    switch (tokOperator) {
    case T.opLT:
      return a < b;
    case T.opLE:
      return a <= b;
    case T.opGE:
      return a >= b;
    case T.opGT:
      return a > b;
    case T.opEQ:
      return a == b;
    case T.opNE:
      return a != b && !Float.isNaN(a);
    }
    return false;
  }

  private BS compareString(int tokWhat, int tokOperator, String comparisonString)
      throws ScriptException {
    BS bs = new BS();
    Atom[] atoms = vwr.ms.at;
    int ac = vwr.ms.ac;
    boolean isCaseSensitive = (tokOperator == T.opLIKE || tokWhat == T.chain && vwr
        .getBoolean(T.chaincasesensitive));
    if (!isCaseSensitive)
      comparisonString = comparisonString.toLowerCase();
    for (int i = ac; --i >= 0;) {
      String propertyString = atoms[i]
          .atomPropertyString(vwr, tokWhat);
      if (!isCaseSensitive)
        propertyString = propertyString.toLowerCase();
      if (compareStringValues(tokOperator, propertyString, comparisonString))
        bs.set(i);
    }
    return bs;
  }

  private boolean compareStringValues(int tokOperator, String propertyValue,
                                      String comparisonValue)
      throws ScriptException {
    switch (tokOperator) {
    case T.opEQ:
    case T.opNE:
      return (PT.isMatch(propertyValue, comparisonValue, true, true) == (tokOperator == T.opEQ));
    case T.opLIKE:
      return PT.isLike(propertyValue, comparisonValue);
    default:
      invArg();
    }
    return false;
  }

  private BS compareInt(int tokWhat, int tokOperator, int ival) {
    int ia = Integer.MAX_VALUE;
    BS propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = ival;
    ModelSet modelSet = vwr.ms;
    Atom[] atoms = modelSet.at;
    int ac = modelSet.ac;
    int imax = -1;
    int imin = 0;
    int iModel = -1;
    int[] cellRange = null;
    int nOps = 0;
    BS bs;
    // preliminary setup
    switch (tokWhat) {
    case T.symop:
      switch (bitsetComparator) {
      case T.opGE:
      case T.opGT:
        imax = Integer.MAX_VALUE;
        break;
      }
      break;
    case T.atomindex:
      try {
        switch (tokOperator) {
        case T.opLT:
          return BSUtil.newBitSet2(0, ival);
        case T.opLE:
          return BSUtil.newBitSet2(0, ival + 1);
        case T.opGE:
          return BSUtil.newBitSet2(ival, ac);
        case T.opGT:
          return BSUtil.newBitSet2(ival + 1, ac);
        case T.opEQ:
          return (ival < ac ? BSUtil.newBitSet2(ival, ival + 1) : new BS());
        case T.opNE:
        default:
          bs = BSUtil.setAll(ac);
          if (ival >= 0)
            bs.clear(ival);
          return bs;
        }
      } catch (Exception e) {
        return new BS();
      }
    }
    bs = BS.newN(ac);
    for (int i = 0; i < ac; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        ia = atom.atomPropertyInt(tokWhat);
        break;
      case T.subsystem:
      case T.configuration:
        // these are all-inclusive; no need to do a by-atom comparison
        return BSUtil.copy(vwr.ms.getConformation(-1, ival - 1, false, null));
      case T.symop:
        propertyBitSet = atom.atomSymmetry;
        if (propertyBitSet == null)
          continue;
        if (atom.mi != iModel) {
          iModel = atom.mi;
          cellRange = modelSet.getModelCellRange(iModel);
          nOps = modelSet.getModelSymmetryCount(iModel);
        }
        if (bitsetBaseValue >= 200) {
          if (cellRange == null)
            continue;
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555) for
           * this the comparision is only with the translational component; the
           * symop itself must match thus: select symop!=1655 selects all
           * symop=1 and translation !=655 select symop>=2555 selects all
           * symop=2 and translation >555 symop >=200 indicates any symop in the
           * specified translation (a few space groups have > 100 operations)
           * 
           * Note that when normalization is not done, symop=1555 may not be in
           * the base unit cell. Everything is relative to wherever the base
           * atoms ended up, usually in 555, but not necessarily.
           * 
           * The reason this is tied together an atom may have one translation
           * for one symop and another for a different one.
           * 
           * Bob Hanson - 10/2006
           */
          ival = bitsetBaseValue % 1000;
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0) {
            match = true;
          } else if (nOps == 0 || symop >= 0
              && !(match = propertyBitSet.get(symop))) {
            continue;
          }
          bitsetComparator = T.none;
          if (symop < 0)
            ia = atom.getCellTranslation(ival, cellRange, nOps);
          else
            ia = atom.getSymmetryTranslation(symop, cellRange, nOps);
        } else if (nOps > 0) {
          if (ival > nOps) {
            if (bitsetComparator != T.opLT && bitsetComparator != T.opLE)
              continue;
          }
          if (bitsetComparator == T.opNE) {
            if (ival > 0 && ival <= nOps && !propertyBitSet.get(ival)) {
              bs.set(i);
            }
            continue;
          }
          BS bs1 = BSUtil.copy(propertyBitSet);
          bs1.clearBits(nOps, bs1.length());
          propertyBitSet = bs1;

        }
        switch (bitsetComparator) {
        case T.opLT:
          imax = ival - 1;
          break;
        case T.opLE:
          imax = ival;
          break;
        case T.opGE:
          imin = ival - 1;
          break;
        case T.opGT:
          imin = ival;
          break;
        case T.opEQ:
          imax = ival;
          imin = ival - 1;
          break;
        case T.opNE:
          match = !propertyBitSet.get(ival);
          break;
        }
        if (imin < 0)
          imin = 0;
        if (imin < imax) {
          int pt = propertyBitSet.nextSetBit(imin);
          if (pt >= 0 && pt < imax)
            match = true;
        }
        // note that a symop property can be both LE and GT !
        if (!match || ia == Integer.MAX_VALUE)
          tokOperator = T.none;
      }
      switch (tokOperator) {
      case T.none:
        break;
      case T.opLT:
        match = (ia < ival);
        break;
      case T.opLE:
        match = (ia <= ival);
        break;
      case T.opGE:
        match = (ia >= ival);
        break;
      case T.opGT:
        match = (ia > ival);
        break;
      case T.opEQ:
        match = (ia == ival);
        break;
      case T.opNE:
        match = (ia != ival);
        break;
      }
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private SV getBitsetPropertySelector(int i, int xTok) throws ScriptException {
    int tok = getToken(i).tok;
    switch (tok) {
    case T.min:
    case T.max:
    case T.average:
    case T.stddev:
    case T.sum:
    case T.sum2:
    case T.property:
      break;
    default:
      if (T.tokAttrOr(tok, T.atomproperty, T.mathproperty) || xTok == T.hash)
        break;
      if (tok != T.opIf && !T.tokAttr(tok, T.identifier))
        break;
      String name = paramAsStr(i);
      if (vwr.isFunction(name.toLowerCase())) {
        tok = T.function;
        break;
      }
    }
    return SV.newSV(T.propselector, tok, paramAsStr(i));
  }

  @SuppressWarnings("unchecked")
  public Object getBitsetProperty(BS bs, Lst<SV> pts, int tok, P3 ptRef,
                                  P4 planeRef, Object tokenValue,
                                  Object opValue, boolean useAtomMap,
                                  int index, boolean asVectorIfAll) throws ScriptException {

    // index is a special argument set in parameterExpression that
    // indicates we are looking at only one atom within a for(...) loop
    // the bitset cannot be a BondSet in that case

    boolean haveIndex = (index != Integer.MAX_VALUE);

    boolean isAtoms = haveIndex || !(tokenValue instanceof BondSet);
    // check minmax flags:

    int minmaxtype = tok & T.minmaxmask;
    boolean selectedFloat = (minmaxtype == T.selectedfloat);
    int ac = vwr.ms.ac;
    float[] fout = (minmaxtype == T.allfloat ? new float[ac] : null);
    boolean isExplicitlyAll = (minmaxtype == T.minmaxmask || selectedFloat);
    tok &= ~T.minmaxmask;
    Object[] info = null;
    if (tok == T.nada)
      tok = (isAtoms ? T.atoms : T.bonds);

    // determine property type:

    boolean isPt = false;
    boolean isHash  = false;
    boolean isInt = false;
    boolean isString = false;
    switch (tok) {
    case T.__:
      return ((Map<String, Object>)vwr.getAuxiliaryInfoForAtoms(bs)).get("models");
    case T.xyz:
    case T.vibxyz:
    case T.fracxyz:
    case T.fuxyz:
    case T.modxyz:
    case T.unitxyz:
    case T.color:
    case T.screenxyz:
      isPt = true;
      break;
    case T.polyhedra:
      isHash = true;
      info = new Object[] {null, null};
      break;
    case T.function:
    case T.distance:
      break;
    default:
      isInt = T.tokAttr(tok, T.intproperty) && !T.tokAttr(tok, T.floatproperty);
      // occupancy and radius considered floats here
      isString = !isInt && T.tokAttr(tok, T.strproperty);
      // structure considered int; for the name, use .label("%[structure]")
    }

    // preliminarty checks we only want to do once:

    P3 pt = (isPt || !isAtoms ? new P3() : null);
    if (isExplicitlyAll || isString && !haveIndex && minmaxtype != T.allfloat
        && minmaxtype != T.min)
      minmaxtype = T.all;
    Lst<Object> vout = (minmaxtype == T.all ? new Lst<Object>() : null);
    BS bsNew = null;
    String userFunction = null;
    Lst<SV> params = null;
    BS bsAtom = null;
    SV tokenAtom = null;
    P3 ptT = null;
    float[] data = null;

    switch (tok) {
    case T.atoms:
    case T.bonds:
      if (chk)
        return bs;
      bsNew = (tok == T.atoms ? (isAtoms ? bs : vwr.ms.getAtoms(T.bonds, bs))
          : (isAtoms ? (BS) BondSet.newBS(vwr.getBondsForSelectedAtoms(bs), null)
              : bs));
      int i;
      switch (minmaxtype) {
      case T.min:
        i = bsNew.nextSetBit(0);
        break;
      case T.max:
        i = bsNew.length() - 1;
        break;
      case T.stddev:
      case T.sum:
      case T.sum2:
        return Float.valueOf(Float.NaN);
      default:
        return bsNew;
      }
      bsNew.clearAll();
      if (i >= 0)
        bsNew.set(i);
      return bsNew;
    case T.identify:
      switch (minmaxtype) {
      case 0:
      case T.all:
        return getCmdExt().getBitsetIdent(bs, null, tokenValue, useAtomMap,
            index, isExplicitlyAll);
      }
      return "";
    case T.function:
      userFunction = (String) ((Object[]) opValue)[0];
      params = (Lst<SV>) ((Object[]) opValue)[1];
      bsAtom = BS.newN(ac);
      tokenAtom = SV.newV(T.bitset, bsAtom);
      break;
    case T.dssr:
      for (int j = fout.length; --j >= 0;)
        fout[j] = Float.NaN;
      //$FALL-THROUGH$
    case T.straightness:
    case T.surfacedistance:
      vwr.autoCalculate(tok, (String) tokenValue);
      break;
    case T.distance:
      if (ptRef == null && planeRef == null)
        return new P3();
      break;
    case T.color:
      ptT = new P3();
      break;
    case T.property:
      data = (float[]) vwr.getDataObj((String) opValue, null, JmolDataManager.DATA_TYPE_AF);
      break;
    }

    int n = 0;
    int ivMinMax = 0;
    float fvMinMax = 0;
    double sum = 0;
    double sum2 = 0;
    switch (minmaxtype) {
    case T.min:
      ivMinMax = Integer.MAX_VALUE;
      fvMinMax = Float.MAX_VALUE;
      break;
    case T.max:
      ivMinMax = Integer.MIN_VALUE;
      fvMinMax = -Float.MAX_VALUE;
      break;
    }
    ModelSet modelSet = vwr.ms;
    int mode = (isHash ? 4 : isPt ? 3 : isString ? 2 : isInt ? 1 : 0);
    if (isAtoms) {
      boolean haveBitSet = (bs != null);
      int i0, i1;
      if (pts != null) {
        i0 = 0;
        i1 = pts.size(); 
      } else if (haveIndex) {
        i0 = index;
        i1 = index + 1;
      } else if (haveBitSet) {
        i0 = bs.nextSetBit(0);
        i1 = Math.min(ac, bs.length());
      } else {
        i0 = 0;
        i1 = ac;
      }
      if (chk)
        i1 = 0;
      for (int i = i0; i >= 0 && i < i1; i = (haveBitSet ? bs.nextSetBit(i + 1)
          : i + 1)) {
        n++;
        Atom atom = (pts == null ? modelSet.at[i] : null);
        switch (mode) {
        case 0: // float
          float fv = Float.MAX_VALUE;
          switch (tok) {
          case T.function:
            bsAtom.set(i);
            fv = SV.fValue(((ScriptEval) this).getUserFunctionResult(userFunction, params, tokenAtom));
            bsAtom.clear(i);
            break;
          case T.property:
            fv = (data == null ? 0 : data[i]);
            break;
          case T.distance:
            if (planeRef != null)
              fv = Measure.distanceToPlane(planeRef, atom);
            else
              fv = (pts != null ? SV.ptValue(pts.get(i)).distance(ptRef) : atom != ptRef || minmaxtype != T.min ? atom.distance(ptRef) : Float.NaN);
            break;
          default:
            fv = atom.atomPropertyFloat(vwr, tok, ptTemp);
          }
          if (fv == Float.MAX_VALUE || Float.isNaN(fv) && minmaxtype != T.all) {
            n--; // don't count this one
            continue;
          }
          switch (minmaxtype) {
          case T.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case T.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case T.allfloat:
            fout[i] = fv;
            break;
          case T.all:
            vout.addLast(Float.valueOf(fv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += ((double) fv) * fv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += fv;
          }
          break;
        case 1: // isInt
          int iv = 0;
          switch (tok) {
          case T.configuration:
          case T.cell:
            errorStr(ERROR_unrecognizedAtomProperty, T.nameOf(tok));
            break;
          default:
            iv = atom.atomPropertyInt(tok);
          }
          switch (minmaxtype) {
          case T.min:
            if (iv < ivMinMax)
              ivMinMax = iv;
            break;
          case T.max:
            if (iv > ivMinMax)
              ivMinMax = iv;
            break;
          case T.allfloat:
            fout[i] = iv;
            break;
          case T.all:
            vout.addLast(Integer.valueOf(iv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += ((double) iv) * iv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += iv;
          }
          break;
        case 2: // isString
          String s = atom.atomPropertyString(vwr, tok);
          switch (minmaxtype) {
          case T.allfloat:
            fout[i] = PT.parseFloat(s);
            break;
          default:
            if (vout == null)
              return s;
            vout.addLast(s);
          }
          break;
        case 3: // isPt
          T3 t = atom.atomPropertyTuple(vwr, tok, ptTemp);
          switch (minmaxtype) {
          case T.allfloat:
            fout[i] = (pt == null ? -1 : t == null ? 0 : t.length());
            break;
          case T.all:
            vout.addLast(t == null ? Integer.valueOf(-1) : P3.newP(t));
            break;
          default:
            if (t == null)
              n--;
            else
              pt.add(t);
          }
          break;
        case 4: // isHash
          switch (tok) {
          case T.polyhedra:
            info[0] = Integer.valueOf(i);
            info[1] = "";
            vwr.shm.getShapePropertyData(JC.SHAPE_POLYHEDRA, "info", info);
            if (info[1] != null) {
              if (vout == null)
                return info[1];
              vout.addLast(info[1]);
            }
            break;
          }
        }
        if (haveIndex)
          break;
      }
    } else { // bonds
      boolean isAll = (bs == null);
      int i0 = (isAll ? 0 : bs.nextSetBit(0));
      int i1 = vwr.ms.bondCount;
      for (int i = i0; i >= 0 && i < i1; i = (isAll ? i + 1 : bs
          .nextSetBit(i + 1))) {
        n++;
        Bond bond = modelSet.bo[i];
        switch (tok) {
        case T.length:
          float fv = bond.atom1.distance(bond.atom2);
          switch (minmaxtype) {
          case T.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case T.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case T.all:
            vout.addLast(Float.valueOf(fv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += (double) fv * fv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += fv;
          }
          break;
        case T.xyz:
          switch (minmaxtype) {
          case T.all:
            pt.ave(bond.atom1, bond.atom2);
            vout.addLast(P3.newP(pt));
            break;
          default:
            pt.add(bond.atom1);
            pt.add(bond.atom2);
            n++;
          }
          break;
        case T.color:
          CU.colorPtFromInt(vwr.gdata.getColorArgbOrGray(bond.colix), ptT);
          switch (minmaxtype) {
          case T.all:
            vout.addLast(P3.newP(ptT));
            break;
          default:
            pt.add(ptT);
          }
          break;
        default:
          errorStr(ERROR_unrecognizedBondProperty, T.nameOf(tok));
        }
      }
    }
    if (minmaxtype == T.allfloat)
      return fout;
    if (minmaxtype == T.all) {
      if (asVectorIfAll)
        return vout;
      int len = vout.size();
      if ((isString || isHash) && !isExplicitlyAll && len == 1)
        return vout.get(0);
      if (selectedFloat) {
        fout = new float[len];
        for (int i = len; --i >= 0;) {
          Object v = vout.get(i);
          switch (mode) {
          case 0:
            fout[i] = ((Float) v).floatValue();
            break;
          case 1:
            fout[i] = ((Integer) v).floatValue();
            break;
          case 2:
            fout[i] = PT.parseFloat((String) v);
            break;
          case 3:
            fout[i] = (v == null ? -1 : ((P3) v).length());
            break;
          }
        }
        return fout;
      }
      if (tok == T.sequence) {
        SB sb = new SB();
        for (int i = 0; i < len; i++)
          sb.append((String) vout.get(i));
        return sb.toString();
      }
      String[] sout = new String[len];
      for (int i = len; --i >= 0;) {
        Object v = vout.get(i);
        if (v instanceof P3)
          sout[i] = Escape.eP((P3) v);
        else
          sout[i] = "" + vout.get(i);
      }
      return sout; // potential j2s issue here
    }
    if (isPt)
      return (n == 0 ? Integer.valueOf(-1) : P3.new3(pt.x / n, pt.y / n, pt.z / n));
    if (isHash)
      return new Hashtable<String, Object>();
    if (n == 0 || n == 1 && minmaxtype == T.stddev)
      return Float.valueOf(Float.NaN);
    if (isInt) {
      switch (minmaxtype) {
      case T.min:
      case T.max:
        return Integer.valueOf(ivMinMax);
      case T.sum2:
      case T.stddev:
        break;
      case T.sum:
        return Integer.valueOf((int) sum);
      default:
        if (sum / n == (int) (sum / n))
          return Integer.valueOf((int) (sum / n));
        return Float.valueOf((float) (sum / n));
      }
    }
    switch (minmaxtype) {
    case T.min:
    case T.max:
      sum = fvMinMax;
      break;
    case T.sum:
      break;
    case T.sum2:
      sum = sum2;
      break;
    case T.stddev:
      // because SUM (x_i - X_av)^2 = SUM(x_i^2) - 2X_av SUM(x_i) + SUM(X_av^2)
      // = SUM(x_i^2) - 2nX_av^2 + nX_av^2
      // = SUM(x_i^2) - nX_av^2
      // = SUM(x_i^2) - [SUM(x_i)]^2 / n
      sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
      break;
    default:
      sum /= n;
      break;
    }
    return Float.valueOf((float) sum);
  }

  private BS bitSetForModelFileNumber(int m) {
    // where */1.0 or */1.1 or just 1.1 is processed
    BS bs = BS.newN(vwr.ms.ac);
    if (chk)
      return bs;
    int modelCount = vwr.ms.mc;
    boolean haveFileSet = vwr.haveFileSet();
    if (m < 1000000 && haveFileSet)
      m *= 1000000;
    int pt = m % 1000000;
    if (pt == 0) {
      int model1 = vwr.ms.getModelNumberIndex(m + 1, false, false);
      if (model1 < 0)
        return bs;
      int model2 = (m == 0 ? modelCount : vwr.ms.getModelNumberIndex(
          m + 1000001, false, false));
      if (model1 < 0)
        model1 = 0;
      if (model2 < 0)
        model2 = modelCount;
      if (vwr.ms.isTrajectory(model1))
        model2 = model1 + 1;
      for (int j = model1; j < model2; j++)
        bs.or(vwr.getModelUndeletedAtomsBitSet(j));
    } else {
      int modelIndex = vwr.ms.getModelNumberIndex(m, false, true);
      if (modelIndex >= 0)
        bs.or(vwr.getModelUndeletedAtomsBitSet(modelIndex));
    }
    return bs;
  }

  private Object getStringObjectAsVariable(Object obj) {
    if (obj == null)
      return  obj;
    if (obj instanceof String) {
      String s = (String) obj;
      if (s.length() == 0)
        return s;
      return SV.unescapePointOrBitsetAsVariable(s);
    }
    @SuppressWarnings("unchecked")
    Lst<SV> lst = (Lst<SV>) obj;
    if (lst.size() == 0)
      return "";
    if (lst.get(0).asString().contains("|"))
      return vwr.ms.getAtoms(T.sequence, SV.newV(T.varray, lst).asString());
    BS bs = SV.unEscapeBitSetArray(lst, true);
    return (bs == null ? "" : bs);
  }

  protected BS getAtomBits(int tokType, Object specInfo) {
    return (chk ? new BS() : vwr.ms.getAtoms(tokType, specInfo));
  }

  static protected int getSeqCode(T instruction) {
    return (instruction.intValue == Integer.MAX_VALUE ? ((Integer) instruction.value)
        .intValue() : Group.getSeqcodeFor(instruction.intValue, ' '));
  }

  /**
   * 
   * @param pt
   *        starting point in command token sequence
   * @param ptMax
   *        ending point in command token sequenec, possibly -1 for "all"
   * @param key
   *        the variable name to save the result in. This must be a standard
   *        user variable, either local or global
   * @param isSet
   *        from Set ... or Var .... or just xxx ....
   * 
   * @return a variable or null
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  protected SV setVariable(int pt, int ptMax, String key, boolean isSet)
      throws ScriptException {
    BS bs = null;
    String propertyName = "";
    boolean settingData = key.startsWith("property_");
    boolean isThrown = key.equals("thrown_value");
    boolean isExpression = (tokAt(1) == T.expressionBegin || tokAt(1) == T.leftparen);
    SV t = (settingData ? null : key.length() == 0 ? new SV()
        : getContextVariableAsVariable(key, false));
    // determine whether this is some sort of 
    // special assignment of a known variable

    if (isSet && !isExpression) {
      // not {....}.x = 
      switch (tokAt(2)) {
      default:
        // standard case
        // a = ...
        // set a ...
        pt = 2;
        break;
      case T.opEQ:
        // standard case for var
        // var a = ...
        pt = 3;
        break;
      case T.spacebeforesquare:
      case T.leftsquare:
        // x[n] = ...
        if (st[0].intValue == 61) { // '='
          // not clear what this is about...
          pt = 2;
          break;
        }
        //$FALL-THROUGH$
      case T.per:
      case T.perper:
        // x.y = ...
        // pt will be 1 in this case -- evaluate x
        // result array will hold selector .y and result
        // do not assign a variable
        // isspecialAssignment
        key = null;
        break;
      }
    }
    int nv = 0;
    Lst<SV> v = (Lst<SV>) parameterExpression(pt, ptMax, key, true, true, -1,
        false, null, null, isSet && pt == 1);
    nv = v.size();
    if (nv == 0)
      invArg();
    if (chk || v.get(0).tok == T.nada)
      return null;
    SV tv = SV.selectItemVar(SV.newS("").setv(v.get(nv - 1)));
    if (nv > 1) {
      SV sel = (nv > 2 ? v.get(1) : null);
      t = v.get(0);
      // -- hash, key, value
      // -- array[i], value
      // -- array, index, value
      // -- string, index, value
      // -- matrix, index/index, value
      // -- bitset, property, value

      boolean selectOne = false;
      switch (t.tok) {
      case T.hash:
      case T.context:
        if (nv > 3)
          invArg();
        t.mapPut(sel.asString(), tv);
        break;
      case T.varray:
        if (nv > 2 + (sel == null ? 0 : 1))
          invArg();
        if (sel == null) {
          sel = t;
        } else {
          t = SV.selectItemVar(t);
        }
        selectOne = true;
        break;
      case T.string:
        if (sel.tok != T.integer) {
          // stringVar["t"] = .....
          t.value = PT.rep(t.asString(), sel.asString(), tv.asString());
          t.intValue = Integer.MAX_VALUE;
          break;
        }
        //$FALL-THROUGH$
      case T.matrix3f:
      case T.matrix4f:
        if (t.intValue == Integer.MAX_VALUE)
          selectOne = true;
        else
          t.setSelectedValue(t.intValue, sel.asInt(), tv);
        break;
      case T.point3f:
        P3 p = (P3) (t.value = P3.newP((P3) t.value));
        float f = tv.asFloat();
        switch (T.getTokFromName(sel.asString())) {
        case T.x:
          p.x = f;
          break;
        case T.y:
          p.y = f;
          break;
        case T.z:
          p.z = f;
          break;
        }
        break;
      case T.bitset:
        bs = SV.getBitSet(t, true);
        int nAtoms = vwr.ms.ac;
        int nbs = bs.cardinality();
        propertyName = sel.asString();
        int tok = T.getTokFromName(propertyName);
        switch (tok) {
        case T.nada:
          if (propertyName.startsWith("property_")) {
            Object obj = (tv.tok == T.varray ? SV.flistValue(tv, tv.getList()
                .size() == nbs ? nbs : nAtoms) : tv.asString());
            vwr.setData(
                propertyName,
                new Object[] { propertyName, obj, BSUtil.copy(bs),
                    Integer.valueOf(JmolDataManager.DATA_TYPE_UNKNOWN) },
                nAtoms, 0, 0, tv.tok == T.varray ? Integer.MAX_VALUE
                    : Integer.MIN_VALUE, 0);
            break;
          }
          iToken = pt;
          error(ERROR_cannotSet);
          break;
        case T.label:
        case T.format:
          vwr.shm.loadShape(JC.SHAPE_LABELS);
          //$FALL-THROUGH$
        default:
          setBitsetProperty(bs, tok, tv.asInt(), tv.asFloat(), tv);
          break;
        }
        break;
      }
      if (selectOne)
        t.setSelectedValue(sel.intValue, Integer.MAX_VALUE, tv);
      return null;
    }

    // -- simple assignment; single value only

    // create user variable if needed for list now, so we can do the copying
    // no variable needed if it's a String, integer, float, or boolean.

    boolean needVariable = (!settingData && t == null && (isThrown || !(tv.value instanceof String
        || tv.tok == T.integer
        || tv.value instanceof Integer
        || tv.value instanceof Float || tv.value instanceof Boolean)));

    if (needVariable && key != null) {
      if (key.startsWith("_")
          || (t = vwr.g.getAndSetNewVariable(key, true)) == null)
        errorStr(ERROR_invalidArgument, key);
    }
    if (t != null)
      return t.setv(tv);

    Object vv = SV.oValue(tv);

    if (settingData) {
      if (tv.tok == T.varray)
        vv = tv.asString();
      // very inefficient!
      vwr.setData(key, new Object[] { key, "" + vv, BSUtil.copy(vwr.bsA()),
          Integer.valueOf(0) }, vwr.ms.ac, 0, 0, Integer.MIN_VALUE, 0);
      return null;
    }

    if (vv instanceof Boolean) {
      setBooleanProperty(key, ((Boolean) vv).booleanValue());
    } else if (vv instanceof Integer) {
      setIntProperty(key, ((Integer) vv).intValue());
    } else if (vv instanceof Float) {
      setFloatProperty(key, ((Float) vv).floatValue());
    } else if (vv instanceof String) {
      setStringProperty(key, (String) vv);
    } else {
      // could be an array and be OK   Logger.error("ERROR -- return from propertyExpression was " + vv);
    }
    return tv;
  }

  private void setBitsetProperty(BS bs, int tok, int iValue, float fValue,
                                 T tokenValue) throws ScriptException {
    if (chk || bs.cardinality() == 0)
      return;
    String[] list = null;
    String sValue = null;
    float[] fvalues = null;
    P3 pt;
    Lst<SV> sv = null;
    int nValues = 0;
    boolean isStrProperty = T.tokAttr(tok, T.strproperty);
    if (tokenValue.tok == T.varray) {
      sv = ((SV) tokenValue).getList();
      if ((nValues = sv.size()) == 0)
        return;
    }
    switch (tok) {
    case T.xyz:
    case T.fracxyz:
    case T.fuxyz:
    case T.vibxyz:
      switch (tokenValue.tok) {
      case T.point3f:
        vwr.setAtomCoords(bs, tok, tokenValue.value);
        break;
      case T.varray:
        theToken = tokenValue;
        vwr.setAtomCoords(bs, tok, getPointArray(-1, nValues, true));
        break;
      }
      return;
    case T.color:
      Object value = null;
      String prop = "color";
      switch (tokenValue.tok) {
      case T.varray:
        int[] values = new int[nValues];
        for (int i = nValues; --i >= 0;) {
          SV svi = sv.get(i);
          pt = SV.ptValue(svi);
          if (pt != null) {
            values[i] = CU.colorPtToFFRGB(pt);
          } else if (svi.tok == T.integer) {
            values[i] = svi.intValue;
          } else {
            values[i] = CU.getArgbFromString(svi.asString());
            if (values[i] == 0)
              values[i] = svi.asInt();
          }
          if (values[i] == 0)
            errorStr2(ERROR_unrecognizedParameter, "ARRAY", svi.asString());
        }
        value = values;
        prop = "colorValues";
        break;
      case T.point3f:
        value = Integer.valueOf(CU.colorPtToFFRGB((P3) tokenValue.value));
        break;
      case T.string:
        value = tokenValue.value;
        break;
      default:
        value = Integer.valueOf(SV.iValue(tokenValue));
        break;
      }
      ((ScriptEval) this).setAtomProp(prop, value, bs);
      return;
    case T.label:
    case T.format:
      if (tokenValue.tok != T.varray)
        sValue = SV.sValue(tokenValue);
      break;
    case T.element:
    case T.elemno:
      ((ScriptEval) this).clearDefinedVariableAtomSets();
      isStrProperty = false;
      break;
    }
    switch (tokenValue.tok) {
    case T.varray:
      if (isStrProperty)
        list = SV.strListValue(tokenValue);
      else
        fvalues = SV.flistValue(tokenValue, nValues);
      break;
    case T.string:
      if (sValue == null)
        list = PT.getTokens(SV.sValue(tokenValue));
      break;
    }
    if (list != null) {
      nValues = list.length;
      if (!isStrProperty) {
        fvalues = new float[nValues];
        for (int i = nValues; --i >= 0;)
          fvalues[i] = (tok == T.element ? Elements.elementNumberFromSymbol(
              list[i], false) : PT.parseFloat(list[i]));
      }
      if (tokenValue.tok != T.varray && nValues == 1) {
        if (isStrProperty)
          sValue = list[0];
        else
          fValue = fvalues[0];
        iValue = (int) fValue;
        list = null;
        fvalues = null;
      }
    }
    if (!T.tokAttr(tok, T.settable))
      error(ERROR_cannotSet);
    vwr.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
  }


  /**
   * provides support for @x and @{....} in statements. The compiler passes on
   * these, because they must be integrated with the statement dynamically.
   * 
   * @param st0
   *        aaToken[i]
   * @param pt0
   * @return a fixed token set -- with possible overrun of unused null tokens
   * 
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  protected boolean setStatement(T[] st0, int pt0) throws ScriptException {
    st = st0;
    slen = st.length;
    if (slen == 0)
      return true;
    T[] fixed;
    int i;
    int tok;
    for (i = pt0; i < slen; i++) {
      if (st[i] == null) {
        slen = i;
        return true;
      }
      if (st[i].tok == T.define)
        break;
    }
    if (i == slen)// || isScriptCheck)
      return i == slen;
    switch (st[0].tok) {
    case T.parallel:
    case T.function:
    case T.identifier:
      if (tokAt(1) == T.leftparen)
        return true;
    }
    fixed = new T[slen];
    fixed[0] = st[0];
    boolean isExpression = false;
    int j = pt0;
    for (i = pt0; i < slen; i++) {
      if (st[i] == null)
        continue;
      switch (tok = getToken(i).tok) {
      default:
        fixed[j] = st[i];
        break;
      case T.expressionBegin:
      case T.expressionEnd:
        // @ in expression will be taken as SELECT
        isExpression = (tok == T.expressionBegin);
        fixed[j] = st[i];
        break;
      case T.define:
        if (++i == slen)
          invArg();
        Object v;
        // compiler can indicate that a definition MUST
        // be interpreted as a String
        boolean forceString = (theToken.intValue == T.string);
        // Object var_set;
        String s;
        String var = paramAsStr(i);
        boolean isClauseDefine = (tokAt(i) == T.expressionBegin);
        boolean isSetAt = (pt0 == 1 && j == 1 && st[0] == T.tokenSetCmd);
        if (isClauseDefine) {
          SV vt = parameterExpressionToken(++i);
          // For select @{x} where x is an array, we leave it as SV.
          // This will for an evaluation of each member of the array.
          // I suppose this allowed for select @{["ala", "leu",...]}.
          // Otherwise we get its object. Q: Why?
          if (chk) {
            v = null;
          } else if (vt.tok != T.varray) {
            v = SV.oValue(vt);
          } else if (!isExpression) {
            v = vt;
          } else {
            // select @{x} where x is an array 
            BS bs = SV.getBitSet(vt, true);
            // I can't remember why we have to be checking list variables
            // for atom expressions in select @{x} but not select x
            if (bs == null) {
              String sv = SV.sValue(vt);
              v = (sv.indexOf("|") < 0 ? getAtomBitSet(sv) : sv);
            } else {
              v = bs;
            }
          }
          i = iToken;
        } else if (chk) {
          v = new BS();
        } else {
          if (tokAt(i) == T.integer) {
            v = vwr.ms.getAtoms(T.atomno, Integer.valueOf(st[i].intValue));
          } else if (tokAt(i) == T.define && tokAt(i + 1) == T.integer) {
            v = vwr.ms.getAtomsFromAtomNumberInFrame(st[++i].intValue);
          } else {
            v = getParameter(var, T.nada, true);
          }
          if (!isExpression && !isSetAt)
            isClauseDefine = true;
        }
        tok = tokAt(0);
        forceString |= (T.tokAttr(tok, T.implicitStringCommand) || tok == T.script); // for the file names
        if (v == null) {
          // 
          fixed[j] = T.tokenAll;
        } else if (v instanceof SV) {
          // select @{...}
          fixed[j] = (T) v;
        } else if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? T.tokenOn : T.tokenOff);
        } else if (v instanceof Integer) {
          fixed[j] = T.tv(T.integer, ((Integer) v).intValue(), v);
        } else if (v instanceof Float) {
          fixed[j] = T.tv(T.decimal, getFloatEncodedInt("" + v), v);
        } else if (v instanceof String) {
          if (!forceString && !isExpression) {
            if ((tok != T.set || j > 1 && st[1].tok != T.echo && !"labelfor".equalsIgnoreCase(st[1].value.toString()))
                && T.tokAttr(tok, T.mathExpressionCommand)) {
              v = getParameter((String) v, T.variable, true);
            }
            if (v instanceof String) {
              v = getStringObjectAsVariable(v);
            }
          }
          if (v instanceof SV) {
            // was a bitset 
            fixed[j] = (T) v;
          } else {
            s = (String) v;
            if (isExpression && !forceString) {
              // select @x  where x is "arg", for example
              // but not chain=@x  -- in that case we need a literal value

              fixed[j] = (s.indexOf("|") >= 0
                  || T.tokAttr(fixed[j - 1].tok, T.comparator) ? T.o(T.string,
                  s) : T.o(T.bitset, getAtomBitSet(s)));
            } else {

              // bit of a hack here....
              // identifiers cannot have periods; file names can, though
              // TODO: this is still a hack
              // what we really need to know is what the compiler
              // expects here -- a string or an identifier, because
              // they will be processed differently.
              // a filename with only letters and numbers will be
              // read incorrectly here as an identifier.

              // note that command keywords cannot be implemented as variables
              // because they are not Token.identifiers in the first place.
              // but the identifier tok is important here because just below
              // there is a check for SET parameter name assignments.
              // even those may not work...

              tok = (isSetAt ? T.getTokFromName(s) : isClauseDefine
                  || forceString || s.length() == 0 || s.indexOf(".") >= 0
                  || s.indexOf(" ") >= 0 || s.indexOf("=") >= 0
                  || s.indexOf(";") >= 0 || s.indexOf("[") >= 0
                  || s.indexOf("{") >= 0 ? T.string : T.identifier);
              fixed[j] = T.o(tok, v);
            }
          }
        } else if (v instanceof BArray) {
          fixed[j] = SV.newV(T.barray, v);
        } else if (v instanceof BS) {
          fixed[j] = SV.newV(T.bitset, v);
        } else if (v instanceof P3) {
          fixed[j] = SV.newV(T.point3f, v);
        } else if (v instanceof P4) {
          fixed[j] = SV.newV(T.point4f, v);
        } else if (v instanceof M34) {
          fixed[j] = SV.newV(v instanceof M4 ? T.matrix4f : T.matrix3f, v);
        } else if (v instanceof Map<?, ?> || v instanceof ScriptContext
            && (v = ((ScriptContext) v).getFullMap()) != null) {
          // x = @y -- do a deep copy -- Jmol 14.3.16
          fixed[j] = SV.newV(T.hash, (isExpression ? v : SV.deepCopy(v, true, true)));
        } else if (v instanceof Lst<?>) {
          if (!isExpression) {
            // do a deep copy -- Jmol 14.3.16
            fixed[j] = SV.newV(T.varray, SV.deepCopy(v, false, true));
            break;
          }
          // if v is a list, we check to to see if it is an array of 
          // bitsets, and it if is, we OR all those.
          Lst<SV> sv = (Lst<SV>) v;
          BS bs = null;
          for (int k = 0; k < sv.size(); k++) {
            SV svk = sv.get(k);
            if (svk.tok != T.bitset) {
              bs = null;
              break;
            }
            if (bs == null)
              bs = new BS();
            bs.or((BS) svk.value);
          }
          fixed[j] = (bs == null ? SV.getVariable(v) : T.o(T.bitset, bs));
        } else {
          // assume we want a center
          P3 center = ((ScriptEval) this).getObjectCenter(var, Integer.MIN_VALUE, Integer.MIN_VALUE);
          if (center == null)
            invArg();
          fixed[j] = T.o(T.point3f, center);
        }
        if (isSetAt && !T.tokAttr(fixed[j].tok, T.setparam))
          invArg();
        break;
      }

      j++;
    }
    st = fixed;
    for (i = j; i < st.length; i++)
      st[i] = null;
    slen = j;

    return true;
  }
  
}
