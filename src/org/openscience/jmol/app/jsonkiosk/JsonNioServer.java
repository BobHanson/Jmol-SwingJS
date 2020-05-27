package org.openscience.jmol.app.jsonkiosk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.jmol.util.JSONWriter;
import org.jmol.viewer.Viewer;

public interface JsonNioServer {

  public static final int INSOCKET = 1;
  public static final int OUTSOCKET = 2;


  public int getPort();

  public boolean hasOuputSocket();
 

  /**
   * from JmolPanel and SYNC command
   * 
   * @param port
   * @param msg
   */
  public  void sendToJmol(int port, String msg);

  public  void startService(int port, JsonNioClient client,
                                    Viewer jmolViewer, String name, int version)
      throws IOException;

  public  void close();

  public void reply(int port, Object data);

}
