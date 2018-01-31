/*

ChimeToJmol.js
hansonr@stolaf.edu 8:07 AM 4/21/2010
# last update 10/18/2010 7:24:31 AM

A set of functions that will (when fully developed) 
allow just about any Chime page to be converted automatically 
from Chime EMBED tags to Jmol applets and buttons.

Also changes older Jmol "applet" code to use Jmol.js

1) Set jmolDirectory (below) to the desired directory to contain
   the standard Jmol files (Jmol.js, JmolApplet*.jar) 
   as well as this file, ChimeToJmol.js
   and add those files to it.

2) add these lines to your page's <HEAD> section (with the appropriate directory, if needed):

 <script language="javascript" SRC="Jmol.js">  </script>
 <script language="javascript" SRC="ChimeToJmol.js">  </script>

3) If you are using a BODY onload="funcName()" attribute already, 
then add 

  checkJmol()

to it.

4) If you want to test your page, add "?NOJMOL" to the page URL

TODO: more script conversions needed for Chime


*/

// set this as desired
var jmolDirectory = "."
var useSignedApplet = false  // still, always uses signed applet for LOCAL testing
//jmolDebugAlert()


// You will probably have to adjust this next function
// to include more cases. These are just an example of the sort
// of fixes that need to be done to convert Chime scripts to Jmol scripts

function __jmolFixChimeScript(script) {
	script = "set defaultLoadScript 'wireframe only;rotate x 180;';" + 
		script
      //.replace(/\*\./g,"_") // as in *.C, *.H, for some small-molecular installations
			.replace(/stick on/g,"wireframe 0.15;")
			.replace(/ball\&amp\;stick off/g,"wireframe off;spacefill off")
			.replace(/select startanim\=false/g,"animation off")
			.replace(/select startanim\=true/g,"animation play")
			.replace(/set cartoon/g,"#set cartoon")
			.replace(/reset/g,"reset;rotate x 180")
			.replace(/rotate z\s+/g,"rotate z -")
			.replace(/ --/g," ")
			.replace(/set shadow/g,"#set shadow")
	return script
}

function __jmolGenericCallback(a, b, c, d, e) {
  b || (b = "")
  c || (c = "")
  d || (d = "")
  e || (e = "")
  alert([a, b, c, d, e].join("\n\n"))
}

/////////Jmol section //////////

function checkJmol() {
	if (top.location.search.indexOf("NOJMOL") >= 0)return
	var body = document.body.innerHTML
	if (body.indexOf("JmolApplet") >= 0) {
		if(body.indexOf("JmolAppletControl") >= 0)__fixJmol()
		return
	}
	if (body.indexOf("<xembed") >= 0 || body.indexOf("<XEMBED") >= 0) __fixChime()
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

function __fixChime() {
	jmolInitialize(jmolDirectory, 
		(document.location.protocol=="file:" || useSignedApplet ? "JmolAppletSigned0.jar" : "JmolApplet0.jar"))
	jmolSetDocument(0)
	var body = document.body.innerHTML
	body = body.replace(/XEMBED/g,"xembed")
	var Text = body.split("<xembed")
	if (Text.length == 1)Text = body.split("<xembed")
	if (Text.length == 1)return
	
	for (var i = 1; i < Text.length; i++) {
		var text = Text[i]
		var iq = 0;
		var pt = 0;
		for (; pt < text.length && iq >= 0;pt++) {
      switch (text.charAt(pt)) {
      case '"':
      case "'":
        iq += 1
        break
      case '>':
        if (iq%2==0) {
          iq = -1
        }
      }
      if (iq < 0)
        break;
    }
		var isButton = (text.indexOf("target=") >= 0) 
		if (isButton) {
			Text[i] = __jmolFixChimeButton(text.substring(0, pt), i) + text.substring(pt + 1)
		} else {
			Text[i] = __jmolFixChimeApplet(text.substring(0, pt)) + text.substring(pt + 1)
		}
	}
	document.body.innerHTML = Text.join("")
}

function __jmolFixChimeApplet(tag) {
	var A = __jmolGetAttributes(tag)
  if (A.width.indexOf("%") < 0)
  	A.width = parseInt(A.width)
  if (A.height.indexOf("%") < 0)
  	A.height = parseInt(A.height)
	A.script = (A.script ? __jmolFixChimeScript(A.script) : "")
	if (A.animmode) A.script += ";animation mode " + A.animmode + ";"
	if (A.animfps) A.script += ";animation fps " + A.animfps + ";"
	if (A.startanim && A.startanim.toLowerCase() == "true") A.script += ";animation on;"
	if (A.src.indexOf(".spt")>= 0) {
  	A.src = 'chimeScript = load("' + A.src + '"); javascript "__jmolRunChimeScript(\''+A.name+'\')";'	  
  } else {
    A.src = "load = \"" + A.src + "\";"
  }
  if (A.name)
    jmolSetTarget(A.name)
  var s = jmolApplet([A.width,A.height],A.src + A.script + ";set errorCallback '__jmolGenericCallback'", A.name)	
	return s
}

function __jmolRunChimeScript(target) {
  var script = jmolEvaluate('chimeScript', target)
  script = __jmolFixChimeScript(script)
  jmolScript(script, target);
}

function __jmolFixChimeButton(tag, iBtn) {
	var A = __jmolGetAttributes(tag, iBtn) 
	if (!A.width)A.width = 12
	if (!A.height)A.height = 12
	if (A.src) {
  	A.script = 'chimeScript = load("' + A.src + '"); javascript "__jmolRunChimeScript(\''+A.target+'\')"'	  
  } else {
  	A.script = __jmolFixChimeScript(A.script)
  }
  jmolSetTarget(A.target)
  var s = "<span width=\"" + A.width + "\" height=\"" + A.height + "\">" + jmolLink(A.script,"[x]", Math.random(), A.script.replace(/\"/g,"&#39;")) + "&nbsp;</span>"
	return s
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


