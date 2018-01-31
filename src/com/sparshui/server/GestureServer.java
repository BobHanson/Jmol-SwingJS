package com.sparshui.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import org.jmol.api.JmolGestureServerInterface;
import javajs.util.Lst;
import org.jmol.util.Logger;

import com.sparshui.common.ConnectionType;
import com.sparshui.common.Location;
import com.sparshui.common.NetworkConfiguration;
import com.sparshui.common.messages.events.EventType;

/**
 * The main gesture server class. In the Jmol version, this server is created by
 * org.jmol.multitouch.sparshui.JmolSparshClientAdapter so there is no main
 * method.
 * 
 * adapted by Bob Hanson for Jmol 11/29/2009
 * 
 * @author Tony Ross
 * 
 */

public class GestureServer implements Runnable, JmolGestureServerInterface {
  GestureServer clientServer, deviceServer, main;
  Thread clientThread, deviceThread;
  ServerSocket _clientSocket;
  ServerSocket _deviceSocket;
  ServerSocket _mySocket;
  private Lst<ClientConnection> _clients = new Lst<ClientConnection>();
  private int port;

  InputDeviceConnection ic = null;

  public GestureServer() {
    // for reflection
    Logger.info(this + " constructed");
  }

  @Override
  public void finalize() {
    if (Logger.debugging)
      Logger.debug(this + " finalized");
  }
  
  public GestureServer(int port, GestureServer main) {
    this.port = port;
    this.main = main;
  }
  
  /**
   * two independent threads -- one accepting multiple clients, one not.
   */
  @Override
  public void startGestureServer() {
    clientServer = new GestureServer(NetworkConfiguration.CLIENT_PORT, this);
    clientThread = new Thread(clientServer);
    clientThread.setName("Jmol SparshUI Client GestureServer on port "
        + NetworkConfiguration.CLIENT_PORT);
    clientThread.start();
    deviceServer = new GestureServer(NetworkConfiguration.DEVICE_PORT, this);
    deviceThread = new Thread(deviceServer);
    deviceThread.setName("Jmol SparshUI Device GestureServer on port "
        + NetworkConfiguration.DEVICE_PORT);
    deviceThread.start();
  }

  @Override
  public void dispose() {
    try {
      _clientSocket.close();
    } catch (Exception e) {
      // ignore
    }
    try {
      _deviceSocket.close();
    } catch (Exception e) {
      // ignore
    }
    try {
      clientThread.interrupt();
    } catch (Exception e) {
      // ignore
    }
    try {
      deviceThread.interrupt();
    } catch (Exception e) {
      // ignore
    }
    _clientSocket = null;
    clientThread = null;
    _deviceSocket = null;
    deviceThread = null;
    clientServer = null;
    deviceServer = null;
  }

  /**
   * Start accepting connections.
   */
  @Override
  public void run() {
    try {
      openSocket();
      acceptConnections();
    } catch (Exception e) {
      Logger.info("[GestureServer] connection unavailable");
    }
  }

  /**
	 * 
	 */
  private void openSocket() {
    try {
      if (port == NetworkConfiguration.CLIENT_PORT)
        _mySocket = main._clientSocket = new ServerSocket(port);
      else
        _mySocket = main._deviceSocket = new ServerSocket(port);
      Logger.info("[GestureServer] Socket Open: " + port);
      main.myState = JmolGestureServerInterface.OK;  
    } catch (IOException e) {
      Logger.error("[GestureServer] Failed to open a server socket.");
      e.printStackTrace();
      main.myState = 0;  
    }
  }

