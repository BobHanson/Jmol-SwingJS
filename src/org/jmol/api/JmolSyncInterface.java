package org.jmol.api;

import java.util.Map;

public interface JmolSyncInterface {

  public abstract void syncScript(String script);
  public abstract void register(String id, JmolSyncInterface jsi);
  public Map<String, Object> getJSpecViewProperty(String key);

}
