package javajs.api;

/**
 * GenericColor allows both java.awt.Color and org.jmol.awtjs.Color to be
 * handled by methods that need not distinguish between them. It is used
 * in the javajs package for the background color of a org.jmol.awtjs.swing.JComponent
 * 
 * @author hansonr
 *
 */
public interface GenericColor {

	int getRGB();

	int getOpacity255();

	void setOpacity255(int a);
	
}
