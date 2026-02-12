package fr.orsay.lri.varna.models;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.orsay.lri.varna.models.rna.ModeleBase;

public class BaseList {
	private HashSet<ModeleBase> _bases = new HashSet<ModeleBase>(); 
	private String _caption;

	public BaseList( BaseList b)
	{
		_caption = b._caption;
		_bases = new HashSet<ModeleBase>(b._bases);
	}

	
	public BaseList( String caption)
	{
		_caption = caption;
	}
	
	
	public BaseList( String caption, ModeleBase mb)
	{
		this(caption);
		addBase(mb);
	}

	public boolean contains(ModeleBase mb)
	{
		return _bases.contains(mb);
	}

	
	public String getCaption()
	{
		return _caption;
	}
	
	public void addBase(ModeleBase b)
	{
		_bases.add(b);
	}

	public void removeBase(ModeleBase b)
	{
		_bases.remove(b);
	}

	
	public void addBases(Collection<? extends ModeleBase> mbs)
	{
		_bases.addAll(mbs);
	}

	public ArrayList<ModeleBase> getBases()
	{
		return new ArrayList<ModeleBase>(_bases);
	}

	public void clear()
	{
		_bases.clear();
	}

	public static Color getAverageColor(ArrayList<Color> cols)
	{
		int r=0,g=0,b=0;
		for (Color c : cols)
		{
			r += c.getRed();
			g += c.getGreen();
			b += c.getBlue();
		}
		if (cols.size()>0)
		{ 
			r /= cols.size();
			g /= cols.size();
			b /= cols.size();
		}
		return new Color(r,g,b);
	}
	
	public Color getAverageOutlineColor()
	{
		ArrayList<Color> cols = new ArrayList<Color>(); 
		for (ModeleBase mb : _bases)
		{  cols.add(mb.getStyleBase().getBaseOutlineColor()); }
		return getAverageColor(cols);
	}

	public Color getAverageNameColor()
	{
		ArrayList<Color> cols = new ArrayList<Color>(); 
		for (ModeleBase mb : _bases)
		{  cols.add(mb.getStyleBase().getBaseNameColor()); }
		return getAverageColor(cols);
	}

	public Color getAverageNumberColor()
	{
		ArrayList<Color> cols = new ArrayList<Color>(); 
		for (ModeleBase mb : _bases)
		{  cols.add(mb.getStyleBase().getBaseNumberColor()); }
		return getAverageColor(cols);
	}

	public Color getAverageInnerColor()
	{
		ArrayList<Color> cols = new ArrayList<Color>(); 
		for (ModeleBase mb : _bases)
		{  cols.add(mb.getStyleBase().getBaseInnerColor()); }
		return getAverageColor(cols);
	}

	public String getNumbers()
	{
		String result = ""; 
		boolean first = true;
		for (ModeleBase mb:_bases)
		{  
			if (!first)
			{ result += ","; }
			else
			{ first = false; }
			result += "" + mb.getBaseNumber(); 
		}
		result += "";
		return result;
	}

	public String getContents()
	{
		String result = ""; 
		boolean first = true;
		for (ModeleBase mb:_bases)
		{  
			if (!first)
			{ result += ","; }
			else
			{ first = false; }
			result += "" + mb.getContent(); 
		}
		result += "";
		return result;
	}

	public ArrayList<Integer> getIndices()
	{
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (ModeleBase mb : _bases)
		{
			indices.add(mb.getIndex());
		}
		return indices;
	}
	
	/**
	 * Returns, in a new BaseList, the intersection of the current BaseList and of the argument.
	 * @param mb The base list to be used for the intersection
	 * @return The intersection of the current base list and the argument.
	 */
	
	public BaseList retainAll(BaseList mb)
	{
		HashSet<ModeleBase> cp = new HashSet<ModeleBase>();
		cp.addAll(_bases);
		cp.retainAll(mb._bases);
		BaseList result = new BaseList("TmpIntersection");
		result.addBases(cp);
		return result;
	}

	/**
	 * Returns, in a new BaseList, the list consisting of the current BaseList minus the list passed as argument.
	 * @param mb The base list to be subtracted from the current one
	 * @return The current base list minus the list passed as argument.
	 */
	
	public BaseList removeAll(BaseList mb)
	{
		HashSet<ModeleBase> cp = new HashSet<ModeleBase>();
		cp.addAll(_bases);
		cp.removeAll(mb._bases);
		BaseList result = new BaseList("TmpMinus");
		result.addBases(cp);
		return result;
	}	
	
	public int size()
	{
		return _bases.size();
	}

	
}
