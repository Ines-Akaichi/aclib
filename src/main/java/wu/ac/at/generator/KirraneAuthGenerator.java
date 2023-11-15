package wu.ac.at.generator;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import wu.ac.at.rewriter.RDFQuad;
import wu.ac.at.utils.FileUtils;

import wu.ac.at.generator.AuthGenerator;

public class KirraneAuthGenerator implements AuthGenerator{
private ArrayList<String> auths;
	
    public static void main(String[] args) {

    	try {
	    	//logging 
	    	System.out.println("START AuthGenerator main");
	    	
			//Read the config file
			Properties prop = new Properties();
			prop.load(KirraneAuthGenerator.class.getResourceAsStream("/config.properties"));
			
			String dataFileName = prop.getProperty("dataFilePath");
			
    		KirraneAuthGenerator generator = new KirraneAuthGenerator();
    		
    		generator.execute(prop, dataFileName); 
    		
	    	//logging 
	    	System.out.println("END AuthGenerator main");
	    	
    	} catch (Exception e) {
    		System.out.println(e.toString());
    	}
	}
    
    public KirraneAuthGenerator(){
        auths = new ArrayList<String>();
    }
       
    public void execute(Properties prop, String dataFileName) throws Exception{
					
		FileUtils utils = new FileUtils();
    	
		//Read in the data
    	List<RDFQuad> data = utils.readFromFileIntoRDFQuadList(dataFileName);	    	
    	if(data.isEmpty())
    		throw new FileNotFoundException("Failed to read data from [" + dataFileName + "]");
    	
    	
    	generate(prop, data);
    	
    }
    
    public ArrayList<String> generate(Properties prop, RDFQuad quad) throws Exception{
	 	
		//?S ?P ?O ?G
		itemConcat("?s","?p","?o","?g");
		
		generatePatterns(quad);
	    	
    	return auths;
    }
    
    public void generate(Properties prop, List<RDFQuad> data) throws Exception{
    	    	
		//?S ?P ?O ?G
		itemConcat("?s","?p","?o","?g");

		RDFQuad quad1=null;
    	for(RDFQuad quad : data)
    	{
    		//System.out.println(quad.toString());
    		generatePatterns(quad);
    	}

    	if(!auths.isEmpty()){
			// Write the authorisations to a file
			String authOutPath = prop.getProperty("authOutPath");	
			//System.out.println("here++++++++++");
			FileUtils fileUtils = new FileUtils();
			fileUtils.writeFromArrayIntoFile(auths, authOutPath);  // -> error is here!!
			
    	} else
    		throw new Exception("No Authorisations Generated");    	
    }
    
    private void generatePatterns(RDFQuad quad) throws Exception{
		//S1 ?P ?O ?G
		itemConcat(quad.getSubject(),"?p","?o","?g");
		//?S P1 ?O ?G
		itemConcat( "?s",quad.getPredicate(),"?o","?g"); 
		//?S ?P O1 ?G
		itemConcat( "?s","?p",quad.getObject(),"?g");
		//?S ?P ?O G1
		itemConcat( "?s","?p","?o",quad.getGraph());
		//S1 P1 ?O ?G
		itemConcat( quad.getSubject(),quad.getPredicate(),"?o","?g"); 
		//?S P1 O1 ?G
		itemConcat( "?s",quad.getPredicate(),quad.getObject(),"?g");
		//?S ?P O1 G1
		itemConcat( "?s","?p",quad.getObject(),quad.getGraph()); 
		//S1 ?P O1 ?G
		itemConcat( quad.getSubject(),"?p",quad.getObject(),"?g");
		//S1 ?P ?O G1
		itemConcat( quad.getSubject(),"?p","?o",quad.getGraph());
		//?S P1 ?O G1
		itemConcat( "?s",quad.getPredicate(),"?o",quad.getGraph());
		//?S P1 O1 G1
		itemConcat( "?s",quad.getPredicate(),quad.getObject(),quad.getGraph());
		//S1 ?P O1 G1
		itemConcat( quad.getSubject(),"?p",quad.getObject(),quad.getGraph());
		//S1 P1 ?O G1
		itemConcat( quad.getSubject(),quad.getPredicate(),"?o",quad.getGraph());
		//S1 P1 O1 ?G
		itemConcat( quad.getSubject(),quad.getPredicate(),quad.getObject(),"?g"); 
		//S1 P1 O1 G1
		itemConcat( quad.getSubject(),quad.getPredicate(),quad.getObject(),quad.getGraph());
    }
    
    private void itemConcat(String s, String p, String o, String g){
    	
    	String item = s + " " + p + " " + o + " " + g +"\n";
		if(!auths.contains(item))
			auths.add(item);
    }
}
