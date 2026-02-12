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
package fr.orsay.lri.varna.exceptions;

public class MappingException extends Exception {
	private static final long serialVersionUID = 1L;

	public static final int MULTIPLE_PARTNERS_DEFINITION_ATTEMPT = 1;
	public static final int BAD_ALIGNMENT_INPUT = 2;
	public static final int CUSTOM_MESSAGE = 3;

	private String _errorMessage;
	private int _type;

	public MappingException(String errorMessage) {
		_errorMessage = errorMessage;
		_type = CUSTOM_MESSAGE;
	}

	public MappingException(int type) {
		_type = type;
	}

	public String getMessage() {
		switch (_type) {
		case MULTIPLE_PARTNERS_DEFINITION_ATTEMPT:
			return "Mapping error: Attempt to define multiple partners for a base-pair";
		case BAD_ALIGNMENT_INPUT:
			return "Mapping error: Bad input alignment";

		case CUSTOM_MESSAGE:
			return _errorMessage;
		default:
			return "Mapping error: Type is unknown.";
		}
	}

}
