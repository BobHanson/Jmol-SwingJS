package fr.orsay.lri.varna.applications.newGUI;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.RNA;


public class Watcher  extends Thread {
	
	private VARNAGUITreeModel _model;
	private boolean _terminated = false; 
	
	public Watcher(VARNAGUITreeModel model)
	{
		_model = model;
	}
	
	public void run() {
		while (!_terminated)
		{
			ArrayList<String> folders = _model.getFolders();
			for (String path: folders)
			{
			  _model.addFolder(path);
			  System.out.println("Watching ["+path+"]");
			}
			try {
				this.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		
	}
	


	
	
	public void finish()
	{
		_terminated = true;
	}

}
