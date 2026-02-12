/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.templates;

/**
 * @author Raphael Champeimont
 * How should we draw unpaired regions?
 */
public enum DrawRNATemplateCurveMethod {
	 // use what is in the template
	EXACTLY_AS_IN_TEMPLATE("Bezier"),
	// use ellipses, ignoring Bezier parameters from template (useful mostly for debugging ellipses)
	ALWAYS_REPLACE_BY_ELLIPSES("Ellipses"),
	// replace by ellipse where it is a good idea
	SMART("Auto-Select");

	String _msg;
	
	private DrawRNATemplateCurveMethod(String msg){
		_msg=msg;
	}
	
	public String toString()
	{
		return _msg;
	}
	
	public static DrawRNATemplateCurveMethod getDefault() {
		return SMART;
	}
}
