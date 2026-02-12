package fr.orsay.lri.varna.models.treealign;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;



/**
 * This class translates a Tree to a graphviz file.
 * @author Raphael Champeimont
 *
 * @param <? extends GraphvizDrawableNodeValue> the type of values in the tree
 */
public class TreeGraphviz {

	/**
	 * Generates a PostScript file using graphviz.
	 * The dot command must be available.
	 */
	public static void treeToGraphvizPostscript(Tree<? extends GraphvizDrawableNodeValue> tree, String filename, String title) throws IOException {
		// generate graphviz source
		String graphvizSource = treeToGraphviz(tree, title);
		
		// open output file
		BufferedWriter fbw;
		fbw = new BufferedWriter(new FileWriter(filename));
    	
    	// execute graphviz
		Process proc = Runtime.getRuntime().exec("dot -Tps");
    	BufferedWriter bw  = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
    	BufferedReader br  = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    	BufferedReader bre = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    	bw.write(graphvizSource);
    	bw.close();
    	{
	    	String line = null;
            while ((line = br.readLine()) != null) {
            	fbw.write(line + "\n");
            }
    	}
    	{
	    	String line = null;
            while ((line = bre.readLine()) != null) {
            	System.err.println(line);
            }
    	}
    	
    	// wait for graphviz to end
        try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        // close file
		fbw.close();
	}
	
	/**
	 * Like treeToGraphvizPostscript(Tree,String,String) but with the title
	 * equal to the filename.
	 */
	public static void treeToGraphvizPostscript(Tree<? extends GraphvizDrawableNodeValue> tree, String filename) throws IOException {
		treeToGraphvizPostscript(tree, filename, filename);
	}
	
	/**
	 * Creates a graphviz source file from a Tree.
	 * @param title the title of the graph
	 */
	public static void treeToGraphvizFile(Tree<? extends GraphvizDrawableNodeValue> tree, String filename, String title) throws IOException {
		BufferedWriter bw;
		bw = new BufferedWriter(new FileWriter(filename));
    	bw.write(treeToGraphviz(tree, filename));
    	bw.close();
	}
	
	/**
	 * Like treeToGraphvizFile(Tree,String,String) but with the title
	 * equal to the filename.
	 */
	public static void treeToGraphvizFile(Tree<? extends GraphvizDrawableNodeValue> tree, String filename) throws IOException {
		treeToGraphvizFile(tree, filename, filename);
	}
	
	/**
	 * Creates a graphviz source from a Tree.
	 * @param title the title of the graph
	 */
	public static String treeToGraphviz(Tree<? extends GraphvizDrawableNodeValue> tree, String title) {
		return "digraph \"" + title + "\" {\n" + subtreeToGraphviz(tree) + "}\n";
	}
	
	private static String subtreeToGraphviz(Tree<? extends GraphvizDrawableNodeValue> tree) {
		String s = "";
		String myId = tree.toGraphvizNodeId();
		
		s +=
			"\""
			+ myId
			+ "\" [label=\""
			+ ((tree.getValue() != null) ? tree.getValue().toGraphvizNodeName() : "null")
			+ "\"]\n";
		for (Tree<? extends GraphvizDrawableNodeValue> child: tree.getChildren()) {
			s += "\"" + myId + "\" -> \"" + child.toGraphvizNodeId() + "\"\n";
			s += subtreeToGraphviz(child);
		}
		
		return s;
	}
}
