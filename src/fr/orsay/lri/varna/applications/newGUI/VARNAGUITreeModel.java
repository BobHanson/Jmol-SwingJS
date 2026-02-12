package fr.orsay.lri.varna.applications.newGUI;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class VARNAGUITreeModel extends DefaultTreeModel{
	
  private TreeSet<String> _folders = new TreeSet<String>();
  private TreeSet<String> _ids = new TreeSet<String>();
  private Hashtable<String,TreeSet<VARNAGUIModel>> _criterionToFiles = new Hashtable<String,TreeSet<VARNAGUIModel>>();
  private Hashtable<String,DefaultMutableTreeNode> _criterionToNodes = new Hashtable<String,DefaultMutableTreeNode>();
  private ArrayList<DefaultMutableTreeNode> _fileNodes = new ArrayList<DefaultMutableTreeNode>();
  
  public enum SORT_MODE{
	  PATH,
	  ID
  }
  
  private SORT_MODE _mode = SORT_MODE.PATH; 
	
  public VARNAGUITreeModel()
  {
	  super(new DefaultMutableTreeNode("Folders"));
  }


  public void removeFolder(String path)
  {
	  if (_mode==SORT_MODE.PATH)
	  {
		  int pos = _folders.headSet(path).size();
		  DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getRoot().getChildAt(pos);
		  parent.removeAllChildren();
		  reload(parent);
		  getRoot().remove(parent);
		  _criterionToNodes.remove(path);
		  _criterionToFiles.remove(path);
	  }
	  else if (_mode==SORT_MODE.ID)
	  {
		  ArrayList<DefaultMutableTreeNode> toBeRemoved = new ArrayList<DefaultMutableTreeNode>(); 
		  for(DefaultMutableTreeNode leafNode : _fileNodes)
		  {
			  VARNAGUIModel m = (VARNAGUIModel)leafNode.getUserObject();
			  if (m.getFolder().equals(path))
			  {
				  toBeRemoved.add(leafNode);
			  }
		  }
		  for(DefaultMutableTreeNode leafNode : toBeRemoved)
		  {
			  _fileNodes.remove(leafNode);
			  DefaultMutableTreeNode parent = (DefaultMutableTreeNode) leafNode.getParent();
			  parent.remove(leafNode);
		  }
	  }
	  _folders.remove(path);
  }
  
  
  public DefaultMutableTreeNode insertGroupNode(String crit, TreeSet<String> t)
  {
	  DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(crit);
	  DefaultMutableTreeNode parent = getRoot();
	  int pos = t.headSet(crit).size();
	  parent.insert(groupNode, pos);	 
	  reload(groupNode);
	  return groupNode;
  }

  public void insertLeafNode(DefaultMutableTreeNode parent, VARNAGUIModel m, TreeSet<VARNAGUIModel> t)
  {
	  DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode(m);
	  int pos = t.headSet(m).size();
	  parent.insert(leafNode, pos);
	  _fileNodes.add(leafNode);
  }

  public DefaultMutableTreeNode addFolder(String path)
  {
	  DefaultMutableTreeNode groupNode = null;
	  try {
		  if (!_folders.contains(path))
		  {
			  System.out.println("Folder: "+path);
			  File dir = new File(path);
			  if (dir.isDirectory())
			  {
				  path = dir.getCanonicalPath();
				  _folders.add(path);
				  if (_mode==SORT_MODE.PATH)
				  {
					  System.out.println("  Adding: "+path);
					  groupNode = insertGroupNode(path, _folders);
					  _criterionToNodes.put(path,groupNode);
					  _criterionToFiles.put(path, new TreeSet<VARNAGUIModel>());
				  }
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
	  VARNAGUIModel m = new VARNAGUIModel(folder,path);
	  if (_mode==SORT_MODE.PATH)
	  {
		  addFolder(folder);
		  insertLeafNode(_criterionToNodes.get(folder), m, _criterionToFiles.get(folder));
	  }
	  else if (_mode==SORT_MODE.ID)
	  {
		  String id = m.getID();
		  if (!_criterionToNodes.containsKey(id))
		  {
			  _criterionToNodes.put(id, insertGroupNode(id, _ids));
		  }
		  insertLeafNode(_criterionToNodes.get(id), m, _criterionToFiles.get(id));
	  }
  }
  
  public DefaultMutableTreeNode getRoot()
  {
	  return (DefaultMutableTreeNode) super.getRoot();
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

}
