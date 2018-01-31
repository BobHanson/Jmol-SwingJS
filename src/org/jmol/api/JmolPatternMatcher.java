package org.jmol.api;

import java.util.regex.Pattern;

public interface JmolPatternMatcher {

  Pattern compile(String sFind, boolean isCaseInsensitive);

}
