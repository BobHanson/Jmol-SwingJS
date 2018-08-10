package org.jmol.script;

import javajs.api.GenericPlatform;
import javajs.util.PT;

import org.jmol.api.JmolScriptEvaluator;
import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

/**
 * Error handling for ScriptEvaluator, ScriptProcess, and ScriptParams
 * 
 */
public abstract class ScriptError implements JmolScriptEvaluator {

  abstract protected void showStringPrint(String msg, boolean mustDo);
  
  public Viewer vwr;
  public boolean chk;

  protected boolean ignoreError;
  protected boolean error;
  protected String errorMessage;
  protected String errorMessageUntranslated;
  protected String errorType;
  protected int iCommandError;

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String getErrorMessageUntranslated() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  public void invArg() throws ScriptException {
    error(ERROR_invalidArgument);
  }

  public void bad() throws ScriptException {
    error(ERROR_badArgumentCount);
  }
  
  public void integerOutOfRange(int min, int max) throws ScriptException {
    errorOrWarn(ERROR_integerOutOfRange, "" + min, "" + max, null, true);
  }

  protected void numberOutOfRange(float min, float max) throws ScriptException {
    errorOrWarn(ERROR_numberOutOfRange, "" + min, "" + max, null, true);
  }

  public void error(int iError) throws ScriptException {
    errorOrWarn(iError, null, null, null, false);
  }

  public void errorStr(int iError, String value) throws ScriptException {
    errorOrWarn(iError, value, null, null, false);
  }

  public void errorStr2(int iError, String value, String more)
      throws ScriptException {
    errorOrWarn(iError, value, more, null, false);
  }

  void errorMore(int iError, String value, String more, String more2)
      throws ScriptException {
    errorOrWarn(iError, value, more, more2, false);
  }

  protected void warning(int iError, String value, String more)
      throws ScriptException {
    errorOrWarn(iError, value, more, null, true);
  }

  private void errorOrWarn(int iError, String value, String more, String more2,
                           boolean warningOnly) throws ScriptException {
    String strError = (ignoreError ? null : errorString(iError, value, more,
        more2, true));
    String strUntranslated = (ignoreError || !GT.getDoTranslate() ? null
        : errorString(iError, value, more, more2, false));
    if (!warningOnly)
      evalError(strError, strUntranslated);
    showStringPrint(strError, true);
  }

  public void evalError(String message, String strUntranslated)
      throws ScriptException {
    
    // called by ScriptError, ScriptParam, ScriptExt
    
    if (ignoreError)
      throw new NullPointerException();
    if (strUntranslated == null)
      strUntranslated = message;
    if (!chk) {
      // String s = vwr.getSetHistory(1);
      // vwr.addCommand(s + CommandHistory.ERROR_FLAG);
      setCursorWait(false);
      vwr.setBooleanProperty("refreshing", true);
      vwr.setStringProperty("_errormessage", strUntranslated);
    }
    throw new ScriptException(this, message, strUntranslated, true);
  }

  public void setCursorWait(boolean TF) {
    if (!chk)
      vwr.setCursor(TF ? GenericPlatform.CURSOR_WAIT
          : GenericPlatform.CURSOR_DEFAULT);
  }

