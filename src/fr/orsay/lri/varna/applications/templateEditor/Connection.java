package fr.orsay.lri.varna.applications.templateEditor;


public class Connection {
	public GraphicalTemplateElement _h1;
	public GraphicalTemplateElement.RelativePosition _edge1;
	public GraphicalTemplateElement _h2;
	public GraphicalTemplateElement.RelativePosition _edge2;
	
	public Connection(GraphicalTemplateElement h1, GraphicalTemplateElement.RelativePosition edge1,GraphicalTemplateElement h2, GraphicalTemplateElement.RelativePosition edge2)
	{
		_h1 = h1;
		_h2 = h2;
		_edge1 = edge1;
		_edge2 = edge2;
	}
	
	public boolean equals( Connection c)
	{
		return ((_h1==c._h1)&&(_h2==c._h2)&&(_edge1==c._edge1)&&(_edge2==c._edge2));
	}

}
