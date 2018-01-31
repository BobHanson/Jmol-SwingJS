/*jmolAnimationCntrl.js

J. Gutow May 2010 
A. Herraez June 2010 + December 2015
 Updated for JSmol

This includes 
- the CSS rules for the box and the buttons
- one function that controls the highlighting of the
   animation mode buttons
*/

document.writeln('<style type="text/css"> \n'+
    '  .AnimBox { border:1px solid gray; border-radius:0.6ex; } \n'+
    '  .AnimBox>div { white-space:nowrap; padding:1px 4px; } \n'+
    '  .AnimBox span { display:inline-block; vertical-align:middle; padding:1px;; } \n'+
    '  .AnimBox button { display:inline-block; font-size:4px; padding:0; } \n'+
    '  .AnimBox .jmol_playDefault { background-color:blue; } \n'+
    '<' + '/style>');

function jmol_animationmode(selected, n){
		var cellID;
		var s = "animation mode ";
    // reset styles:
		cellID = "jmol_loop_"+n;
    document.getElementById(cellID).style.backgroundColor = "transparent";
    cellID = "jmol_playOnce_"+n;
    document.getElementById(cellID).style.backgroundColor = "transparent";
    cellID = "jmol_palindrome_"+n;
    document.getElementById(cellID).style.backgroundColor = "transparent";
		// end reset
    if (selected=="loop") {
        cellID = "jmol_loop_"+n;
				s += "loop 0.2 0.2";
    } else if (selected=="playOnce") {
        cellID = "jmol_playOnce_"+n;
				s += "once";
    } else if (selected=="palindrome") {
        cellID = "jmol_palindrome_"+n;
				s += "palindrome 0.2 0.2";
    } else {
        return false; 
    }
		Jmol.script(window["jmolApplet"+n], s);
    document.getElementById(cellID).style.backgroundColor = "blue";
}
