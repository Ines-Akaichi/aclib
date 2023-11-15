package wu.ac.at.rewriter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import wu.ac.at.generator.KirraneQueryGenerator;
import wu.ac.at.utils.FileUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.riot.RDFDataMgr;


public class QueryRewriter {


    public QueryRewriter ()
    
    {


    }
    

    	
    private static ArrayList<String> selectVerification(QueryEngine queryEngine, String query) throws Exception
    	{	
    		ArrayList<String> results = new ArrayList<String>();
    		
    		//Iterate through the queries 
    		query = query.replace("\n", " ");
    		List<QuerySolution> list = queryEngine.select(query);
    		
    		if(list != null)
    			for(QuerySolution sol : list)
    			{
    				results.add(sol.toString());
    			}
    		return results;
    	} 
    
    

 
public void rewriteQueries(String queryInput, String policyFileName, String queryOutPath, boolean filters, String queryType) throws Exception{
		
    	List<RDFQuad> policyQuads = new ArrayList<RDFQuad>();
		FileUtils utils = new FileUtils();
		

    	//Read in the policies
    	//String policyFileName = prop.getProperty("policyFileName");
    	ArrayList<String> policies = utils.readFromFileIntoArray(policyFileName);
    	if(policies.isEmpty())
    		throw new FileNotFoundException("Failed to read policy from [" + policyFileName + "]"); 	
    	
    	
		//Read in the query
    	String q1 = queryInput;
    	//System.out.println("query before rewriting "+q1.toString());
  

    	//Create RDF quads for each authorisation
    	for(String s : policies){
    		
    		//N.B auths have brackets inorder to match the data
    		//s = s.replace("<", "");
    		//s = s.replace(">", "");
    		//System.out.println(s);
    		RDFQuad policy = new RDFQuad(s);
    		policyQuads.add(policy);
    	}
    	  	
    	if(queryType.equals("FILTER_QUERY")){
        	//create a new SPARQL query 
    		RDFQuery query;
			query = new KirraneQuery(q1);
			
			//System.out.println("heey");
	    	//add the filters to the query
	    	query.addBGPFilter(policyQuads, filters);
	    		
	        //print the updated query
	        //System.out.println("query after rewriting " + query.toString());
	        	
	        //output the updated query
	        utils.writeFromStringIntoFile(query.toString(), queryOutPath);	
	    	
        	
    	} 
    	else if(queryType.equals("FILTER_UPDATE")){
        	
    		String prefix = q1.substring(0, q1.indexOf("WHERE"));
    		
    		q1 = q1.substring(q1.indexOf("WHERE"));
    		q1 = "SELECT * " + q1.substring(0, q1.length());
    		
    		//create a new SPARQL query 
        	KirraneQuery query = new KirraneQuery(q1); 
        	
    		//add the filters to the query
    		query.addBGPFilter(policyQuads, filters);
    		
    		q1 = query.toString();
    		q1 = q1.substring(q1.indexOf("WHERE"));
    		q1 = prefix + q1;
    		
        	//print the updated query
        	//System.out.println(query.toString());
        	
        	//output the updated query
        	//System.out.println("query after rewriting" + q1.toString());

        	utils.writeFromStringIntoFile(q1, queryOutPath);	
        	
    	} 
    	else if(queryType.equals("CONSTRUCT_UPDATE")){
    		//add the filters to the query
    		
    		List<String> queries = addGraphFilter(policyQuads, q1);   
    		
    		int cnt=1;
        	for(String q : queries)
        	{
        		String newPath = queryOutPath;
        		if(cnt!=1)
        			newPath = queryOutPath.replace(".txt", "_" + cnt +".txt");
	        	//System.out.println("query after rewriting" + q.toString());
        		utils.writeFromStringIntoFile(q, newPath);	
        		cnt++;
        	}
        	
    	} 
    	   else {
        	
    		//System.out.println(filters);
    		//System.out.println("heeere");
    		//add the filters to the query
    		
    		q1 = removeTriples(policyQuads, q1);  // The error seems heeeeeeeeeeere!!
    		//System.out.println("q1 " + q1.toString()); 
    		
    		if(!q1.isEmpty())
    			//output the updated query
    			//System.out.println("updated!");
	        	//System.out.println("query after rewriting" + q1.toString());
    			utils.writeFromStringIntoFile(q1, queryOutPath);	    		
    	}	    	
	}
		
