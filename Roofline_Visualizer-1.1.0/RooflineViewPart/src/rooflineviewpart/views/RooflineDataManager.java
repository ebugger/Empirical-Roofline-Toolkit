package rooflineviewpart.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import rooflineviewpart.views.Roofline.JSONRoofline;

import com.google.gson.Gson;

public class RooflineDataManager {
	static final String JSON=".json";
	static final String A="a";
	static final String HREF="href";
	static final String SLASH="/";
	
	
	private static List<String> getFiles(final String baseURL, final String suffix, final boolean directories){
		ArrayList<String> files = new ArrayList<String>();
		boolean firstDirectory = true;
		//URL url;
		StringBuilder base = new StringBuilder(baseURL);
		base.append(SLASH);
		Connection c = Jsoup.connect(baseURL);
		 Document doc=null;
		try {
			doc = c.get();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	        for (Element file : doc.select(A)) {
	        	String name = file.attr(HREF);
	        	//System.out.println(name);
	        	
	        	if(directories){
	        		if(name.endsWith(SLASH)){
	        		if(firstDirectory){
	        			firstDirectory=false;
	        			
	        		}
	        		else{
	        			//System.out.println(name);
	        		files.add(new StringBuilder(base).append(name).toString());
	        		}
	        	}
	        	}
	        	else if(name.endsWith(suffix))
	        	{
	        		//System.out.println(name);
	        		files.add(new StringBuilder(base).append(name).toString());
	        	}
	        }
		
		
		return files;
	}
	
	public static List<JSONRoofline> getRemoteRooflines(String baseurl){
		List<String> topDirs = getFiles(baseurl,null,true);
		List<String> jsonFiles;
		List<JSONRoofline> rooflines = new ArrayList<JSONRoofline>();
		for(String url: topDirs){
			jsonFiles=getFiles(url,JSON,false);
			for(String file: jsonFiles){
				//System.out.println(file);
        		
        		InputStream input=null;
        		URL rooflineURL=null;
				try {
					rooflineURL=new URL(file);
					input = rooflineURL.openStream();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(input==null){
					continue;
				}
				
        		BufferedReader br = new BufferedReader(new InputStreamReader(input));
        		Gson gson = new Gson();
				 JSONRoofline RL = gson.fromJson(br, JSONRoofline.class);
				 
				 String rooflineFilePrefix=rooflineURL.getFile();
				 rooflineFilePrefix=rooflineFilePrefix.substring(rooflineFilePrefix.lastIndexOf('/')+1,rooflineFilePrefix.lastIndexOf('.'));
				 
				 while(url.endsWith("/"))
				 {
					 url=url.substring(0,url.length()-1);
				 }
				 
				 String system = url.substring(url.lastIndexOf('/')+1);
				 RL.setSystem(system+"-"+rooflineFilePrefix);
				 rooflines.add(RL);
				 //System.out.println(RL.System);
			}
		}
		return rooflines;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String baseurl="http://nic.uoregon.edu/~wspear/roofline";
		
		getRemoteRooflines(baseurl);
		
	}

}
