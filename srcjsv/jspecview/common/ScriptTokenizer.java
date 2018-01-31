package jspecview.common;

import javajs.util.PT;

public class ScriptTokenizer {

  private String str;
	private int pt = -1;
	private int len;
	private boolean isCmd;
	private boolean doCheck = true;

	public ScriptTokenizer(String str, boolean isCmd) {
  	this.str = str;
  	this.len = str.length();
  	this.isCmd = isCmd;
  }

  static String nextStringToken(ScriptTokenizer eachParam,
                                        boolean removeQuotes) {
    String s = eachParam.nextToken();
    return (removeQuotes && s.charAt(0) == '"' && s.endsWith("\"") && s.length() > 1 ? PT.trimQuotes(s) : s);
  }

  public String nextToken() {
  	if (doCheck)
  		hasMoreTokens();
  	int pt0 = pt;
  	boolean inQuote = (str.charAt(pt) == '"');
  	while (++pt < len) {
  		switch (str.charAt(pt)) {
  		case '"':
  			if (inQuote) {
  				if (isCmd) {
  					inQuote = false;
  					continue;
  				}
  				pt++;
    			break;
  			}
  			if (isCmd)
  				inQuote = true;
  			continue;
  		case ' ':
  			if (!isCmd && !inQuote)
  				break;
  			continue;
  		case ';':
  		case '\n':
  			if (isCmd && !inQuote)
  				break;
  			continue;
  		default:
  			continue;
  		}
  		break;
  	}
  	doCheck = true;
		return str.substring(pt0, pt);
  }

	public boolean hasMoreTokens() {
		while (++pt < len) {
			switch (str.charAt(pt)) {
			case ' ':
			case ';':
			case '\n':
				continue;
			}
			break;
		}
		doCheck = false;
		return (pt < len);
	}

	public String getRemainingScript() {
		return str.substring(pt);
	}

}
