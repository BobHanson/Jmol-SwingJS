package fr.orsay.lri.varna.applications.templateEditor;


public class Couple<T,U> {
	public T first;
	public U second;
	private static final int HASH_PRIME = 1000003;
    
	
	public Couple(T a, U b)
	{
		first = a;
		second = b;
	}

	public boolean equals( Object c)
	{
		if (!(c instanceof Couple))
		{
			return false;
		}
		Couple<T,U> cc = (Couple<T,U>) c; 
		return (cc.first.equals(first) && (cc.second.equals(second)));
	}

	public int hashCode()
	{
		return HASH_PRIME*first.hashCode()+second.hashCode();
	}
	
	public String toString()
	{
		return "("+first+","+second+")";
	}
}