  /**
	 * 
	 */
  private void acceptConnections() {
    while (!_mySocket.isClosed()) {
      try {
        if (port == NetworkConfiguration.DEVICE_PORT) {
          Logger.info("[GestureServer] Accepting device connections");
          acceptConnection(_mySocket.accept());
          return; // only one of these
        }
        Logger.info("[GestureServer] Accepting client connections");
        acceptConnection(_mySocket.accept());
      } catch (IOException e) {
        Logger.error("[GestureServer] Failed to establish connection on port " + port);
        e.printStackTrace();
      }
    }
    Logger.info("[GestureServer] Socket Closed on port " + port);
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptConnection(Socket socket) throws IOException {
    // no remote access!
    byte[] add = socket.getInetAddress().getAddress();
    if (add[0] != 127 || add[1] != 0 || add[2] != 0 || add[3] != 1)
      return;
    int type = socket.getInputStream().read();
    if (type == ConnectionType.CLIENT) {
      Logger.info("[GestureServer] client connection established on port " + port);
      acceptClientConnection(socket);
    } else if (type == ConnectionType.INPUT_DEVICE) {
      Logger.info("[GestureServer] device connection established on port " + port);
      acceptInputDeviceConnection(socket);
    }
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptClientConnection(Socket socket) throws IOException {
    Logger.info("[GestureServer] Client connection accepted");
    ClientConnection cc = new ClientConnection(socket);
    main._clients.addLast(cc);
    if (main.ic == null) {
      cc.processError(EventType.DRIVER_NONE);
    } else {
      main.myState |= JmolGestureServerInterface.HAS_CLIENT;  
    }
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptInputDeviceConnection(Socket socket) throws IOException {
    Logger.info("[GestureServer] Input device connection accepted");
    main.ic = new InputDeviceConnection(this, socket);
    main.myState |= JmolGestureServerInterface.HAS_DRIVER;
  }

  /**
   * 
   * notify clients that we lost contact with the input device
   * 
   */
  void notifyInputLost() {
    Logger
        .error("[GestureServer] sending clients message that input device was lost.");
    main.ic = null;
    main.myState &= ~JmolGestureServerInterface.HAS_DRIVER;
    processBirth(null);
  }

  /**
   * This method was tucked into InputDeviceConnection but really has to do more
   * with server-to-client interaction, so I moved it here. BH
   * 
   * @param inputDeviceTouchPoints
   *          container for this input device's touchPoints
   * @param id
   * @param location
   * @param time
   * @param state
   * @return whether a client has claimed this touchPoint;
   */
  boolean processTouchPoint(Map<Integer, TouchPoint> inputDeviceTouchPoints, int id,
                            Location location, long time, int state) {
    if (Logger.debugging) {
      Logger.debug("[GestureServer] processTouchPoint id=" + id + " state=" + state + " " + location
          + " " + time);
    }
    Integer iid = Integer.valueOf(id);
    if (inputDeviceTouchPoints.containsKey(iid)) {
      TouchPoint touchPoint = inputDeviceTouchPoints.get(iid);
      if (!touchPoint.isClaimed())
        return false;
      if (Logger.debugging)
        Logger.debug("[GestureServer] OK");
      synchronized (touchPoint) {
        touchPoint.update(location, time, state);
      }
      return true;
    }
    TouchPoint touchPoint = new TouchPoint(id, location, time);
    inputDeviceTouchPoints.put(iid, touchPoint);
    return processBirth(touchPoint);
  }

  /**
   * Process a touch point birth by getting the groupID and gestures for the
   * touch point. NULL touchpoint means we have a driver failure
   * 
   * @param touchPoint
   *          The new touch point.
   * @return whether a client has claimed this touchPoint as its own.
   * 
   */
  private boolean processBirth(TouchPoint touchPoint) {
    Lst<ClientConnection> clients_to_remove = null;
    boolean isClaimed = false;
    for (int i = 0; i < main._clients.size(); i++) {
      ClientConnection client = main._clients.get(i);
      // Return if the client claims the touch point
      try {
        if (touchPoint == null)
          client.processError(EventType.DRIVER_NONE);
        else
          isClaimed = client.processBirth(touchPoint);
        if (isClaimed)
          break;
      } catch (IOException e) {
        // This occurs if there is a communication error
        // with the client. In this case, we will want
        // to remove the client.
        if (clients_to_remove == null)
          clients_to_remove = new Lst<ClientConnection>();
        clients_to_remove.addLast(client);
      }
    }
    if (clients_to_remove != null)
      for (int i = 0; i < clients_to_remove.size(); i++) {
        main._clients.removeObj(clients_to_remove.get(i));
        Logger.info("[GestureServer] Client Disconnected");
      }
    return isClaimed;
  }

  private int myState;
  @Override
  public int getState() {
    return myState;
  }

}
