package fr.orsay.lri.varna.models;

import java.io.Serializable;

import fr.orsay.lri.varna.models.rna.RNA;

public class FullBackup implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5468893731117925140L;
    public VARNAConfig config; 
    public RNA rna;
    public String name; 
    
    public FullBackup(VARNAConfig c, RNA r, String n){
    	config = c;
    	rna = r;
    	name = n;
    }

    public FullBackup(RNA r, String n){
    	config = null;
    	rna = r;
    	name = n;
    }
    
    public String toString()
    {
    	return name;
    }
    public boolean hasConfig()
    {
    	return config!=null;
    }
    
}
