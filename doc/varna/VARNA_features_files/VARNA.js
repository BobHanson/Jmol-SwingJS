/**
 * @author ponty
 */
 

function addChemProb(appletid,numFrom,numTo,type,intensity,color,orientation)
{
    var applet = document.getElementById(appletid);
    var script = "addChemProb("+numFrom+","+numTo+",\""+type+"\","+intensity+","+color+","+orientation+")";
	applet.runScript(script);
};

function resetChemProb(appletid)
{
    var applet = document.getElementById(appletid);
    var script = "resetChemProb()";
	applet.runScript(script);
};


function setTitle(appletid,ntitle)
{
    var applet = document.getElementById(appletid);
    var script = "setTitle(\""+ntitle+"\")";
	applet.runScript(script);
};

function setSeq(appletid,nseq)
{
    var applet = document.getElementById(appletid);
    var script = "setSeq(\""+nseq+"\")";
	applet.runScript(script);
};

function eraseSeq(appletid)
{
    var applet = document.getElementById(appletid);
    var script = "eraseSeq()";
	applet.runScript(script);
};


function setStruct(appletid,nstr)
{
    var applet = document.getElementById(appletid);
    var script = "setStruct(\""+nstr+"\")";
	applet.runScript(script);
};

function setStructSmooth(appletid,nstr)
{
    var applet = document.getElementById(appletid);
    var script = "setStructSmooth(\""+nstr+"\")";
	applet.runScript(script);
};

function setRNA(appletid,nseq,nstr)
{
    var applet = document.getElementById(appletid);
    var script = "setRNA(\""+nseq+"\",\""+nstr+"\")";
	applet.runScript(script);
};
		

function setRNASmooth(appletid,nseq,nstr)
{
    var applet = document.getElementById(appletid);
    var script = "setRNASmooth(\""+nseq+"\",\""+nstr+"\")";
	applet.runScript(script);
};
		
function redraw(appletid,nalgo)
{
    var applet = document.getElementById(appletid);
    var script = "redraw(\""+nalgo+"\")";
	applet.runScript(script);
};

function setColorMapValues(appletid,values)
{
    var applet = document.getElementById(appletid);
	var txt = "";
	for(var i=0;i<values.length;i++)
	{
		if (i>0)
		  txt += ", ";
		txt += values[i];
	}
    var script = "setValues(["+txt+"])";
	applet.runScript(script);
};

function setColorMapMinValue(appletid,value)
{
    var applet = document.getElementById(appletid);
    var script = "setColorMapMinValue("+value+")";
	applet.runScript(script);
};

function setColorMapMaxValue(appletid,value)
{
    var applet = document.getElementById(appletid);
    var script = "setColorMapMaxValue("+value+")";
	applet.runScript(script);
};

function setColorMap(appletid,val)
{
    var applet = document.getElementById(appletid);
    var script = "setColorMap("+val+")";
	  applet.runScript(script);
};


function setCustomColorMap(appletid,val)
{
    var applet = document.getElementById(appletid);
    var script = "setCustomColorMap("+val+")";
	applet.runScript(script);
};

function toggleShowColorMap(appletid)
{
	var applet = document.getElementById(appletid);
	var script = "toggleShowColorMap()";
	applet.runScript(script);   
};

function setSelection(appletid,values)
{
    var applet = document.getElementById(appletid);
	var txt = "";
	for(var i=0;i<values.length;i++)
	{
		if (i>0)
		  txt += ", ";
		txt += values[i];
	}
    var script = "setSelection(["+txt+"])";
	applet.runScript(script);
};


function getCurrentSelection(appletid)
{
	var applet = document.getElementById(appletid);
	return applet.getSelection();   
};



