package fr.orsay.lri.varna.models.treealign;

public interface TreeAlignLabelDistanceAsymmetric<ValueType1, ValueType2> {
	/** 
	 * Returns the substitution cost between v1 and v2.
	 * We use the convention that a null reference is a blank,
	 * ie. f(x, null) is the cost of deleting x
	 * and f(null, x) is the cost of inserting x.
	 * We won't use f(null, null).
	 * We suppose f is such that:
	 * f(x,x) = 0
	 * 0 <= f(x,y) < +infinity
	 * You may also want to have the triangle inequality,
	 * although the alignment algorithm does not require it:
	 * f(x,z) <= f(x,y) + f(y,z)
	 */
	public double f(ValueType1 x, ValueType2 y);
}
