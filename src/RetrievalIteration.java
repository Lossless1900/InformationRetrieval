import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


public class RetrievalIteration {
	static CharArraySet stopSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
	static CharArraySet sets = null;	
	
	public RetrievalIteration(){
		Object[] rawArray = stopSet.toArray();
		sets = new CharArraySet(Version.LUCENE_46, 0, false);
		for (Object cha: rawArray){
			//System.out.println((char[])cha);
			sets.add((char[])cha);
		}
		List<String> stopWords=Arrays.asList("i","me","my","myself","we","us","our","ours","ourselves","you","your","yours",
				"yourself","yourselves","he","him","his","himself","she","her","hers","herself","it","its",
				"itself","they","them","their","theirs","themselves","what","which","who","whom","whose",
				"this","that","these","those","am","is","are","was","were","be","been","being","have","has",
				"had","having","do","does","did","doing","will","would","should","can","could","ought",
				"i'm","you're","he's","she's","it's","we're","they're","i've","you've","we've","they've",
				"i'd","you'd","he'd","she'd","we'd","they'd","i'll","you'll","he'll","she'll","we'll","they'll",
				"isn't","aren't","wasn't","weren't","hasn't","haven't","hadn't","doesn't","don't","didn't",
				"won't","wouldn't","shan't","shouldn't","can't","cannot","couldn't","mustn't","let's","that's",
				"who's","what's","here's","there's","when's","where's","why's","how's","a","an","the","and",
				"but","if","or","because","as","until","while","of","at","by","for","with","about","against",
				"between","into","through","during","before","after","above","below","to","from","up","upon",
				"down","in","out","on","off","over","under","again","further","then","once","here","there",
				"when","where","why","how","all","any","both","each","few","more","most","other","some",
				"such","no","nor","not","only","own","same","so","than","too","very","say","says","said",
				"shall","a","fuck","shit","bitch");
		sets.addAll(stopWords);
	}
	
	public void startIteration(Query query) throws IOException, DocumentException{
		query.iteration += 1;
		System.out.println("Iteration: "+query.iteration);
		String content = getContent(query);
		ArrayList<Doc> docs = content2Doc(content);
		
		for(int j=0;j<docs.size();j++){
			TokenStream tokenStream = new LowerCaseTokenizer(Version.LUCENE_46, new StringReader(docs.get(j).summary.toString()));
			tokenStream = new StopFilter(Version.LUCENE_46, tokenStream, sets);
			tokenStream.reset();
			while (tokenStream.incrementToken()) {
				// construct tfij array and dfi map
			}
		}
	}
	
	public String getContent(Query query) throws IOException{
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
	
	public ArrayList<Doc> content2Doc(String content) throws DocumentException, IOException{
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
			
			// convert content to lower case
            Doc doc = new Doc(title,url,summary.toLowerCase(),relevant);
            docs.add(doc);
        }
        return docs;
	}
}
