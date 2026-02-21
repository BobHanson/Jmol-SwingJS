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

public class Region {
	private int _start1, _end1, _start2, _end2;

	public int getStart1() {
		return _start1;
	}

	public void setStart1(int start1) {
		this._start1 = start1;
	}

	public int getEnd1() {
		return _end1;
	}

	public void setEnd1(int end1) {
		this._end1 = end1;
	}

	public int getStart2() {
		return _start2;
	}

	public void setStart2(int start2) {
		this._start2 = start2;
	}

	public int getEnd2() {
		return _end2;
	}

	public void setEnd2(int end2) {
		this._end2 = end2;
	}
}
