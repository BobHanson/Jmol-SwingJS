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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import java.util.Map;
import java.util.Map.Entry;

import org.jmol.script.SV;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.PT;
import javajs.util.SB;
import javajs.util.P3;
import org.jmol.viewer.Viewer;

//import com.json.JSONArray;
//import com.json.JSONException;
//import com.json.JSONObject;
//import com.json.JSONTokener;

import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.NIOSocket;
import naga.SocketObserverAdapter;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

/*
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
 * Sent from Jmol (via outSocket): 
 * 
 * version 1:
 *   {"magic" : "JmolApp", "role" : "out"}  (socket initialization for messages TO jmol)
 *   {"magic" : "JmolApp", "role" : "in"}   (socket initialization for messages FROM jmol)
 * version 2:
 *   {"type" : "login", "source" : "Jmol"}  (socket initialization for messages TO/FROM jmol)
 * both versions:
 *   {"type" : "script", "event" : "done"}  (script completed)
 *   
 * Sent to Jmol (via inSocket):
 * 
 *   {"type" : "banner", "mode" : "ON" or "OFF" }   (set banner for kiosk)
 *   {"type" : "banner", "text" : bannerText }      (set banner for kiosk)
 *   {"type" : "command", "command" : command, "var": vname, "data":vdata}
 *       (script command request, with optional definition of a Jmol user variable prior to execution)
 *   {"type" : "content", "id" : id }            (load content request)
 *   {"type" : "move", "style" : (see below) }   (mouse command request)
 *   {"type" : "quit" }                          (shut down request)
 *   {"type" : "sync", "sync" : (see below) }    (sync command request)
 *   {"type" : "touch",                          (a raw touch event)
 *        "eventType" : eventType,
 *        "touchID"   : touchID,
 *        "iData"     : idata,
 *        "time"      : time,
 *        "x" : x, "y" : y, "z" : z }
 *    
 *   For details on the "touch" type, see org.jmol.viewer.ActionManagerMT::processEvent
 *   Content is assumed to be in a location determined by the Jmol variable
 *   nioContentPath, with %ID% being replaced by some sort of ID number of tag provided by
 *   the other half of the system. That file contains more JSON code:
 *   
 *   {"startup_script" : scriptFileName, "banner_text" : text } 
 *   
 *   An additional option "banner" : "off" turns off the title banner.
 *   The startup script must be in the same directory as the .json file, typically as a .spt file
 *   
 *   Move commands include:
 *   
 *   {"type" : "move", "style" : "rotate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "translate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "zoom", "scale" : scale }  (1.0 = 100%)
 *   {"type" : "sync", "sync" : syncText }
 *   
 *   Note that all these moves utilize the Jmol sync functionality originally intended for
 *   applets. So any valid sync command may be used with the "sync" style. These include 
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
 */

public class JsonNioService extends NIOService implements JsonNioServer {

  protected String myName;
  protected boolean halt;
  protected boolean isPaused;
  protected long latestMoveTime;
  protected int port;

  private Thread thread;
  private Thread serverThread;

  private NIOSocket inSocket;
  protected NIOSocket outSocket;
  private NIOServerSocket serverSocket;
  Viewer vwr;
  private JsonNioClient client;

  private boolean wasSpinOn;
  private String contentPath = "./%ID%.json";
  private String terminatorMessage = "NEXT_SCRIPT";
  private String resetMessage = "RESET_SCRIPT";


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

  /* (non-Javadoc)
   * @see org.openscience.jmol.app.jsonkiosk.JsonNioServer#scriptCallback(java.lang.String)
   */
  @Override
  public void scriptCallback(String msg) {
    if (msg == null)
      return;
    if (msg.startsWith("banner:")) {
      setBanner(msg.substring(7).trim(), false);
    } else if (msg.equals(terminatorMessage)) {
      sendMessage(null, "!script_terminated!", null);
    } else if (contentDisabled && msg.equals(resetMessage)) {
      client.nioRunContent(null);
    }
  }

