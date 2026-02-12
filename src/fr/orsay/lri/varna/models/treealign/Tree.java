package fr.orsay.lri.varna.models.treealign;
import java.util.*;


/**
 * An object of this class is a rooted tree, where children are ordered.
 * The tree is iterable, and the default iterator is DFS
 * (depth-first search), with the fathers given before the children.
 * 
 * @param <T> The type of values on nodes.
 * @author Raphael Champeimont
 */
public class Tree<T> implements Iterable<Tree<T>> {
	private List<Tree<T>> children;
	private T value;
	private Tree<T> tree = this;
	
	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	/**
	 * Returns the list of children.
	 * The return list has a O(1) access time to any of its elements
	 * (ie. it is like an array and unlike a linked list)
	 */
	public List<Tree<T>> getChildren() {
		return children;
	}
	
	/**
	 * This method replaces the list of children of a tree with the list given
	 * as argument. Be careful, because it means the list will be kept as a
	 * reference (it will not be copied) so if you later modify the list
	 * you passed as an argument here, it will modify the list of children.
	 * Note that the List object you give must have a 0(1) access time to
	 * elements (because someone calling getChildren() can expect that property).
	 * This method may be useful if you have already built a list of children
	 * and you don't want to use this.getChildren.addAll() to avoid a O(n) copy.
	 * In that case you would simply call the constructor that takes no argument
	 * to create an empty tree and then call replaceChildrenListBy(children)
	 * where children is the list of children you have built.
	 * @param children the new list of children
	 */
	public void replaceChildrenListBy(List<Tree<T>> children) {
		this.children = children;
	}

	/**
	 * Creates a tree, with the given set of children.
	 * The given set is any collection that implements Iterable<Tree>.
	 * The set is iterated on and its elements are copied (as references).
	 */
	public Tree(Iterable<Tree<T>> children) {
		this();
		for (Tree<T> child: children) {
			this.children.add(child);
		}
	}
	
	/**
	 * Creates a tree, with an empty list of children.
	 */
	public Tree() {
		children = new ArrayList<Tree<T>>();
	}
	
	/**
	 * Returns the number of children of the root node.
	 */
	public int rootDegree() {
		return children.size();
	}
	
	
	/**
	 * Count the nodes in the tree.
	 * Time: O(n)
	 * @return the number of nodes in the tree
	 */
	public int countNodes() {
		int count = 1;
		for (Tree<T> child: children) {
			count += child.countNodes();
		}
		return count;
	}
	
	/**
	 * Compute the tree degree, ie. the max over nodes of the node degree.
	 * Time: O(n)
	 * @return the maximum node degree
	 */
	public int computeDegree() {
		int max = children.size();
		for (Tree<T> child: children) {
			int maxCandidate = child.computeDegree();
			if (maxCandidate > max) {
				max = maxCandidate;
			}
		}
		return max;
	}
	
	/**
	 * Returns a string unique to this node.
	 */
	public String toGraphvizNodeId() {
		return super.toString();
	}
	
	
	public Iterator<Tree<T>> iterator() {
		return (new DFSPrefixIterator());
	}
	
	/**
	 * An iterator that returns the nodes in prefix (fathers before
	 * children) DFS (go deep first) order.
	 */
	public class DFSPrefixIterator implements Iterator<Tree<T>> {
		private LinkedList<Tree<T>> remainingNodes = new LinkedList<Tree<T>>();
		
		public boolean hasNext() {
			return !remainingNodes.isEmpty();
		}
		
		public Tree<T> next() {
			if (remainingNodes.isEmpty()) {
				throw (new NoSuchElementException());
			}
			Tree<T> currentNode = remainingNodes.getLast();
			remainingNodes.removeLast();
			List<Tree<T>> children = currentNode.getChildren();
			int n = children.size();
			// The children access is in O(1) so this loop is O(n)
			for (int i=n-1; i>=0; i--) {
				// We add the children is their reverse order so they
				// are given in the original order by the iterator
				remainingNodes.add(children.get(i));
			}
			return currentNode;
		}
		
		public DFSPrefixIterator() {
			remainingNodes.add(tree);
		}

		public void remove() {
			throw (new UnsupportedOperationException());
		}
	}
}
