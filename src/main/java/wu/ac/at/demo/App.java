package wu.ac.at.demo;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import wu.ac.at.rewriter.QueryEngine;
import wu.ac.at.rewriter.QueryRewriter;
import wu.ac.at.utils.FileUtils;

/**
 * Hello world!
 *
 */

public class App 
{
    public static void main( String[] args )
    {
    	//Read Args
    	
    	//String configFilePath = "/config.properties" ;  
    	//String inputQuery = "SELECT ?id ?name ?salary WHERE \r\n"   
    			//+ "{ GRAPH <http://example.org/enterprisex#EmployeeDetails> \r\n"
    			//+ "{ ?id <http://xmlns.com/foaf/0.1/name> ?name. ?id <http://example.org/enterprisex#salary> ?salary }  } \r\n" ;
        String configFilePath = args[0] ;  //path for config file       
        String inputQuery =    args [1] ;   //query input string 
    	//Get values from properties file
    	MyProperties properties = new MyProperties (configFilePath);
    	String policyFileName = properties.getPolicyFilePath();
    	String queryOutPath = properties.getQueryOutPath();
    	Boolean filters  = properties.getFilters() ;
        String queryType  = properties.getQueryType();
        
    	// create rewriter object
    	QueryRewriter rewriter = new QueryRewriter (); 
    	try {
			rewriter.rewriteQueries(inputQuery, policyFileName, queryOutPath, filters, queryType);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	//call in memory query engine and input queryOutPath + datafile
    	String dataFilePath = properties.getDataFilePath();
    	Dataset dataset = RDFDataMgr.loadDataset(dataFilePath);  
    	QueryEngine engine = new QueryEngine (dataset);
    	FileUtils utils = new FileUtils ();
    	String rewrittenQuery ;
		try {
			rewrittenQuery = utils.readFromFileIntoString(queryOutPath).trim();
			engine.getResults(rewrittenQuery, engine);    
			//System.out.println(resultQuery.toString());
	    	String rewrQuery1= engine.parseQuery(rewrittenQuery);
	    	System.out.println(rewrQuery1);
			/* In case Fuseki is involved
	    	QueryEngine engineFuseki = new QueryEngine ();
	    	engineFuseki.getResults("http://localhost:3030/ines/sparql", rewrittenQuery);
	    	*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    			
    }
}
