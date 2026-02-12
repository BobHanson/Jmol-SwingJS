package fr.orsay.lri.varna.applications.fragseq;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
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

public class FragSeqRNASecStrModel extends FragSeqModel {
 
	private RNA _r =null;
	
  public FragSeqRNASecStrModel(RNA r)
  {
	_r = r;
  }


  public String toString()
  {
	  return _r.getName();
  }
  
  public String getID()
  {
	  return _r.getID();
  }
  

  public RNA getRNA()
  {
	  return _r;
  }
  public static DataFlavor Flavor = new DataFlavor(FragSeqRNASecStrModel.class, "RNA Sec Str Object");

}