  /* (non-Javadoc)
   * @see org.openscience.jmol.app.jsonkiosk.JsonNioServer#getPort()
   */
  @Override
  public int getPort() {
    return port;
  }
  
  /* (non-Javadoc)
   * @see org.openscience.jmol.app.jsonkiosk.JsonNioServer#send(int, java.lang.String)
   */
  @Override
  public void send(int port, String msg) {
    try {
      if (port != this.port) {
        if (inSocket != null) {
          inSocket.close();
          if (outSocket != null)
            outSocket.close();
        }
        if (thread != null) {
          thread.interrupt();
          thread = null;
        }
        startService(port, client, vwr, myName, 1);
      }
      if (msg.startsWith("Mouse:"))
        msg = "{\"type\":\"sync\", \"sync\":\""
            + msg.substring(6) + "\"}";
      sendMessage(null, msg, null);
    } catch (IOException e) {
      // ignore
    }
  }

  protected int version = 1;
  
  /* (non-Javadoc)
   * @see org.openscience.jmol.app.jsonkiosk.JsonNioServer#startService(int, org.openscience.jmol.app.jsonkiosk.JsonNioClient, org.jmol.api.JmolViewer, java.lang.String)
   */
  @Override
  public void startService(int port, JsonNioClient client,
                           Viewer jmolViewer, String name, int version)
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

    if (name != null) {
      String s = getJmolValueAsString(jmolViewer, "NIOcontentPath");
      if (s != "")
        contentPath = s;
      s = getJmolValueAsString(jmolViewer, "NIOterminatorMessage");
      if (s != "")
        terminatorMessage = s;
      s = getJmolValueAsString(jmolViewer, "NIOresetMessage");
      if (s != "")
        resetMessage = s;
      
      setEnabled();
      Logger.info("NIOcontentPath=" + contentPath);
      Logger.info("NIOterminatorMessage=" + terminatorMessage);
      Logger.info("NIOresetMessage=" + resetMessage);
      Logger.info("NIOcontentDisabled=" + contentDisabled);
      Logger.info("NIOmotionDisabled=" + motionDisabled);
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
          processMessage(packet, null);
        }

        @Override
        public void connectionBroken(NIOSocket nioSocket, Exception exception) {
          halt = true;
          Logger.info(Thread.currentThread().getName()
              + " inSocket connectionBroken");
        }

