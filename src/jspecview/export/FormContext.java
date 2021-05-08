/* Copyright (c) 2006-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.export;

import java.util.Hashtable;

import java.util.Map;

import javajs.util.OC;
import javajs.util.DF;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Logger;

import jspecview.common.Coordinate;

/**
 * A simple Velocity-like template form filler
 * -- Supports only "==" "=" "!=" and no SET
 * -- # directives must be first non-whitespace character of the line
 *
 * @author Bob Hanson, hansonr@stolaf.edu
 *
 */
class FormContext {

  String[] tokens;
  Hashtable<String, Object> context = new Hashtable<String, Object>();
  Lst<FormToken> formTokens;

  FormContext() {
  }

  void put(String key, Object value) {
    if (value == null)
      value = "";
    context.put(key, value);
  }

  String setTemplate(String template) {
    String errMsg = getFormTokens(template);
    if (errMsg != null)
      return errMsg;
    return null;
  }

  int commandLevel;
  Lst<Integer> cmds = new Lst<Integer>();
  String strError;

  final static int VT_DATA = 0;
  final static int VT_IF = 1;
  final static int VT_ELSE = 2;
  final static int VT_ELSEIF = 3;
  final static int VT_END = 4;
  final static int VT_FOREACH = 5;
  final static int VT_SET = 6;

  class FormToken {

    boolean hasVariable;
    int cmdType;
    int cmdPtr = -1;
    int endPtr = -1;
    int ptr;
    String var;
    Lst<Object> vc;
    int pointCount;
    String data;

    FormToken(String token, int firstChar) {
      hasVariable = token.indexOf("$") >= 0;
      data = token;
      if (token.indexOf("#") != firstChar) {
        formTokens.addLast(this);
        return;
      }
      //System.out.println(firstChar + " " + token);
      ptr = formTokens.size();
      boolean checkIf = false;
      if (token.indexOf("#end") == firstChar) {
        cmdType = VT_END;
        endPtr = ptr;
        commandLevel--;
        if (commandLevel < 0) {
          strError = "misplaced #end";
          return;
        }
        cmdPtr = cmds.removeItemAt(0).intValue();
        formTokens.get(cmdPtr).endPtr = ptr;
      } else {
        commandLevel++;
        if (token.indexOf("#if") == firstChar) {
          cmdType = VT_IF;
          cmds.add(0, new Integer(ptr));
        } else if (token.indexOf("#foreach") == firstChar) {
          cmdType = VT_FOREACH;
          cmds.add(0, new Integer(ptr));
          cmdPtr = ptr;
          if (token.indexOf("#end") > 0) {
            int pt = token.indexOf(")") + 1;
            data = token.substring(0, pt);
            formTokens.addLast(this);
            new FormToken(token.substring(pt, token.indexOf("#end")), 0);
            new FormToken("#end", 0);
            return;
          }
        } else if (token.indexOf("#elseif") == firstChar) {
          if (cmds.size() == 0) {
            strError = "misplaced #elseif";
            return;
          }
          cmdType = VT_ELSEIF;
          cmdPtr = cmds.removeItemAt(0).intValue();
          FormToken vt = formTokens.get(cmdPtr);
          checkIf = true;
          vt.endPtr = ptr;
          cmds.add(0, new Integer(ptr));
        } else if (token.indexOf("#else") == firstChar) {
          if (cmds.size() == 0) {
            strError = "misplaced #else";
            return;
          }
          cmdType = VT_ELSE;
          checkIf = true;
          cmdPtr = cmds.removeItemAt(0).intValue();
          formTokens.get(cmdPtr).endPtr = ptr;
          cmds.add(0, new Integer(ptr));
        } else {
          Logger.warn("??? " + token);
        }
        if (checkIf) {
          FormToken vt = formTokens.get(cmdPtr);
          if (vt.cmdType != VT_IF && vt.cmdType != VT_ELSEIF) {
            strError = "misplaced " + token.trim();
            return;
          }
        }
      }
      formTokens.addLast(this);
    }
  }

