/*

ChimeToJmol.js
hansonr@stolaf.edu 8:07 AM 4/21/2010
    Changes by A.Herraez 26 April 2010

A set of functions that will (when fully developed) 
allow the following on just about any Chime page to 
convert the Chime EMBED tags to Jmol applets and buttons.

Also changes older Jmol "applet" code to use Jmol.js

1) add these lines to your page's <HEAD> section:

 <script type="text/javascript" src="Jmol/Jmol.js">  </script>
 <script type="text/javascript" src="Jmol/ChimeToJmol.js">  </script>

2) Set jmolDirectory (below) to the desired directory and load it with
the standard Jmol files (Jmol.js, JmolApplet*.jar) and ChimeToJmol.js

3) If you are using a BODY onload="funcName()" attribute already, 
then add 

  checkJmol()

to it.

4) If you want to test your page, add NOJMOL to the page URL

TODO: more script conversions needed for Chime.

done # default Jmol Ball&Stick vs. defalt Chime wireframe
# default orientation / axes - are different, but also depend on file format (Chime PDB, Chime MOL, Jmol both)
done # embed tag options: display3d, color3d, bgcolor, hbonds, ssbonds, frank, hide popup menu, ...
done # zoom  (partially done, not foolproof; needs testing in different situations)
done # Hbonds on , Hbonds N  need previous calculate
# external script files cannot be parsed and fixed: manual edit seems necessary

*/

// set this as desired
var jmolDirectory = "./Jmol"


/////////Jmol section //////////

function checkJmol() {
	if (document.location.search.indexOf("NOJMOL") >= 0)return
	var body = document.body.innerHTML
	if (body.indexOf("JmolApplet") >= 0) {
		if(body.indexOf("JmolAppletControl") >= 0)__fixJmol()
		return
	}
	if (body.indexOf("<EMBED") >= 0 || body.indexOf("<embed") >= 0) __fixChime()
}

window.onload = checkJmol

function __fixJmol() {
	var body = document.body.innerHTML
	var ptAPPLET = -1
	var Text = body.split("<applet")
	if (Text.length == 1)return
	for (var i = 1; i < Text.length; i++) {
		var text = Text[i]
		var pt = text.indexOf("</applet>")
		if (text.indexOf("JmolAppletControl") >= 0) {
			Text[i] = __jmolFixJmolAppletControl(text.substring(0, pt)) + text.substring(pt + 9)
		} else if (text.indexOf("JmolApplet") >= 0) {
			Text[i] = __jmolFixJmolApplet(text.substring(0, pt)) + text.substring(pt + 9)
		} else {
			Text[i] = "<applet" + text
			continue
		}
	}
	document.body.innerHTML = Text.join("")
}

function __jmolFixJmolApplet(tag) {
	var A = __jmolGetAttributes(tag)
	if (!A.width)A.width = 300
	if (!A.height)A.height = 300
	if (!A.script)A.script = ""
	if (!A.codebase)A.codebase = jmolDirectory
	if (A.archive == "JmolApplet.jar") A.archive = "JmolApplet0.jar"
	if (document.location.protocol == "file:" && A.archive.indexOf("Signed") < 0) A.archive = "JmolAppletSigned0.jar"
	if (A.load)A.script = "load \"" + A.load + "\";" + A.script
	jmolInitialize(A.codebase, A.archive)
	jmolSetDocument(0)
	return jmolApplet([A.width,A.height],A.script, A.name)	
}

function __jmolFixJmolAppletControl(tag) {
	var A = __jmolGetAttributes(tag) 
	if (!A.width)A.width = 12
	if (!A.height)A.height = 12
	_jmol.buttonCssText = "style=\"width:" + A.width + "\""
	if (A.altscript)
		return jmolCheckbox(A.script, A.altscript, "", false, A.target)
	return "<span width=\"" + A.width + "\" height=\"" + A.height + "\">" + jmolButton(A.script,"X",A.target) + "</span>"
}

///////// Chime section //////////

