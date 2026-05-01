package fr.orsay.lri.varna.interfaces;

import fr.orsay.lri.varna.models.rna.ModeleBase;

public interface InterfaceVARNABasesListener {

	/**
	 * Reacts to click over base
	 * @param mb The base which has just been clicked
	 * @param id 
	 */
	public void onBaseClicked(ModeleBase mb, int id);
}
