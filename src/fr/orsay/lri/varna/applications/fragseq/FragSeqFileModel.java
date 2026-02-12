package fr.orsay.lri.varna.applications.fragseq;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.models.rna.RNA;

public class FragSeqFileModel implements Comparable<FragSeqFileModel> {
	private ArrayList<FragSeqModel> _models = new ArrayList<FragSeqModel>();
	  protected Date _lastModified;
	  protected boolean _outOfSync = false;
	  protected String _caption = "";
	  protected String _path = "";
	  protected String _folder = "";
	  protected boolean _cached = false;

	  
	  public static Date lastModif(String path)
	  {
		 return new Date(new File(path).lastModified()) ;
	  }
	  
	  public FragSeqFileModel(String folder, String path)
	  {
		this(folder,path,lastModif(path));
	  }
	  
	  
	  private static Random _rnd = new Random();
	  
	  public FragSeqFileModel(String folder, String path,Date lastModified)
	  {
		  _lastModified = lastModified;
		  _outOfSync = false;
		  _folder =folder;
		  _path = path;
		  String[] s = path.split(Pattern.quote(File.separator));
		  if (s.length>0)
		    _caption = s[s.length-1];
	  }
	  
	  public void load()
	  {
		  ArrayList<RNA> rnas = null;
		  try {
			  rnas = createRNAs();
			for (RNA r: rnas)
			{  
				this.addModel(new FragSeqRNASecStrModel(r));
				  int nb =_rnd.nextInt(5); 
				  for(int i=0;i<nb;i++)
				  {
					  FragSeqAnnotationDataModel data = new FragSeqAnnotationDataModel(r.getID(),""+i+"-"+r.getID());
					  FragSeqAnnotationDataModel.addRandomAnnotations(r,data);
					  addModel(data);
				  }		  
			}
		} catch (ExceptionUnmatchedClosingParentheses e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExceptionFileFormatOrSyntax e) {
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
		  _cached = true;
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
	  
	  
	  public String toString()
	  {
		  return _caption + (this._outOfSync?"*":"");
	  }
	  
	  
	  public String getCaption()
	  {
		  return _caption;
	  }

	  public String getFolder()
	  {
		  return _folder;
	  }

	  public String getPath()
	  {
		  return _path;
	  }


	public int compareTo(FragSeqFileModel o) {
		return _caption.compareTo(o._caption);
	}
	  
	public ArrayList<FragSeqModel> getModels()
	{
		if (!_cached)
		{  load();  }
		return _models;
	}
	public void addModel(FragSeqModel f)
	{
		_models.add(f);
	}

	
	private ArrayList<RNA> createRNAs() throws ExceptionUnmatchedClosingParentheses, ExceptionFileFormatOrSyntax, FileNotFoundException, ExceptionExportFailed, ExceptionPermissionDenied, ExceptionLoadingFailed
	  {
		  Collection<RNA> r = RNAFactory.loadSecStr(_path);
		  for (RNA r2 : r)
		  {
			  r2.drawRNARadiate();
		  }
		  return new ArrayList<RNA>(r);
	  }
	  
}
