/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.controlers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import fr.orsay.lri.varna.VARNAPanel;

/**
 * BH SwingJS converted to Timer mechanism for compatibility with JavaScript 
 *
 */
public class ControleurBlinkingThread extends Thread implements ActionListener {
	public static final long DEFAULT_FREQUENCY = 50;
	private long _period;
	private VARNAPanel _parent;
	private double _minVal, _maxVal, _val, _incr;
	private boolean _increasing = true;
	private boolean _active = false;

	public ControleurBlinkingThread(VARNAPanel vp) {
		this(vp, DEFAULT_FREQUENCY, 0, 1.0, 0.0, 0.2);
	}

	public ControleurBlinkingThread(VARNAPanel vp, long period, double minVal,
			double maxVal, double val, double incr) {
		_parent = vp;
		_period = period;
		_minVal = minVal;
		_maxVal = maxVal;
		_incr = incr;
	}

	public void setActive(boolean b) {
		if (_active == b)
		{}
		else
		{
		_active = b;
		if (_active) {
			interrupt();
		}
		}
	}

	public boolean getActive() {
		return _active;
	}
	
	
	public double getVal() {
		return _val;
	}

	protected final int START = 0;
	protected final int LOOP = 1;
	protected final int STOP = -1;
	
	protected int nextMode = START;
	private Timer timer;
	
	
	public void interrupt() {
		super.interrupt();
		stopTimer();
		run();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		run();
	}
	public void run() {
	//   same as:
	//			while (true) {
	//			try {
	//				if (_active) {
	//					sleep(_period);
	//					if (_increasing) {
	//						_val = Math.min(_val + _incr, _maxVal);
	//						if (_val == _maxVal) {
	//							_increasing = false;
	//						}
	//					} else {
	//						_val = Math.max(_val - _incr, _minVal);
	//						if (_val == _minVal) {
	//							_increasing = true;
	//						}
	//					}
	//					_parent.repaint();
	//				} else {
	//					sleep(10000);
	//				}
	//			} catch (InterruptedException e) {
	//			}
	//		}
		long delay = 0;
		while (true) { 
			try {
				switch (nextMode) {
				case START:
					if (_active) {
						delay = _period;
						nextMode = LOOP;
					} else {
						delay = 10000;
						nextMode = START;
					}
					startTimer(delay);
					return;
				case STOP:
					break;
				case LOOP:
					if (_increasing) {
						_val = Math.min(_val + _incr, _maxVal);
						if (_val == _maxVal) {
							_increasing = false;
						}
					} else {
						_val = Math.max(_val - _incr, _minVal);
						if (_val == _minVal) {
							_increasing = true;
						}
					}
					_parent.repaint();
					nextMode = START;
					continue;
				}
				sleep(0);
			} catch (InterruptedException e) {
				// ignore??
			}
			break;
			}
	}

	private void startTimer(long delay) {
		stopTimer();
		timer = new Timer((int) delay, this);
		timer.setRepeats(false);
		timer.start();
	}

	private void stopTimer() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
	}

}
