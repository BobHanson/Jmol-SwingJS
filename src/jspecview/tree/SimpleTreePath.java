package jspecview.tree;

import jspecview.api.JSVTreePath;

public class SimpleTreePath implements JSVTreePath {

	private Object[] path;

	public SimpleTreePath(Object[] path) {
		this.path = path;
	}

	@Override
	public Object getLastPathComponent() {
		return (path == null || path.length == 0 ? null : path[path.length - 1]);
	}
	
}
