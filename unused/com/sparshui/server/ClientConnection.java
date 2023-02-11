package com.sparshui.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;

import com.sparshui.GestureType;

/**
 * Represents a network connection to a client.
 * 
 * @author Tony Ross
 * 
 */
class ClientConnection {

	/**
	 * 
	 */
	private ServerToClientProtocol _protocol;

	/**
	 * 
	 */
	private Map<Integer, Group> _groups;

	/**
	 * Instantiate the connection on the specified socket.
	 * 
	 * @param socket
	 * 		The socket that has been opened to the client.
	 * @throws IOException
	 * 		If there is a communication error.
	 */
	ClientConnection(Socket socket) throws IOException {
		_protocol = new ServerToClientProtocol(socket);
		_groups = new Hashtable<Integer, Group>();
	}

	/**
   * Process a touch point birth.
   * 
   * @param touchPoint
   *          The touch point to process.
   * @return True if a group was found for the touch point, false otherwise.
   * @throws IOException
   *           If there is a communication error.
   */
  boolean processBirth(TouchPoint touchPoint) throws IOException {

    int groupID = (touchPoint == null ? 0x10000000 : getGroupID(touchPoint));
    int jmolFlags = (groupID & 0xF0000000);
    if (jmolFlags != 0) {
      switch (jmolFlags) {
      case 0x10000000:
        // reset flag
        _groups = new Hashtable<Integer, Group>();
        break;
      }
      groupID &= ~jmolFlags;
    }

    //System.out.println("[ClientConnection] Has GroupID: " + groupID);
    Group group = getGroup(groupID);
    if (group != null) {
      // Client claims point
      //System.out.println("Client claims point");
      touchPoint.setGroup(group);
      return true;
    }
    //System.out.println("[ClientConnection] Client did not claim point");
    // Client does not claim point
    return false;
  }

	/**
	 * 
	 * @param groupID 
	 * @return Vector
	 * @throws IOException
	 */
	private Lst<GestureType> getGestures(int groupID) throws IOException {
		return _protocol.getGestures(groupID);
	}

	/**
	 * 
	 * @param touchPoint
	 * @return groupId
	 * @throws IOException
	 */
	private int getGroupID(TouchPoint touchPoint) throws IOException {
		return _protocol.getGroupID(touchPoint);
	}

	/**
	 * 
	 * @param groupID
	 * @return Group
	 * @throws IOException
	 */
	private Group getGroup(int groupID) throws IOException {
		if (groupID == 0)
			return null;
		Group group = null;
		Integer gid = Integer.valueOf(groupID);
		if (_groups.containsKey(gid)) {
			group = _groups.get(gid);
		} else {
			// This is a new group, so get its allowed gestures and construct

			//System.out.println("[ClientConnection] Getting Group Gestures ID:
			// " + groupID);
		  // gestureID may be a string indicating a user-defined class to load.
			Lst<GestureType> gestureTypes = getGestures(groupID);
			group = new Group(groupID, gestureTypes, _protocol);
			_groups.put(gid, group);
		}

		//System.out.println("[ClientConnection] Returning Group");
		return group;
	}

  public void processError(int errCode) {
    try {
      _protocol.processError(errCode);
    } catch (IOException e) {
      //ignore
    }
  }

}
