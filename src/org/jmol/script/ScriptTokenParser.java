/* $Author: hansonr $
 * $Date: 2009-06-08 18:20:22 -0500 (Mon, 08 Jun 2009) $
 * $Revision: 10975 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import javajs.util.Lst;
import javajs.util.PT;

import java.util.Map;

import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;

import javajs.util.P3;

import org.jmol.viewer.Viewer;
import org.jmol.i18n.GT;


abstract class ScriptTokenParser {
  
  /*
   * An abstract class taking care of the second phase of 
   * syntax checking, after all the tokens are created,  
   * these methods ensure that they are in the proper order
   * in terms of expressions, primarily.
   * 
   * Here we are going from an "infix" to a "postfix" set
   * of tokens and then back to infix for final storage.
   * 
   */

  protected Viewer vwr;

  protected String script;
  protected boolean isStateScript;

  protected short lineCurrent;
  protected int iCommand;
 
  protected int ichCurrentCommand, ichComment, ichEnd;
  protected int ichToken;
  
  protected T theToken;
  protected T lastFlowCommand;
  protected T tokenCommand;
  protected T lastToken;
  protected T tokenAndEquals;

  protected int theTok;
  protected int nTokens;
  protected int tokCommand;
  
  protected int ptNewSetModifier;
  protected boolean isNewSet;
  protected boolean haveMacro;

  
