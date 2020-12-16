/*

appletToJSmol.js
BH 12/8/2020 for VOT (Virtual Organic Textbook)

Changes older Jmol "applet" code to use Jmol.js

Fixes tags in variables and in the HTML

1) make sure your page is declared <!DOCTYPE html>

2) Set j2sPath (below) to the correct path to the j2s/ directory

3) add these lines to your file:

 <script src="jsmol/jsmol.min.js"></script>
 <script>j2sPath = "jsmol/j2s";loadOptions="filter 'noOrient'"</script>
 <script src="AppletToJSmol.js"></script>

 Note that the middle line sets two variables:
  (a) j2sPath -- the path to the j2s files, and 
  (b) loadOptions -- an option to adjust the Jmol LOAD command. In this case, 
      we are telling the Spartan reader to disregard the orientation information 
      in the file. Apparently, these files were created prior to that being
      read as the default. This is for older applets that access Spartan files.
*/

self.jmolPath || (jmolPath = "jsmol");
self.loadOptions || (loadOptions = "");
Jmol.setDocument(null);

function __fixJmolAppletTags() {
	var applets = document.getElementsByTagName("APPLET");
	for (var i = 0; i < applets.length; i++) {
		var text = applets[i].outerHTML;
		var newHtml;
		if (text.indexOf("JmolAppletControl") >= 0) {
			applets[i].innerHTML = __jmolFixJmolAppletControl(text, applets[i]);

		} else if (text.indexOf("JmolApplet") >= 0) {
			newHtml = __jmolFixJmolApplet(text, applets[i]);
		} else {
			continue
		}
	}
}


function __jmolFixJmolApplet(tag, applet) {
	var A = __jmolGetAttributes(tag) 
	if (!A.width)A.width = 300
	if (!A.height)A.height = 300
	if (!A.script)A.script = ""
	if (!A.codebase)A.codebase = j2sPath
	if (A.load)A.script = "load \"" + A.load + "\" " + loadOptions + ";set antialiasdisplay;" + A.script
	//Jmol.initialize(A.codebase, A.archive)
	var info = {
		name:A.name,
		j2sPath: j2sPath,
		width: A.width,
		height: A.height,
		script: A.script
	};
	var html = Jmol.getAppletHtml(A.name, info);
	if (applet) {
		applet.innerHTML = html;
		window[A.name]._cover(false);
	}
	return html;
}

function __jmolFixJmolAppletControl(tag) {
	var A = __jmolGetAttributes(tag) 
	if (!A.width)A.width = 12
	if (!A.height)A.height = 12
	Jmol.setButtonCss(null,"style=\"width:" + A.width + "\"")
	var s = (A.altscript ? Jmol.jmolCheckbox(A.target,A.script, A.altscript, "", false)
		: "<span width=\"" + A.width + "\">" + Jmol.jmolButton(A.target,A.script,"X"));
	return s;
}

function fixChars(s) {
	var pt;
	var S = s.split("&#");
	for (var i = 1; i < S.length; i++) {
		var d = document.createElement("div");
		d.innerHTML = "&#" + S[i].substring(0, (pt = S[i].indexOf(";")+1));
		S[i] = d.innerText + S[i].substring(pt);
	}
	return S.join("");
}

function __jmolGetAttributes(tag, iBtn) {
  var S = {}
  var name = ""
  var value = ""
  var inName = false
  var inValue = false
  tag = tag.replace(/\<br\>/g, " ")
  tag = tag.replace(/\<br \/\>/g, " ")
  if (tag.substring(tag.length-1, tag.length) == "/")
  	tag = tag.substring(0, tag.length-1)
  if (tag.indexOf("<param") >= 0)
  	tag = tag.replace(/\<param name\=/g," ")
	  	.replace(/value\=/g,"=")
		  .replace(/\/\>/g, " ")
		  .replace(/\>/g, " ")
	tag = tag.replace(/\s+/g, " ")
  tag = tag.replace(/ \=/g, "=")
  tag = tag.replace(/\= /g, "=")
  tag += " ="
  for (var i = 0; i < tag.length - 2; i++) {
    if (tag.charAt(i) == " ")
    	continue
    var pts = tag.indexOf(" ", i)
    var pte = tag.indexOf("=", i)
    var ptq = tag.indexOf("'", pte + 1)
    var ptqq = tag.indexOf("\"", pte + 1)
    if (ptqq == pte + 1)ptq = ptqq
    var ptq2 = tag.indexOf((ptq == ptqq ? "\"" : "'"), ptq + 1)
    if (pts < pte) {
    // <xembed  xxxx ...
	S[tag.substring(i, pts).toLowerCase().replace(/[\'\"]/g,"")] = ""
	i = pts
	  } else if (pts < ptq || ptq < 0) {
    // <xembed  xxxx=yyy ...
	S[tag.substring(i, pte).toLowerCase().replace(/[\'\"]/g,"")] = tag.substring(pte + 1, pts)
	i = pts
    } else {
    // <xembed  xxxx="yyy" ...
	S[tag.substring(i, pte).toLowerCase().replace(/[\'\"]/g,"")] = tag.substring(ptq + 1, ptq2)
	i = ptq2    
    }
    if (i < 0){quit}
  }
  return S
}

function __fixAppletVar(x, val) {
	var ptAPPLET = -1
	var Text = val.split("<applet")
	if (Text.length == 1)return
	for (var i = 1; i < Text.length; i++) {
		var text = fixChars(Text[i])
		var pt = text.indexOf("</applet>")
		if (text.indexOf("JmolAppletControl") >= 0) {
			Text[i] = __jmolFixJmolAppletControl(text.substring(0, pt)) + fixChars(text.substring(pt + 9))
		} else if (text.indexOf("JmolApplet") >= 0) {
			Text[i] = __jmolFixJmolApplet(text.substring(0, pt)) + fixChars(text.substring(pt + 9))
		} else {
			Text[i] = "<applet" + text
			continue
		}
	}
	var s = Text.join("");
	if (s.indexOf("<CENTER>") == 0 && s.lastIndexOf("</CENTER>") < 0) s += "</CENTER>"
	window[x] = s;
}

function fixAppletVars() {
	for (x in window) {
		if (typeof window[x] == "string" && window[x].indexOf("<applet") >= 0)
		__fixAppletVar(x, window[x]);
	}
}

function fixAppletTags() {
	$("body").ready(function(){__fixJmolAppletTags()});
}


fixAppletVars();
fixAppletTags();