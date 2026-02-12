package fr.orsay.lri.varna.applications;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

public class FileNameExtensionFilter extends FileFilter {

    Hashtable<String,Integer> _exts = new Hashtable<String,Integer>();
    String _desc = "";
	
	public FileNameExtensionFilter(String desc,String ext1)
	{
		_desc = desc;
		_exts.put(ext1,0);
	}

	public FileNameExtensionFilter(String desc,String ext1,String ext2)
	{
		this(desc,ext1);
		_exts.put(ext2,1);
	}
	
	public FileNameExtensionFilter(String desc,String ext1,String ext2,String ext3)
	{
		this(desc,ext1,ext2);
		_exts.put(ext3,2);
	}

	public FileNameExtensionFilter(String desc,String ext1,String ext2,String ext3,String ext4)
	{
		this(desc,ext1,ext2,ext3);
		_exts.put(ext4,3);
	}
	
	
	public boolean accept(File path) {
		String name = path.getName();
		if (path.isDirectory())
			return true;
		int index = name.lastIndexOf(".");
		if (index != -1)
		{
		  String suffix = name.substring(index+1);
		  if (_exts.containsKey(suffix))
		  {return true;}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return _desc;
	}
	
	public String[] getExtensions()
	{
		String[] exts = new String[_exts.size()];
		Enumeration<String> k = _exts.keys();
		int n = 0;
		while(k.hasMoreElements())
		{
			exts[n] = k.nextElement();
			n++;
		}
		return exts;
	}	

}