  final static int ERROR_axisExpected = 0;
  final static int ERROR_backgroundModelError = 1;
  public final static int ERROR_badArgumentCount = 2;
  final static int ERROR_badMillerIndices = 3;
  public final static int ERROR_badRGBColor = 4;
  final static int ERROR_booleanExpected = 5;
  final static int ERROR_booleanOrNumberExpected = 6;
  final static int ERROR_booleanOrWhateverExpected = 7;
  final static int ERROR_colorExpected = 8;
  final static int ERROR_colorOrPaletteRequired = 9;
  final static int ERROR_commandExpected = 10;
  final static int ERROR_coordinateOrNameOrExpressionRequired = 11;
  final static int ERROR_drawObjectNotDefined = 12;
  public final static int ERROR_endOfStatementUnexpected = 13;
  public final static int ERROR_expressionExpected = 14;
  public final static int ERROR_expressionOrIntegerExpected = 15;
  final static int ERROR_filenameExpected = 16;
  public final static int ERROR_fileNotFoundException = 17;
  public final static int ERROR_incompatibleArguments = 18;
  public final static int ERROR_insufficientArguments = 19;
  final static int ERROR_integerExpected = 20;
  final static int ERROR_integerOutOfRange = 21;
  public final static int ERROR_invalidArgument = 22;
  public final static int ERROR_invalidParameterOrder = 23;
  public final static int ERROR_keywordExpected = 24;
  public final static int ERROR_moCoefficients = 25;
  public final static int ERROR_moIndex = 26;
  public final static int ERROR_moModelError = 27;
  public final static int ERROR_moOccupancy = 28;
  public final static int ERROR_moOnlyOne = 29;
  public final static int ERROR_multipleModelsDisplayedNotOK = 30;
  public final static int ERROR_noData = 31;
  public final static int ERROR_noPartialCharges = 32;
  final static int ERROR_noUnitCell = 33;
  public final static int ERROR_numberExpected = 34;
  final static int ERROR_numberMustBe = 35;
  final static int ERROR_numberOutOfRange = 36;
  final static int ERROR_objectNameExpected = 37;
  final static int ERROR_planeExpected = 38;
  final static int ERROR_propertyNameExpected = 39;
  final static int ERROR_spaceGroupNotFound = 40;
  final static int ERROR_stringExpected = 41;
  final static int ERROR_stringOrIdentifierExpected = 42;
  final static int ERROR_tooManyPoints = 43;
  final static int ERROR_tooManyScriptLevels = 44;
  final static int ERROR_unrecognizedAtomProperty = 45;
  final static int ERROR_unrecognizedBondProperty = 46;
  final static int ERROR_unrecognizedCommand = 47;
  final static int ERROR_unrecognizedExpression = 48;
  final static int ERROR_unrecognizedObject = 49;
  final static int ERROR_unrecognizedParameter = 50;
  final static int ERROR_unrecognizedParameterWarning = 51;
  final static int ERROR_unrecognizedShowParameter = 52;
  public final static int ERROR_what = 53;
  public final static int ERROR_writeWhat = 54;
  final static int ERROR_multipleModelsNotOK = 55;
  public final static int ERROR_cannotSet = 56;