function __fixChime() {
	jmolInitialize(jmolDirectory, 
		(document.location.protocol=="file:" ? "JmolAppletSigned0.jar" : "JmolApplet0.jar"))
	jmolSetDocument(0)
	jmolSetButtonCssClass("JmolChimeButton")

	var body = document.body.innerHTML
	var Text = body.split("<EMBED")
	if (Text.length == 1)Text = body.split("<embed")
	if (Text.length == 1)return
	for (var i = 1; i < Text.length; i++) {
		var text = Text[i]
		var pt = text.indexOf(">")
		if (text.substring(0,pt).indexOf("src=") >= 0) {
			Text[i] = __jmolFixChimeApplet(text.substring(0, pt)) + text.substring(pt + 1)
		} else if (text.substring(0,pt).indexOf("button=") >= 0) {
			Text[i] = __jmolFixChimeButton(text.substring(0, pt)) + text.substring(pt + 1)
		}
	}
	document.body.innerHTML = Text.join("")
}

function __jmolFixChimeApplet(tag) {  
	var A = __jmolGetAttributes(tag) 
	var zoomSet = null
	var s = "spacefill off;wireframe on;"
	if (__jmolIsChimeFalse(A.frank)) s += "frank off;"
	if (__jmolIsChimeTrue(A.nomenus)) s += "set disablePopupMenu on;"
	if (A.display3d) s += __jmolFixChimeDisplay3d(A.display3d)
	if (A.color3d) s += __jmolFixChimeColor3d(A.color3d)
	if (A.bgcolor) s += __jmolFixChimeBgcolor(A.bgcolor)
	if (A.spinx || A.spiny || A.spinz) s += "set spinX 0; set spinY 0; set spinZ 0;" // in Chime, setting any spin cancels the default 0 10 0; in Jmol, it does not
	if (A.spinx) s += "set spinX " + A.spinx + ";"
	if (A.spiny) s += "set spinY " + A.spiny + ";"
	if (A.spinz) s += "set spinZ " + A.spinz + ";"
	if (A.spinfps) s += "set spinFps " + A.spinfps + ";"
	if (__jmolIsChimeTrue(A.startspin)) s += "spin on;"
	if (A.hbonds && A.hbonds!="off") s += "hbonds calculate; hbonds " + A.hbonds + ";"
	if (A.ssbonds) s += "ssbonds " + A.ssbonds + ";"
	if (A.animmode) s += "animation mode " + A.animmode + ";"
	if (A.animfps) s += "animation fps " + A.animfps + ";"
	if (__jmolIsChimeTrue(A.startanim)) s += "animation on;"
	
	// if Chime size was set in pixels, let's do an approximated zoom conversion: store width and pass it to the script parser
	if (A.width.indexOf("%")==-1) { A.width = parseInt(A.width); zoomSet = A.width }
	if (A.height.indexOf("%")==-1) { A.height = parseInt(A.height) }
	if (!A.script || A.script.indexOf("zoom ")==-1) { zoomSet=null }
	
	A.script = (A.script ? __jmolFixChimeScript(A.script,zoomSet) : "")
	
	return jmolApplet([A.width,A.height],"load \"" + A.src + "\";" + s + A.script, A.name)	
}

function __jmolFixChimeButton(tag) {
	var A = __jmolGetAttributes(tag) 
	A.script = (A.script ? __jmolFixChimeScript(A.script) : "")
	if(A.target) jmolSetTarget(A.target)
	A.button = A.button.toLowerCase()	// 'button' may be push|pushed|radio#|toggle|followed
	if (A.button=="followed" || A.button=="push" || A.button=="pushed") {
		if (!A.height)A.height = 12
		return '<span style="font-size:' + (A.height-4) + 'px;">' + jmolButton(A.script,"X") + '</span>'
	}
	if (A.button=="toggle") {
		A.altscript = (A.altscript ? __jmolFixChimeScript(A.altscript) : "")
		return jmolCheckbox(A.script,A.altscript,"")
	}
	if (A.button.indexOf("radio")!=-1) {
		return jmolRadio(A.script, "", false, "", "Chime"+A.button) 
	}
	return jmolLink(A.script,"[x]")	//this shouldn't be reached
}

