package fr.orsay.lri.varna.applications.newGUI;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.models.rna.RNA;

public class VARNAGUIModel implements Comparable<VARNAGUIModel> {
  private Date _lastModified;
  private boolean _outOfSync = false;
  private RNA _r = null;
  private String _caption = "";
  private String _path = "";
  private String _folder = "";

  
  public static Date lastModif(String path)
  {
	 return new Date(new File(path).lastModified()) ;
  }
  
  public VARNAGUIModel(String folder, String path)
  {
	this(folder,path,lastModif(path));
  }
  
  public VARNAGUIModel(String folder, String path,Date lastModified)
  {
	  _lastModified = lastModified;
	  _outOfSync = false;
	  _folder =folder;
	  _path = path;
	  String[] s = path.split(Pattern.quote(File.separator));
	  if (s.length>0)
	    _caption = s[s.length-1];
  }
  
  public boolean hasChanged()
  {
	  return _outOfSync;
  }
  
  public boolean checkForModifications()
  {
	if (!lastModif(_path).equals(_lastModified) && !_outOfSync)
	{
		_outOfSync = true;
		  return true;
	}
	return false;
  }
  
  
  public RNA getRNA()
  {
	  if (_r ==null)
	  {
		  try {
			createRNA();
		} catch (ExceptionUnmatchedClosingParentheses e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionFileFormatOrSyntax e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionExportFailed e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionPermissionDenied e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionLoadingFailed e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  return _r;
  }
  
  private RNA createRNA() throws ExceptionUnmatchedClosingParentheses, ExceptionFileFormatOrSyntax, FileNotFoundException, ExceptionExportFailed, ExceptionPermissionDenied, ExceptionLoadingFailed
  {
	  Collection<RNA> r = RNAFactory.loadSecStr(_path);
	  if (r.size()>0)
	  {
		  _r = r.iterator().next();
		  _r.drawRNARadiate();
	  }
	  else
	  {
		  throw new ExceptionFileFormatOrSyntax("No valid RNA defined in this file.");
	  }
	  return _r;
  }
  
  public String toString()
  {
	  return _caption + (this._outOfSync?"*":"");
  }
  
  public String getID()
  {
	  return getRNA().getID();
  }
  
  public String getCaption()
  {
	  return _caption;
  }

  public String getFolder()
  {
	  return _folder;
  }

  public static DataFlavor Flavor = new DataFlavor(VARNAGUIModel.class, "VARNA Object");


public int compareTo(VARNAGUIModel o) {
	return _caption.compareTo(o._caption);
}
  

  
}
