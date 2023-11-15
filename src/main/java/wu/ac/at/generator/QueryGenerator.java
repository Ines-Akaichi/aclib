package wu.ac.at.generator;

import java.util.ArrayList;
import java.util.List;

import wu.ac.at.rewriter.RDFQuad;

public interface QueryGenerator {
    abstract public String generateAggr(String mode) throws Exception;
    
    abstract public String generateBGPExtended(ArrayList<RDFQuad> quadPatterns, String mode) throws Exception;
   
    abstract public String generateNegation(ArrayList<RDFQuad> quadPatterns, String mode) throws Exception;
    
    abstract public String generatePropertyPaths() throws Exception;
       
    abstract public String generateUpdate(String source, String mode) throws Exception;
    
    abstract public String generateInsertDeleteData(List<RDFQuad> data, String mode) throws Exception;
        
    abstract public String generateDeleteInsert(ArrayList<String> quadPatterns, String mode) throws Exception;
}
