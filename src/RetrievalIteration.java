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
import java.util.HashMap;
import java.util.HashSet;
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
	static final int MAX_DOC = 10;
	
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
		Doc qDoc = new Doc("query","",getPlainText(query.keywords),true);
		ArrayList<Doc> docs = content2Doc(content);
		docs.add(qDoc);
		ArrayList<ArrayList<Integer>> termfreqs = new ArrayList<ArrayList<Integer>>();	// t_{ji}
		ArrayList<Integer> docfreq = new ArrayList<Integer>(); 							// df_{i}
		HashMap<String, Integer> termPos = new HashMap<String,Integer>(); 				// term -> pos
		HashMap<Integer,String> posTerm = new HashMap<Integer,String>(); 				// pos -> term
		
		for(int j=0;j<docs.size();j++){
			TokenStream tokenStream = new LowerCaseTokenizer(Version.LUCENE_46, new StringReader(docs.get(j).summary.toString()));
			tokenStream = new StopFilter(Version.LUCENE_46, tokenStream, sets);
			tokenStream.reset();
			ArrayList<Integer> termfreq = new ArrayList<Integer>(); 
			while (tokenStream.incrementToken()) {
				String term = tokenStream.getAttribute(CharTermAttribute.class).toString();
				// get position of term
				int pos = 0;
				if(termPos.containsKey(term)){
					pos = termPos.get(term); 
				}
				else{
					pos = termPos.size();
					termPos.put(term,pos);
					posTerm.put(pos, term);
					docfreq.add(0);
				}
				
				// add 0 if terms before do not occur
				while(termfreq.size()<=pos){
					termfreq.add(0);
				}
				
				// add docfreq for the first occurrence in the document
				if(termfreq.get(pos)==0){
					docfreq.set(pos, docfreq.get(pos)+1);
				}
				
				termfreq.set(pos, termfreq.get(pos)+1);
			}
			tokenStream.close();
			termfreqs.add(termfreq);
		}
		
		for(ArrayList<Integer> termfreq:termfreqs){
			while(termfreq.size()<termPos.size()){
				termfreq.add(0);
			}
		}
		
		System.out.println("Finished construction of frequency tables");
	}
	
	public String getContent(Query query) throws IOException{
		String q  = getPlainText(query.keywords);
		String bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27" + q.replace(" ", "%20") +  "%27&$top=10&$format=Atom";
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
            Doc doc = new Doc(title,url,summary,relevant);
            docs.add(doc);
        }
        return docs;
	}
	
	public String getPlainText(HashSet<String> set){
		StringBuilder plainKeywords = new StringBuilder();
		for(String word:set){
			plainKeywords.append(word);
			plainKeywords.append(" ");
		}
		return plainKeywords.toString().trim();
	}
	
	public void quickSortPos(int[] value, int[] pos, int start, int end){
		if(value==null || pos==null)
			return;
		
		if(end<=start || start<0 || value.length<=0 || pos.length<=0)
			return;
		
		int pivot = value[start];
		int p=start;
		int q=start+1;
		while(q<=end){
			if(value[q]>=pivot){
				p++;
				int temp = value[p];
				value[p] = value[q];
				value[q] = temp;
				
				temp = pos[p];
				pos[p] = pos[q];
				pos[q] = temp;
			}
			q++;
		}
		int temp = value[p];
		value[p] = value[start];
		value[start] = temp;
		
		temp = pos[p];
		pos[p] = pos[start];
		pos[start] = temp;
		
		quickSortPos(value,pos,start,p-1);
		quickSortPos(value,pos,p+1,end);
	}
}