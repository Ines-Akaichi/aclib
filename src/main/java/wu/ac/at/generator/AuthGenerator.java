package wu.ac.at.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import wu.ac.at.rewriter.RDFQuad;

public interface AuthGenerator {
    abstract public void execute(Properties prop, String dataFileName) throws Exception;

    abstract public ArrayList<String> generate(Properties prop, RDFQuad data) throws Exception;

    abstract public void generate(Properties prop, List<RDFQuad> data) throws Exception;
}
