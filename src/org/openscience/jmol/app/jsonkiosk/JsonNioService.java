/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-19 08:25:14 -0500 (Wed, 19 May 2010) $
 * $Revision: 13133 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openscience.jmol.app.jsonkiosk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.jmol.util.JSONWriter;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import javajs.util.JSJSONParser;
import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

/**
 * A class for interacting with Jmol over local sockets.
 * 
 * See also org.molecularplayground.MPJmolApp.java for how this works.
 * Note that this service does not require MPJmolApp -- it is a package
 * in the standard Jmol app. 
 * 
 * Listens over a port on the local host for instructions on what to display.
 * Instructions come in over the port as JSON strings.
 * 
 * This class uses the Naga asynchronous socket network I/O package (NIO), the
 * JSON.org JSON package and Jmol.
 * 
 * http://code.google.com/p/naga/
 * 
 * Initial versions of this code, including the JSON-base protocol were created
 * by Adam Williams, U-Mass Amherst see http://MolecularPlayground.org and
 * org.openscience.jmol.molecularplayground.MPJmolApp.java
 * 
 * <code>
 * 
 * Sequence of events:
 * 
 * 1) Jmol initiates server listening on a port using the JmolScript 
 *    command with an arbitrary negative port number.
 *    (-30000 used here just for an example):
 * 
 *    sync -30000
 * 
 * This can be done also through the command line using
 * 
 *    jmol -P -30000
 * 
 * or 
 * 
 *    jmol --port -30000
 * 
 * Jmol will respond to System.out:
 * 
 *    JsonNioServerThread-JmolNioServer JsonNioServerSocket on 30000
 *    
 * 
 * 2) Client sends handshake to port 30000. As with all communications to this service, 
 *    there must be no new-line characters (\n) ANYWHERE in the JSON being sent EXCEPT 
 *    for a single message terminator:
 * 
 * 
 *   {"magic": "JmolApp", "role": "out"}\n
 * 
 * where "out" here indicates that this socket is for Jmol (reply) output.
 *   
 * Jmol will reply with the 30-byte response: 
 *   
 *   {"type":"reply","reply":"OK"}\n
 *   
 * (The client may see only 29 bytes, as it may or may not strip the final \n.)
 *
 * Optionally, the client may also indicate a specified port for Jmol input. 
 * But typically this is just the currently active port. 
 *   
 *   {"magic": "JmolApp", "role": "in"}\n 
 *   
 *   Jmol will reply with 
 *   
 *   {"type": "reply", "reply": "OK"}\n;
 *
 *  
 * 3) Client sequentially sends Jmol script commands over the "in" socket:
 * 
 *   {"type": "command", "command": command}
 * 
 * where required command is some JSON-escaped string such as "rotate x 30" or "load $caffeine". 
 * For example:
 * 
 *   {"type": "command", "command": "var atoms = {_C or _H};select atoms"}\n
 *  
 * 
 * For the rest of this discussion, we will use the Jmol command that communicates with another Jmol instance
 * rather than this JSON context:
 * 
 *   SYNC 30000 "var atoms = {_C or _H};select atoms"
 *   
 * in this case.
 * 
 * 
 * 4) Jmol throughout this process is sending replies that come 
 *    from the Jmol Statuslistener class. For example:
 *    
 *  {"type":"reply","reply":"SCRIPT:script 8 started"}
 *  {"type":"reply","reply":"SCRIPT:Script completed"}
 *  {"type":"reply","reply":"SCRIPT:Jmol script terminated"}
 * 
 * Note that your client will be subscribed to many of the Jmol status callbacks 
 * (see org.openscience.jmol.app.jmolpanel.StatusListener), including:
 * 
 *    LOADSTRUCT
 *    ANIMFRAME
 *    SCRIPT
 *    ECHO
 *    PICK
 *    CLICK
 *    RESIZE
 *    ERROR
 *    MINIMIZATION
 *    STRUCTUREMODIFIED
 * 
 * All scripts and callback messages run in order but asynchronously in Jmol. You do not need
 * to wait for one script to be finished before issuing another; there is a queue that handles that.
 * If you want to be sure that a particular script has been run, simply add a MESSAGE command 
 * as its last part:
 *    
 *    sync 30000 "background blue;message The background is blue now"
 *  
 *  and it will appear as a SCRIPT callback:
 *  
 *     {"type":"reply","reply":"SCRIPT:The background is blue now"}
 *  
 *  after which you can handle that event appropriately.
 *  
 *  The SCRIPT callback can be particularly useful to monitor:
 *
 *   sync 30000 "backgrund blue"
 *   
 *  {"type":"reply","reply":"SCRIPT:script compiler ERROR: command expected\n----\n          >>>> backgrund blue <<<<"}
 *
 *  Note that the ERROR callback does not fire for compile errors, 
 *  only for errors found while running a parsed script:
 *  
 *  {"type":"reply","reply":"ERROR:ScriptException"}
 *   
 *  Note that all of these messages are "thumbnails" in the sense that they are just a message string. 
 *  You can subscribe to a full report for any of these callbacks using 'SYNC:ON' for the 
 *  callback function:
 *  
 *     set XxxxxCallback SYNC:ON
 *     
 *  For example, issuing
 *  
 *     sync 30000 "load $caffeine"
 *     
 *  gives the simple reply:
 *  
 *     {"type":"reply","reply":"LOADSTRUCT:https://cactus.nci.nih.gov/chemical/structure/caffeine/file?format=sdf&get3d=true"}
 *
 *  but after
 *  
 *     sync 30000 "set LoadStructCallback 'SYNC:ON'
 *  
 *  we get additional details, and array of data with nine elements:
 *  
 *  {"type":"reply","reply":["LOADSTRUCT",
 *                           "https://cactus.nci.nih.gov/chemical/structure/caffeine/file?format=sdf&get3d=true",
 *                           "file?format=sdf&get3d=true",
 *                           "C8H10N4O2", null, 3, "1.1", "1.1", null]}
 *  
 *  Exact specifications for these callbacks are not well documented. 
 *  See org.jmol.viewer.StatusManager code for details.
 *  
 *  Remove the callback listener using
 *  
 *     set XxxxxCallback SYNC:OFF
 *  
 *  Note that unlike Java, you get only one SYNC callback; this is not an array of listeners.
 *  
 *  
 * 5) Shutdown can be requested by sending
 *   
 *   {"type": "quit"}\n
 *  
 *  or by issuing the command
 *  
 *    sync 30000 "exitjmol"
 *    
 *   
 * Note that the Molecular Playgournd implemented an extensive set of gesture-handling methods 
 * that are also available via this interface. Many of these methods utilize the JmolViewer.syncScript() 
 * method, which directly manipulates the display as though someone were using a mouse.
 *   
 *   {"type" : "move", "style" : "rotate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "translate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "zoom", "scale" : scale }  (1.0 = 100%)
 *   {"type" : "sync", "sync" : syncText }
 *   {"type" : "touch", 
 *        "eventType" : eventType,
 *        "touchID"   : touchID,
 *        "iData"     : idata,
 *        "time"      : time, "x" : x, "y" : y, "z" : z }
 *    
 *   For details on the "touch" type, see org.jmol.viewer.ActionManagerMT::processEvent
 *   
 *   Note that all of the move and sync commands utilize the Jmol sync functionality originally 
 *   intended for applets. So any valid sync command may be used with the "sync" style. These include 
 *   essentially all the actions that a user can make with a mouse, including the
 *   following, where the notation <....> represents a number of a given type. These
 *   events interrupt any currently running script, just as with typical mouse actions.
 *   
 *   "centerAt <int:x> <int:y> <float:ptx> <float:pty> <float:ptz>"
 *      -- set {ptx,pty,ptz} at screen (x,y)
 *   "rotateMolecule <float:deltaX> <float:deltaY>"
 *   "rotateXYBy <float:deltaX> <float:deltaY>"
 *   "rotateZBy <int:degrees>"
 *   "rotateZBy <int:degrees> <int:x> <int:y>" (with center reset)
 *   "rotateArcBall <int:x> <int:y> <float:factor>"
 *   "spinXYBy <int:x> <int:y> <float:speed>"
 *      -- a "flick" gesture
 *   "translateXYBy <float:deltaX, float:deltaY>"
 *   "zoomBy <int:pixels>"
 *   "zoomByFactor <float:factor>"
 *   "zoomByFactor <float:factor> <int:x> <int:y>" (with center reset)
 * 
 * 
 * </code>
 */