  private String getFormTokens(String template) {
    formTokens = new Lst<FormToken>();
    if (template.indexOf("\r\n") >= 0)
      template = PT.replaceAllCharacters(template, "\r\n", "\n");
    template = template.replace('\r', '\n');
    String[] lines = template.split("\n");
    String token = "";
    for (int i = 0; i < lines.length && strError == null; i++) {
      String line = lines[i];
  	  int m = line.length();
  	  // right-trim line of whitespace
  	  char ch;
  	  while (--m >= 0 && ((ch = line.charAt(m)) == ' ' || ch == '\t')) {}
  	  line = line.substring(0, m + 1);
      if (line.length() == 0)
        continue;
      int firstChar = -1;
      int nChar = line.length();
      while (++firstChar < nChar && Character.isWhitespace(line.charAt(firstChar))) {
      }
      if (line.indexOf("#") == firstChar) {
        if (token.length() > 0) {
          new FormToken(token, 0);
          token = "";
        }
        if (strError != null)
          break;
        new FormToken(line, firstChar);
        continue;
      }
      token += line + "\n";
    }
    if (token.length() > 0 && strError == null) {
      new FormToken(token, 0);
    }
    return strError;
  }

	@SuppressWarnings("unchecked")
	public String merge(OC out) {
		int ptr;
		for (int i = 0; i < formTokens.size() && strError == null; i++) {
			FormToken vt = formTokens.get(i);
			// System.out.println(i + " " + vt.ptr + " " + vt.cmdType + " " +
			// vt.cmdPtr + " "
			// + vt.endPtr + vt.data);
			switch (vt.cmdType) {
			case VT_DATA:
				String data = fillData(vt.data);
				out.append(data);
				continue;
			case VT_IF:
				if (evaluate(vt.data, true)) {
					vt.endPtr = -vt.endPtr;
				} else {
					i = vt.endPtr - 1;
				}
				continue;
			case VT_ELSE:
			case VT_ELSEIF:
				if ((ptr = formTokens.get(vt.cmdPtr).endPtr) < 0) {
					// previous block was executed -- skip to end
					formTokens.get(vt.cmdPtr).endPtr = -ptr;
					while ((vt = formTokens.get(vt.endPtr)).cmdType != VT_END) {
						// skip
					}
					i = vt.ptr;
					continue;
				}
				if (vt.cmdType == VT_ELSEIF) {
					if (evaluate(vt.data, true)) {
						vt.endPtr = -vt.endPtr;
					} else {
						i = vt.endPtr - 1;
					}
				}
				continue;
			case VT_FOREACH:
				foreach(vt);
				//$FALL-THROUGH$
			case VT_END:
				if ((vt = formTokens.get(vt.cmdPtr)).cmdType != VT_FOREACH)
					continue;
				if (vt.vc == null)
					continue;
				if (++vt.pointCount == vt.vc.size()) {
					i = vt.endPtr;
					continue;
				}
				Object varData = vt.vc.get(vt.pointCount);
				if (varData instanceof Coordinate) {
					Coordinate c = (Coordinate) varData;
					context.put("pointCount", new Integer(vt.pointCount));
					context.put(vt.var + ".xVal", new Double(c.getXVal()));
					context.put(vt.var + ".yVal", new Double(c.getYVal()));
					context.put(vt.var + ".getXString()", getXString(c));
					context.put(vt.var + ".getYString()", getYString(c));
				} else if (varData instanceof Map<?, ?>) {
					for (Map.Entry<String, String> entry : ((Map<String, String>) varData)
							.entrySet())
						context.put(vt.var + "." + entry.getKey(), entry.getValue());
				}
				i = vt.cmdPtr;
				continue;
			}
		}
		return (strError != null ? strError : out != null ? out.toString() : null);
	}

