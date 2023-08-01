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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolParallelProcessor;

import javajs.util.SB;

public class ScriptContext {
  
  private static int contextCount = 0;

  private T[][] aatoken;
  boolean allowJSThreads;
  boolean chk;
  public String contextPath = " >> ";
  public Map<String, SV> vars;
  boolean displayLoadErrorsSave;
  public String errorMessage;
  String errorMessageUntranslated;
  public String errorType;
  public boolean executionPaused;
  public boolean executionStepping;
  public boolean isEditor;
  public boolean isEditorScript;
  public String functionName;
  public int iCommandError = -1;
  public int id;
  public boolean isComplete = true;
  boolean isFunction;
  public boolean isJSThread;
  boolean isStateScript;
  boolean isTryCatch;
  SV[] forVars;
  int iToken;
  int lineEnd = Integer.MAX_VALUE;
  public int[][] lineIndices;
  short[] lineNumbers;
  public boolean mustResumeEval;
  public SB outputBuffer;
  JmolParallelProcessor parallelProcessor;
  public ScriptContext parentContext;
  public int pc;
  public int pc0;
  public int pcEnd = Integer.MAX_VALUE;
  public String script;
  String scriptExtensions;
  public String scriptFileName;
  int scriptLevel;
  public T[] statement;
  Map<String, String> htFileCache;
  int statementLength;
  ContextToken token;
  int tryPt;
  T theToken;
  int theTok;

  private int[] pointers;

  public String why;
  
  public Map<String, ScriptFunction> privateFuncs;
  
  ScriptContext() {
    id = ++contextCount;
  }

  public void setMustResume() {
    ScriptContext sc = this;
    while (sc != null) {
      sc.mustResumeEval = true;
      sc.pc = sc.pc0;
      sc = sc.parentContext;
    }
  }

  /**
   * Context variables go up the stack until a 
   * function is found. That is considered to be
   * the highest level. 
   * 
   * @param var
   * @return  context variables
   */
  public SV getVariable(String var) {
    ScriptContext context = this;
    SV v;
    while (context != null && !context.isFunction) {
      if (context.vars != null
          && (v = context.vars.get(var)) != null)
        return v;
      context = context.parentContext;
    }
    return null;
  }

  public Map<String, SV> getFullMap() {
    Map<String, SV> ht = new Hashtable<String, SV>();
    ScriptContext context = this;
    if (contextPath != null)
      ht.put("_path", SV.newS(contextPath));
    while (context != null && !context.isFunction) {
      if (context.vars != null)
        for (String key : context.vars.keySet())
          if (!ht.containsKey(key)) {
            SV val = context.vars.get(key);
            if (val.tok != T.integer || val.intValue != Integer.MAX_VALUE)
              ht.put(key, val);
          }
      context = context.parentContext;
    }
    return ht;
  }

  /**
   * save pointers indicating state of if/then
   * @param aa the command array token list
   * 
   */
  void saveTokens(T[][] aa) {
    aatoken = aa;
    if (aa == null) {
      pointers = null;
      return;
    }
    pointers = new int[aa.length];
    for (int i = pointers.length; --i >= 0;)
      pointers[i] = (aa[i] == null ? -1 : aa[i][0].intValue);
  }
  
  T[][] restoreTokens() {
    if (pointers != null)
      for (int i = pointers.length; --i >= 0;)
        if (aatoken[i] != null)
          aatoken[i][0].intValue = pointers[i];
    return aatoken;
  }

  public int getTokenCount() {
    return (aatoken == null ? -1 : aatoken.length);
  }

  public T[] getToken(int i) {
    return aatoken[i];
  }

}