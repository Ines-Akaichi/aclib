package wu.ac.at.rewriter;

import java.util.List;

import org.apache.jena.query.Query;


public interface RDFQuery {
	
    abstract public void addBGPFilter(List<RDFQuad> policies, boolean filter) throws Exception;

    abstract public void addBGPFilter(List<RDFQuad> policies, Query query, boolean filter) throws Exception;
       
    abstract public void updateBGPQuery(RDFQuad policy, Query query, boolean filter) throws Exception;			
}
