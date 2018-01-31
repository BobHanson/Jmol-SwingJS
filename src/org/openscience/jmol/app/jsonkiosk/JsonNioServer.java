package org.openscience.jmol.app.jsonkiosk;

import java.io.IOException;

import org.jmol.viewer.Viewer;

public interface JsonNioServer {

  /**
   * from StatusManager
   * 
   * @param msg
   */
  public abstract void scriptCallback(String msg);

  public abstract int getPort();

  /**
   * from JmolPanel and SYNC command
   * 
   * @param port
   * @param msg
   */
  public abstract void send(int port, String msg);

  public abstract void startService(int port, JsonNioClient client,
                                    Viewer jmolViewer, String name, int version)
      throws IOException;

  public abstract void close();

}
