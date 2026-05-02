package fr.orsay.lri.varna.interfaces;

import fr.orsay.lri.varna.models.BaseSet;
import fr.orsay.lri.varna.models.rna.ModeleBase;

public interface InterfaceVARNASelectionListener {
	int SEL_SET = 101;
  int SEL_ADD = 102;
  int SEL_REMOVE = 103;
  int SEL_CLEAR = 104;
  int SEL_COMPLETE = 105;

  /**
	 * Specifies an action that should be performed upon changing the hovered base.
	 * @param oldbase Previously hovered base (possibly null).
	 * @param newBase Newly hovered base (possibly null).
   * @param doNotify only true if this is a plugin and hover is originating from VARNA
	 */
	public void onHoverChanged(ModeleBase oldbase, ModeleBase newBase, boolean doNotify);

  /**
	 * Specifies the action to be performed upon changing the selection.
   * @param selectionMode SEL_XXX (see above)
	 * @param selection The list of bases currently selected 
	 * @param addedBases The list of bases added since previous selection event
	 * @param removedBases The list of bases removed since previous selection event
	 */
	public void onSelectionChanged(int selectionMode, BaseSet selection, BaseSet addedBases, BaseSet removedBases);
}
