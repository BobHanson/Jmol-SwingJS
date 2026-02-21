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
package fr.orsay.lri.varna.models.naView;

public class Connection {
	private Loop loop = new Loop();
	private Region region = new Region();
	// Start and end form the 1st base pair of the region.
	private int start, end;
	private double xrad, yrad, angle;
	// True if segment between this connection and the
	// next must be extruded out of the circle
	private boolean extruded;
	// True if the extruded segment must be drawn long.
	private boolean broken;
	
	private boolean _isNull=false;

	public boolean isNull() {
		return _isNull;
	}

	public void setNull(boolean isNull) {
		_isNull = isNull;
	}

	
	public Loop getLoop() {
		return loop;
	}

	public void setLoop(Loop loop) {
		this.loop = loop;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public double getXrad() {
		return xrad;
	}

	public void setXrad(double xrad) {
		this.xrad = xrad;
	}

	public double getYrad() {
		return yrad;
	}

	public void setYrad(double yrad) {
		this.yrad = yrad;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public boolean isExtruded() {
		return extruded;
	}

	public void setExtruded(boolean extruded) {
		this.extruded = extruded;
	}

	public boolean isBroken() {
		return broken;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}
}