	public String removeTriples(List<RDFQuad> policies, String q1) throws Exception{ 
		String query = q1;
		query = query.replace("{", "");
		query = query.replace("}", "");
		query = query.replace("  ", " ");
		//System.out.println("query    " + query.toString());
		if(query.length() > 19){
			query = query.substring(20,query.length());   // The error is heeeere!!!! 20 is changed from 19!
			//System.out.println("query substring    " + query.toString());
			String[] bgp = query.split("GRAPH");
					
			for(int i=0; i < bgp.length; i++){
				String q = bgp[i];
				//System.out.println ("1 "+q);
				q = q.trim();
				q = q.replace("\n", "");
				//System.out.println ("2 "+q);
				
				String g = q.substring(0,q.indexOf(" "));
				//System.out.println ("g " + g);
				q = q.substring(q.indexOf(" ")+1);
				//System.out.println ("q " + q);

				String s = q.substring(0,q.indexOf(" "));
				q = q.substring(q.indexOf(" ")+1);
				//System.out.println ("s " + s); 
				
				String p = q.substring(0,q.indexOf(" "));
				// System.out.println ("p " + p);
				String o = q.substring(q.indexOf(" ")+1);
				//System.out.println ("o " + o);
				
				RDFQuad quad = new RDFQuad(s, p, o, g);
				for(RDFQuad policy : policies){
					Quad authorisation = policy.getQuad();
					if(isMatch(quad.getQuad(), authorisation)){
						String tbr = "GRAPH " + g + " { " + s + " " + p + " " + o + " }";						
						q1 = q1.replace(tbr, "");
					}
				}
			}
		}
		if(q1.trim().equals("DELETE DATA {        }"))
			q1="";
		else if(q1.trim().equals("INSERT DATA {        }"))
			q1="";
		return q1;
	}
	    
    public List<String> addGraphFilter(List<RDFQuad> policies, String query) throws Exception{    
		
    	List<String> queries = new ArrayList<String>();
    	String updatedQuery="";

    	//update the query for each matched authorisation
		
    	if(query.toString().contains("CLEAR") || query.toString().contains("DROP")){
    		
    		String source = query.substring(query.indexOf("GRAPH") + 6);
    		source = source.replace("\n", "");
    		
    		updatedQuery = generateQuery(policies, source, "", "DELETE");
    		if(!updatedQuery.isEmpty())
    			queries.add(updatedQuery);
		}
    	else if(query.toString().contains("ADD") || query.toString().contains("LOAD")
    			|| query.toString().contains("COPY") || query.toString().contains("MOVE")){
			
    		String source = query.substring(query.indexOf("GRAPH") + 6, query.indexOf("TO"));
    		source = source.replace("\n", "");
    		
    		//clear the destination
    		String destination = query.substring(query.lastIndexOf("GRAPH") + 6);	
    		destination = destination.replace("\n", "");
    		
			if(query.toString().contains("COPY") || query.toString().contains("MOVE")){
	    		updatedQuery = generateQuery(policies, "", destination, "DELETE");
	    		if(!updatedQuery.isEmpty())
	    			queries.add(updatedQuery);
			}
			
	    	updatedQuery = generateQueryFromTo(policies, source, destination, "INSERT");
	    	if(!updatedQuery.isEmpty())
	    		queries.add(updatedQuery);
							
			//clear the source
			if(query.toString().contains("MOVE")){
	    		updatedQuery = generateQuery(policies, source, "", "DELETE");
	    		if(!updatedQuery.isEmpty())
	    			queries.add(updatedQuery);
			}
    	}    	
    	return queries;
    }   
    