public class JsonNioService extends NIOService implements JsonNioServer {

  public static final int VERSION = 1;

  protected String myName;
  protected boolean halt;
  protected int port;

  private Thread clientThread;
  private Thread serverThread;

  private NIOSocket inSocket;
  protected NIOSocket outSocket;
  private NIOServerSocket serverSocket;
  Viewer vwr;
  protected JsonNioClient client;

  /*
   * When Jmol gets the terminator message, we tell the Hub that we're done
   * with the script and need the name of the next one to load.
   */

  public JsonNioService() throws IOException {
    /* called by reflection so as to allow interface that may
     * not include Naga NioService
     */
    super();
  }

  @Override
  public int getPort() {
    return port;
  }

  protected int version = 2;

  @Override
  public void startService(int port, JsonNioClient client, Viewer jmolViewer,
                           String name, int version)
      throws IOException {
    this.version = version;
    this.port = Math.abs(port);
    this.client = client;
    this.vwr = jmolViewer;
    myName = (name == null ? "" : name);

    if (port < 0) {
      startServerService();
      return;
    }

    Logger.info("JsonNioService" + myName + " using port " + port);

    // inSocket listens for JSON commands from the NIO server
    // when initialized, it identifies itself to the server as the "out" connection

    if (port != 0) {
      inSocket = openSocket("127.0.0.1", port);
      inSocket.setPacketReader(new AsciiLinePacketReader());
      inSocket.setPacketWriter(RawPacketWriter.INSTANCE);
      inSocket.listen(new SocketObserver() {

        @Override
        public void connectionOpened(NIOSocket nioSocket) {
          initialize("out", nioSocket);
        }

        @Override
        public void packetReceived(NIOSocket socket, byte[] packet) {
          processMessage(packet, socket);
        }

        @Override
        public void connectionBroken(NIOSocket nioSocket, Exception exception) {
          halt = true;
          Logger.info(
              Thread.currentThread().getName() + " inSocket connectionBroken");
        }

        @Override
        public void packetSent(NIOSocket arg0, Object arg1) {
        }
      });

      outSocket = inSocket;
    }
    if (port != 0) {
      clientThread = new Thread(new JsonNioClientThread(), "JsonNiosThread" + myName);
      clientThread.start();
    }
  }

