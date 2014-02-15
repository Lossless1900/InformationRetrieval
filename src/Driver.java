import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.apache.commons.codec.binary.Base64;


public class Driver {
	public static void main(String[] args) throws IOException, DocumentException{
		if(args.length<3){
			System.out.println("Usage: java FeedbackBing <client-key>  <precision> <'query'> <full feedback> <# additional terms>");
			return;
		}
		
		String accountKey = args[0];
		double precision = Double.valueOf(args[1]);
		String keywords = args[2];
		Query query = new Query(keywords,0,accountKey);
		
		System.out.println("***************************************");
		System.out.println("Client key: "+query.accountKey);
		System.out.println("Query: "+query.keywords);
		System.out.println("Precision: "+precision);
		System.out.println("***************************************");
		
		if(query.precision<precision){
			startIteration(query);
		}
	}
	
	public static void startIteration(Query query) throws IOException, DocumentException{
		query.iteration += 1;
		System.out.println("Iteration: "+query.iteration);
		String content = getContent(query);
		ArrayList<Doc> docs = content2Doc(content);
		// write iteration logic
		
	}
	
	public static String getContent(Query query) throws IOException{
		String bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27" + query.keywords +  "%27&$top=10&$format=Atom";
		//Provide your account key here. 
		String accountKey = query.accountKey;
		
		byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
		String accountKeyEnc = new String(accountKeyBytes);

		URL url = new URL(bingUrl);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
				
		InputStream inputStream = (InputStream) urlConnection.getContent();		
		byte[] contentRaw = new byte[urlConnection.getContentLength()];
		inputStream.read(contentRaw);
		String content = new String(contentRaw);
		return content;
	}
	
	public static ArrayList<Doc> content2Doc(String content) throws DocumentException, IOException{
		ArrayList<Doc> docs = new ArrayList<Doc>();
		Document document = DocumentHelper.parseText(content);
		Element root = document.getRootElement();
		BufferedInputStream inStream = new BufferedInputStream(System.in);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		String line = "";
		// iterate through child elements of root with element name "foo"
        for ( Iterator<Element> i = root.elementIterator("entry"); i.hasNext(); ) {
            Element entry = (Element) i.next();
            List<Element> elist = entry.element("content").element("properties").elements();
            String title = (String) elist.get(1).getData();
            String summary = (String) elist.get(2).getData();
            String url = (String) elist.get(3).getData();
            int count = docs.size()+1;
            
            System.out.println("============================================");
            System.out.println("Result "+ count);
            System.out.println("Title: "+title);
            System.out.println("Url:"+ url);
            System.out.println("Summary: "+summary);
            System.out.println("============================================");
            
            boolean  relevant = false;
            System.out.print("Relevant (Y/N)?");
			if ((line = reader.readLine()) != null) {
				if(line.equals("Y")||line.equals("y"))
					relevant = true;
			}
            Doc doc = new Doc(title,url,summary,relevant);
            docs.add(doc);
        }
        return docs;
	}
}
