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

import java.util.ArrayList;
import java.util.Hashtable;

public class Loop {
	private int nconnection;
	private ArrayList<Connection> connections = new ArrayList<Connection>();
	private Hashtable<Integer,Connection> _connections = new Hashtable<Integer,Connection>();
	private int number;
	private int depth;
	private boolean mark;
	private double x, y, radius;

	public int getNconnection() {
		return nconnection;
	}

	public void setNconnection(int nconnection) {
		this.nconnection = nconnection;
	}

	public void setConnection(int i, Connection c)
	{
		Integer n = new Integer(i);
		if (c != null)
    		_connections.put(n, c);
		else
		{
			if (!_connections.containsKey(n))
			{
				_connections.put(n, new Connection());
			}
			_connections.get(i).setNull(true);
		}
	}

	public Connection getConnection(int i)
	{
		Integer n = new Integer(i);
		if (!_connections.containsKey(n))
		{ _connections.put(n, new Connection()); }
		Connection c = _connections.get(n);
		if (c.isNull())
			return null;
		else
			return c;
	}
	
	public void addConnection(int i, Connection c)
	{
		_connections.put(_connections.size(),c);
	}
	

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public boolean isMark() {
		return mark;
	}

	public void setMark(boolean mark) {
		this.mark = mark;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public String toString()
	{
		String result = "Loop:";
		result += " nconnection "+nconnection;
		result += " depth "+depth;
		return result;
	}
}
