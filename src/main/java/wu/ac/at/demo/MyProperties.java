package wu.ac.at.demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyProperties extends Properties {
	
    public static String  configFilePath ;
	String dataFilePath ;
	String policyFilePath ;
	String queryOutPath ;
	String queryType ;
	Boolean filters ;

    
    public MyProperties (String  InputConfigFilePath) {
    	
    try (InputStream input = getClass().getResourceAsStream(InputConfigFilePath)) {
        Properties prop = new Properties();
        // load a properties file
        prop.load(input);
        // get the property value and print it out
        dataFilePath=prop.getProperty("dataFilePath");
        policyFilePath=prop.getProperty("policyFilePath");
        queryOutPath=prop.getProperty("queryOutPath");
        queryType=prop.getProperty("queryType");
        filters=Boolean.parseBoolean(prop.getProperty("filters"));
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    
    }


	public static String getConfigFilePath() {
		return configFilePath;
	}


	public static void setConfigFilePath(String configFilePath) {
		MyProperties.configFilePath = configFilePath;
	}


	public String getDataFilePath() {
		return dataFilePath;
	}


	public void setDataFilePath(String dataFilePath) {
		this.dataFilePath = dataFilePath;
	}


	public String getPolicyFilePath() {
		return policyFilePath;
	}


	public void setPolicyFilePath(String policyFilePath) {
		this.policyFilePath = policyFilePath;
	}


	public String getQueryOutPath() {
		return queryOutPath;
	}


	public void setQueryOutPath(String queryOutPath) {
		this.queryOutPath = queryOutPath;
	}


	public String getQueryType() {
		return queryType;
	}


	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}


	public Boolean getFilters() {
		return filters;
	}


	public void setFilters(Boolean filters) {
		this.filters = filters;
	}
    

    
}
