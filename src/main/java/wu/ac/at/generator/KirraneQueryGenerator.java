package wu.ac.at.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import  wu.ac.at.generator.KirraneQueryGenerator;
import  wu.ac.at.rewriter.RDFQuad;
import  wu.ac.at.utils.FileUtils;

public class KirraneQueryGenerator implements QueryGenerator{
	static ArrayList<String> queries;
	final String SELECT = "SELECT";
	final String SELECTQUAD = " ?g ?s ?p ?o ";
	final String WHERE = " WHERE { ";
	final String WHEREQUAD = " GRAPH ?g { ?s ?p ?o ";
	Properties prop;
	FileUtils fileUtils;
	
    public static void main(String[] args) {

    	try {
	    	//logging 
	    	System.out.println("START QueryGenerator main");
	    	
    		//Read the config file
			Properties prop = new Properties();
			prop.load(KirraneQueryGenerator.class.getResourceAsStream("/config.properties"));
						
			String dataFileName = prop.getProperty("rdfFileName");
			String dataOutFileName = prop.getProperty("queryOutPath");
			KirraneQueryGenerator generator = new KirraneQueryGenerator(prop);
						
			//generator.execute(dataFileName,"INSERT",dataOutFileName);
			String modeBGP = "COUNT", modeNeg = "SELECT", modeInDe = "INSERT";
			generator.execute(dataFileName,modeBGP,modeNeg,modeInDe,dataOutFileName);
		       
			//logging 
	    	System.out.println("END QueryGenerator main");
	    	
    	} catch (Exception e) {
    		System.out.println(e.toString());
    	}
	}
    
    public KirraneQueryGenerator(Properties prop){
    	this.prop = prop;
        queries = new ArrayList<String>(); 
        fileUtils = new FileUtils();
    }
    
    public void execute(String dataFileName) throws Exception{
		
    }
    
   public void execute(String dataFileName, String modeBGP,String modeNeg, String modeInDe, String dataOutFileName) throws Exception{
		ArrayList<RDFQuad> data = (ArrayList<RDFQuad>) fileUtils.readFromTrigIntoRDFQuadList(dataFileName); 
	    generateBGPExtended(data, modeBGP);
	    generateNegation(data,modeNeg);
		generateInsertDeleteData (data,modeInDe);
	    fileUtils.writeFromArrayIntoFile(queries,dataOutFileName);
    }
        
    public String generateAggr(String mode) throws Exception{
    	
    	String query = "";
    	
    	if(mode.equals("SUM"))
    		query = SELECT + " ( SUM(?o) AS ?sum ) WHERE {";
    	else if(mode.equals("MIN"))
    		query = SELECT + " ( MIN(?o) AS ?min ) WHERE {";
    	else if(mode.equals("MAX"))
    		query = SELECT + " ( MAX(?o) AS ?max ) WHERE {";
    	else if(mode.equals("AVG"))
    		query = SELECT + " ( AVG(?o) AS ?avg ) WHERE {";   	    
    	
    	query = query + " GRAPH ?g  { " + 
		" ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/Offer> " + " } ";
    	query = query + " GRAPH ?g  { " + 
		" ?s <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/deliveryDays> ?o " + " } ";
		query += " } ";
    	if(!query.isEmpty())
    		if(!queries.contains(query))
    			queries.add(query); 
    	
    	return query; 
    }
    
    
    public String generateBGPExtended(ArrayList<RDFQuad> quadPatterns, String mode) throws Exception{          //What is missing is adding other agg functions! checkfunction generateAggr
    	
    	String query = ""; 	
    	try {

    	if(mode.equals("COUNT"))
    		query = SELECT + " ( COUNT(*) AS ?count ) WHERE {";
    	else if(mode.equals("SAMPLE"))
    		query = SELECT + " ( SAMPLE(*) AS ?sample ) WHERE {";
    	else if(mode.equals("CONCAT"))
    		query = SELECT + " ( GROUP_CONCAT(*;  separator = \" , \" ) AS ?concat ) WHERE {";    	
    	else
    		query = SELECT + " * WHERE {";

    	int size = quadPatterns.size();
    	Random rand = new Random();
    	int max = rand.nextInt(size);
    	
   	    max=2;
   	    
    	for(int i=0; i <= max ; i++){
    		RDFQuad quadPattern = quadPatterns.get(i);
    	   	//System.out.println(quadPattern.toString());
        	//RDFQuad quad = new RDFQuad(quadPattern);
        	query = query + " GRAPH " + quadPattern.getGraph() + " { " + 
        			quadPattern.getTripleString() + " } ";
    	}   	    			
		query += " } ";
		
		if(mode.equals("SAMPLE") || mode.equals("CONCAT")){
			int i = query.lastIndexOf("?");
			
			if(i != -1){
				String r = query.substring(i,i+2); 
				query = query.replace("*", r);
			}
		}
    	   	    	
    	if(!query.isEmpty())
    		if(!queries.contains(query))
    			queries.add(query); 
    	} catch (Exception e) {
    		System.out.println(e.toString());
    	}
 	    //System.out.println(query);
    	return query; 
    }
   
