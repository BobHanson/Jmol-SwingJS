package com.sparshui.server;

import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.sparshui.common.Location;
import com.sparshui.common.NetworkConfiguration;
import com.sparshui.common.TouchState;

/**
 * Represents a connection to the input device.
 * 
 * @author Tony Ross
 *
 */
public class InputDeviceConnection implements Runnable {
	
	/**
	 * 
	 */
	private GestureServer _gestureServer;
	
	/**
	 * 
	 */
	private Socket _socket;
	
	/**
	 * 
	 */
	private DataInputStream _in;
  //private DataOutputStream _out;
  
	
	/**
	 * 
	 */
	private Map<Integer, TouchPoint>  _touchPoints;

	private List<Integer> _flaggedids;

	/**
	 * Create a new input device connection with the given
	 * gesture server and socket.
	 * 
	 * @param gestureServer
	 * 		The gesture server.
	 * @param socket 
	 * @throws IOException 
	 * 		If there is a communication error.
	 */
	public InputDeviceConnection(GestureServer gestureServer, Socket socket) throws IOException {
		_gestureServer = gestureServer;
		_socket = socket;
    _in = new DataInputStream(socket.getInputStream());
    //_out = new DataOutputStream(socket.getOutputStream());
		_touchPoints = new Hashtable<Integer, TouchPoint>();
		_flaggedids = new ArrayList<Integer>();
		startListening();
	}
	
	/**
	 * 
	 */
	private void removeDeadTouchPoints() {
		for (int i = 0; i < _flaggedids.size(); i++) {
		  Integer id = _flaggedids.get(i);
		_touchPoints.remove(id);
		}
		_flaggedids.clear();
	}
	
	/**
	 * 
	 * @param id
	 */
	private void flagTouchPointForRemoval(int id) {
		_flaggedids.add(Integer.valueOf(id));
	}
	
	/**
	 * 
	 */
	private void receiveData() {
		try {
			while(!_socket.isInputShutdown()) {
				/*boolean doConsume = */ readTouchPoints();
				//_out.write((byte) (doConsume ? 1 : 0)); 
			}
		} catch (IOException e) {
			System.out.println("[InputDeviceConnection] InputDevice Disconnected");
			_gestureServer.notifyInputLost();
		}
	}
	
	/**
   * -n where n is the number of points to be transmitted. 0 here closes the
   * socket
   * 
   * @return doConsume
   * @throws IOException
   */
  private boolean readTouchPoints() throws IOException {
    // With Count
    int count = _in.readInt();
    if (count == 0) {
      _in.close();
      return false;
    }
    int touchPointDataLength;

    if (count < 0) {
      count = -count;
      touchPointDataLength = _in.readInt();
    } else {
      // original SparshUI style
      touchPointDataLength = 13;
    }
    boolean doConsume = false;
    //System.out.println("Reading '"+count+"' Input Events.");
    for (int i = 0; i < count; i++)
      doConsume |= readTouchPoint(touchPointDataLength);
    removeDeadTouchPoints();
    return doConsume;
  }
	
  /**
   * Modified for Jmol to also transmit the time as a long
   * so here we have 21 bytes. If more bytes are sent, 
   * then we ignore those.
   * 
   * @param len 
   * @return doConsume
   * @throws IOException
   */
  private boolean readTouchPoint(int len) throws IOException {
    int id = _in.readInt();
    float x = _in.readFloat();
    float y = _in.readFloat();
    int state = _in.readByte();
    long time = (len >= 21 ? _in.readLong() : System.currentTimeMillis());
    if (len > 21) 
      _in.read(new byte[len - 21]);
    Location location = new Location(x, y);
    boolean doConsume = _gestureServer.processTouchPoint(_touchPoints, id,
        location, time, state);
    if (state == TouchState.DEATH)
      flagTouchPointForRemoval(id);
    return doConsume;
  }
  
	/**
	 * 
	 */
	private void startListening() {
		Thread thread = new Thread(this);
		thread.setName("SparshUI Server->InputDeviceConnection on port " + NetworkConfiguration.DEVICE_PORT);
		thread.start();
	}

	/**
	 * Begin receiving data from the input device.
	 */
	//@override
	@Override
  public void run() {
		receiveData();
	}
	
}
