package com.sparshui.client;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.sparshui.GestureType;
import com.sparshui.common.ClientProtocol;
import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.messages.events.DragEvent;
import com.sparshui.common.messages.events.EventType;
import com.sparshui.common.messages.events.FlickEvent;
import com.sparshui.common.messages.events.RelativeDragEvent;
import com.sparshui.common.messages.events.RotateEvent;
import com.sparshui.common.messages.events.SpinEvent;
import com.sparshui.common.messages.events.TouchEvent;
import com.sparshui.common.messages.events.ZoomEvent;
import com.sparshui.common.utils.Converter;

/**
 * ClientToServerProtocol implements the Client side protocol. It is the
 * interface between Client.java and the lower level socket calls to communicate
 * with the Gesture Server.
 * 
 * adapted by Bob Hanson for Jmol 11/29/2009
 *
 * @author Jay Roltgen
 * 
 */
public class ClientToServerProtocol extends ClientProtocol {

	/**
	 * Constructor calls ClientProtocol's constructor and sets up data input and
	 * output streams for the specified socket.
	 * 
	 * @param socket
	 *            The socket that is connected to the Gesture Server.
	 * @throws IOException
	 *             If there is a communication error.
	 */
	public ClientToServerProtocol(Socket socket) throws IOException {
		super(socket);
	}

	/**
	 * Processes a request from the Gesture Server
	 * 
	 * @param client
	 *            The Client object that the server wants to communicate with.
	 * @return true for success, false for failure
	 */
	public boolean processRequest(SparshClient client) {
		try {
			// Read the message type
		  // SparshUI client waits here for server message:
		  // byte TYPE
		  // int LENGTH
		  // byte[] DATA
		  
	    int type = _in.readByte();
			int length = _in.readInt();
			byte[] data = new byte[length];
			if (length > 0)
  			_in.readFully(data);

			// Dispatch to appropriate handler method
			switch (type) {
			case MessageType.EVENT:
				handleEvents(client, data);
				break;
			case MessageType.GET_GROUP_ID:
				handleGetGroupID(client, data);
				break;
			case MessageType.GET_ALLOWED_GESTURES:
				handleGetAllowedGestures(client, data);
				break;
			}
		} catch (IOException e) {
			System.err
					.println("[Client Protocol] GestureServer Connection Lost");
			handleEvents(client, null);
			return false;
		}
		return true;
	}

	/**
	 * Handle the EVENT message by sending it on to the client.
	 * 
	 * @param client
	 *            The client to send events to.
	 * @param data
	 *            The data associated with this event.
	 */
	private void handleEvents(SparshClient client, byte[] data) {
	  
	  if (data == null) {	    // connection lost
      client.processEvent(EventType.SERVICE_LOST, null);
      return;
	  }
		// If there are no events, return immediately.
		if (data.length < 1) {
			return;
		}
		// Get the group ID - the first four bytes
		int groupID = Converter.byteArrayToInt(data);
		// Get the first integer of the new array - this is the event type.
		int eventType = Converter.byteArrayToInt(data, 4);
		
		// Copy the new data into a new byte array, omitting the group ID and
		// event type
		byte[] newData = new byte[data.length - 8];
		System.arraycopy(data, 8, newData, 0, data.length - 8);

		Event event = null;

		switch (eventType) {
		case EventType.DRIVER_NONE:
      client.processEvent(EventType.DRIVER_NONE, null);
		  return;
		case EventType.DRAG_EVENT:
			event = new DragEvent(newData);
			break;
		case EventType.ROTATE_EVENT:
			event = new RotateEvent(newData);
			break;
		case EventType.SPIN_EVENT:
			// TODO change from default constructor
			event = new SpinEvent();
			break;
		case EventType.TOUCH_EVENT:
			event = new TouchEvent(newData);
			break;
		case EventType.ZOOM_EVENT:
			event = new ZoomEvent(newData);
			break;
//		case EventType.DBLCLK_EVENT:
//			event = new ClickEvent(newData);
//			break;
		case EventType.FLICK_EVENT:
			event = new FlickEvent(newData);
			break;
		case EventType.RELATIVE_DRAG_EVENT:
			event = new RelativeDragEvent(newData);
			break;
		}

		if (event != null)
			client.processEvent(groupID, event);

	}

	/**
	 * Handle the get group ID message.
	 * 
	 * @param client
	 *            The client the server wants to request group ID for.
	 * @param data
	 *            The data specific to the group ID message.
	 * @throws IOException
	 *             If there is a connection error.
	 */
	private void handleGetGroupID(SparshClient client, byte[] data)
			throws IOException {
    _out.writeInt(client.getGroupID(new Location(Converter.byteArrayToFloat(data, 0),
        Converter.byteArrayToFloat(data, 4))));
	}

	/**
   * Returns a list of valid gesture IDs to the gesture server. The message
   * protocol format is adapted by Bob Hanson to allow user-defined classes as
   * follows:
   * 
   * - 4 byte int n >= 0 --> one of the known SparshUI gesture IDs 
   * - 4 byte int n < 0  --> a string follows of negative this length 
   * - [-n bytes follow]
   * 
   * adapted by Bob Hanson for Jmol 11/29/2009
   * 
   * @param client
   *          The client that this call is pushed to.
   * @param data
   *          The data that holds the groupID for the call.
   * @throws IOException
   *           If there is a connection error.
   */
  private void handleGetAllowedGestures(SparshClient client, byte[] data)
      throws IOException {
    GestureType gType;
    List<GestureType> gestureTypes = client.getAllowedGestures(Converter.byteArrayToInt(data));
    int length = (gestureTypes == null ? 0 : gestureTypes.size());
    int blen = length * 4;
    for (int i = 0; i < length; i++) {
      gType = gestureTypes.get(i);
      if (gType.sType != null)
        blen += gType.sType.length();
    }
    _out.writeInt(blen);

    // Write the gesture IDs
    for (int i = 0; i < length; i++) {
      gType = gestureTypes.get(i);
      if (gType.sType == null) {
        _out.writeInt(gType.iType);
      } else {
        int len = gType.sType.length();
        if (len > 0) {
          _out.writeInt(-len);
          _out.write(Converter.stringToByteArray(gType.sType));
        }
      }
    }
  }
}
