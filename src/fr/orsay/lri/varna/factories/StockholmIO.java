package fr.orsay.lri.varna.factories;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class StockholmIO {
	public static RNAAlignment readAlignementFromFile(String path) throws IOException
	{
		return StockholmIO.readAlignement(new BufferedReader(new FileReader(path)));
	}
	public static RNAAlignment readAlignementFromURL(String url) throws UnsupportedEncodingException, IOException
	{
		URL urlAb = new URL(url);
		URLConnection urlConn = urlAb.openConnection(); 
		urlConn.setUseCaches(false);
		Reader r = new InputStreamReader(urlConn.getInputStream(),"UTF-8");
		return readAlignement(new BufferedReader(r));
	}

	
	/*public static Alignment readAlignement(Reader r) throws IOException
	{
		return readAlignement(new BufferedReader(r));
	}*/
	
	public static RNAAlignment readAlignement(BufferedReader r) throws IOException
	{
		LinkedHashMap<String,StringBuffer> rawSeqs = new LinkedHashMap<String,StringBuffer>();
		RNAAlignment result = new RNAAlignment();
		String line = r.readLine(); 
		String str = "";

		while(line!=null)
		{
			if (!line.startsWith("#"))
			{
				String[] data = line.split("\\s+");
				if (data.length>1)
				{
					String seqName = data[0].trim();
					String seq = data[1].trim();
					if (!rawSeqs.containsKey(seqName))
					{
						rawSeqs.put(seqName,new StringBuffer());
					}
					StringBuffer val =  rawSeqs.get(seqName);
					val.append(seq);
				}
				
			}
			else if (line.startsWith("#"))
			{
				String[] data = line.split("\\s+");
				if (line.startsWith("#=GC SS_cons"))
				{
					str += data[2].trim();	
				}
				else if (line.startsWith("#=GS"))
				{
					if (data[2].trim().equals("AC"))
					{
					  result.setAccession(data[1].trim(),data[3].trim());
					  
					}
				}
			}
				
			line = r.readLine(); 
		}
		result.setSecStr(str);
		for(Map.Entry<String,StringBuffer> entry : rawSeqs.entrySet())
		{
			String s = entry.getValue().toString();
			result.addSequence(entry.getKey(), s);
		}
		return result;
	}
}
