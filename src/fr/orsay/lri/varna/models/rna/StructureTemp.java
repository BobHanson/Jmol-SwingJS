package fr.orsay.lri.varna.models.rna;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class StructureTemp  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -436852923461989105L;
	private ArrayList<ModeleStrand> _struct = new ArrayList<ModeleStrand>();
	
	public StructureTemp(){
		
	}
	
	public void addStrand(ModeleStrand ms){
		this._struct.add(ms);
		
	}
	
	public int sizeStruct() {
		return this._struct.size();
	}
	
	public ModeleStrand getStrand(int a) {
		return this._struct.get(a);
	}

	public ArrayList<ModeleStrand> getListStrands() {
		return _struct;
	}

	public void clearListStrands() {
		this._struct.clear();
	}
	
	public boolean isEmpty() {
		return this._struct.isEmpty();
	}

}
