package fr.orsay.lri.varna.interfaces;

import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.rna.ModeleBase;

public interface InterfaceVARNASelectionListener {
	/**
	 * Specifies an action that should be performed upon changing the hovered base.
	 * @param oldbase Previously hovered base (possibly null).
	 * @param newBase Newly hovered base (possibly null).
	 */
	public void onHoverChanged(ModeleBase oldbase, ModeleBase newBase);
	
	/**
	 * Specifies the action to be performed upon changing the selection.
	 * @param selection The list of bases currently selected 
	 * @param addedBases The list of bases added since previous selection event
	 * @param removedBases The list of bases removed since previous selection event
	 */
	public void onSelectionChanged(BaseList selection, BaseList addedBases, BaseList removedBases);
}
