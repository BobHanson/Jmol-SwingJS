/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

public class ScriptInterruption extends ScriptException {
  //boolean willResume;
  public ScriptInterruption(ScriptEval eval, String why, int millis) {
    super(eval, why, "!", millis == Integer.MIN_VALUE || eval.vwr.autoExit);
    //willResume = (millis != Integer.MAX_VALUE && millis != Integer.MIN_VALUE);
    // the delay execution is here just to ensure that even a delay of 0 
    // still forces the interruption.
    if (why.startsWith(ScriptEval.CONTEXT_DELAY)) {
      int pt = why.indexOf(":");
      String script = (pt > 0 ? why.substring(pt + 1) : null);
      eval.delayScriptAndRun(millis, script);
    }
  }  
}