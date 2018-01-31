package jspecview.api;

import javajs.util.Lst;

public interface JSVAppInterface extends JSVAppletInterface, ScriptInterface {

	Lst<String> getScriptQueue();

}
