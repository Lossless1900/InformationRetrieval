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
import java.util.LinkedHashSet;
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
	static final double alpha = 1.0;
	static final double beta = 0.75;
	static final double gamma = 0.15;
	
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
				"shall","a","fuck","shit","bitch","s","ve");
		sets.addAll(stopWords);
	}
	
	public void startIteration(Query query) throws IOException, DocumentException{
		query.iteration += 1;
		System.out.println("***************************************");
		System.out.println("Client key: "+query.accountKey);
		System.out.println("Query: "+ getPlainText(query.keywords));
		System.out.println("Current Precision: "+query.precision);
		System.out.println("Goal Precision: "+query.goalprecision);
		System.out.println("***************************************");
		System.out.println("Iteration: "+query.iteration);
		String content = getContent(query);
		Doc qDoc = new Doc("query","",getPlainText(query.keywords).toLowerCase(),true);
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
//				System.out.println(term);
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
		
		System.out.println("Finished construction of frequency tables.");
		
		int numOfDocs = termfreqs.size();
		int numOfTerms = docfreq.size();
		double weight[][] = new double[numOfDocs][numOfTerms];
		
		for(int j = 0; j < numOfDocs; j++)
			for(int i = 0; i < numOfTerms; i++)
				weight[j][i] = ((double)termfreqs.get(j).get(i)) * Math.log((double)docs.size()/(double)docfreq.get(i));
		
		double modifiedQuery[] = new double[numOfTerms];
		double sumOfRelev[] = new double[numOfTerms];
		double sumOfNonrelev[] = new double[numOfTerms];
		int numOfRelevDocs = 0;
		for(int j = 0; j < docs.size() - 1; j++)		// exclude the last document: query
		{
			if(docs.get(j).relevant)
			{
				numOfRelevDocs++;
				for(int i = 0; i < numOfTerms; i++)
					sumOfRelev[i] += weight[j][i];
			}
			else
			{
				for(int i = 0; i < numOfTerms; i++)
					sumOfNonrelev[i] += weight[j][i];
			}
		}
		int numOfNonrelevDocs = numOfDocs -1 - numOfRelevDocs;
		double temp1 = beta/(double)numOfRelevDocs;
		double temp2 = gamma/(double)numOfNonrelevDocs;
		
		for(int i = 0; i < numOfTerms; i++){
			modifiedQuery[i] = alpha * weight[numOfDocs-1][i] + temp1 * sumOfRelev[i] - temp2 * sumOfNonrelev[i];
			if(modifiedQuery[i]<0)
				modifiedQuery[i]=0;
		}
		
		int queryPos[] = new int[numOfTerms];
		for(int i = 0; i < numOfTerms; i++)
			queryPos[i] = i;
		
		double[] modifiedQueryOrdered = modifiedQuery.clone();
		quickSortPos(modifiedQuery, queryPos, 0, numOfTerms-1);
		
//		System.out.println("Expansion: ");
//		for(int i=0;i<queryPos.length;i++){
//			System.out.println(posTerm.get(queryPos[i]));
//		}
		
		// Expand query keywords
		int i=0;
		int size = query.keywords.size();
		while(query.keywords.size()<size+2 && i<modifiedQuery.length){
			String newword = posTerm.get(queryPos[i]);
			boolean exist = false;
			for(String keyword:query.keywords){
				if(keyword.toLowerCase().contains(newword)){
					exist=true;
					break;
				}
			}
			if(!exist)
				query.keywords.add(newword);
			i++;
		}
		
		// Reorder query keywords
		double[] keywordWeight = new double[query.keywords.size()];
		int[] keywordPos = new int[query.keywords.size()];
		int j=0;
		for(String keyword:query.keywords){
			int pos = findPos(keyword,termPos);
			if(pos==-1){
				keywordWeight[j]=0;
			}
			else{
				keywordWeight[j]=modifiedQueryOrdered[pos];
			}
			j++;
		}
		for(i=0;i<query.keywords.size();i++){
			keywordPos[i]=i;
		}
		quickSortPos(keywordWeight, keywordPos, 0, query.keywords.size()-1);
		
		// Construct new keywords set
		ArrayList<String> keywords= new ArrayList<String>();
		for(i=0;i<query.keywords.size();i++){
			keywords.add(query.keywords.get(keywordPos[i]));
		}
		query.resultCount = docs.size()-1;
		query.keywords = keywords;
		query.precision = (double)numOfRelevDocs/(double)(docs.size()-1);
		
		System.out.println("Finished expanding the query.");
		System.out.println("***************************************");
		System.out.print("Iteration "+query.iteration+" Finished.");
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
            Doc doc = new Doc(title,url,summary.toLowerCase(),relevant);
            docs.add(doc);
        }
        return docs;
	}
	
	public String getPlainText(ArrayList<String> set){
		StringBuilder plainKeywords = new StringBuilder();
		for(String word:set){
			plainKeywords.append(word);
			plainKeywords.append(" ");
		}
		return plainKeywords.toString().trim();
	}
	
	public void quickSortPos(double[] value, int[] pos, int start, int end){
		if(value==null || pos==null)
			return;
		
		if(end<=start || start<0 || value.length<=0 || pos.length<=0)
			return;
		
		double pivot = value[start];
		int p=start;
		int q=start+1;
		double td = 0;
		int ti = 0;
		while(q<=end){
			if(value[q]>=pivot){
				p++;
				td = value[p];
				value[p] = value[q];
				value[q] = td;
				
				ti = pos[p];
				pos[p] = pos[q];
				pos[q] = ti;
			}
			q++;
		}
		td = value[p];
		value[p] = value[start];
		value[start] = td;
		
		ti = pos[p];
		pos[p] = pos[start];
		pos[start] = ti;
		
		quickSortPos(value,pos,start,p-1);
		quickSortPos(value,pos,p+1,end);
	}
	
	public int findPos(String keyword,HashMap<String, Integer> termPos){
		if(termPos.containsKey(keyword.toLowerCase())){
			return termPos.get(keyword.toLowerCase());
		}
		else{
			for(String term:termPos.keySet()){
				if(keyword.toLowerCase().contains(term)){
					return termPos.get(term);
				}
			}
		}
		return -1;
	}
}
