/*	Allows to open Jmol models on request, in a div

	-- this file is the successor of JmolPopIn.js 
	-- when its code changed so much (JSmol)

	Available from  https://biomodel.uah.es/Jmol/  and  https://wiki.jmol.org/ 
	Author: Angel Herr�ez.  Version 2007.04.23
	
	This template is offered freely for anyone to use or adapt it, 
	according to Creative Commons Attribution-ShareAlike 3.0 License,
	https://creativecommons.org/licenses/by-sa/3.0/

	Modified 2007.07.17  by Jonathan Gutow
	
	Main change is that the JmolSize is specified in the call.
	Image file is forced to fit within the div boundaries by scaling it.
	Image file name is passed explicitely.
	Removed the passing of the molecule name as labels should be set in script.
	
	Modified 2007.08.13 by Bob Hanson
	
	-- integration into Jmol application
	
	Modified 2010.3.9 by Jonathan Gutow
	-- addition of widget activation upon pop-in.
	-- addition of widget switching on view change in ScriptButton pages.
	
	Modified 2010 June by Jonathan Gutow and Angel Herraez
	-- bug fixes for IE support

	Modified 2015 Dec by Angel Herraez
	-- this file evolved from JmolPopIn.js
	-- less unneeded tags, less tables, more css
	-- adapted to JSmol
	-- removing unused parts

*/

// Support for ScriptButton template

var jmolWidgetStrs = [];

function fixScriptButtonWidgets(numberOfButtons) { //stores code from all widgets in variables
		var divID=null;
    for (i=0;i<numberOfButtons;i++) {
			divID = document.getElementById("jmolCntl"+i);
      if (divID) { 
				jmolWidgetStrs[i] = divID.innerHTML;
				divID.innerHTML = "";	//empty them to avoid duplicate IDs
			}
    }
		// and moves the first
		updateScriptButtonWidgets(0);
}

function updateScriptButtonWidgets(buttonNumber){ // moves the n'th
    document.getElementById("JmolCntl").innerHTML = jmolWidgetStrs[buttonNumber];
}


// Support for PopIn template

function revealPopinWidgets(i) {
	document.getElementById("leftJmolCntl" + i).style.visibility = "visible";
	document.getElementById("rightJmolCntl" + i).style.visibility = "visible";
}