    public String generateNegation (ArrayList<RDFQuad> quadPatterns, String mode) throws Exception {   //nested query
    	String query = "";
    	String pattern = "";
    	
    	int size = quadPatterns.size()-1;
    	List<RDFQuad> qps = new ArrayList<RDFQuad>();   	
    	    	
    	Random rand = new Random();
    	for(int i=0; i < rand.nextInt(2)+1; i++){
        	int index = rand.nextInt(size)+1;
        	RDFQuad quadPattern = quadPatterns.get(index);
        	if(!qps.contains(quadPattern))
        		qps.add(quadPattern);
    	}
    	
    	for(RDFQuad qp : qps){
        	RDFQuad quad = qp;
        	pattern = pattern + " GRAPH " + quad.getGraph() + " { " + 
        			quad.getTripleString() + " } ";
    	}  
    	
    	if(mode.equals("SELECT")){
    		query = SELECT + SELECTQUAD + WHERE + WHEREQUAD;
    		query += " { SELECT ?g ?s ?p ?o  WHERE { " + pattern + " } } } }";	 	
    	}else{
    		query = SELECT + SELECTQUAD + WHERE + WHEREQUAD;
    		query += mode + " { " + pattern + " } } }";	    
    	}
    	if(!query.isEmpty())
    		if(!queries.contains(query))
    			queries.add(query); 
    	
    	//System.out.println("query" + query);
    	return query; 	
    }
    
     public String generatePropertyPaths() throws Exception{

    	String item = "SELECT * WHERE { GRAPH ?g {?s <http://www.w3.org/2000/01/rdf-schema#label>* ?o } }";
        	
		return item;
    }
       
    public String generateUpdate(String source, String mode) throws Exception{
    	String pattern = "";
    	
    	if(mode.equals("CLEAR"))
    		pattern = "CLEAR GRAPH " + source;    	  	
    	else if(mode.equals("DROP"))
    		pattern = "DROP GRAPH " + source;    	  	
    	if(mode.equals("ADD"))
        	pattern = "ADD GRAPH " + source + " TO GRAPH " + source.replace(">", "V2>");     	  	
    	else if(mode.equals("COPY"))
        	pattern = "COPY GRAPH " + source + " TO GRAPH " + source.replace(">", "V2>");    	  	
    	else if(mode.equals("MOVE"))
        	pattern = "MOVE GRAPH " + source + " TO GRAPH " + source.replace(">", "V2>");    	  	
   	
    	return pattern;
    }
    
    public String generateInsertDeleteData(List<RDFQuad> data, String mode) throws Exception{
    	
    	String query = "";
    	int size = data.size();
    	
    	if(mode.equals("DELETE DATA"))
    		query = "DELETE DATA { ";
    	else
        	query = "INSERT DATA { ";
    	
    	Random rand = new Random();
    	for(int i=0; i < 3; i++){
        	int index = rand.nextInt(size);
        	RDFQuad quad = data.get(index);
        	query = query + " GRAPH " + quad.getGraph() + " { " + 
        			quad.getTripleString() + " } ";
    	}   	    			
		query += " } ";
    	
    	if(!query.isEmpty())
    		if(!queries.contains(query))
    			queries.add(query); 
    	//System.out.println(query);
    	return query; 
    }
        
    public String generateDeleteInsert(ArrayList<String> quadPatterns, String mode) throws Exception{
    	
    	String query = "";

    	String pattern = "";
    	int size = quadPatterns.size()-1;
    	String quadPattern = "";
    	int index;
    	
    	try{    	
	    	Random rand = new Random();
	    	for(int i=0; i < rand.nextInt(2)+1; i++){
	    		if(size > 0)
	    			index = rand.nextInt(size);
	    		else 
	    			index=0;
	    		quadPattern = quadPatterns.get(index);
	        	RDFQuad quad = new RDFQuad(quadPattern);
	        	pattern = pattern + " GRAPH " + quad.getGraph() + " { " + 
	        			quad.getTripleString() + " } ";
	    	}  
	    	
	    	if(mode.equals("DELETE") || mode.equals("DELETEINSERT"))
	    		query = "DELETE { " + pattern + " } " + WHERE + pattern + " } ";
	    	else if(mode.equals("INSERT") || mode.equals("DELETEINSERT"))
	    		query = "INSERT { " + pattern + " } " + WHERE + pattern + " } ";	    
	    	
	    	if(!query.isEmpty())
	    		if(!queries.contains(query))
	    			queries.add(query); 
		} catch (Exception e) {
			System.out.println(e.toString());
		}
    	return query; 
    }



}
