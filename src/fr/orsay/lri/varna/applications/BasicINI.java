package fr.orsay.lri.varna.applications;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

public class BasicINI {

	private Hashtable<String,Hashtable<String,String>> _data = new Hashtable<String,Hashtable<String,String>>();


	public void addItem(String category, String key, String val)
	{
		if (!_data.containsKey(category))
		{
			_data.put(category, new Hashtable<String,String>());
		}
		System.out.println("[E]"+key+"->"+val);
		_data.get(category).put(key,val);
	}


	public String getItem(String category, String key)
	{
		String result = "";
		if (_data.containsKey(category))
		{
			if (_data.get(category).containsKey(key))
			{
				result = _data.get(category).get(key);
			}
		}
		return result;
	}

	public ArrayList<String> getItemList(String category)
	{
		ArrayList<String> result = new ArrayList<String>();
		if (_data.containsKey(category))
		{
			for (String key: _data.get(category).keySet())
			{
				result.add(_data.get(category).get(key));
			}
		}
		return result;
	}

	public BasicINI(){

	}

	public static void saveINI(BasicINI data, String filename)  
	{
		try
		{
			FileWriter out = new FileWriter(filename);
			Set<String> cats = data._data.keySet();
			String[] sortedCats = new String[cats.size()];
			sortedCats = cats.toArray(sortedCats); 
			Arrays.sort(sortedCats);
			for (int i=0;i<sortedCats.length;i++)
			{
				String cat = sortedCats[i];      	
				out.write("["+cat+"]\n"); 
				Hashtable<String,String> vals = data._data.get(cat);
				Set<String> keys = vals.keySet();
				String[] sortedKeys = new String[keys.size()];
				sortedKeys = keys.toArray(sortedKeys); 
				for(int j=0;j<sortedKeys.length;j++)
				{
					String key = sortedKeys[j];
					String val = vals.get(key);
					out.write(key+"="+val+"\n");		        	
				}
			}
			out.close();
		}
		catch(Exception e3)
		{e3.printStackTrace();}
	}

	public static BasicINI loadINI(String filename)  
	{
		BasicINI result = new BasicINI();    

		// Etats du parsing simplifie ...
		final int CATEGORY = 0;
		final int KEY = 1;
		final int VAL = 2;
		int state = KEY;
		String category = "";
		String key = "";
		String val = "";

		try
		{
			System.out.println("Loading "+new File(filename).getAbsolutePath());

			Reader r = new FileReader(filename);
			StreamTokenizer s = new StreamTokenizer(r);
			s.resetSyntax();
			s.eolIsSignificant(true);
			s.wordChars('\u0000','\u00FF');
			s.whitespaceChars('\u0000','\u000F');
			s.ordinaryChar('[');
			s.ordinaryChar(']');
			s.ordinaryChar('=');
			int token = s.nextToken();
			while(token != StreamTokenizer.TT_EOF)
			{
				switch(token)
				{
				case('[') :
				{
					state = CATEGORY;
				}
				break;
				case(']') :
				{
					state = KEY;
				}
				break;
				case('=') :
				{
					state = VAL;
				}
				break;
				case(StreamTokenizer.TT_EOL) :
				{
					if (state==VAL)
					{
					  state = KEY;
					  result.addItem(category, key, val);
					  key="";
					  val="";
					}
				}
				case(StreamTokenizer.TT_WORD) :
				{
					String word = s.sval;
					switch(state)
					{
					case(CATEGORY) :
					{
						category = word;
					}
					break;
					case(KEY) :
					{
						key = word;
					}
					break;
					case(VAL) :
					{
						val = word;
					}
					break;
					}
				}
				break;
				}
				token = s.nextToken();
			}
		}
		catch(Exception exc1)
		{exc1.printStackTrace();}
		return result;
	}


}
