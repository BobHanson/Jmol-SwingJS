package jspecview.application;

import javax.swing.tree.TreePath;

import jspecview.api.JSVTreePath;

public class AwtTreePath extends TreePath implements JSVTreePath {

	public AwtTreePath(Object[] path) {
		super(path);
	}

}
