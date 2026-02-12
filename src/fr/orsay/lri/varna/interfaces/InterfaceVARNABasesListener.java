package fr.orsay.lri.varna.interfaces;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;

public interface InterfaceVARNABasesListener {

	/**
	 * Reacts to click over base
	 * @param mb The base which has just been clicked
	 */
	public void onBaseClicked(ModeleBase mb, MouseEvent e);
}