  @Override
  public boolean hasOuputSocket() {
    return (outSocket != null);
  }

  /**
   * send the message - not for replies.
   */
  @Override
  public void sendToJmol(int port, String msg) {
    try {
      if (port == OUTSOCKET) {
        return;
      }

      if (port != this.port) {
        if (inSocket != null && inSocket != outSocket) {
          inSocket.close();
        }
        if (outSocket != null)
          outSocket.close();
        if (clientThread != null) {
          clientThread.interrupt();
          clientThread = null;
        }
        startService(port, client, vwr, myName + ".", JsonNioService.VERSION);
      }
      sendMessage(null, msg, outSocket);
    } catch (IOException e) {
      // ignore
    }
  }

  protected void processMessage(byte[] packet, NIOSocket socket) {
    try {
      Logger.info("JNIOS received " + packet.length + " bytes from socket "
          + socket.getPort());
      if (packet.length < 100) {
        Map<String, Object> json = toMap(packet);
        if ("JmolApp".equals(json.get("magic"))) {
          switch (getString(json, "role")) {
          case "out":
            outSocket = socket;
            if (inSocket == null)
              inSocket = outSocket;
            reply(OUTSOCKET, "OK");
            break;
          case "in":
            inSocket = socket;
            if (outSocket == null)
              outSocket = inSocket;
            reply(OUTSOCKET, "OK");
            break;
          }

        } else {
          switch (getString(json, "type")) {
          case "quit":
            halt = true;
            reply(OUTSOCKET, "JsonNioService" + myName + " closing");
            Logger.info("JsonNiosService quitting");
            SwingUtilities.invokeLater(new Runnable() {

              @Override
              public void run() {
                System.exit(0);
              }
            });
            break;
          }
        }
      }
      client.processNioMessage(packet);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  protected void sendMessage(Map<String, Object> map, String msg,
                           NIOSocket socket) {
    if (socket == null && (socket = outSocket) == null
        || map == null && msg == null)
      return;
    byte[] out;
    try {
      if (map != null) {
        out = toJSONBytes(map);
      } else if (msg.indexOf("{") != 0) {
        map = new LinkedHashMap<>();
        map.put("type", "command");
        map.put("command", msg);
        out = toJSONBytes(map);
      } else {
        out = clean(msg.getBytes("UTF-8"));
      }
      sendBytes(out, socket);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void reply(int port, final Object data) {
    if (port != OUTSOCKET || outSocket == null)
      return;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "reply");
        map.put("reply", data);
        sendMessage(map, null, outSocket);
      }
    });
  }

