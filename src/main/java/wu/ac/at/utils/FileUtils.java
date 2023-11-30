package wu.ac.at.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import wu.ac.at.rewriter.RDFQuad;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;

public class FileUtils
{
	public FileUtils()
	{
		
	}

	public ArrayList<String> readFromFileIntoArray(String filepath) throws Exception
	{
				
		ArrayList<String> items = new ArrayList<String>();
		
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		//System.out.println(filepath);
		
		String item;
		while ((item = br.readLine()) != null) {
			
			if(!item.trim().isEmpty())
			{
				items.add(item);
			}
		}
		br.close();		
		
		return items;	  		  
	}	
	
	//Read the query into a string
	public List<RDFQuad> readFromFileIntoRDFQuadList(String filepath) throws Exception
	{	
		List<RDFQuad> dataset  = null;
		int start = filepath.lastIndexOf(".") + 1;
		
		if(filepath.substring(start).toLowerCase().equals("trig")){
			dataset = readFromTrigIntoRDFQuadList(filepath);
		} else {
				
			dataset = new ArrayList<RDFQuad>();
			
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			//System.out.println(filepath);
					
			String item;
			while ((item = br.readLine()) != null) {
				
				if(!item.trim().isEmpty())
				{
					RDFQuad quad = new RDFQuad(item);
					dataset.add(quad);
				}

			}
			br.close();
		}
		
		return dataset;	  		  		  
	}
	
	public List<RDFQuad> readFromTrigIntoRDFQuadList(String filepath) throws Exception
	{	
		List<RDFQuad> dataset = new ArrayList<RDFQuad>();
		Iterator<Quad> sIter = null;
        DatasetGraph dsg = null ;
        
        // Read a TriG file into quad storage in-memory.
        dsg = RDFDataMgr.loadDatasetGraph(filepath);
        //System.out.println(filepath);
        
		if(dsg != null)
		{			
			// Output content to string
		   sIter  = dsg.find();	
		   while(sIter.hasNext()){
			    Quad q = sIter.next();
			    String s, p , o, g;
			    s = q.getSubject().toString();
			    p = q.getPredicate().toString();
			    o = q.getObject().toString();
			    g = q.getGraph().toString();
			    if(s.contains("http://"))
			    	s= s.replace("http://", "<http://") + ">";			   
			    if(p.contains("http://"))
			    	p= p.replace("http://", "<http://") + ">";
			    if(o.contains("http://"))
			    	o= o.replace("http://", "<http://") + ">";
			    if(g.contains("http://"))
			    	g= g.replace("http://", "<http://") + ">";
			    
			    if(s.contains("localhost:"))
			    	s= s.replace("localhost:", "<localhost:") + ">";			   
			    if(p.contains("localhost:"))
			    	p= p.replace("localhost:", "<localhost:") + ">";
			    if(o.contains("localhost:"))
			    	o= o.replace("localhost:", "<localhost:") + ">";
			    if(g.contains("localhost:"))
			    	g= g.replace("localhost:", "<localhost:") + ">";
				RDFQuad quad = new RDFQuad(s, p, o, g);
				dataset.add(quad);
			    //System.out.println(sIter.next());
		   }
			   
		}
		
		return dataset;	  		  		  
	}
	
	//Read the query into a string
	public String readFromFileIntoString(String filepath) throws Exception
	{				
		String query ="";
		
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		//System.out.println(filepath);
			
		String item;
		while ((item = br.readLine()) != null) {
			
			if(!item.trim().isEmpty())
			{
				query = query + item + "\n";
			}
		}
		br.close();		
		
		return query;	  		  
	}
	
	public void writeFromArrayIntoFile(ArrayList<String> data, String filepath) throws Exception
	{
		Writer writer = null;
		try 
		{
			String fileDir = filepath.substring(0, filepath.lastIndexOf("\\"));
			File f = new File(fileDir);
			if(!f.exists())
				f.mkdirs();
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
			for (String item : data)
			{				
			    writer.write(item);
			    writer.write("\n");
			}
		} 
		catch (IOException ex)
		{
			throw ex;
		} 
		finally 
		{
		   try {writer.close();} catch (Exception ex) {}
		}
	}
	
	public void writeFromStringIntoFile(String item, String filepath) throws Exception
	{
		Writer writer = null;
		try 
		{
			//String fileDir = filepath.substring(0, filepath.lastIndexOf("\\"));   //Keep an aye on this

			//File f = new File(fileDir);
			//if(!f.exists())
				//f.mkdirs();
				
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));			
			writer.write(item);
		    //writer.write("\n");
		} 
		catch (IOException ex)
		{
			throw ex;
		} 
		finally 
		{
		   try {writer.close();} catch (Exception ex) {}
		}
	}

	/*public PrintWriter createFile(String filepath) throws IOException{

		DateTime date = DateTime.now();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYYMMddhhmmss");
		filepath = filepath + "results" + fmt.print(date) + ".txt";
		
		String fileDir = filepath.substring(0, filepath.lastIndexOf("/"));
		File f = new File(fileDir);
		if(!f.exists())
			f.mkdirs();
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filepath, true)));
		
		return out;
	}
	*/
	
	public void deleteFilesInFolder(String folderPath) throws Exception{				
		final File queryDir = new File(folderPath);
		//System.out.println(folderPath);
	    for (final File fileEntryQuery : queryDir.listFiles()) {
	    	if (fileEntryQuery.isDirectory())
	    		deleteFilesInFolder(fileEntryQuery.getPath());
	    	else if (fileEntryQuery.isFile())
	        	fileEntryQuery.delete();
	    }
	}
	
	public void copyFilesInFolder(String source, String dest) throws Exception{					
		final File queryDir = new File(source);
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		
		try {
		    for (final File fileEntryQuery : queryDir.listFiles()) {
		        if (fileEntryQuery.isFile()) {
		        
			        inputChannel = new FileInputStream(fileEntryQuery.getAbsolutePath()).getChannel();
			        outputChannel = new FileOutputStream(dest + fileEntryQuery.getName()).getChannel();
			        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		        }
		    } 
		}
	    catch(Exception e){
	    	throw e;
	    } finally {
	        inputChannel.close();
	        outputChannel.close();
		}
	}
	
	public void copy(String source, String dest) throws Exception{					
		final File sourceFile = new File(source);
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		
		try {
			inputChannel = new FileInputStream(sourceFile.getAbsolutePath()).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		}
	    catch(Exception e){
	    	throw e;
	    } finally {
	        inputChannel.close();
	        outputChannel.close();
		}
	}
}
