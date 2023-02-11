package com.sparshui.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.sparshui.common.ConnectionType;
import com.sparshui.common.NetworkConfiguration;

/**
 * Represents the connection to the GestureServer.
 * 
 * @author Jay Roltgen
 *
 */
public class ClientServerConnection extends Thread {
	
	private SparshClient _client;
	private Socket _socket;
	private ClientToServerProtocol _protocol;
	
	/**
	 * Instantiate a Server Connection object.
	 * 
	 * @param address 
	 * 		The ip address of the server to connect to.
	 * @param client 
	 * 		The client object that the gesture client is using to
	 * 		listen for messages from the server.
	 * @throws UnknownHostException
	 * 		If "address" is an unknwown host.
	 * @throws IOException
	 * 		If a communication error ocurrs.
	 */
	public ClientServerConnection(String address, SparshClient client) throws UnknownHostException, IOException {
		_client = client;
		_socket = new Socket(address, NetworkConfiguration.CLIENT_PORT);
		DataOutputStream out = new DataOutputStream(_socket.getOutputStream());
		out.writeByte(ConnectionType.CLIENT);
		_protocol = new ClientToServerProtocol(_socket);
		this.start();
	}

	/**
	 * Begin processing requests.
	 */
	//@override
	@Override
  public void run() {
	  Thread.currentThread().setName("SparshUI Client->ServerConnection");
		while(_socket.isConnected()) {
			if (!_protocol.processRequest(_client)) //BH -- must allow for server failure
			  break;
		}
	}
	
	public void close() {
	  try {
      _socket.close();
    } catch (IOException e) {
      // TODO
    }
	}

}
