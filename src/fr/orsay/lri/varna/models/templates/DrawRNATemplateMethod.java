/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.templates;

/**
 * What to do in case some helices are of a different length
 * in the template and the actual helix.
 * Possibles values are:
 * 0 - No adjustment is done.
 *     A longer than expected helix might bump into an other helix.
 * 1 - Scaling factors (actual length / template length) are calculated,
 *     the maximum scaling factor L is extracted, then all helix positions
 *     are multiplied by L. 
 * 2 - Same as 1, but L is computed as the minimum value such that there
 *     are no backbone intersections.
 */
public enum DrawRNATemplateMethod {
	NOADJUST("No Adjust"), MAXSCALINGFACTOR("Max Scaling"), NOINTERSECT("Non-crossing"), HELIXTRANSLATE("Helix Translation");
	
	String _msg;
	
	public static DrawRNATemplateMethod getDefault() {
		return HELIXTRANSLATE;
	}
	

	public String toString()
	{
		return _msg;
	}
	
	private DrawRNATemplateMethod(String msg)
	{
		_msg = msg;
	}
}