function __jmolFixChimeScript(script,isZoomSet) { 
	script = script//.replace(/\*\./g,"_")
			.replace(/hbonds /,"hbonds calculate;hbonds ")			
			.replace(/\&gt;/g, ">").replace(/\&lt;/g, "<")	// angle brackets are read as & entities and may be used in scripts as less than / more than
		// if Chime size was set in pixels, we'll do an approximated zoom conversion: 
		// Chime zoom value divided by Chime width gives an idea of the needed zoom value (percent)
	var j1 = script.indexOf("zoom ")
	if (j1!=-1 && isZoomSet) {
		var j2 = script.substring(j1).indexOf(";")
		if (j2==-1) { j2=script.length-j1 }
		var z = parseInt(script.substring(j1+5,j1+j2))	// this is the Chime zoom value
		script = script.substring(0,j1) + "zoom " + parseInt(z/isZoomSet*100) + script.substring(j1+j2) 
	}
	return script
}

function __jmolFixChimeDisplay3d(v) {
	if (v=="spacefill") return "spacefill on;wireframe off;"
	if (v=="sticks") return "spacefill off;wireframe 0.15;"
	if (v=="wireframe") return "spacefill on;wireframe on;"
	if (v=="ball&amp;stick") return "spacefill 30%;wireframe 0.15;"
	if (v=="cartoons") return "spacefill off;wireframe off;cartoons on;"
	if (v=="ribbons") return "spacefill off;wireframe off;ribbons on;"
	if (v=="strands") return "spacefill off;wireframe off;strands on;"
}
function __jmolFixChimeBgcolor(v) {
	if (v.charAt(0)=="#") v = "[x" + v.substring(1) + "]"
	return "color background " + v + ";"
}
function __jmolFixChimeColor3d(v) {
	if (v=="monochrome" || v=="user") v="white"
	return "color " + v + ";"
	//color3d = {chain|cpk|group|monochrome|shapely|structure|temperature|user}
}

function __jmolGetAttributes(tag) {
  var S = {}
  var name = ""
  var value = ""
  var inName = false
  var inValue = false
  if (tag.indexOf("<param") >= 0)
	tag = tag.replace(/\<param name\=/g," ")
		.replace(/value\=/g,"=")
		.replace(/\/\>/g, " ")
		.replace(/\>/g, " ")
		.replace(/[\r|\n|\t]/g, " ")
  if (tag.substring(tag.length-1, tag.length) == "/")
	tag = tag.substring(0, tag.length-1)
  tag = tag.replace(/\<br\>/g, " ")
  tag = tag.replace(/\<br \/\>/g, " ")
  tag = tag.replace(/\s+\=/g, "=")
  tag = tag.replace(/\=\s+/g, "=")
  tag += " ="
  for (var i = 0; i < tag.length - 2; i++) {
    switch(tag.charAt(i)) {
    case " ":
    case "\t":
    case "\n":
    case "\r":
	continue
    }
    var pts = tag.indexOf(" ", i)
    var pte = tag.indexOf("=", i)
    var ptq = tag.indexOf("'", pte + 1)
    var ptqq = tag.indexOf("\"", pte + 1)
    if (ptqq == pte + 1)ptq = ptqq
    var ptq2 = tag.indexOf((ptq == ptqq ? "\"" : "'"), ptq + 1)

    // <EMBED  xxxx ...
    if (pts < pte) {
	S[tag.substring(i, pts).toLowerCase().replace(/[\'\"]/g,"")] = ""
	i = pts
	continue
    }
    
    // <EMBED  xxxx=yyy ...
    if (pts < ptq) {
	S[tag.substring(i, pte).toLowerCase().replace(/[\'\"]/g,"")] = tag.substring(pte + 1, pts)
	i = pts
	continue
    }

    // <EMBED  xxxx="yyy" ...
   
    S[tag.substring(i, pte).toLowerCase().replace(/[\'\"]/g,"")] = tag.substring(ptq + 1, ptq2)
    i = ptq2    
  }
  return S
}

function __jmolIsChimeTrue(v) {
	if (!v) { return false; }
	v = v.toLowerCase();
	if (v=="yes"||v=="true"||v=="on"||v=="1") { return true; }
	return false;
}
function __jmolIsChimeFalse(v) {
	if (!v) { return false; }
	v = v.toLowerCase();
	if (v=="no"||v=="false"||v=="off"||v=="0") { return true; }
	return false;
}

document.writeln('<style type="text/css"> .JmolChimeButton { background-color:#C0C0C0; border:1px outset #C0C0C0; padding:0; font:inherit; } <' + '/style>')
