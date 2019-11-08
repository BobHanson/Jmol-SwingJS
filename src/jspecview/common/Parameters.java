package jspecview.common;

import java.util.Hashtable;
import java.util.Map;

public class Parameters {

	public String name;
  public double integralMinY = IntegralData.DEFAULT_MINY;
  public double integralRange = IntegralData.DEFAULT_RANGE;
  public double integralOffset = IntegralData.DEFAULT_OFFSET;
	public boolean integralDrawAll = false;
	public double viewOffset = 0;

  public double peakListThreshold = Double.NaN; // <= 0 disables these
  public String peakListInterpolation = "parabolic";
  public int precision = 2;

	public Parameters() {
    htBooleans = new Hashtable<ScriptToken, Boolean>();
    setBoolean(ScriptToken.TITLEON, true);
    setBoolean(ScriptToken.ENABLEZOOM, true);
    setBoolean(ScriptToken.DISPLAY2D, true);
    setBoolean(ScriptToken.COORDINATESON, true);
    setBoolean(ScriptToken.PEAKTABSON, true);
    setBoolean(ScriptToken.POINTSONLY, false);
    setBoolean(ScriptToken.GRIDON, true);
    setBoolean(ScriptToken.XSCALEON, true);
    setBoolean(ScriptToken.YSCALEON, true);
    setBoolean(ScriptToken.XUNITSON, true);
    setBoolean(ScriptToken.YUNITSON, true);
	}

	public Parameters setName(String name) {
		this.name = name;
		return this;
	}
	
  protected Map<ScriptToken, Boolean> htBooleans;
	
  public Map<ScriptToken, Boolean> getBooleans() {
    return htBooleans;
  }

  public boolean setBoolean(ScriptToken st, boolean val) {
    htBooleans.put(st, Boolean.valueOf(val));
    return val;
  }

  public boolean getBoolean(ScriptToken t) {
    return Boolean.TRUE == htBooleans.get(t);
  }
    
  public static boolean isTrue(String value) {
    return (value.length() == 0 || Boolean.parseBoolean(value)); 
  }
  
	public static Boolean getTFToggle(String value) {
		return (value.equalsIgnoreCase("TOGGLE") ? null
				: isTrue(value) ? Boolean.TRUE : Boolean.FALSE);
	}

	public void setP(PanelData pd, ScriptToken st, String value) {
		switch (st) {
		default:
			return;
		case COORDINATESON:
		case DISPLAY1D:
		case DISPLAY2D:
		case ENABLEZOOM:
		case GRIDON:
		case POINTSONLY:
		case PEAKTABSON:
		case REVERSEPLOT:
		case TITLEON:
		case TITLEBOLDON:
		case XSCALEON:
		case XUNITSON:
		case YSCALEON:
		case YUNITSON:
			Boolean tfToggle = getTFToggle(value);
			if (tfToggle != null) {
				setBoolean(st, tfToggle.booleanValue());
				break;
			}
			if (pd == null)
				return;
			boolean b = !pd.getBoolean(st);
			switch (st) {
			default:
				break;
			case XSCALEON:
				setBoolean(ScriptToken.XUNITSON, b);
				pd.setBoolean(ScriptToken.XUNITSON, b);
				break;
			case YSCALEON:
				setBoolean(ScriptToken.YUNITSON, b);
				pd.setBoolean(ScriptToken.YUNITSON, b);
				break;
			}
			setBoolean(st, b);
			break;
		}
		if (pd == null)
			return;
		pd.setBooleans(this, st);
	}
	
	public static boolean isMatch(String match, String key) {
		return match == null || key.equalsIgnoreCase(match);
	}

	public static void putInfo(String match, Map<String, Object> info,
	                           String key, Object value) {
	  if (value != null && isMatch(match, key))
	    info.put(match == null ? key : match, value);
	}



}
