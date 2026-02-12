package fr.orsay.lri.varna.models.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.orsay.lri.varna.applications.templateEditor.Couple;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;

/**
 * A RNATemplateMapping is a mapping between bases in an RNA sequence
 * and elements in a RNA template.
 * A base is mapped to only one template element
 * but a template element can be mapped to several bases.
 * This class is designed to be similar to the Mapping class.
 * 
 * @author Raphael Champeimont
 */
public class RNATemplateMapping {
	private Map<Integer, RNATemplateElement> map = new HashMap<Integer, RNATemplateElement>();
	private Map<RNATemplateElement, ArrayList<Integer>> invmap = new HashMap<RNATemplateElement, ArrayList<Integer>>();
	
	/**
	 * Alignment distance.
	 */
	private double distance;
	
	
	public double getDistance() {
		return distance;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	/**
	 * Tell this mapping object that this base index and this element are
	 * mapped with each other. This will throw RNATemplateMappingException
	 * and do nothing if the base index is already in the mapping.
	 */
	public void addCouple(int baseIndex, RNATemplateElement templateElement) throws RNATemplateMappingException {
		if (map.containsKey(baseIndex)) {
			throw (new RNATemplateMappingException("Base index already in mapping: " + baseIndex));
		}
		if (baseIndex < 0) {
			throw (new RNATemplateMappingException("Invalid base index: " + baseIndex));
		}
		map.put(baseIndex, templateElement);
		if (!invmap.containsKey(templateElement)) {
			invmap.put(templateElement, new ArrayList<Integer>());
		}
		invmap.get(templateElement).add(baseIndex);
	}


	public String showCompact(RNA r)
	{
		HashMap<String,Couple<Integer,Integer> > ranges = new HashMap<String,Couple<Integer,Integer> >();
		for(int i:map.keySet())
		{
			RNATemplateElement t = map.get(i);
			String k = t.getName();
			if (t instanceof RNATemplate.RNATemplateHelix)
			{
				k += " (" + ((RNATemplateHelix) t).getCaption() + ")";
				ModeleBase mb = r.getBaseAt(i);
				if (mb.getElementStructure()>i)
				  k = k+":5'";
				else
				  k = k+":3'";
			}
			if (!ranges.containsKey(k))
			{  ranges.put(k, new Couple<Integer,Integer>(Integer.MAX_VALUE,Integer.MIN_VALUE));  }
			Couple<Integer,Integer> c = ranges.get(k);
			c.first = Math.min(c.first, i);
			c.second = Math.max(c.second, i);			
		}
		String result = "";
		for(String k:ranges.keySet())
		{
			Couple<Integer,Integer> c = ranges.get(k);
			RNATemplateElement t = map.get(c.first);
			String type = ((t instanceof RNATemplate.RNATemplateHelix)?"strand":"loop");
			if (t instanceof RNATemplate.RNATemplateHelix)
			{
				if (k.endsWith("5'"))
				{
					Couple<Integer,Integer> c3 = ranges.get(k.replace("5'", "3'"));
					result += "dummyID\t1\t"+k.replace(":5'", "")+"\t"+type+"\t"+c.first+"-"+c.second+":"+c3.first+"-"+c3.second+"\n";
				}
			}
			else if (t instanceof RNATemplate.RNATemplateUnpairedSequence)
			{
			  result += "dummyID\t1\t"+k+"\t"+type+"\t"+c.first+"-"+c.second+"\n";
			}
		}
		result += "alignment distance = " + distance;
		return result;
	}
	
	
	/**
	 * If the given base index is in the mapping, return the
	 * corresponding template element, otherwise return null.
	 */
	public RNATemplateElement getPartner(int baseIndex) {
		if (map.containsKey(baseIndex)) {
			return map.get(baseIndex);
		} else {
			return null;
		}
	}
	
	
	
	/**
	 * If the given template element is in the mapping, return an ArrayList
	 * containing the corresponding base indexes, otherwise return null.
	 * Note that you should not modify the returned ArrayList because
	 * no copy is made, so if you modify it this mapping object will
	 * contain inconsistent data.
	 */
	public ArrayList<Integer> getAncestor(RNATemplateElement templateElement) {
		if (invmap.containsKey(templateElement)) {
			return invmap.get(templateElement);
		} else {
			return null;
		}
	}
	
	
	
	/**
	 * Return a set containing all the base indexes in the mapping.
	 * You should not modify the returned set.
	 */
	public Set<Integer> getSourceElemsAsSet() {
		return map.keySet();
	}
	
	
	
	/**
	 * Return a set containing all the template elements in the mapping.
	 * You should not modify the return set.
	 */
	public Set<RNATemplateElement> getTargetElemsAsSet() {
		return invmap.keySet();
	}
	
	
	
}
