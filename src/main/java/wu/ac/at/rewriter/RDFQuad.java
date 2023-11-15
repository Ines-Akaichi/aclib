package wu.ac.at.rewriter;


import java.util.Random;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.NodeFactoryExtra;

public class RDFQuad
{
	private String subject;
	private String object;
	private String predicate;
	private String graph;
	
	public RDFQuad(String quad) throws Exception
	{
		quad.trim();
		
		if(quad.startsWith("\"")){
			if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("@")){
				subject = quad.substring(0, quad.indexOf("@", 1)+3);
				quad = quad.substring(quad.indexOf("@", 1)+3);
			}else if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("^^")){
				subject = quad.substring(0, quad.indexOf(">", 1)+1);
				quad = quad.substring(quad.indexOf(">", 1)+1);
			}else{
				subject = quad.substring(0, quad.indexOf("\"", 1)+1);
				quad = quad.substring(quad.indexOf("\"", 1)+1);
			}
		} else { 
			subject = quad.substring(0, quad.indexOf(" "));				
			quad = quad.substring(quad.indexOf(" ")+1);
		}
		
		if(quad.startsWith("\"")){
			if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("@")){
				predicate = quad.substring(0, quad.indexOf("@", 1)+3);
				quad = quad.substring(quad.indexOf("@", 1)+3);
			}else if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("^^")){
				predicate = quad.substring(0, quad.indexOf(">", 1)+1);
				quad = quad.substring(quad.indexOf(">", 1)+1);
			}else{
				predicate = quad.substring(0, quad.indexOf("\"", 1)+1);
				quad = quad.substring(quad.indexOf("\"", 1)+1);
			}
		} else { 
			predicate = quad.substring(0, quad.indexOf(" "));		
			quad = quad.substring(quad.indexOf(" ")+1);
		}
		
		if(quad.startsWith("\"")){
			if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("@")){
				object = quad.substring(0, quad.indexOf("@", 1)+3);
				quad = quad.substring(quad.indexOf("@", 1)+3);
			}else if(quad.substring(quad.indexOf("\"", 1)+1).startsWith("^^")){
				object = quad.substring(0, quad.indexOf(">", 1)+1);
				quad = quad.substring(quad.indexOf(">", 1)+1);
			}else{
				object = quad.substring(0, quad.indexOf("\"", 1)+1);
				quad = quad.substring(quad.indexOf("\"", 1)+1);
			}
		} else { 
			object = quad.substring(0, quad.indexOf(" "));
			quad = quad.substring(quad.indexOf(" ")+1);
		}
		setGraph(quad);
	}
	
	public RDFQuad(String s, String p, String o, String g) throws Exception
	{
		subject = s;
		predicate = p;
		object = o;
		
		setGraph(g);
	}

	public RDFQuad getPattern() throws Exception {
		Random rand = new Random();
		int i = rand.nextInt(4);
		
		String s = subject;
		String p = predicate;
		String o = object;
		String g = graph;
		
		if(i == 0)
			s = "?s";
		else if(i == 1)
			p = "?p";	
		else if(i == 2)
			o = "?o";
		else if(i == 3)
			g = "?g";
		return new RDFQuad(s, p, o, g);
	}
	
	public String getSubject() {
		return subject;
	}

	public Node getSubjectNode() throws Exception{
		return getNode(subject);
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getPredicate() {
		return predicate;
	}

	public Node getPredicateNode() throws Exception{
		return getNode(predicate);
	}
	
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
	
	public String getObject() {
		return object;
	}
	
	public Node getObjectNode() throws Exception{
		return getNode(object);
	}

	public void setObject(String object) {
		this.object = object;
	}
	
	public String getGraph() {
		return graph;
	}

	public Node getGraphNode() throws Exception{
		return getNode(graph);
	}
	
	public void setGraph(String graph) {
		graph = graph.trim();
		if(graph.substring(graph.length()-1, graph.length()).equals(".")) 			
			this.graph = graph.substring(0, graph.length()-1);
		else
			this.graph = graph;
	}
	
	public String getTripleString() {
		return subject + " " + predicate + " " + object;
	}
	
	public void setTriple(String triple) throws Exception {
		triple = triple.replace("{", " ");
		triple = triple.replace("}", " ");
		triple.trim();
		
		subject = triple.substring(0, triple.indexOf(" "));
		triple = triple.substring(triple.indexOf(" ")+1);
		
		predicate = triple.substring(0, triple.indexOf(" "));
		triple = triple.substring(triple.indexOf(" ")+1);
		
		object = triple;
	}
	
	public Quad getQuad() throws Exception{
    	return Quad.create(getGraphNode(), getSubjectNode(), getPredicateNode(), getObjectNode());	
	}
	
	public Triple getTriple() throws Exception{
    	return Triple.create(getSubjectNode(), getPredicateNode(), getObjectNode());	
	}
	
	private Node getNode(String s) throws Exception{
		
		if(s.contains("?"))
			return NodeFactory.createVariable(s.replace("?", ""));
		else {		
			Node n = NodeFactoryExtra.parseNode(s);
			return n;
		}
	}
	
	private boolean isMatch(Node q, Node p) throws Exception{
		if ((q.toString().equals(p.toString())) 
		|| (q.isVariable() && p.isURI())
		|| (q.isVariable() && p.isLiteral())
		|| (p.isVariable() && q.isURI())
		|| (p.isVariable() && q.isLiteral())
		|| (p.isVariable() && q.isVariable()))
			return true;
		else
			return false;
	}

	public boolean isPatternMatch(Quad pattern) throws Exception{
		//check if the graph pattern matches the authorisation graph pattern
		if(isMatch(getGraphNode(), pattern.getGraph()) &&
					isMatch(getSubjectNode(), pattern.getSubject()) &&
							isMatch(getPredicateNode(), pattern.getPredicate()) &&
									isMatch(getObjectNode(), pattern.getObject()))

			return true;
		else
			return false;
	}
	
	@Override 
	public String toString() {
		return subject + " " + predicate + " " + object + " " + graph;
	}

}