  /**
   * Returns the x value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the x value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getXString(Coordinate c) {
    return DF.formatDecimalTrimmed0(c.getXVal(), 8);
  }

  /**
   * Returns the y value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the y value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getYString(Coordinate c) {
    return DF.formatDecimalTrimmed0(c.getYVal(), 8);
  }



  @SuppressWarnings("unchecked")
  private void foreach(FormToken vt) {
    String data = vt.data;
    data = data.replace('(', ' ');
    data = data.replace(')', ' ');
    String[] tokens = PT.getTokens(data);
    if (tokens.length != 4) {
      return;
    }
    // #foreach  $xxx in XXX
    vt.var = tokens[1].substring(1);
    Object vc = context.get(tokens[3].substring(1));
    if (vc instanceof Lst)
      vt.vc = (Lst<Object>) vc;
    vt.cmdPtr = vt.ptr;
    vt.pointCount = -1;
  }

  private final static String[] ops = { "==", "!=", "=" };
  private final static int OP_EEQ = 0;
  private final static int OP_NE = 1;
  private final static int OP_EQ = 2;

  private static int findOp(String op) {
    for (int i = ops.length; --i >= 0;)
      if (ops[i].equals(op))
        return i;
    return -1;
  }

  /**
	 * @param data 
   * @param isIf  
   * @return  false if a problem
	 */
  private boolean evaluate(String data, boolean isIf) {
    int pt = data.indexOf("(");
    if (pt < 0) {
      strError = "missing ( in " + data;
      return false;
    }
    data = data.substring(pt + 1);
    pt = data.lastIndexOf(")");
    if (pt < 0) {
      strError = "missing ) in " + data;
      return false;
    }
    data = data.substring(0, pt);
    data = PT.rep(data, "=", " = ");
    data = PT.rep(data, "!", " ! ");
    data = PT.rep(data, "<", " < ");
    data = PT.rep(data, ">", " > ");
    data = PT.rep(data, "=  =", "==");
    data = PT.rep(data, "<  =", "<=");
    data = PT.rep(data, ">  =", ">=");
    data = PT.rep(data, "!  =", "!=");
    String[] tokens = PT.getTokens(data);
    String key = tokens[0].substring(1);
    boolean isNot = false;
    boolean x = false;
    String value = null;
    String compare = "";
    try {
    switch (tokens.length) {
    case 1:
      // #if($x)
      value = getValue(key);
      return (!value.equals("") && !value.equals("false"));
    case 2:
      // #if(!$x)
      if (key.equals("!")) {
        key = PT.trim(tokens[1], "$ ");
        value = getValue(key);
        return (value.equals("false") || value.equals(""));
      }
      break;
    case 3:
      // #if($x op "y")
      key = PT.trim(tokens[0], "$ ");
      value = getValue(key);
      compare = PT.trim(tokens[2], " \"");
      switch (findOp(tokens[1])) {
      case OP_EQ:
      case OP_EEQ:
        return (value.equals(compare));
      case OP_NE:
        return (!value.equals(compare));
      default:
        Logger.warn("???? " + key + " " + compare + " " + value);
      }
      break;
    }
    } catch (Exception e) {
      Logger.warn(e.toString() + " in VelocityContext.merge");
    }
//    if (value != null) {
  //    x = !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("0");
    //}
    return isNot ? !x : x;
  }

  private String getValue(String key) {
    return (context.containsKey(key) ? context.get(key).toString() : "");
  }

  private String fillData(String data) {
    int i = 0;
    int ccData = data.length();
    while (i < ccData) {
      while (i < ccData && data.charAt(i++) != '$') {
        // continue looking for start
      }
      if (i == ccData)
        break;
      int j = i;
      char ch;
      while (++j < ccData
          && (Character.isLetterOrDigit(ch = data.charAt(j)) || ch == '.' || ch == '_')) {
        // continue looking for end
      }
      if (j < ccData && data.charAt(j) == '(')
        j += 2;
      String key = data.substring(i, j);
      if (context.containsKey(key)) {
        Object value = context.get(key);
        String strValue;
        if (value instanceof Coordinate) {
          strValue = value.toString();
        } else {
          strValue = value.toString();
        }
        //System.out.println(key + " = " + value);
        data = data.substring(0, i - 1) + strValue + data.substring(j);
        ccData = data.length();
        i += strValue.length();
      }
    }
    return data;
  }

}
