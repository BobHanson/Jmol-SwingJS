/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.script;

class ScriptFlowContext {
  /*
   * Flow Contexts in Jmol 11.3.23+  -- Bob Hanson
   * 
   * As of Jmol 11.3.23, the Jmol scripting language includes a variety of 
   * standard flow control structures:
   * 
   * script filename.spt  # the principal file-based context
   * 
   * function xxx(a,b,c)/end function
   * 
   * if/elseif/else/endif
   * for/break/continue/end for
   * while/break/continue/end while
   * 
   * This is being handled by creating a "flow context" for each of these elements. 
   * There can be only one currently active flow context. 
   * 
   * The primary context is the script file. Variables declared using the "var"
   * keyword are isolated to that script file. 
   * 
   * Function contexts may only be created within the .spt file context. Note that
   * functions are saved in a STATIC Hashtable, htFunctions, so these definitions
   * apply to all applets present from a given server and for an entire user session.
   * There is no particular reason they HAVE to be static, however.
   * 
   * Other contexts may be nested to potentially any depth, as in all programming
   * languages.
   * 
   * The original syntactic model was related most closely to Visual Basic, where
   * braces are not used and, instead, one has "END IF" and "NEXT" commands. While
   * that is still in place, it has been extended in two ways in Jmol 11.7.40:
   * 
   *  (1) If the complete statement is all on one line, then no terminator is required:
   *  
   *      if (i > 3) print "OK"
   *      for (i = 0; i < 10; ++i) {atomno=i}.color = myColors[i]
   *      while (++i < 10) doThis(i)
   * 
   *  (2) Braces work as well - same as Java or JavaScript, ALMOST:
   *  
   *      if (i > 3) { print "OK" } else { print "nope" }
   *      for (i = 0; i < 10; ++i) { {atomno=i}.color = myColors[i] }
   *      while (++i < 10) { doThis(i) }
   *      
   * Both of these syntaxes can extend to multiple lines, where in the first 
   * we need a terminator (END IF or ENDIF, END FOR, END WHILE), while in the
   * second case we need braces. Partially this is just for readability. 
   * 
   * Notice the difference between this syntax and Java/JavaScript when it comes to 
   * not using terminators or braces: The reason JavaScript and Java allow no braces
   * is that in the case where there is ONE result statement, that is unambiguous. 
   * Here I have chosen to allow multiple statements between IF/ELSE/ENDIF, so when
   * using braces across multiple lines, we need the braces around everything. 
   * The advantage of this is that we do NOT need braces for simple single-line cases:
   * 
   * if (i > 3) set echo top left; echo "i is greater than 3" else background red; echo "no"
   * 
   * The basic flow control syntax uses the Visual Basic-like "end" syntax. So we have;
   * 
   * if (x)                   if (x) {
   *  [do this]                 [do this]
   * else if (y)              } else if (y) {
   *  [do this]     or          [do this]
   * else                     } else {
   *  [do this]                 [do this]
   * end if                   }
   * 
   * The placement of the braces at the end of the line, at the beginning of the 
   * line, wherever, is totally up to the developer. Jmol will figure it out.
   * 
   * The keywords "elseif" and "endif" are synonymous with "else if" and "end if",
   * just to make that a non-issue. Documentation will refer to "else if" and "end if"
   * so as to be fully consistent with "end for", "end while", and "end function".
   * 
   * TOKENIZATION OF FLOW CONTROL
   * ----------------------------
   * 
   * The .tok field of the command token maintains a variety of attributes for all
   * commands. Two new attributes include 
   * 
   *   noeval        function/end function are not evaluated
   *   flowcommand   function/if/else/elseif/endif/for/while/break/continue/end
   *                   indicates special intValue is in effect and that parameter
   *                   number checking is done separately. 
   * 
   * Tokens in general have three fields: int tok, int intValue, and Object value.  
   * The system implemented in jmol 11.3.23 involves coopting the intValue field of
   * the first token of each statement -- the command token. This formerly static field is 
   * generally used to indicate the number of allowed parameters, but that's not
   * necessary for this small set of special commands. Because we are using it dynamically,
   * all flowcommand tokens are copies, not the originals. 
   * 
   * The commands if/elseif/else/endif are implemented as a singly-linked list. 
   * Each intValue field points to the next in the series. In the case of elseif and else, 
   * if this pointer is negative, it indicates that a previous block has been 
   * executed, and this block should be skipped.   
   *  
   * The commands for/end for and while/end while implement intValue as circularly-linked
   * lists. The for/while statement intValue field points to its end statement, and the 
   * end statement points to its corresponding for/while statement. 
   * 
   * In addition, break and continue point to their corresponding for or while 
   * statement so that the end statement pointer can be retrieved (in the case of break)
   * or used for direction (continue). 
   * 
   * If a number is added after break or continue, it indicates the number of levels 
   * of for/while to skip. Thus, "break 1" breaks to one level above the current context.  
   * 
   */
  
  private ScriptCompiler compiler;
  ContextToken token;
  int pt0;
  int ptDefault;
  ScriptFunction function;
  SV var;
  ScriptFlowContext parent;
  int lineStart;
  int commandStart;
  int ptLine;
  int ptCommand;
  boolean forceEndIf = true;
  String ident;
  int addLine;
  int tok0;
  public int ichCommand;
  short line0;
  
  ScriptFlowContext(ScriptCompiler compiler, ContextToken token, int pt0, ScriptFlowContext parent, int ich, short line0) {
    this.compiler = compiler;
    this.token = token;
    tok0 = token.tok;
    this.ident = (String)token.value;
    this.pt0 = pt0;
    this.line0 = line0;
    this.parent = parent;
    ichCommand = ich;
    lineStart = ptLine = this.compiler.lineCurrent;
    commandStart = ptCommand = this.compiler.iCommand;
    //System.out.println ("FlowContext: init " + this);  
  }
  
  ScriptFlowContext getBreakableContext(int nLevelsUp) {
    ScriptFlowContext f = this;
    while (f != null && (!ScriptCompiler.isBreakableContext(f.token.tok) || nLevelsUp-- > 0))
      f = f.parent;
    return f;
  }
  
  boolean checkForceEndIf(int offset) {
    if (ptCommand == compiler.iCommand && addLine > 0)
      addLine++;
    boolean test = forceEndIf 
        && ptCommand < compiler.iCommand 
        && ptLine + (addLine == 0 ? 0 : addLine + offset) == compiler.lineCurrent;
    if (test) // only once!
      forceEndIf = false;
    return test;
  }

  int setPt0(int pt0, boolean isDefault) {
    this.pt0 = pt0;
    if (isDefault)
      ptDefault = pt0;
    setLine();
    return pt0;
  }

  void setLine() {
    ptLine = compiler.lineCurrent;
    ptCommand = compiler.iCommand + 1;
  }
  
  @Override
  public String toString() {
    return "ident " + ident
        + " line " + lineStart 
        + " command " + commandStart;  
  }
  
  String path() {
    String s = "";
    ScriptFlowContext f = this;
    while (f != null) {
      s = f.ident + "-" + s;
      f = f.parent;
    }
    return "[" + s + "]";
  }
  
  void setFunction(ScriptFunction function) {
    this.function = function;
  }
}