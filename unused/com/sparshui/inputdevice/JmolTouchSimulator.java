package com.sparshui.inputdevice;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
//import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.jmol.api.JmolTouchSimulatorInterface;
import org.jmol.util.Logger;

import com.sparshui.common.ConnectionType;
import com.sparshui.common.NetworkConfiguration;
import com.sparshui.common.TouchState;

/**
 * allows Jmol to implement multitouch testing using the mouse
 * press CTRL-LEFT and drag twice for a two-stroke multitouch gesture
 * that will be sent to the server in an interlaced fashion.
 * 
 * adapted by Bob Hanson for Jmol 11/29/2009
 * 
 */
public class JmolTouchSimulator implements JmolTouchSimulatorInterface {

  private TreeSet<TouchData> _events = new TreeSet<TouchData>(new TouchDataComparator());
	protected Map<Integer, TouchData> _active = new Hashtable<Integer, TouchData>();
	private boolean _recording = false;
	private int _touchID = 0;
	private long _when = 0;
	private Timer _timer;
	
	private Component _display;
	
  //private DataInputStream _in;
	private DataOutputStream _out;

	public JmolTouchSimulator() {
	}

	@Override
  public void dispose() {
    try {
      //_in.close();
    } catch (Exception e) {
      
    }
    try {
      _out.close();
    } catch (Exception e) {
      
    }
    try {
     _timer.cancel();
    } catch (Exception e) {
      
    }
	}
	
	/* (non-Javadoc)
   * @see com.sparshui.inputdevice.JmolTouchSimulatorInterface#initialize(java.awt.Component)
   */
	@Override
  public boolean startSimulator(Object display) {
	   _display = (Component) display;
	   String address = "localhost";
	    _timer = new Timer();
	    try {
	      Socket socket = new Socket(address, NetworkConfiguration.DEVICE_PORT);
        //_in = new DataInputStream(socket.getInputStream());
	      _out = new DataOutputStream(socket.getOutputStream());
	      _out.writeByte(ConnectionType.INPUT_DEVICE);
	      socket.close();
	      return true;
	    } catch (UnknownHostException e) {
	      Logger.error("Could not locate a server at " + address);
	    } catch (IOException e) {
	      Logger.error("Failed to connect to server at " + address);
	    }
	    return false;
	}
	
	/* (non-Javadoc)
   * @see com.sparshui.inputdevice.JmolTouchSimulatorInterface#toggleMode()
   */
	@Override
  public void toggleMode() {
		if(_recording) {
			endRecording();
		} else {
		  startRecording();
		}

	}
	
	@Override
  public void startRecording() {
		_recording = true;
		_active.clear();
	}
	
	@Override
  public void endRecording() {
		_recording = false;
		dispatchTouchEvents();
	}

	/* (non-Javadoc)
   * @see com.sparshui.inputdevice.JmolTouchSimulatorInterface#mousePressed(java.awt.event.MouseEvent)
   */
	@Override
  public void mousePressed(long time, int x, int y) {
		handleMouseEvent(time, x, y, TouchState.BIRTH);
	}

	/* (non-Javadoc)
   * @see com.sparshui.inputdevice.JmolTouchSimulatorInterface#mouseReleased(java.awt.event.MouseEvent)
   */
	@Override
  public void mouseReleased(long time, int x, int y) {
		handleMouseEvent(time, x, y, TouchState.DEATH);
	}

	/* (non-Javadoc)
   * @see com.sparshui.inputdevice.JmolTouchSimulatorInterface#mouseDragged(java.awt.event.MouseEvent)
   */
	@Override
  public void mouseDragged(long time, int x, int y) {
		handleMouseEvent(time, x, y, TouchState.MOVE);
	}

	private void handleMouseEvent(long time, int x, int y, int type) {
		// Ignore any input except the left mouse button
		// Construct the touch event
		TouchData te = new TouchData();
		te.id = (type == TouchState.BIRTH) ? ++_touchID : _touchID;
		Point p = new Point(x, y);
		try {
		  SwingUtilities.convertPointToScreen(p, _display);
		} catch (Throwable e) {
		  // no Swing
		  return;
		}
		te.x = p.x;
		te.y = p.y;
		//te.x = e.getLocationOnScreen().x;
		//te.y = e.getLocationOnScreen().y;
		te.type = type;
		te.when = time;
		
		if(_recording) {
			// Store the event to be played later
			if(type == TouchState.BIRTH) {
				te.delay = 0;
				_when = te.when;
			} else {
				te.delay = te.when - _when;
			}
			_events.add(te);
		} else {
			// Dispatch the event now
			dispatchTouchEvent(te);
			if (Logger.debugging)
			  Logger.debug("[JmolTouchSimulator] dispatchTouchEvent("+te.id+", "+te.x+", "+te.y+", "+te.type+")");
		}
	}
	
	private void dispatchTouchEvents() {
    for (TouchData data: _events) {
			TouchTimerTask task = new TouchTimerTask(data);
			_timer.schedule(task, data.delay + 250);
		}
		_events.clear();
		_touchID = 0;
	}

	/**
	 * protocol modified by Bob Hanson for Jmol to demonstrate 
	 * extended SparshUI protocol to include a return from the
	 * server indicating whether or not to consume this event.
	 * 
	 * server return == (byte) 1 --> do consume this event
	 * server return == (byte) 0 --> do not consume this event
	 * 
	 * @param data
	 */
	protected void dispatchTouchEvent(TouchData data) {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension dim = tk.getScreenSize();
    if (Logger.debugging)
      Logger.debug("[JmolTouchSimulator] dispatchTouchEvent("+data.id+", "+data.x+", "+data.y+", "+data.type+")");
    try {
      _out.writeInt(-1);
      _out.writeInt(21);
      _out.writeInt(data.id);
      _out.writeFloat(((float) data.x / (float) dim.width));
      _out.writeFloat(((float) data.y / (float) dim.height));
      _out.writeByte((byte) data.type);
      _out.writeLong(data.when);
      //boolean doConsume = (_in.readByte() == 1);
      //if (Logger.debugging)
       //System.out.println("[JmolTouchSimulator] doConsume=" + doConsume);
    } catch (IOException e1) {
      System.err.println("Failed to send event to server.");
    }
  }

	protected class TouchData {
		public int type;
		public int id;
		public int x;
		public int y;
		public long when;
		public long delay;
	}
	
	protected class TouchDataComparator implements Comparator<TouchData> {

		@Override
    public int compare(TouchData o1, TouchData o2) {
      return (o1.delay == o2.delay ? (o1.when < o2.when ? -1 : 1) 
          : o1.delay < o2.delay ? -1 : 1);
    }
	}
	
	private class TouchTimerTask extends TimerTask {
		private TouchData data;
		
		TouchTimerTask(TouchData data) {
			this.data = data;
		}

		//@Override
		@Override
    public void run() {
		  Thread.currentThread().setName("JmolTouchSimulator for type " + data.id);
			dispatchTouchEvent(data);
			Integer iid = Integer.valueOf(data.id);
			if(data.type == TouchState.DEATH) {
        _active.remove(iid);
			} else {
        _active.put(iid, data);
			}
      Thread.currentThread().setName("JmolTouchSimulator idle");
		}
	}	
}
