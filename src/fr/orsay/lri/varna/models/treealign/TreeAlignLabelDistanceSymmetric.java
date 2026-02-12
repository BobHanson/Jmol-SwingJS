package fr.orsay.lri.varna.models.treealign;

/**
 * Same as TreeAlignLabelDistanceAsymmetric, but elements on both
 * trees are of the same type, and the function is symmetric, ie.
 * f(x,y) = f(y,x) .
 *
 * @param <ValueType>
 */
public interface TreeAlignLabelDistanceSymmetric<ValueType> extends TreeAlignLabelDistanceAsymmetric<ValueType, ValueType> {
	
}
