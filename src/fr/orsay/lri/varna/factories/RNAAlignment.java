package fr.orsay.lri.varna.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Stack;

import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * BH SwingJS -- must explicitly check for array out of bounds
 */
public class RNAAlignment {
   private ArrayList<String> _lst = new ArrayList<String> (); 
   private Hashtable<String, Integer> _index = new Hashtable<String, Integer> ();
   private Hashtable<String, String> _accession = new Hashtable<String, String> ();
   private String _secStr = "";
   
   public void addSequence(String id, String s)
   {
	   if (!_index.containsKey(id))
	   {
		   _index.put(id,_lst.size());
		   _lst.add(s);
	   }
	   _lst.set(_index.get(id),s);
   }
   
   public void setSecStr(String s)
   {
	   _secStr = s;
   }
   
  public void setAccession(String id, String AC)
  {
	  _accession.put(id,AC);
  }
   
   public ArrayList<RNA> getRNAs() throws ExceptionUnmatchedClosingParentheses
   {
	   ArrayList<RNA> result = new ArrayList<RNA>(); 
	   int[] str = RNAFactory.parseSecStr(_secStr);
	   ArrayList<String> ids = new ArrayList<String>(_index.keySet());
	   Collections.sort(ids,new Comparator<String>(){
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}});
	   for (String id: ids )
	   {
		   int n = _index.get(id);
		   String seq = _lst.get(n);
		   if (seq.length() != str.length)
			   throw new ArrayIndexOutOfBoundsException(); // BH SwingJS -- must explicitly check for array out of bounds
		   String nseq ="";
		   String nstr ="";
		   for(int i=0;i<seq.length();i++)
		   {
			   char c = seq.charAt(i);
			   int j = str[i];
			   
			   if (!(c=='.' || c==':' || c=='-'))
			   {
				   nseq += c;
				   if (j==-1)
				   {
					   nstr += '.';
				   }
				   else
				   {
					   int cp = seq.charAt(j);
					   if (cp=='.' || cp==':' || cp=='-')
					   {
						   nstr += '.';						   
					   }
					   else
					   {
						   nstr += _secStr.charAt(i);
					   }
				   }
			   }
		   }
		   RNA r = new RNA();
		   try {
			r.setRNA(nseq, nstr);
			r.setName(id);
			if (_accession.containsKey(id))
			{
				r.setID(_accession.get(id));
			}
			else
			{
				r.setID(id);
			}
			result.add(r);
		} catch (ExceptionFileFormatOrSyntax e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   return result;
   }
}
