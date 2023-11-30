package wu.ac.at.rewriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.*;

import wu.ac.at.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class QueryEngine {

	public String configFilePath ;
	private String policyFilePath;
	private String queryOutPath ;
	private Boolean filters ;
	private String queryType ;
	private String dataFilePath ;
	private String rewrittenQuery ;
	private Dataset dataset;

	
	public QueryEngine(String InputConfigFilePath)
	{
		this.configFilePath = InputConfigFilePath;
	}	
	
	
	public QueryEngine(Dataset dataset)
	{
    	this.dataset = dataset;  

	}	
	

	
	public String getRewrittenQuery() {
		return rewrittenQuery;
	}


	public void setRewrittenQuery(String rewrittenQuery) {
		this.rewrittenQuery = rewrittenQuery;
	}


	public void LoadSettings ()
	
	{
       // InputStream input = getClass().getResourceAsStream(this.configFilePath); 
		InputStream input;
		try {
			input = new FileInputStream(this.configFilePath);
		
        System.out.print("config file "+input.toString());
        Properties prop = new Properties();
        // load a properties file
		prop.load(input);
        // get the property value 
        dataFilePath=prop.getProperty("dataFilePath");
        policyFilePath=prop.getProperty("policyFilePath");
        queryOutPath=prop.getProperty("queryOutPath");
        queryType=prop.getProperty("queryType");
        filters=Boolean.parseBoolean(prop.getProperty("filters"));
		dataset = RDFDataMgr.loadDataset(dataFilePath) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public List<QuerySolution> select(String qs) throws Exception
	{	
		List<QuerySolution> list = null;
		//qs=qs.trim();
		//qs = qs.replace("\n", " "); // I added this!
	  	Query sparql = QueryFactory.create(qs);
//	  	System.out.println(sparql.serialize());
	   
		QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
		
		try {
		//Iterate through the results
		ResultSet results = qe.execSelect();
		if(results != null){
			if(results.hasNext())
			{
				list = ResultSetFormatter.toList(results);
			}
		}
		} catch (NullPointerException e){
			//do nothing
		}
		
		return list;
	}
	
	public Boolean ask(String qs) throws Exception
	{	
	  	Query sparql = QueryFactory.create(qs);
	   
		QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
		
		// Execute the query
		return qe.execAsk();
		
	}
	
	public Dataset update(String qs) throws Exception
	{	 
		UpdateRequest request = UpdateFactory.create() ;
		request.add(qs);
		
		// Execute the update
		UpdateAction.execute(request, dataset) ;
		
		// return the updated dataset
		return dataset;			 
	}
	
	public ArrayList<String> selectVar(String qs, String var) throws Exception
	{	
		ArrayList<String> objs = new ArrayList<String>();
		
	  	Query sparql = QueryFactory.create(qs);
   
		QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
		
		// Execute the query
		ResultSet results = qe.execSelect();
		
		//Iterate through the results
		while(results.hasNext()){
			QuerySolution qsol = results.next();
			
			if(qsol.get(var) != null)
				objs.add(qsol.get(var).toString());
	     }
		
		return objs;
	}
	
	public void  getResults (String query, QueryEngine engine) throws Exception  
	{ 
	
		//Iterate through the queries 
		try {
		List<QuerySolution> list = engine.select(query);
		if(list != null)
			for(QuerySolution sol : list)
			{
				System.out.println (sol);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	public void  getResults (String serviceURL, String query) throws Exception 
	{ 
		// Execute the query
		
		QueryExecution qe = QueryExecutionFactory.sparqlService(serviceURL,query);  
		ResultSet results = qe.execSelect();
		ResultSetFormatter.out(results) ; 
		
	}
	
	public void executeQuery (String serviceURL, String inputQuery)      // give service url as input such as fuseki + DB is TDB: works with constructor QueryEngine(String InputConfigFilePath, String inputQuery)
	{
    	FileUtils utils = new FileUtils ();
		QueryRewriter rewriter = new QueryRewriter (); 
		String parseQuery;
		try {
			rewriter.rewriteQueries(inputQuery, policyFilePath, queryOutPath, filters, queryType);
			rewrittenQuery= utils.readFromFileIntoString(queryOutPath).trim();
			// Parse Query here!
			parseQuery=parseQuery(rewrittenQuery);
			System.out.println("parsed query "+parseQuery);
			getResults (serviceURL,parseQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void executeQuery (String inputQuery)         // Database is in input config file : works with constructor  QueryEngine(Dataset dataset)
	{
    	FileUtils utils = new FileUtils ();
		QueryRewriter rewriter = new QueryRewriter (); 
		QueryEngine engine = new QueryEngine (dataset);
		try {
			rewriter.rewriteQueries(inputQuery, policyFilePath, queryOutPath, filters, queryType);
			rewrittenQuery=utils.readFromFileIntoString(queryOutPath).trim();
			//Parse rewritten Query and give it to getResults
			getResults(rewrittenQuery,engine);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
	public String parseQuery (String inputQuery)
	{
		String inputQuery1;
		inputQuery1=inputQuery.replaceAll("\\bGRAPH\\b[^{]*\\{", "");
		//inputQuery1=inputQuery1.replaceAll("}\n}","");
		inputQuery1=inputQuery1.replaceFirst("}", "");
		inputQuery1=inputQuery1.replaceFirst("}", "");
		//inputQuery.replaceAll("", "" );
		//inputQuery.replaceAll("", "" );
		//inputQuery = inputQuery.replaceAll("GRAPH", "{");
		return inputQuery1;
	}
	
	
	
}