  private void sendBytes(byte[] bytes, NIOSocket socket) {
    if (socket == null && (socket = outSocket) == null || bytes == null)
      return;
    Logger.info("JsonNioService sending " + bytes.length + " bytes to port "
        + socket.getPort());
    if (Logger.debugging)
      Logger.debug(new String(bytes));
    try {
      socket.write(bytes);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * remove all new-line characters, and terminate this message with a single
   * \n.
   * 
   * @param out
   * @return cleaned bytes
   */
  private byte[] clean(byte[] out) {
    int pt = -1;
    int n = out.length;
    for (int i = n; --i >= 0;) {
      if (out[i] == '\n') {
        if (pt < 0)
          pt = i;
        out[i] = ' ';
      }
    }
    if (out[n - 1] > ' ') {
      if (pt >= 0) {
        for (int i = pt; ++i < n;) {
          out[i - 1] = out[i];
        }
      } else {
        byte[] buf = new byte[n + 1];
        System.arraycopy(out, 0, buf, 0, n);
        out = buf;
      }
    }
    out[n - 1] = '\n';
    return out;
  }
  
  @Override
  public void close() {
    Logger.info("JsonNioService" + myName + " close");
    try {
      halt = true;
      super.close();
      if (clientThread != null) {
        clientThread.interrupt();
        clientThread = null;
      }
      if (serverThread != null) {
        serverThread.interrupt();
        serverThread = null;
      }
      if (inSocket != null)
        inSocket.close();
      if (outSocket != null)
        outSocket.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    if (client != null)
      client.nioClosed(this);
  }

  protected void initialize(String role, NIOSocket nioSocket) {
    Logger.info("JsonNioService" + myName + " initialize " + role);
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("magic", "JmolApp");
    json.put("role", role);
    json.put("from", hashCode() + myName);
    sendMessage(json, null, nioSocket);
  }

  private void startServerService() {
    try {
      serverSocket = openServerSocket(port);
      serverSocket.listen(new ServerSocketObserverAdapter() {

        @Override
        public void newConnection(NIOSocket nioSocket) {
          Logger.info(Thread.currentThread().getName()
              + " Received connection: " + nioSocket);

          nioSocket.setPacketReader(new AsciiLinePacketReader());
          nioSocket.setPacketWriter(RawPacketWriter.INSTANCE);
          nioSocket.listen(new SocketObserverAdapter() {

            @Override
            public void packetReceived(NIOSocket socket, byte[] packet) {
              processMessage(packet, socket);
            }

            @Override
            public void connectionOpened(NIOSocket arg0) {
              //
            }

            @Override
            public void connectionBroken(NIOSocket socket, Exception arg1) {
              Logger.info(
                  "JsonNioService" + myName + " server connection broken");
              if (socket == outSocket)
                outSocket = null;
            }

          });
        }
      });

      serverSocket.setConnectionAcceptor(new ConnectionAcceptor() {
        @Override
        public boolean acceptConnection(InetSocketAddress arg0) {
          boolean isOK = arg0.getAddress().isLoopbackAddress();
          return isOK;
        }
      });

    } catch (IOException e) {

    }

    if (serverThread != null)
      serverThread.interrupt();
    serverThread = new Thread(new JsonNioServerThread(),
        "JsonNioServerThread" + myName);
    serverThread.start();
  }

  protected class JsonNioServerThread implements Runnable {
    @Override
    public void run() {
      try {
        while (!halt) {
          selectBlocking();
          Logger.info(
              Thread.currentThread().getName() + " JsonNioServerSocket on " + port);
        }
      } catch (IOException e) {
        // exit
      }
      close();
    }
  }

  protected class JsonNioClientThread implements Runnable {

    @Override
    public void run() {
      Logger
          .info(Thread.currentThread().getName() + " JsonNioSocket on " + port);
      try {
        while (!halt) {
          selectNonBlocking();
          client.serverCycle();
//          System.out.println("ClientThread active");
          Thread.sleep(50);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
      close();
    }

  }
  
  
  // server utils
  
  /**
   * Guaranteed to create a clean no-whitespace JSON stream terminated by a
   * single \n.
   * 
   * @param map
   * @return clean bytes
   */
  public static byte[] toJSONBytes(Map<String, Object> map) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    JSONWriter writer = new JSONWriter();
    writer.setWhiteSpace(false);
    writer.setStream(stream);
    writer.writeObject(map);
    writer.closeStream();
    return stream.toByteArray();
  }

 // client aids
  
  public static Map<String, Object> toMap(byte[] packet) {
    return new JSJSONParser().parseMap(new String(packet), false);
  }

  public static String getString(Map<String, Object> map, String key) {
    Object val = map.get(key);
    return (val == null ? "" : val.toString());
  }

  public static long getLong(Map<String, Object> map, String key) throws Exception {
    if (!map.containsKey(key))
      throw new Exception("JSON key not found:" + key);
    return Long.parseLong(map.get(key).toString());
  }

  public static int getInt(Map<String, Object> map, String key) throws Exception {
    if (!map.containsKey(key))
      throw new Exception("JSON key not found:" + key);
    return Integer.parseInt(map.get(key).toString());
  }

  public static double getDouble(Map<String, Object> map, String key)
      throws Exception {
    if (!map.containsKey(key))
      throw new Exception("JSON key not found:" + key);
    return Double.parseDouble(map.get(key).toString());
  }



  
  

}