  /**
   * @param iError
   * @param value
   * @param more
   * @param more2
   * @param translated
   * @return constructed error string
   * 
   */
  static String errorString(int iError, String value, String more,
                            String more2, boolean translated) {
    boolean doTranslate = false;
    if (!translated && (doTranslate = GT.getDoTranslate()) == true)
      GT.setDoTranslate(false);
    String msg;
    switch (iError) {
    default:
      msg = "Unknown error message number: " + iError;
      break;
    case ERROR_axisExpected:
      msg = GT._("x y z axis expected");
      break;
    case ERROR_backgroundModelError:
      msg = GT._("{0} not allowed with background model displayed");
      break;
    case ERROR_badArgumentCount:
      msg = GT._("bad argument count");
      break;
    case ERROR_badMillerIndices:
      msg = GT._("Miller indices cannot all be zero.");
      break;
    case ERROR_badRGBColor:
      msg = GT._("bad [R,G,B] color");
      break;
    case ERROR_booleanExpected:
      msg = GT._("boolean expected");
      break;
    case ERROR_booleanOrNumberExpected:
      msg = GT._("boolean or number expected");
      break;
    case ERROR_booleanOrWhateverExpected:
      msg = GT._("boolean, number, or {0} expected");
      break;
    case ERROR_cannotSet:
      msg = GT._("cannot set value");
      break;
    case ERROR_colorExpected:
      msg = GT._("color expected");
      break;
    case ERROR_colorOrPaletteRequired:
      msg = GT._("a color or palette name (Jmol, Rasmol) is required");
      break;
    case ERROR_commandExpected:
      msg = GT._("command expected");
      break;
    case ERROR_coordinateOrNameOrExpressionRequired:
      msg = GT._("{x y z} or $name or (atom expression) required");
      break;
    case ERROR_drawObjectNotDefined:
      msg = GT._("draw object not defined");
      break;
    case ERROR_endOfStatementUnexpected:
      msg = GT._("unexpected end of script command");
      break;
    case ERROR_expressionExpected:
      msg = GT._("valid (atom expression) expected");
      break;
    case ERROR_expressionOrIntegerExpected:
      msg = GT._("(atom expression) or integer expected");
      break;
    case ERROR_filenameExpected:
      msg = GT._("filename expected");
      break;
    case ERROR_fileNotFoundException:
      msg = GT._("file not found");
      break;
    case ERROR_incompatibleArguments:
      msg = GT._("incompatible arguments");
      break;
    case ERROR_insufficientArguments:
      msg = GT._("insufficient arguments");
      break;
    case ERROR_integerExpected:
      msg = GT._("integer expected");
      break;
    case ERROR_integerOutOfRange:
      msg = GT._("integer out of range ({0} - {1})");
      break;
    case ERROR_invalidArgument:
      msg = GT._("invalid argument");
      break;
    case ERROR_invalidParameterOrder:
      msg = GT._("invalid parameter order");
      break;
    case ERROR_keywordExpected:
      msg = GT._("keyword expected");
      break;
    case ERROR_moCoefficients:
      msg = GT._("no MO coefficient data available");
      break;
    case ERROR_moIndex:
      msg = GT._("An MO index from 1 to {0} is required");
      break;
    case ERROR_moModelError:
      msg = GT._("no MO basis/coefficient data available for this frame");
      break;
    case ERROR_moOccupancy:
      msg = GT._("no MO occupancy data available");
      break;
    case ERROR_moOnlyOne:
      msg = GT._("Only one molecular orbital is available in this file");
      break;
    case ERROR_multipleModelsDisplayedNotOK:
      msg = GT._("{0} require that only one model be displayed");
      break;
    case ERROR_multipleModelsNotOK:
      msg = GT._("{0} requires that only one model be loaded");
      break;
    case ERROR_noData:
      msg = GT._("No data available");
      break;
    case ERROR_noPartialCharges:
      msg = GT
          ._("No partial charges were read from the file; Jmol needs these to render the MEP data.");
      break;
    case ERROR_noUnitCell:
      msg = GT._("No unit cell");
      break;
    case ERROR_numberExpected:
      msg = GT._("number expected");
      break;
    case ERROR_numberMustBe:
      msg = GT._("number must be ({0} or {1})");
      break;
    case ERROR_numberOutOfRange:
      msg = GT._("decimal number out of range ({0} - {1})");
      break;
    case ERROR_objectNameExpected:
      msg = GT._("object name expected after '$'");
      break;
    case ERROR_planeExpected:
      msg = GT
          ._("plane expected -- either three points or atom expressions or {0} or {1} or {2}");
      break;
    case ERROR_propertyNameExpected:
      msg = GT._("property name expected");
      break;
    case ERROR_spaceGroupNotFound:
      msg = GT._("space group {0} was not found.");
      break;
    case ERROR_stringExpected:
      msg = GT._("quoted string expected");
      break;
    case ERROR_stringOrIdentifierExpected:
      msg = GT._("quoted string or identifier expected");
      break;
    case ERROR_tooManyPoints:
      msg = GT._("too many rotation points were specified");
      break;
    case ERROR_tooManyScriptLevels:
      msg = GT._("too many script levels");
      break;
    case ERROR_unrecognizedAtomProperty:
      msg = GT._("unrecognized atom property");
      break;
    case ERROR_unrecognizedBondProperty:
      msg = GT._("unrecognized bond property");
      break;
    case ERROR_unrecognizedCommand:
      msg = GT._("unrecognized command");
      break;
    case ERROR_unrecognizedExpression:
      msg = GT._("runtime unrecognized expression");
      break;
    case ERROR_unrecognizedObject:
      msg = GT._("unrecognized object");
      break;
    case ERROR_unrecognizedParameter:
      msg = GT._("unrecognized {0} parameter");
      break;
    case ERROR_unrecognizedParameterWarning:
      msg = GT
          ._("unrecognized {0} parameter in Jmol state script (set anyway)");
      break;
    case ERROR_unrecognizedShowParameter:
      msg = GT._("unrecognized SHOW parameter --  use {0}");
      break;
    case ERROR_what:
      msg = "{0}";
      break;
    case ERROR_writeWhat:
      msg = GT._("write what? {0} or {1} \"filename\"");
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
      if (msg.indexOf("{2}") >= 0)
        msg = PT.rep(msg, "{2}", more);
    }
    if (doTranslate)
      GT.setDoTranslate(true);
    return msg;
  }

  public static String getErrorLineMessage(String functionName,
                                           String filename, int lineCurrent,
                                           int pcCurrent, String lineInfo) {
    String err = "\n----";
    if (filename != null || functionName != null)
      err += "line "
          + lineCurrent
          + " command "
          + (pcCurrent + 1)
          + " of "
          + (functionName == null ? filename
              : functionName.equals("try") ? "try" : "function " + functionName)
          + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  protected void setErrorMessage(String err) {
    errorMessageUntranslated = null;
    if (err == null) {
      error = false;
      errorType = null;
      errorMessage = null;
      iCommandError = -1;
      return;
    }
    error = true;
    if (errorMessage == null) // there could be a compiler error from a script
      // command
      errorMessage = GT._("script ERROR: ");
    errorMessage += err;
  }


}