        @Override
        public void packetSent(NIOSocket arg0, Object arg1) {
          // TODO
          
        }
      });

      // outSocket is used to send JSON commands to the NIO server
      // when initialized, it identifies itself to the server as the "in" connection
      // only for version 1

      if (version == 1) {
        outSocket = openSocket("127.0.0.1", port);
        outSocket.setPacketReader(new AsciiLinePacketReader());
        outSocket.setPacketWriter(RawPacketWriter.INSTANCE);
        outSocket.listen(new SocketObserver() {

          @Override
          public void connectionOpened(NIOSocket nioSocket) {
            initialize("in", nioSocket);
          }

          @Override
          public void packetReceived(NIOSocket nioSocket, byte[] packet) {
            Logger.info("outpacketreceived");
            // not used
          }

          @Override
          public void connectionBroken(NIOSocket nioSocket, Exception exception) {
            halt = true;
            Logger.info(Thread.currentThread().getName()
                + " outSocket connectionBroken");
          }

          @Override
          public void packetSent(NIOSocket arg0, Object arg1) {
            // TODO
            
          }
        });
      }

    }
    if (port != 0) {
      thread = new Thread(new JsonNioThread(), "JsonNiosThread" + myName);
      thread.start();
    }
    if (port == 0 && contentDisabled)
      client.nioRunContent(this);
  }

  private void setEnabled() {
    contentDisabled = (getJmolValueAsString(vwr, "NIOcontentDisabled").equals("true"));
    motionDisabled = (getJmolValueAsString(vwr, "NIOmotionDisabled").equals("true"));
  } 

  public static String getJmolValueAsString(Viewer vwr, String var) {
    return (vwr == null ? "" : "" + vwr.getP(var));
  }

  protected class JsonNioThread implements Runnable {

    @Override
    public void run() {
      Logger.info(Thread.currentThread().getName() + " JsonNioSocket on " + port);
      try {
        while (!halt) {
          selectNonBlocking();
          long now = System.currentTimeMillis();
          // No commands for 5 seconds = unpause/restore Jmol
          if (isPaused && now - latestMoveTime > 5000)
            pauseScript(false);
          Thread.sleep(50);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
      close();
    }

  }

  /* (non-Javadoc)
   * @see org.openscience.jmol.app.jsonkiosk.JsonNioServer#close()
   */
  @Override
  public void close() {
    Logger.info("JsonNioService" + myName + " close");
    try {
      halt = true;
      super.close();
      if (thread != null) {
        thread.interrupt();
        thread = null;
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
    JSONObject json = new JSONObject();
    if (version == 1) {
      json.put("magic", "JmolApp");
      json.put("role", role);
    } else {
      // role will be "out"; socket will be inSocket
      json.put("source", "Jmol");
      json.put("type", "login");
    }
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
              Logger.info("JsonNioService" + myName + " server connection broken");
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
      // TODO
    }

    if (serverThread != null)
      serverThread.interrupt();
    serverThread = new Thread(new JsonNioServerThread(), "JsonNioServerThread"
        + myName);
    serverThread.start();
  }

  protected class JsonNioServerThread implements Runnable {
    @Override
    public void run() {
      Logger.info(Thread.currentThread().getName() + " JsonNioServerSocket on " + port);
      try {
        while (!halt)
          selectBlocking();
      } catch (IOException e) {
        // exit
      }
      close();
    }
  }

  private int nFast;
  private float swipeCutoff = 100;
  private int swipeCount = 2;
  private float swipeDelayMs = 3000;
  private long previousMoveTime;
  private long swipeStartTime;
  private float swipeFactor = 30;
  private boolean motionDisabled;
  private boolean contentDisabled;

  protected void processMessage(byte[] packet, NIOSocket socket) {
    try {
      String msg = new String(packet);
      Logger.info("JNIOS received " + msg);
      if (vwr == null) {
        return;
      }
      JSONObject json = new JSONObject(msg);
      if (version == 1) {
      if (socket != null && json.has("magic")
          && json.getString("magic").equals("JmolApp")
          && json.getString("role").equals("out"))
        outSocket = socket;
      } else {
        outSocket = inSocket;
      }
      if (!json.has("type"))
        return;
      processJSON(json, msg);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void processJSON(JSONObject json, String msg)
      throws Exception {
    if (json == null)
      json = new JSONObject(msg);
    int pt = ("banner...." + "command..." + "content..." + "move......"
        + "quit......" + "sync......" + "touch.....").indexOf(json
        .getString("type"));
    setEnabled();
    switch (pt) {
    case 0: // banner
      if (contentDisabled)
        break;
      setBanner((json.has("text") ? json.getString("text") : json.getString(
          "visibility").equalsIgnoreCase("off") ? null : ""), false);
      break;
    case 10: // command
      if (contentDisabled)
        break;
      if (json.containsKey("var") && json.containsKey("data"))
        vwr.g.setUserVariable(json.get("var").toString(), SV.getVariable(json.get("data")));
      sendScript(json.getString("command"));
      break;
    case 20: // content
      if (contentDisabled) {
        client.nioRunContent(this);
        break;
      }
      String id = json.getString("id");
      String path = PT.rep(contentPath, "%ID%", id).replace(
          '\\', '/');
      File f = new File(path);
      Logger.info("JsonNiosService Setting path to " + f.getAbsolutePath());
      pt = path.lastIndexOf('/');
      if (pt >= 0)
        path = path.substring(0, pt);
      else
        path = ".";
      JSONObject contentJSON = null;
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        SB sb = SB.newN(8192);
        String line;
        while ((line = br.readLine()) != null)
          sb.append(line).appendC('\n');
        br.close();
        contentJSON = new JSONObject(sb.toString());
      } catch (UnsupportedEncodingException e) {
        // should not be possible
      }

      String script = null;
      if (contentJSON.has("scripts")) {
        //TODO -- this is not implemented, because JSONObject.getJSONArray is not implemented
        List<JSONObject> scripts = contentJSON.getJSONArray("scripts");
        for (int i = scripts.size(); --i >= 0;) {
          JSONObject scriptInfo = scripts.get(i);
          if (scriptInfo.getString("startup").equals("yes")) {
            script = scriptInfo.getString("filename");
            break;
          }
        }
        if (script == null)
          throw new Exception("scripts startup:yes not found");
      } else {
        script = contentJSON.getString("startup_script");
      }
      Logger.info("JsonNiosService startup_script=" + script);
      setBanner("", false);
      sendScript("exit");
      sendScript("zap;cd \"" + path + "\";script " + script);
      setBanner(contentJSON.getString("banner").equals("off") ? null
          : contentJSON.getString("banner_text"), true);
      break;
    case 30: // move
      pt = ("rotate...." + "translate." + "zoom......").indexOf(json
          .getString("style"));
      if (motionDisabled)
        break;
      if (pt != 0 && !isPaused)
        pauseScript(true);
      long now = latestMoveTime = System.currentTimeMillis();
      switch (pt) {
      case 0: // rotate
        float dx = (float) json.getDouble("x");
        float dy = (float) json.getDouble("y");
        float dxdy = dx * dx + dy * dy;
        boolean isFast = (dxdy > swipeCutoff);
        boolean disallowSpinGesture = vwr
            .getBooleanProperty("isNavigating")
            || !vwr.getBooleanProperty("allowGestures");
        if (disallowSpinGesture || isFast
            || now - swipeStartTime > swipeDelayMs) {
          // it's been a while since the last swipe....
          // ... send rotation in all cases
          msg = null;
          if (disallowSpinGesture) {
            // just rotate
          } else if (isFast) {
            if (++nFast > swipeCount) {
              // critical number of fast motions reached
              // start spinning
              swipeStartTime = now;
              msg = "Mouse: spinXYBy " + (int) dx + " " + (int) dy + " "
                  + (Math.sqrt(dxdy) * swipeFactor / (now - previousMoveTime));
            }
          } else if (nFast > 0) {
            // slow movement detected -- turn off spinning
            // and reset the number of fast actions
            nFast = 0;
            msg = "Mouse: spinXYBy 0 0 0";
          }
          if (msg == null)
            msg = "Mouse: rotateXYBy " + dx + " " + dy;
          syncScript(msg);
        }
        previousMoveTime = now;
        break;
      case 10: // translate
        vwr.syncScript("Mouse: translateXYBy " + json.getString("x")
            + " " + json.getString("y"), "=", 0);
        break;
      case 20: // zoom
        float zoomFactor = (float) (json.getDouble("scale") / (vwr
            .tm.zmPct / 100.0f));
        syncScript("Mouse: zoomByFactor " + zoomFactor);
        break;
      }
      break;
    case 40: // quit
      halt = true;
      Logger.info("JsonNiosService quitting");
      break;
    case 50: // sync
      if (motionDisabled)
        break;
      //sync -3000;sync slave;sync 3000 '{"type":"sync","sync":"rotateZBy 30"}'
      syncScript("Mouse: " + json.getString("sync"));
      break;
    case 60: // touch
      if (motionDisabled)
        break;
      // raw touch event
      vwr.acm.processMultitouchEvent(0, json.getInt("eventType"), json
          .getInt("touchID"), json.getInt("iData"), P3.new3((float) json
          .getDouble("x"), (float) json.getDouble("y"), (float) json
          .getDouble("z")), json.getLong("time"));
      break;
    }
  }

  private void sendScript(String script) {
    Logger.info("JsonNiosService sendScript " + script);
    vwr.evalStringQuiet(script);
  }

  private void syncScript(String script) {
    Logger.info("JsonNiosService syncScript " + script);
    vwr.syncScript(script, "=", 0);
  }

  private void setBanner(String bannerText, boolean andCenter) {
    if (bannerText == null) {
      client.setBannerLabel(null);
    } else {
      if (andCenter)
        bannerText = "<center>" + bannerText + "</center>";
      client.setBannerLabel("<html>" + bannerText
          + "</html>");
    }
  }

  protected void pauseScript(boolean isPause) {
    String script;
    if (isPause) {
      // Pause the script and save the state when interaction starts
      wasSpinOn = vwr.getBooleanProperty("spinOn");
      script = "pause; save orientation 'JsonNios-save'; spin off";
      isPaused = true;
    } else {
      script = "restore orientation 'JsonNios-save' 1; resume; spin " + wasSpinOn;
      wasSpinOn = false;
    }
    isPaused = isPause;
    sendScript(script);
  }

  private void sendMessage(JSONObject json, String msg, NIOSocket socket) {
    if (socket == null && (socket = outSocket) == null)
      return;
    try {
      if (json != null) {
        msg = json.toString();
      } else if (msg != null && msg.indexOf("{") != 0) {
        json = new JSONObject();
        if (msg.equalsIgnoreCase("!script_terminated!")) {
          json.put("type", "script");
          json.put("event", "done");
        } else {
          json.put("type", "command");
          json.put("command", msg);
        }
        msg = json.toString();
      }
      msg += "\r\n";
      Logger.info(Thread.currentThread().getName() + " sending " + msg + " to " + socket);
      socket.write(msg.getBytes("UTF-8"));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  class JSONObject extends Hashtable<String,Object>{

    public JSONObject() {
    }

    @SuppressWarnings("unchecked")
    JSONObject(String msg) throws Exception {
      SV o = vwr.evaluateExpressionAsVariable(msg);
      if (!(o.value instanceof Map<?,?>)) 
        throw new Exception("invalid JSON: " + msg);
      putAll((Map<String, Object>) o.value);
    }

    public JSONObject(Map<String, Object> map) {
      putAll(map);
    }

    boolean has(String key) {
      return containsKey(key);
    }

    String getString(String key) throws Exception {
      return containsKey(key) ? get(key).toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    public List<JSONObject> getJSONArray(String key) throws Exception {
      if (!has(key))
        throw new Exception("JSON key not found:" + key);
      List<JSONObject> list = new  ArrayList<JSONObject>();
      List<SV> svlist = ((SV) get(key)).getList();
      for (int i = 0; i < svlist.size(); i++)
        list.add(new JSONObject((Map<String, Object>)(svlist.get(i).value)));
      return list;
    }

    public Object get(String key) {
      Object o = super.get(key);
      return (o instanceof SV ? SV.oValue(o) : o);
    }
    
    public long getLong(String key) throws Exception {
      if (!has(key))
        throw new Exception("JSON key not found:" + key);
      return Long.parseLong(get(key).toString());
    }

    public int getInt(String key) throws Exception {
      if (!has(key))
        throw new Exception("JSON key not found:" + key);
      return Integer.parseInt(get(key).toString());
    }

    public double getDouble(String key) throws Exception {
      if (!has(key))
        throw new Exception("JSON key not found:" + key);
      return Double.parseDouble(get(key).toString());
    }

    @Override
    public synchronized String toString() {
      SB sb = new SB();
      sb.append("{");
      String sep = "";
      for (Entry<String, Object>e : entrySet()) {
        sb.append(sep).append(PT.esc(e.getKey())).append(":").append(Escape.e(e.getValue()));
        sep = ",";
      }      
      return sb.append("}").toString(); 
    }
    
  }
}
