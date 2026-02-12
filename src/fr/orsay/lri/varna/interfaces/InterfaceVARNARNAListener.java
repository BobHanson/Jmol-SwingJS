package fr.orsay.lri.varna.interfaces;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import fr.orsay.lri.varna.models.rna.ModeleBP;

public interface InterfaceVARNARNAListener {
	/**
	 * Reacts to changes being made at the sequence level. 
	 * @param index The sequence index where a change of base content is observed
	 * @param oldseq Previous base content   
	 * @param newseq New base content
	 */
	public void onSequenceModified(int index, String oldseq, String newseq);
	
	/**
	 * Reacts to modification of the structure (Base-pair addition/removal).
	 * @param current Current list of base-pairs (can be also accessed within the current RNA object).
	 * @param addedBasePairs Newly created base-pairs
	 * @param removedBasePairs Newly removed base-pairs
	 */
	public void onStructureModified(Set<ModeleBP> current, Set<ModeleBP> addedBasePairs, Set<ModeleBP> removedBasePairs);
	
	/**
	 * Reacts to displacement of 
	 * @param previousPositions
	 */
	public void onRNALayoutChanged(Hashtable<Integer,Point2D.Double> previousPositions);
}