//----------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------
//---------------PHASE II -- TOKEN-BASED COMPILING ---------------------------------------
//----------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------

  protected boolean logMessages = true;

  protected T[] atokenInfix;
  protected int itokenInfix;

  protected boolean isSetBrace;
  protected boolean isMathExpressionCommand;
  protected boolean isSetOrDefine;

  private Lst<T> ltokenPostfix;

  protected boolean isEmbeddedExpression;
  protected boolean isCommaAsOrAllowed;
  
  private Object theValue;

  protected boolean compileExpressions() {
    
    boolean isScriptExpression = ((tokCommand == T.script || tokCommand == T.macro) 
        && tokAt(2) == T.leftparen);
    isEmbeddedExpression = (isScriptExpression 
        || (tokCommand != T.nada
             && (tokCommand != T.function 
                && tokCommand != T.parallel 
                && tokCommand != T.trycmd 
                && tokCommand != T.catchcmd
             || tokenCommand.intValue != Integer.MAX_VALUE) 
                && tokCommand != T.end 
                && !T.tokAttr(tokCommand, T.atomExpressionCommand) 
                && (nTokens > 2 || !T.tokAttr(tokCommand, T.implicitStringCommand)
                )
           )
      );
    isMathExpressionCommand = (tokCommand == T.identifier 
        || isScriptExpression
        || T.tokAttr(tokCommand, T.mathExpressionCommand));

    boolean checkExpression = isEmbeddedExpression
        || (T.tokAttr(tokCommand, T.atomExpressionCommand));

    // $ at beginning disallow expression checking for center, delete, hide, or
    // display commands
    if (tokAt(1) == T.dollarsign
        && T.tokAttr(tokCommand, T.atomExpressionCommand))
      checkExpression = false;
    if (checkExpression && !compileExpression())
      return false;

    // check statement length

    int size = atokenInfix.length;

    int nDefined = 0;
    for (int i = 1; i < size; i++) {
      if (tokAt(i) == T.define)
        nDefined++;
    }

    size -= nDefined;
    if (isNewSet) {
      if (size == 1) {
        atokenInfix[0] = T.tv(T.function, 0, atokenInfix[0].value);
        isNewSet = false;
      }
    }

    if ((isNewSet || isSetBrace) && size < ptNewSetModifier + 2) {
      if (!isNewSet || !haveMacro)
        return commandExpected();
      htUserFunctions.put((String) atokenInfix[0].value, Boolean.TRUE);
    }
    return(size == 1 || !T.tokAttr(tokCommand, T.noArgs) ? true
            : error(ERROR_badArgumentCount));
  }


  protected boolean compileExpression() {
    int firstToken = (isSetOrDefine && !isSetBrace ? 2 : 1);
    ltokenPostfix = new  Lst<T>();
    itokenInfix = 0;
    T tokenBegin = null;
    int tok = tokAt(1);
    switch (tokCommand) {
    case T.define:
      int i = (tokAt(1) == T.define ? 2 : 1);
      if (tokAt(i) == T.integer && tokAt(i + 1) == T.per && tokAt(i + 3) == T.opEQ) {
        // @2.xxx = 
        // @@2.xxx = 
        tokCommand = T.set;
        isSetBrace = true;
        ptNewSetModifier = i + 3;
        isMathExpressionCommand = true;
        isEmbeddedExpression = true;
        addTokenToPostfixToken(T.tokenSetProperty);
        addTokenToPostfixToken(T.tokenExpressionBegin);
        for (int j = 0; j++ <= i;)
          addNextToken();
        addTokenToPostfixToken(T.tokenExpressionEnd);
        firstToken = 0;
      }
      break;
    case T.restrict:
      if (tok == T.bonds) 
        firstToken = 2;
      break;
    case T.select:
      switch(tok) {
      case T.on:
      case T.off:
        tok = tokAt(++firstToken);
        break;
      }
      //$FALL-THROUGH$
    case T.hide:
    case T.display:
      switch(tok) {
      case T.add:
      case T.remove:
        tok = tokAt(++firstToken);
        break;
      }
      if (tok == T.group  && !T.tokAttr(tokAt(firstToken + 1), T.mathop))
        firstToken++;
    }
    for (int i = 0; i < firstToken && addNextToken(); i++) {
    }
    while (moreTokens()) {
      if (isEmbeddedExpression) {
        while (!isExpressionNext()) {
          if (tokPeekIs(T.identifier) && !(tokCommand == T.load && itokenInfix == 1)) {
            String name = (String) atokenInfix[itokenInfix].value;
            T t = T.getTokenFromName(name); 
            if (t != null)
              if (!isMathExpressionCommand && lastToken.tok != T.define 
                  || (lastToken.tok == T.per || tokAt(itokenInfix + 1) == T.leftparen)
                        && !isUserFunction(name)) {
                // Checking here for known token masquerading as identifier due to VAR definition.
                // We reset it to its original mapping if it's a known token and:
                //    a) this isn't a math expression command, and not preceded by @, or
                //    b) it is preceded by "." or followed by "(" 
                //             and it isn't the name of a user function

                atokenInfix[itokenInfix] = t;
              }
          }
          if (!addNextToken())
            break;
        }
        if (!moreTokens())
          break;
      }
      if (lastToken.tok == T.define) {
        if (!clauseDefine(true, false))
          return false;
        continue;
      }
      if (!isMathExpressionCommand)
        addTokenToPostfixToken(tokenBegin = T.o(T.expressionBegin, "implicitExpressionBegin"));
      if (!clauseOr(isCommaAsOrAllowed || !isMathExpressionCommand
          && tokPeekIs(T.leftparen)))
        return false;
      if (!isMathExpressionCommand
          && !(isEmbeddedExpression && lastToken == T.tokenCoordinateEnd)) {
        addTokenToPostfixToken(T.tokenExpressionEnd);
      }
      if (moreTokens()) {
        if (tokCommand != T.select && tokCommand != T.delete && !isEmbeddedExpression)
          return error(ERROR_endOfExpressionExpected);
        if (tokCommand == T.select) {
          // advanced select, with two expressions, the first
          // being an atom expression; the second being a property selector expression
          tokenBegin.intValue = 0;
          tokCommand = T.nada;
          isEmbeddedExpression = true;
          isMathExpressionCommand = true;
          isCommaAsOrAllowed = false;
        }
      }
    }
    atokenInfix = ltokenPostfix.toArray(new T[ltokenPostfix.size()]);
    return true;
  }

  protected Map<String, Boolean> htUserFunctions;
  protected boolean isUserFunction(String name) {
    name = name.toLowerCase();
    return (!isStateScript && (vwr.isFunction(name) || htUserFunctions.containsKey(name)));
  }

  private boolean isExpressionNext() {
    return tokPeekIs(T.leftbrace) 
    && !(tokAt(itokenInfix + 1) == T.string
         && tokAt(itokenInfix + 2) == T.colon)
    || !isMathExpressionCommand && tokPeekIs(T.leftparen);
  }

  protected static boolean tokenAttr(T token, int tok) {
    return token != null && T.tokAttr(token.tok, tok);
  }
  
  private boolean moreTokens() {
    return (itokenInfix < atokenInfix.length);
  }
  
  protected int tokAt(int i) {
    return (i < atokenInfix.length ? atokenInfix[i].tok : T.nada);
  }
  
  private int tokPeek() {
    return (itokenInfix >= atokenInfix.length ? T.nada
        : atokenInfix[itokenInfix].tok);
  }

  private boolean tokPeekIs(int tok) {
    return (tokAt(itokenInfix) == tok);
  }

  private int intPeek() {
    return (itokenInfix >= atokenInfix.length ? Integer.MAX_VALUE
        : atokenInfix[itokenInfix].intValue);
  }
  
  private Object valuePeek() {
    return (moreTokens() ? atokenInfix[itokenInfix].value : "");
  }
 
  /**
   * increments the pointer; does NOT set theToken or theValue
   * @return the next token
   */
  private T tokenNext() {
    return (itokenInfix >= atokenInfix.length ? null 
        : atokenInfix[itokenInfix++]);
  }
  
  private boolean tokenNextTok(int tok) {
    T token = tokenNext();
    return (token != null && token.tok == tok);
  }

  private boolean returnToken() {
    itokenInfix--;
    return false;
  }

  /**
   * gets the next token and sets global theToken and theValue
   * @return the next token
   */
  private T getToken() {
    theValue = ((theToken = tokenNext()) == null ? null : theToken.value);
    return theToken;
  }
  
  private boolean getNumericalToken() {
    return (getToken() != null 
        && (theToken.tok == T.integer || theToken.tok == T.decimal));
  }
  
  private float floatValue() {
    switch (theToken.tok) {
    case T.integer:
      return theToken.intValue;
    case T.decimal:
      return ((Float) theValue).floatValue();
    }
    return 0;
  }

  private boolean addTokenToPostfix(int tok, Object value) {
    return addTokenToPostfixToken(T.o(tok, value));
  }

  private boolean addTokenToPostfixInt(int tok, int intValue, Object value) {
    return addTokenToPostfixToken(T.tv(tok, intValue, value));
  }

  private boolean addTokenToPostfixToken(T token) {
    if (token == null)
      return false;
    if (logMessages)
        Logger.debug("addTokenToPostfix" + token);
    if (token.tok == T.leftsquare && (lastToken.tok == T.per || lastToken.tok == T.perper)) {
      // new notation
      int ipt = ltokenPostfix.size() - 1;
      ltokenPostfix.removeItemAt(ipt);
      ltokenPostfix.addLast(T.tokenRightParen);
      int pcount = 0;
      int tok;      
      for (int i = ltokenPostfix.size(); --i >= 0 && pcount >= 0;) {
        switch  (tok = ltokenPostfix.get(i).tok) {
        case T.rightparen:
        case T.rightsquare:
          pcount++;
          break;
        case T.leftparen:
        case T.leftsquare:
          pcount--;
          int tok2;
          if (pcount == 1 && (tok2 = ltokenPostfix.get(i - 1).tok) != T.rightparen && tok2 != T.rightsquare) {
            ipt = (tok == T.leftsquare ? i - 1 : i);
            pcount = -10;
          }
          break;
        default:
          tok2 = (i == 0 ? T.nada : ltokenPostfix.get(i - 1).tok);
          if (tok2 == T.per || tok2 == T.perper) {
            ipt = i - 1;
            break;
          }
          if (i == ipt - 1) {
            ipt = i;
            pcount = -10;
          }
          break;
        }
      }
      if (pcount == -10) {
        ltokenPostfix.add(ipt, T.tokenLeftParen);
      }
    }
    ltokenPostfix.addLast(token);
    lastToken = token;
    return true;
  }

  private boolean addNextToken() {
    return addTokenToPostfixToken(tokenNext());
  }
  
  private boolean addNextTokenIf(int tok) {
    return (tokPeekIs(tok) && addNextToken());
  }
  
  private boolean addSubstituteTokenIf(int tok, T token) {
    if (!tokPeekIs(tok))
      return false;
    itokenInfix++;
    return addTokenToPostfixToken(token);
  }
  
  boolean haveString;
  
  private boolean clauseOr(boolean allowCommaAsOr) {
    haveString = false;
    if (!clauseAnd())
      return false;
    if (isEmbeddedExpression && lastToken.tok == T.expressionEnd)
      return true;

    //for simplicity, giving XOR (toggle) same precedence as OR
    //OrNot: First OR, but if that makes no change, then NOT (special toggle)
    int tok;
    while ((tok = tokPeek())== T.opOr || tok == T.opXor
        || tok==T.opToggle|| allowCommaAsOr && tok == T.comma) {
      if (tok == T.comma && !haveString)
        addSubstituteTokenIf(T.comma, T.tokenOr);
      else
        addNextToken();
      if (!clauseAnd())
        return false;
      if (allowCommaAsOr && (lastToken.tok == T.rightbrace || lastToken.tok == T.bitset))
        haveString = true;
    }
    return true;
  }

  private boolean clauseAnd() {
    if (!clauseNot())
      return false;
    if (isEmbeddedExpression && lastToken.tok == T.expressionEnd)
      return true;
    while (tokPeekIs(T.opAnd)) {
      addNextToken();
      if (!clauseNot())
        return false;
    }
    return true;
  }

  // for RPN processor, not reversed
  private boolean clauseNot() {
    if (tokPeekIs(T.opNot)) {
      addNextToken();
      return clauseNot();
    }
    return (clausePrimitive());
  }
  
  private boolean clausePrimitive() {
    int tok = tokPeek();
    switch (tok) {
    case T.spacebeforesquare:
      itokenInfix++;
      return clausePrimitive();
    case T.nada:
      return error(ERROR_endOfCommandUnexpected);
    case T.all:
    case T.bitset:
    case T.divide:
    case T.helix:
    case T.helix310:
    case T.helixalpha:
    case T.helixpi:
    case T.isaromatic:
    case T.none:
    case T.sheet:
      // nothing special
      return addNextToken();
    case T.string:
      haveString = true;
      return addNextToken();
    case T.decimal:
      // create a file_model integer as part of the token
      return addTokenToPostfixInt(T.spec_model2, fixModelSpec(getToken()), theValue);
    case T.cell:
    case T.centroid:
      return clauseCell(tok);
    case T.connected:
    case T.polyhedra:
      return clauseConnected(tok == T.polyhedra);
    case T.search:
    case T.smiles:
      return clauseSubstructure();
    case T.within:
    case T.contact:
      return clauseWithin(tok == T.within);
    case T.define:
      return clauseDefine(false, false);
    case T.bonds:
    case T.measure:
      addNextToken();
      if (tokPeekIs(T.bitset))
        addNextToken();
      else if (tokPeekIs(T.define))
        return clauseDefine(false, false);
      return true;
    case T.leftparen:
      addNextToken();
      if (!clauseOr(true))
        return false;
      if (!addNextTokenIf(T.rightparen))
        return errorStr(ERROR_tokenExpected, ")");
      return checkForItemSelector(true);
    case T.leftbrace:
      return checkForCoordinate(isMathExpressionCommand);
    default:
      // may be a residue specification
      if (clauseResidueSpec())
        return true;
      if (isError())
        return false;
      if (T.tokAttr(tok, T.atomproperty)) {
        int itemp = itokenInfix;
        boolean isOK = clauseComparator(true);
        if (isOK || itokenInfix != itemp)
          return isOK;
        if (tok == T.substructure) {
          return clauseSubstructure(); 
        }

      }
      //if (tok != Token.integer && !Token.tokAttr(tok, Token.predefinedset))
        //break;
      return addNextToken();

    }
//    return error(ERROR_unrecognizedExpressionToken, "" + valuePeek());
  }

  private boolean checkForCoordinate(boolean isImplicitExpression) {
    /*
     * A bit tricky here: we have three contexts for braces -- 
     * 
     * 1) expressionCommands SELECT, RESTRICT, DEFINE, 
     *    DISPLAY, HIDE, CENTER, and SUBSET
     * 
     * 2) embeddedExpression commands such as DRAW and ISOSURFACE
     * 
     * 3) IF and SET
     * 
     * Then, within these, we have the possibility that we are 
     * looking at a coordinate {0 0 0} (with or without commas, and
     * possibly fractional, {1/2 1/2 1}, and possibly a plane Point4f
     * definition, {a b c d}) or an expression. 
     * 
     * We assume an expression initially and then adjust accordingly
     * if it turns out this is a coordinate. 
     * 
     * Note that due to the nuances of how expressions such as (1-4) are
     * reported as special codes, Eval must still interpret these
     * carefully. This could be corrected for here, I think.
     * 
     */
    boolean isCoordinate = false;
    int pt = ltokenPostfix.size();
    if (isImplicitExpression) {
      addTokenToPostfixToken(T.tokenExpressionBegin);
      tokenNext();
    } else if (isEmbeddedExpression) {
      tokenNext();
      pt--;
    } else {
      addNextToken();
    }
    boolean isHash = tokPeekIs(T.string);
    if (isHash) {
      isImplicitExpression = false;
      returnToken();
      ltokenPostfix.removeItemAt(ltokenPostfix.size() - 1);
      addNextToken();
      int nBrace = 1;
      while (nBrace != 0) {
        if (tokPeekIs(T.leftbrace)) {
          if (isExpressionNext()) {
            addTokenToPostfixToken(T.o(T.expressionBegin,
                "implicitExpressionBegin"));
            if (!clauseOr(false)) // changed to FALSE 10/20 because  @({"center":{0.0 0.0 0.0}, "xxx"...}} failed
              return false;
            if (lastToken != T.tokenCoordinateEnd) {
              addTokenToPostfixToken(T.tokenExpressionEnd);
            }
          } else {
            nBrace++;
          }
        }
        if (tokPeekIs(T.rightbrace))
          nBrace--;
        addNextToken();
      }
    } else {
      if (!tokPeekIs(T.rightbrace) && !clauseOr(false))
        return false;
      int n = 1;
      while (!tokPeekIs(T.rightbrace)) {
        boolean haveComma = addNextTokenIf(T.comma);
        if (!clauseOr(false))
          return (haveComma || n < 3 ? false : errorStr(ERROR_tokenExpected, "}"));
        n++;
      }
      isCoordinate = (n >= 2); // could be {1 -2 3}
    }
    if (isCoordinate && (isImplicitExpression || isEmbeddedExpression)) {
      ltokenPostfix.set(pt, T.tokenCoordinateBegin);
      addTokenToPostfixToken(T.tokenCoordinateEnd);
      tokenNext();
    } else if (isImplicitExpression) {
      addTokenToPostfixToken(T.tokenExpressionEnd);
      tokenNext();
    } else if (isEmbeddedExpression) {
      if (!isHash)
        tokenNext();
    } else {
      addNextToken();
    }
    return checkForItemSelector(!isHash);
  }
  
  private boolean checkForItemSelector(boolean allowNumeric) {
    // {x[1]}  @{x}[1][3]  (atomno=3)[2][5]
    int tok;
    if ((tok = tokAt(itokenInfix + 1)) == T.leftsquare
        || allowNumeric && tok == T.leftbrace)
      return true; // [[, as in a matrix or [{ ... not totally acceptable!
    
    // the real problem is that after an expression you can have
    while (true) {//for (int i = 0; i < (allowNumeric ? 2 : 1); i++) {
      if (!addNextTokenIf(T.leftsquare))
        break;
      if (!clauseItemSelector())
        return false;
      if (!addNextTokenIf(T.rightsquare))
        return errorStr(ERROR_tokenExpected, "]");
    }
    return true;
  }
  
  private boolean clauseWithin(boolean isWithin) {

    //    // contact(distance, {}, {}) // default 100 for distance
    // within ( plane, planeExpression)
    // within ( hkl, hklExpression)
    // within ( distance, plane, planeExpression)
    // within ( distance, hkl, hklExpression)
    // within ( distance, coord, point)
    // within ( distance, point)
    // within ( distance, $surfaceId)
    // within ( distance, orClause)
    // within ( group|branch|etc, ....)
    // within ( distance, group, ....)
    // within ( annotation, "xxxx")

    addNextToken();
    if (!addNextTokenIf(T.leftparen))
      return false;
    if (getToken() == null)
      return false;
    float distance = Float.MAX_VALUE;
    String key = null;
    boolean allowComma = isWithin;
    int tok;
    int tok0 = theToken.tok;
    if (!isWithin) {
      tok = -1;
      for (int i = itokenInfix; tok != T.nada; i++) {
        switch (tok = tokAt(i)) {
        case T.comma:
          tok = T.nada;
          break;
        case T.leftbrace:
        case T.leftparen:
        case T.rightparen:
          distance = 100;
          returnToken();
          tok0 = tok = T.nada;
          break;
        }
      }
    }
    switch (tok0) {
    case T.minus:
      if (getToken() == null)
        return false;
      if (theToken.tok != T.integer)
        return error(ERROR_numberExpected);
      distance = -theToken.intValue;
      break;
    case T.integer:
    case T.decimal:
      distance = floatValue();
      break;
    case T.define:
      addTokenToPostfixToken(theToken);
      if (!clauseDefine(true, false))
        return false;
      key = "";
      allowComma = false;
      break;
    }
    if (isWithin && distance == Float.MAX_VALUE)
      switch (tok0) {
      case T.define:
        break;
      case T.dssr:
      case T.rna3d:
      case T.search:
      case T.smiles:
      case T.substructure:
      case T.domains:
      case T.validation:
        addTokenToPostfix(T.string, theValue);
        if (!addNextTokenIf(T.comma))
          return false;
        allowComma = false;
        tok = tokPeek();
        switch (tok) {
        case T.nada:
          return false;
        case T.string:
          addNextToken();
          key = "";
          break;
        case T.define:
          if (!clauseDefine(false, true))
            return false;
          key = "";
          break;
        default:
          return false;
        }
        break;
      case T.branch:
        allowComma = false;
        //$FALL-THROUGH$
      case T.atomtype:
      case T.atomname:
      case T.basepair:
      case T.boundbox:
      case T.chain:
      case T.coord:
      case T.element:
      case T.group:
      case T.unitcell:
      case T.helix:
      case T.model:
      case T.molecule:
      case T.plane:
      case T.hkl:
      case T.polymer:
      case T.sequence:
      case T.sheet:
      case T.site:
      case T.structure:
      case T.string:
      case T.vanderwaals:
        key = (String) theValue;
        break;
      default:
        key = ((String) theValue).toLowerCase();
        break;
      }
    if (key == null)
      addTokenToPostfix(T.decimal, Float.valueOf(distance));
    else if (key.length() > 0)
      addTokenToPostfix(T.string, key);
    boolean done = false;
    while (!done) {
      if (tok0 != T.nada && !addNextTokenIf(T.comma))
        break;
      if (tok0 == T.nada)
        tok0 = T.contact;
      boolean isCoordOrPlane = false;
      tok = tokPeek();
      if (isWithin) {
        switch (tok0) {
        case T.integer:
        case T.decimal:
          if (tok == T.on || tok == T.off) {
            addTokenToPostfixToken(getToken());
            if (!addNextTokenIf(T.comma))
              break;
            tok = tokPeek();
          }
          break;
        }
        if (key == null) {
          switch (tok) {
          case T.hkl:
          case T.coord:
          case T.plane:
            isCoordOrPlane = true;
            addNextToken();
            break;
          case T.dollarsign:
            getToken();
            getToken();
            addTokenToPostfix(T.string, "$" + theValue);
            done = true;
            break;
          case T.group:
          case T.vanderwaals:
          case T.unitcell:
            getToken();
            addTokenToPostfix(T.string, T.nameOf(tok));
            break;
          case T.leftbrace:
            returnToken();
            isCoordOrPlane = true;
            addTokenToPostfixToken(T
                .getTokenFromName(distance == Float.MAX_VALUE ? "plane"
                    : "coord"));
          }
        if (!done)
          addNextTokenIf(T.comma);
        }
      }
      tok = tokPeek();
      if (done)
        break;
      if (isCoordOrPlane) {
        while (!tokPeekIs(T.rightparen)) {
          switch (tokPeek()) {
          case T.nada:
            return error(ERROR_endOfCommandUnexpected);
          case T.leftparen:
            addTokenToPostfixToken(T.tokenExpressionBegin);
            addNextToken();
            if (!clauseOr(false))
              return errorIntStr2(ERROR_unrecognizedParameter, "WITHIN", ": ?");
            if (!addNextTokenIf(T.rightparen))
              return errorStr(ERROR_tokenExpected, ", / )");
            addTokenToPostfixToken(T.tokenExpressionEnd);
            break;
          case T.define:
            if (!clauseDefine(false, false))
              return false;
            break;
          default:
            addTokenToPostfixToken(getToken());
          }
        }
      } else if (!clauseOr(allowComma)) {// *expression*        return error(ERROR_badArgumentCount);
      }
    }
    if (!addNextTokenIf(T.rightparen))
      return errorStr(ERROR_tokenExpected, ")");
    return true;
  }

  private boolean clauseConnected(boolean isPolyhedra) {
    addNextToken();
    // connected (1,3, single, .....)
    if (!addNextTokenIf(T.leftparen)) {
      addTokenToPostfixToken(T.tokenLeftParen);
      addTokenToPostfixToken(T.tokenRightParen);
      return true;
    }
    while (true) {
      if (addNextTokenIf(T.integer)) {
        if (!addNextTokenIf(T.comma)) {
          break;
        }
        if (isPolyhedra) {
          returnToken();
          break;
        }
      } else if (isPolyhedra && 
          (addNextTokenIf(T.string) || addNextTokenIf(T.identifier))) {
        break;
      }

      if (addNextTokenIf(T.integer))
        if (!addNextTokenIf(T.comma))
          break;
      if (addNextTokenIf(T.decimal))
        if (!addNextTokenIf(T.comma))
          break;
      if (addNextTokenIf(T.decimal))
        if (!addNextTokenIf(T.comma))
          break;
      Object o = getToken().value;
      String strOrder = (o instanceof String ? (String) o : " ");
      int intType = ScriptParam.getBondOrderFromString(strOrder);
      if (intType == Edge.BOND_ORDER_NULL) {
        returnToken();
      } else {
        addTokenToPostfix(T.string, strOrder);
        if (!addNextTokenIf(T.comma))
          break;
      }
      if (addNextTokenIf(T.rightparen))
        return true;
      if (!clauseOr(tokPeekIs(T.leftparen))) // *expression*
        return false;
      if (addNextTokenIf(T.rightparen))
        return true;
      if (!addNextTokenIf(T.comma))
        return false;
      if (!clauseOr(tokPeekIs(T.leftparen))) // *expression*
        return false;

      break;
    }
    if (!addNextTokenIf(T.rightparen))
      return errorStr(ERROR_tokenExpected, ")");
    return true;
  }

  private boolean clauseSubstructure() {
    addNextToken();
    if (!addNextTokenIf(T.leftparen))
      return false;
    if (tokPeekIs(T.define)) {
      if (!clauseDefine(false, true))
        return false;
    } else if (!addNextTokenIf(T.string)) {
      return errorStr(ERROR_tokenExpected, "\"...\"");
    }
    if (addNextTokenIf(T.comma))
      if (!clauseOr(tokPeekIs(T.leftparen))) // *expression*
        return false;
    if (!addNextTokenIf(T.rightparen))
      return errorStr(ERROR_tokenExpected, ")");
    return true;
  }

  private boolean clauseItemSelector() {
    int tok;
    int nparen = 0;
    while ((tok = tokPeek()) != T.nada && tok != T.rightsquare) {
      addNextToken();
      if (tok == T.leftsquare)
        nparen++;
      if (tokPeek() == T.rightsquare && nparen-- > 0)
        addNextToken();
    }
    return true;
  }
  
  private boolean clauseComparator(boolean isOptional) {
    T tokenAtomProperty = tokenNext();
    T tokenComparator = tokenNext();
    if (!tokenAttr(tokenComparator, T.comparator)) {
      if (!isOptional)
        return errorStr(ERROR_tokenExpected, "== != < > <= >=");
      if (tokenComparator != null)
        returnToken();
      returnToken();
      return false;
    }
    if (tokenAttr(tokenAtomProperty, T.strproperty) 
        && tokenComparator.tok != T.opEQ 
        && tokenComparator.tok != T.opLIKE
        && tokenComparator.tok != T.opNE)
      return errorStr(ERROR_tokenExpected, "== !=");
    if (tokPeek() == T.leftsquare) {
      getToken();
      addTokenToPostfixToken(T.tokenLeftParen);
      while (true) {
        if (!addCompare(tokenAtomProperty, tokenComparator))
          return false;
        if (tokPeek() == T.comma)
          getToken();
        else if (tokPeek() == T.rightsquare)
          break;
        addTokenToPostfixToken(tokenComparator.tok == T.opNE ? T.tokenAnd : T.tokenOr);
      }
      getToken();
      addTokenToPostfixToken(T.tokenRightParen);
      return true;
    }
    return addCompare(tokenAtomProperty, tokenComparator);
  }

  private boolean addCompare(T tokenAtomProperty, T tokenComparator) {
    if (getToken() == null)
      return errorStr(ERROR_unrecognizedExpressionToken, "" + valuePeek());
    boolean isNegative = (theToken.tok == T.minus);
    if (isNegative && getToken() == null)
      return error(ERROR_numberExpected);
    switch (theToken.tok) {
    case T.integer:
    case T.decimal:
    case T.identifier:
    case T.string:
    case T.leftbrace:
    case T.define:
      break;
    default:
      if (!T.tokAttr(theToken.tok, T.misc))
        return error(ERROR_numberOrVariableNameExpected);
    }
    addTokenToPostfixInt(tokenComparator.tok, tokenAtomProperty.tok,
        tokenComparator.value + (isNegative ? " -" : ""));
    if (tokenAtomProperty.tok == T.property)
      addTokenToPostfixToken(tokenAtomProperty);
    if (theToken.tok == T.leftbrace) {
      returnToken();
      return clausePrimitive();
    }
    addTokenToPostfixToken(theToken);
    if (theToken.tok == T.define)
      return clauseDefine(true, false);
    return true;
  }

  private boolean clauseCell(int tok) {
    P3 cell = new P3();
    tokenNext(); // CELL
    if (!tokenNextTok(T.opEQ)) // =
      return errorStr(ERROR_tokenExpected, "=");
    if (getToken() == null)
      return error(ERROR_coordinateExpected);
    // 555 = {1 1 1}
    //Token coord = tokenNext(); // 555 == {1 1 1}
    if (theToken.tok == T.integer) {
      SimpleUnitCell.ijkToPoint3f(theToken.intValue,  cell,  1);
      return addTokenToPostfix(tok, cell);
    }
    if (theToken.tok != T.leftbrace || !getNumericalToken())
      return error(ERROR_coordinateExpected); // i
    cell.x = floatValue();
    if (tokPeekIs(T.comma)) // ,
      tokenNext();
    if (!getNumericalToken())
      return error(ERROR_coordinateExpected); // j
    cell.y = floatValue();
    if (tokPeekIs(T.comma)) // ,
      tokenNext();
    if (!getNumericalToken() || !tokenNextTok(T.rightbrace))
      return error(ERROR_coordinateExpected); // k
    cell.z = floatValue();
    return addTokenToPostfix(tok, cell);
  }

  private boolean clauseDefine(boolean haveToken, boolean forceString) {
    if (!haveToken) {
      T token = tokenNext();
      if (forceString) // we know it is @, this forces string type
        token = T.tokenDefineString;
      addTokenToPostfixToken(token);
    }
    if (tokPeekIs(T.nada))
      return error(ERROR_endOfCommandUnexpected);
    // we allow @x[1], which compiles as {@x}[1], not @{x[1]}
    // otherwise [1] gets read as a general atom name selector
    if (!addSubstituteTokenIf(T.leftbrace, T.tokenExpressionBegin)) {
      if (tokPeek() == T.define)
        addNextToken();
      return addNextToken() && checkForItemSelector(true);
    }
    while (moreTokens() && !tokPeekIs(T.rightbrace)) {
      if (tokPeekIs(T.leftbrace)) {
        if (!checkForCoordinate(true))
          return false;
      } else {
        addNextToken();
      }
    }
    return addSubstituteTokenIf(T.rightbrace, T.tokenExpressionEnd)
        && checkForItemSelector(true);
  }

  private boolean residueSpecCodeGenerated;

  private boolean generateResidueSpecCode(T token) {
    if (residueSpecCodeGenerated)
      addTokenToPostfixToken(T.tokenAndSpec);
    addTokenToPostfixToken(token);
    residueSpecCodeGenerated = true;
    return true;
  }

  private boolean clauseResidueSpec() {
    int tok = tokPeek();
    residueSpecCodeGenerated = false;
    boolean checkResNameSpec = false;
    switch (tok) {
    case T.nada:
      // terminal
    case T.dna:
    case T.rna:
      return false;
    case T.integer:
      // select 33    
      // select 33-35
    case T.colon:
      // select :a
    case T.percent:
      // select %1
    case T.inscode:
      // sequence code precompiled
      break;
    case T.times:
      // all
    case T.leftsquare:
      // [ala]
    case T.identifier:
      // ala
      checkResNameSpec = true;
      break;
    default:
      if (T.tokAttr(tok, T.comparator))
        return false;
      String str = "" + valuePeek();
      checkResNameSpec = (str.length() == 2 || str.length() == 3);
      // note: there are many groups that could
      // in principle be here, for example:
      // "AND" "SET" "TO*"
      // these need to have attribute expression to be here
      // but then there are FX FY FZ UX UY UZ .. 
      if (!checkResNameSpec)
        return false;
    }
    boolean specSeen = false;
    if (checkResNameSpec) {
      if (!clauseResNameSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == T.integer || tok == T.times || tok == T.inscode) {
      // [ala]33 
      // ala33
      // [ala]*
      // [ala]<precompiled seqcode
      if (!clauseSequenceSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == T.colon
// BH 1/19/15 reduced allowance here -- just too much possible
// that is undocumented, especially with multiple-character chains 
//        || tok == T.times 
//        || theToken.tok ==  T.rightsquare && 
//            (tok == T.identifier
//              || tok == T.x || tok == T.y || tok == T.z || tok == T.w)
//              || tok == T.integer && !wasInteger
              ) {
      if (!clauseChainSpec(tok))
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == T.per) {
      if (!clauseAtomSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == T.percent) {
      if (!clauseAlternateSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (tok == T.divide) { 
      // BH 1/19/2015: was || tok == T.colon here, but then that would
      // allow undocumented [ala]:a:1 to be the same as [ala]:a/1 ?? 
      if (!clauseModelSpec())
        return false;
      specSeen = true;
      tok = tokPeek();
    }
    if (!specSeen)
      return error(ERROR_residueSpecificationExpected);
    if (!residueSpecCodeGenerated) {
      // nobody generated any code, so everybody was a * (or equivalent)
      addTokenToPostfixToken(T.tokenAll);
    }
    return true;
  }

  /**
   * [a] or just a
   * @return true if handled
   */
  private boolean clauseResNameSpec() {
    getToken();
    int tok = tokPeek();
    switch (theToken.tok) {
    case T.times:
      return true;
    case T.leftsquare:
      String strSpec = "";
      while (getToken() != null && theToken.tok != T.rightsquare)
        strSpec += theValue;
      if (theToken == null)
        return false;
      if (strSpec == "")
        return true;
      int pt;
      return (strSpec.length() > 0 && (pt = strSpec.indexOf("*")) >= 0
          && pt != strSpec.length() - 1 ? error(ERROR_residueSpecificationExpected)
              : generateResidueSpecCode(T.o(T.spec_name_pattern, strSpec.toUpperCase())));
    default:
      if (T.tokAttr(tok, T.comparator)) {
        // a variable, not a name. For example:
        // a > 3
        // _e = 55
        returnToken();
        return false;
      }
      //check for a * in the next token, which
      //would indicate this must be a name with wildcard
      String res = (String) theValue;
      if (tok == T.times) {
        res = theValue + "*";
        getToken();
      }
      return generateResidueSpecCode(T.o(T.identifier, res));
    }
  }

  private boolean clauseSequenceSpec() {
    if (tokPeek() == T.times)
      return  (getToken() != null); // true
    T seqToken = getSequenceCode(false);
    if (seqToken == null)
      return false;
    int tok = tokPeek();
    if (tok == T.minus || tok == T.integer && intPeek() < 0) {
      if (tok == T.minus) {
        tokenNext();
      } else {
         // hyphen masquerading as neg int
          int i = -intPeek();
          tokenNext().intValue = i;
          returnToken();
      }
      seqToken.tok = T.spec_seqcode_range;
      generateResidueSpecCode(seqToken);
      return addTokenToPostfixToken(getSequenceCode(true));
    }
    return generateResidueSpecCode(seqToken);
  }

  private T getSequenceCode(boolean isSecond) {
    int seqcode = Integer.MAX_VALUE;
    int seqvalue = Integer.MAX_VALUE;
    switch (tokPeek()) {
    case T.inscode:
      seqcode = tokenNext().intValue;
      break;
    case T.integer:
      seqvalue = tokenNext().intValue;
      break;
    default:
      if (!isSecond)
        return null;
      // can have open-ended range  
      // select 3-
    }
    return T.tv(T.spec_seqcode, seqvalue, Integer.valueOf(seqcode));
  }

  /**
   * [:] [chars]
   * 
   * [:] ["chars"]
   * 
   * [:] [*]
   * 
   * [:] [0-9]
   * 
   * [:] [?]
   * 
   * [:] (empty chain)
   * 
   * @param tok
   * @return true if chain handled
   */
  private boolean clauseChainSpec(int tok) {
    tokenNext();
    tok = tokPeek();
    String strChain;
    if (isTerminator(tok)) {
      strChain = " ";
    } else {
      switch (tok) {
      case T.times:
        return (getToken() != null); // true
      case T.integer:
        getToken();
        int val = theToken.intValue;
        if (val < 0 || val > 9999)
          return error(ERROR_invalidChainSpecification);
        strChain = "" + val;
        break;
      case T.string:
        vwr.getChainID("a", true); // forces chain case
        //$FALL-THROUGH$
      default:
        strChain = "" + getToken().value;
        break;
      }
      if (strChain.length() == 0)
        strChain = " ";
      else if (strChain.equals("?"))
        return true;
    }
    int chain = vwr.getChainID(strChain, false);
    return generateResidueSpecCode(T.tv(T.spec_chain, chain, "spec_chain"));
  }

  /**
   * check for %x or % (null alternate)
   * 
   * @return true if no error
   */
  private boolean clauseAlternateSpec() {
    tokenNext();
    // check for termination -- is this really the full list?
    if (isTerminator(tokPeek()))
      return generateResidueSpecCode(T.o(T.spec_alternate, null));
    switch (getToken().tok) {
    case T.times:
    case T.string:
    case T.integer:
    case T.identifier:
    case T.opIf:
    case T.x:
    case T.y:
    case T.z:
    case T.w:
      break;
    default:
      return error(ERROR_invalidModelSpecification);
    }
    return generateResidueSpecCode(T.o(T.spec_alternate, theToken.value));
  }

  /**
   * we allow : and % to have null values
   * @param tok
   * @return true if terminating
   */
  private boolean isTerminator(int tok) {
    switch (tok) {
    case T.nada:
    case T.divide:
    case T.opAnd:
    case T.opOr:
    case T.opNot:
    case T.comma:
    case T.rightparen:
    case T.rightbrace:
      return true;
    default:
      return false;
    }
  }


  /**
   * process /1   /1.1   / *  or just /
   * 
   * 
   * @return true if no error
   */
  private boolean clauseModelSpec() {
    getToken();
    switch (tokPeek()) {
    case T.times:
      getToken();
      return true;
    case T.integer:
      return generateResidueSpecCode(T.o(T.spec_model, Integer
          .valueOf(getToken().intValue)));
    case T.decimal:
      return generateResidueSpecCode(T.tv(T.spec_model, fixModelSpec(getToken()), theValue));
    case T.comma:
    case T.rightbrace:
    case T.nada:
      // these are necessary to allow for {1 1 1/} or {1/,1/,1} in fractional coordinates  
      return generateResidueSpecCode(T.o(T.spec_model, Integer
          .valueOf(1)));
    }
    return error(ERROR_invalidModelSpecification);
  }

  private int fixModelSpec(T token) {
    int ival = token.intValue;
    if (ival == Integer.MAX_VALUE) {
      float f = ((Float) theValue).floatValue();
      if (f == (int) f)
        ival = ((int) f) * 1000000; 
      if (ival < 0)
        ival = Integer.MAX_VALUE;
    }
    return ival;
  }


  private boolean clauseAtomSpec() {
    if (!tokenNextTok(T.per))
      return error(ERROR_invalidAtomSpecification);
    if (getToken() == null)
      return true;
    String atomSpec = "";
    if (theToken.tok == T.integer) {
      atomSpec += "" + theToken.intValue;
      if (getToken() == null)
        return error(ERROR_invalidAtomSpecification);
    }
    if (theToken.tok == T.times)
      return true;
    // here we cannot depend upon the atom spec being an identifier
    // in other words, not a known Jmol word. As long as the period 
    // was there, we accept whatever is next
    atomSpec += "" + theToken.value;
    if (tokPeekIs(T.times)) {
      tokenNext();
      // this one is a '*' as a prime, not a wildcard
      atomSpec += "'";
    }
    return generateResidueSpecCode(T.tv(T.spec_atom, vwr.getJBR()
        .lookupSpecialAtomID(atomSpec.toUpperCase()), atomSpec));
  }
  
//----------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------
//------------   ERROR HANDLING   --------------------------------------------------------
//----------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------
  
  protected String errorMessage;
  protected String errorMessageUntranslated;
  protected String errorLine;
  protected String errorType;

  protected final static int ERROR_badArgumentCount  = 0;
  protected final static int ERROR_badContext  = 1;
  protected final static int ERROR_commandExpected = 2;
  protected final static int ERROR_endOfCommandUnexpected  = 4;
  protected final static int ERROR_invalidExpressionToken  = 9;
  protected final static int ERROR_missingEnd  = 11;
  protected final static int ERROR_tokenExpected  = 15;
  protected final static int ERROR_tokenUnexpected  = 16;
  protected final static int ERROR_unrecognizedParameter  = 18;
  protected final static int ERROR_unrecognizedToken  = 19;

  private final static int ERROR_coordinateExpected  = 3;
  private final static int ERROR_endOfExpressionExpected  = 5;
  private final static int ERROR_identifierOrResidueSpecificationExpected  = 6;
  private final static int ERROR_invalidAtomSpecification  = 7;
  private final static int ERROR_invalidChainSpecification  = 8;
  private final static int ERROR_invalidModelSpecification  = 10;
  private final static int ERROR_numberExpected  = 12;
  private final static int ERROR_numberOrVariableNameExpected  = 13;
  private final static int ERROR_residueSpecificationExpected  = 14;
  private final static int ERROR_unrecognizedExpressionToken  = 17;
  
  static String errorString(int iError, String value, String more,
                            boolean translated) {
    boolean doTranslate = false;
    if (!translated && (doTranslate = GT.getDoTranslate()) == true)
      GT.setDoTranslate(false);
    String msg;
    switch (iError) {
    default:
      msg = "Unknown compiler error message number: " + iError;
      break;
    case ERROR_badArgumentCount: // 0;
      msg = GT._("bad argument count"); // 0
      break;
    case ERROR_badContext: // 1;
      msg = GT._("invalid context for {0}"); // 1
      break;
    case ERROR_commandExpected: // 2;
      msg = GT._("command expected"); // 2
      break;
    case ERROR_coordinateExpected: // 3;
      msg = GT._("{ number number number } expected"); // 3
      break;
    case ERROR_endOfCommandUnexpected: // 4;
      msg = GT._("unexpected end of script command"); // 4
      break;
    case ERROR_endOfExpressionExpected: // 5;
      msg = GT._("end of expression expected"); // 5
      break;
    case ERROR_identifierOrResidueSpecificationExpected: // 6;
      msg = GT._("identifier or residue specification expected"); // 6
      break;
    case ERROR_invalidAtomSpecification: // 7;
      msg = GT._("invalid atom specification"); // 7
      break;
    case ERROR_invalidChainSpecification: // 8;
      msg = GT._("invalid chain specification"); // 8
      break;
    case ERROR_invalidExpressionToken: // 9;
      msg = GT._("invalid expression token: {0}"); // 9
      break;
    case ERROR_invalidModelSpecification: // 10;
      msg = GT._("invalid model specification"); // 10
      break;
    case ERROR_missingEnd: // 11;
      msg = GT._("missing END for {0}"); // 11
      break;
    case ERROR_numberExpected: // 12;
      msg = GT._("number expected"); // 12
      break;
    case ERROR_numberOrVariableNameExpected: // 13;
      msg = GT._("number or variable name expected"); // 13
      break;
    case ERROR_residueSpecificationExpected: // 14;
      msg = GT._("residue specification (ALA, AL?, A*) expected"); // 14
      break;
    case ERROR_tokenExpected: // 15;
      msg = GT._("{0} expected"); // 15
      break;
    case ERROR_tokenUnexpected: // 16;
      msg = GT._("{0} unexpected"); // 16
      break;
    case ERROR_unrecognizedExpressionToken: // 17;
      msg = GT._("unrecognized expression token: {0}"); // 17
      break;
    case ERROR_unrecognizedParameter: // 18;
      msg = GT._("unrecognized {0} parameter"); // 18
      break;
    case ERROR_unrecognizedToken: // 19;
      msg = GT._("unrecognized token: {0}"); // 19
      break;
    }
    if (msg.indexOf("{0}") < 0) {
      if (value != null)
        msg += ": " + value;
    } else {
      msg = PT.rep(msg, "{0}", value);
      if (msg.indexOf("{1}") >= 0)
        msg = PT.rep(msg, "{1}", more);
      else if (more != null)
        msg += ": " + more;
    }
    if (!translated)
      GT.setDoTranslate(doTranslate);
    return msg;
  }
  
  protected boolean commandExpected() {
    ichToken = ichCurrentCommand;
    return error(ERROR_commandExpected);
  }

  protected boolean error(int error) {
    return errorIntStr2(error, null, null);
  }

  protected boolean errorStr(int error, String value) {
    return errorIntStr2(error, value, null);
  }
  
  protected boolean errorIntStr2(int iError, String value, String more) {
    String strError = errorString(iError, value, more, true);
    String strUntranslated = (GT.getDoTranslate() ? errorString(iError, value, more, false) : null);
    return errorStr2(strError, strUntranslated);
  }

  private boolean isError() {
    return errorMessage != null;
  }
  
  protected boolean errorStr2(String errorMessage, String strUntranslated) {
    this.errorMessage = errorMessage;
    errorMessageUntranslated = strUntranslated;
    return false;
  }

}