    public String generateQuery(List<RDFQuad> policies, String source, String destination, String type) throws Exception {
    	String query="";
    	String patterns = "";
    	String graph = "";
    	
    	if(!source.isEmpty())
    		graph = source.trim();
    	else
    		graph = destination.trim();
    	
//		for(RDFQuad policy : policies){
//			if(policy.getGraph().contains("?") || policy.getGraph().equals(graph))
//				patterns += "GRAPH " + graph + " { " + 
//						policy.getSubject() + " " + policy.getPredicate() + " " + policy.getObject() + " } ";
//		}
//		if(!patterns.isEmpty())
//			query = type + " { "+ patterns + " } WHERE { " + patterns + " } "; 
		
		String filter = "";
		for(RDFQuad policy : policies){
			if(policy.getGraph().contains("?") && policy.getSubject().contains("?") && 
				policy.getPredicate().contains("?") && policy.getObject().contains("?")){
				
			}
			else {
				if(policy.getGraph().contains("?") || policy.getGraph().equals(graph))
					patterns += "GRAPH " + graph + " { ?s ?p ?o ";
					if(!policy.getSubject().contains("?") ){
						filter = "FILTER ( ?s = " + policy.getSubject() + " ";					
					}
					if(!policy.getPredicate().contains("?") ){
						if(filter.isEmpty()){						
							filter = "FILTER ( ?p = " + policy.getPredicate() + " ";					
						} else {
							filter += " && ?p = " + policy.getPredicate() + " ";
						}
					}
					if(!policy.getObject().contains("?") ){
						if(filter.isEmpty()){						
							filter = "FILTER ( ?o = " + policy.getObject() + " ";					
						} else {
							filter += " && ?o = " + policy.getObject() + " ";	
						}
					}
			}
			if(!patterns.isEmpty()){
				if(!filter.isEmpty()){
					query = type + " { " + patterns + "} } WHERE { " + patterns + 
							" FILTER NOT EXISTS { GRAPH ?g { ?s ?p ?o " + filter + ") } } } }"; 
				} else {
					query = type + " { " + patterns + "} WHERE { " + patterns + " } }"; 		
				}
			}
		}
    	return query;
    }
    
    public String generateQueryFromTo(List<RDFQuad> policies, String source, String destination, String type) throws Exception {
    	String query="";
    	String patterns = "";
    	
    	source = source.trim();
    	destination = destination.trim();
//		for(RDFQuad policy : policies){
//			if(policy.getGraph().contains("?") || policy.getGraph().equals(source) || policy.getGraph().equals(destination))
//				patterns += "GRAPH " + destination + " { " + 
//						policy.getSubject() + " " + policy.getPredicate() + " " + policy.getObject() + " } ";
//		}
//		if(!patterns.isEmpty())
//			query = type + " { "+ patterns + " } WHERE { " + patterns.replace("GRAPH " + destination, "GRAPH " + source) + " } "; 
//		
    	
		String filter = "";
		for(RDFQuad policy : policies){
			if(policy.getGraph().contains("?") && policy.getSubject().contains("?") && 
				policy.getPredicate().contains("?") && policy.getObject().contains("?")){
				
			}
			else {
				if(policy.getGraph().contains("?") || policy.getGraph().equals(source))
					patterns += "GRAPH " + destination + " { ?s ?p ?o ";
					if(!policy.getSubject().contains("?") ){
						filter = "FILTER ( ?s = " + policy.getSubject() + " ";					
					}
					if(!policy.getPredicate().contains("?") ){
						if(filter.isEmpty()){						
							filter = "FILTER ( ?p = " + policy.getPredicate() + " ";					
						} else {
							filter += " && ?p = " + policy.getPredicate() + " ";
						}
					}
					if(!policy.getObject().contains("?") ){
						if(filter.isEmpty()){						
							filter = "FILTER ( ?o = " + policy.getObject() + " ";					
						} else {
							filter += " && ?o = " + policy.getObject() + " ";	
						}
					}
			}
			if(!patterns.isEmpty()){
				if(!filter.isEmpty()){
					query = type + " { " + patterns + "} } WHERE { " + patterns.replace("GRAPH " + destination, "GRAPH " + source) + " } " + 
							" FILTER NOT EXISTS { GRAPH ?g { ?s ?p ?o " + filter + ") } } } "; 
				} else {
					query = type + " { "+ patterns + " } WHERE { " + patterns.replace("GRAPH " + destination, "GRAPH " + source) + " } "; 
				}
			}
		}
    	return query;
    }
    
	private boolean isMatch(Quad q, Quad p) throws Exception{
		//check if the graph pattern matches the authorisation graph pattern
		if(isMatch(q.getGraph(), p.getGraph()) &&
					isMatch(q.getSubject(), p.getSubject()) &&
							isMatch(q.getPredicate(), p.getPredicate()) &&
									isMatch(q.getObject(), p.getObject()))

			return true;
		else
			return false;
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
	
	
	
	
	

	
	




	
}


