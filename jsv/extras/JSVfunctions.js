/*
*  JSpecView Utility functions
*  Version 1.3b, Copyright(c) 2006-2012, Dept of Chemistry, University of the West Indies, Mona
*  Robert J Lancashire  robert.lancashire@uwimona.edu.jm
*
*
*  12:19 PM 3/8/2012 added support for JSpecViewAppletPro  -- BH
*/
var _JSVversionnumber=1.3;

/*
	Inserts the JSpecView applet in any compatible User Agent using the <object> tag
	uses IE conditional comments to distinguish between IE and Mozilla
	see http://msdn.microsoft.com/workshop/author/dhtml/overview/ccomment_ovw.asp
*/
function insertJSVObject(_JSVarchive,_JSVtarget,_JSVwidth,_JSVheight,_JSVscript){

_JSVSyncID = ("" + Math.random()).substring(3)
if (!_JSVscript) _JSVscript = ""
_JSVscript = 'appletID ' + _JSVtarget + ';syncID '+_JSVSyncID+';' + _JSVscript

var myClass = "jspecview.applet.JSVApplet" + (_JSVarchive.indexOf("Signed") >= 0 ? "Pro" : "") + ".class"
var nameParam = '<param name="name" value="'+ _JSVtarget +'" />\n'
// script for Mozilla
_JSVMozscript='<object classid="java:' + myClass + '" '
              +'type="application/x-java-applet;version=1.5" archive= "'+ _JSVarchive+'" '
		  +'id= "'+_JSVtarget
		  +'" name= "'+_JSVtarget
		  +'" height="'+_JSVheight
		  +'" width="'+_JSVwidth+'" >\n'
 	 +'<param name="script" value="'+_JSVscript +'" />\n'
	 +'<param name="mayscript" value="true" />\n'
	 + nameParam;
// script for MSIE (Microsoft Internet Explorer) and SUN plugin version 1.5 at least
_JSVIEscript='<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" \n'
	       +'codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_5_0-windows-i586.cab#Version=1,5,0,0" \n'
		 +'id= "'+_JSVtarget
		 +'" name= "'+_JSVtarget
		 +'" height="'+_JSVheight
		 +'" width="'+_JSVwidth+'" >\n'
         +'<param name="code" value="' + myClass + '" />\n'
	 +'<param name="archive" value="'+_JSVarchive+'" />\n'
      	 +'<param name="script" value="'+_JSVscript +'" />\n'
	 +'<param name="scriptable" value="true" />\n'
	 +'<param name="mayscript" value="true" />\n'
	 + nameParam;
// else no Sun Java Plug-in available or non-compatible UA's ?
_JSVerror='<strong>This browser does not have a Java Plug-in or needs upgrading.<br />'+
            '<a href="http://java.sun.com/products/plugin/downloads/index.html">'+
      	    'Get the latest Sun Java Plug-in from here.</a>'+'<\/strong>';

// edit only the lines above here
// do not remove the comments or the script will fail to work properly!

var s = ("<!--[if !IE]> Mozilla and others will this use outer object -->");
s +=(_JSVMozscript);
s +=("<!--<![endif]-->");
s +=("<!-- MSIE (Microsoft Internet Explorer) will use the inner object -->");
s +=(_JSVIEscript);
s +=(_JSVerror);
s +=("<\/object>");
s +=("<!--[if !IE]> close outer object -->");
s +=("<\/object>");
s +=("<!--<![endif]-->");

document.write(s);

// end of function
}
