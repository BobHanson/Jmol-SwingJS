package fr.orsay.lri.varna.models.rna;

import java.util.ArrayList;

public class ModeleStrand {
	
	private ArrayList<ModeleBase> _strand = new ArrayList<ModeleBase>();
	private boolean hasBeenPlaced = false;
	private boolean strandLeft = false;
	private boolean strandRight = false;
	private int levelPosition;
	
	public ModeleStrand(){
		
	}
	
	public void addBase(ModeleBase mb){
		this._strand.add(mb);
	}
	
	public void addBase(int index, ModeleBase mb){
		this._strand.add(index, mb);
	}
	
	public int sizeStrand() {
		return this._strand.size();
	}
	
	public ModeleBase getMB(int a) {	
		return this._strand.get(a);	
	}
	
	public ArrayList<ModeleBase> getArrayListMB() {	
		return this._strand;	
	}
	
	public int getLevelPosition(){
		return this.levelPosition;
	}
	
	public void setLevelPosition(int a){
		this.levelPosition=a;
	}
	
	public boolean getStrandRight(){
		return this.strandRight;
	}
	
	public void setStrandRight(boolean bool){
		this.strandRight=bool;
	}
	
	public boolean getStrandLeft(){
		return this.strandLeft;
	}
	
	public void setStrandLeft(boolean bool){
		this.strandLeft=bool;
	}
	
	public boolean hasBeenPlaced(){
		return this.hasBeenPlaced;
	}
	
	public void setHasBeenPlaced(boolean bool){
		this.hasBeenPlaced =bool;
	}
	
	public boolean existInStrand(int a){
		int size =sizeStrand();
		boolean exist=false; 
		for (int i=0; i<size;i++){
			if(a==this.getMB(i).getIndex()){
				exist=true;
			}
		}
		return exist;
	}
}