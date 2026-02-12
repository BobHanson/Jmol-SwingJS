package fr.orsay.lri.varna.applications.fragseq;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class FragSeqTreeModel extends DefaultTreeModel implements TreeWillExpandListener{
	
	  private FragSeqNode _rootIDs = new FragSeqNode("IDs");
	  private FragSeqNode _rootFolders = new FragSeqNode("Folders");

	private TreeSet<String> _folders = new TreeSet<String>();
  private Hashtable<String,FragSeqNode> _folderPathToFolderNode = new Hashtable<String,FragSeqNode>();

  private Hashtable<String,FragSeqNode> _idsToNode = new Hashtable<String,FragSeqNode>();
  private Hashtable<String,ArrayList<FragSeqNode>> _pathToIDFileNodes = new Hashtable<String,ArrayList<FragSeqNode>>();
  
  public enum SORT_MODE{
	  PATH,
	  ID
  }
  
  private SORT_MODE _mode = SORT_MODE.PATH; 
	
  public FragSeqTreeModel()
  {
	  this(new FragSeqNode("Folders"));  
	  
  }
  public FragSeqTreeModel(TreeNode t)
  {
	  super(t);
	  this.setRoot(_rootFolders);
  }
  
  
  public FragSeqNode getPathViewRoot()
  {
	  return _rootFolders;
  }

  public FragSeqNode getIDViewRoot()
  {
	  return _rootIDs;
  }

  public void switchToIDView()
  {
	  if (_mode!=SORT_MODE.ID)
	  {
		  this.setRoot(this._rootIDs);
		  
	  }
	  _mode=SORT_MODE.ID;
	  
  }

  private void removeAllNodes(ArrayList<FragSeqNode> toBeRemoved)
  {
	  for(FragSeqNode leafNode : toBeRemoved)
	  {
		  FragSeqNode parent = (FragSeqNode) leafNode.getParent();
		  parent.remove(leafNode);
		  if (parent.getChildCount()==0)
		  {
			  parent.removeFromParent();
			  _folderPathToFolderNode.remove(parent);
			  if (parent.getUserObject() instanceof String)
			  {
				  String path = parent.getUserObject().toString();
			  }
		  }
		  else
		  {
			  reload(parent);
		  }
	  }	  
  }

  
  public FragSeqNode getNodeForId(String id)
  {
	  if(!_idsToNode.containsKey(id))
	  {
		  FragSeqNode idNode = new FragSeqNode(id);
		  _idsToNode.put(id, idNode);
		  _rootIDs.add(idNode);
	  }
	  FragSeqNode idNode = _idsToNode.get(id);
	  return idNode;
  }

  public void removeFolder(String path)
  {
	  ArrayList<FragSeqNode> toBeRemoved = new ArrayList<FragSeqNode>(); 
	  Enumeration en = _folderPathToFolderNode.get(path).children();
	  while(en.hasMoreElements())
	  {
		  FragSeqNode n = (FragSeqNode) en.nextElement();
		  toBeRemoved.add(n);
	  }
	  removeAllNodes(toBeRemoved);
	  _folders.remove(path);
  }
  
  
  public FragSeqNode insertGroupNode(String crit, TreeSet<String> t)
  {
	  FragSeqNode groupNode = new FragSeqNode(crit);
	  FragSeqNode parent = getRoot();
	  int pos = t.headSet(crit).size();
	  parent.insert(groupNode, pos);	 
	  reload(groupNode);
	  return groupNode;
  }
  

  
  public void insertFileNode(FragSeqNode parent, FragSeqFileModel m)
  {
	  FragSeqNode leafNode = new FragSeqNode(m);
	  parent.add(leafNode);
  }

  public FragSeqNode addFolder(String path)
  {
	  FragSeqNode groupNode = null;
	  try {
		  if (!_folders.contains(path))
		  {
			  File dir = new File(path);
			  if (dir.isDirectory())
			  {
				  path = dir.getCanonicalPath();
				  _folders.add(path);
				  groupNode = insertGroupNode(path, _folders);
				  _folderPathToFolderNode.put(path,groupNode);
				  for(File f:dir.listFiles(_f))
				  {
					  addFile(path,f.getCanonicalPath());
				  }
			  }
		  }
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
	  return groupNode;
  }
    
  private void addFile(String folder, String path)
  {
	  System.out.println("  => "+path);
	  FragSeqFileModel m = new FragSeqFileModel(folder,path);
	  addFolder(folder);
	  insertFileNode(_folderPathToFolderNode.get(folder), m);
  }
  
  public FragSeqNode getRoot()
  {
	  return (FragSeqNode) super.getRoot();
  }
  
  public ArrayList<String> getFolders()
  {
	  ArrayList<String> result = new ArrayList<String>(_folders);
	  return result;
  }
  
  
   FilenameFilter _f = new FilenameFilter(){
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".dbn") 
			|| name.toLowerCase().endsWith(".ct")
			|| name.toLowerCase().endsWith(".bpseq")
			|| name.toLowerCase().endsWith(".rnaml");	
		}};
	  
  public FilenameFilter getFileNameFilter()
  {
	return _f;
  }

  public void setFileNameFilter(FilenameFilter f)
  {
	_f = f;
  }

  
private Hashtable<FragSeqNode,Boolean> _isExpanded = new Hashtable<FragSeqNode,Boolean>();
  
public boolean isExpanded(FragSeqNode n)
{
  if(_isExpanded.containsKey(n))
  {
	return _isExpanded.get(n);
  }
  else
	  return false;
}

public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
	if (event.getSource() instanceof FragSeqTree)
	{
		FragSeqTree tree = (FragSeqTree) event.getSource();
		TreePath t = event.getPath();
		FragSeqNode n = (FragSeqNode) t.getLastPathComponent();
		_isExpanded.put(n, true);
		Object o = n.getUserObject();
		if (o instanceof FragSeqFileModel)
		{
			FragSeqFileModel f = (FragSeqFileModel) o;
			if (!f._cached)
			{			  
				String path = f.getPath();
				if (!_pathToIDFileNodes.containsKey(path))
				{
					_pathToIDFileNodes.put(path, new ArrayList<FragSeqNode>());
				}
				ArrayList<FragSeqNode> nodesForID = _pathToIDFileNodes.get(path);
				for(FragSeqModel m: f.getModels())
				{ 
					n.add(new FragSeqNode(m));
					FragSeqNode nid = getNodeForId(m.getID());
					nid.add(new FragSeqNode(m));
					nodesForID.add(nid);
				} 
			}
		}
	}
}

public void treeWillCollapse(TreeExpansionEvent event)
		throws ExpandVetoException {
	// TODO Auto-generated method stub
	TreePath t = event.getPath();
	FragSeqNode n = (FragSeqNode) t.getLastPathComponent();
	_isExpanded.put(n, false);
	
	
}


}
