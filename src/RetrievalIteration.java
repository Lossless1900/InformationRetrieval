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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;



public class RetrievalIteration {
	static CharArraySet stopSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
	static CharArraySet sets = null;
	static final double alpha = 1.0;
	static final double beta = 0.75;
	static final double gamma = 0.15;
	static final double MAX_WEIGHT = 5000;
	PorterStemmer stemmer = new PorterStemmer();
	
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
				"shall","a","fuck","shit","bitch","s","ve","lets","days","ago","retrieved","retweeted","tweeted");
		sets.addAll(stopWords);
	}
	
	public void startIteration(Query query) throws IOException,DocumentException{
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
		int rd = 0;
		for(Doc doc:docs){
			if(doc.relevant==true)
				rd++;
		}
		query.precision = (double) rd/(double) docs.size();
		if(query.precision>=query.goalprecision || query.precision==0)
			return;
		
		docs.add(qDoc);
		ArrayList<ArrayList<Double>> termfreqs = new ArrayList<ArrayList<Double>>();	// t_{ji}
		ArrayList<Integer> docfreq = new ArrayList<Integer>(); 							// df_{i}
		HashMap<String, Integer> termPos = new HashMap<String,Integer>(); 				// term -> pos
		HashMap<Integer,String> posTerm = new HashMap<Integer,String>(); 				// pos -> term
		
		// ConstructFreqTable(docs, docfreq, termfreqs, termPos, posTerm);
		// Tuning parameters......
		for(int j=0;j<docs.size();j++){
			ArrayList<String> docContents = new ArrayList<String>();
			ArrayList<Double> weights = new ArrayList<Double>();
			double aw = 1.0;
			if(docs.get(j).relevant==true){
				aw = 1.0;
				String url = docs.get(j).url;
				if(!url.startsWith("http")){
					url = "http://"+docs.get(j).url;
				}
				try{
					org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
					Elements elements = doc.getAllElements();
					docs.get(j).fulltext = doc.body().text().toLowerCase();
					int length = doc.body().text().split(" ").length;
					for(org.jsoup.nodes.Element element : elements){
						int found = 0;
						for(String str:query.keywords){
							if(element.text().toLowerCase().contains(str)){
								found++;
							}
						}
						if(found>query.keywords.size()/2){
							docContents.add(element.text().toLowerCase());
							weights.add(0.5*(double) docs.get(j).summary.split(" ").length/(double) length);
							continue;
						}
					}
					//System.out.print(sb.toString());
				}
				catch(Exception e){
//					System.out.println(e);
				}
			}
			docContents.add(docs.get(j).summary);
			weights.add(aw);
			
			ArrayList<Double> termfreq = new ArrayList<Double>();
			for(int i=0;i<docContents.size();i++){
				String docContent = docContents.get(i);
				double tw = weights.get(i);
				TokenStream tokenStream = new LowerCaseTokenizer(Version.LUCENE_46, new StringReader(docContent));
				tokenStream = new StopFilter(Version.LUCENE_46, tokenStream, sets);
				tokenStream.reset();
				while (tokenStream.incrementToken()) {
					String term = tokenStream.getAttribute(CharTermAttribute.class).toString();
//					String term = word;
//					if(word.length()>4){
//						stemmer.setCurrent(word);
//						stemmer.stem();
//					    term = stemmer.getCurrent();
//					}
					
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
						termfreq.add(0.0);
					}
					
					// add docfreq for the first occurrence in the document
					if(termfreq.get(pos)==0){
						docfreq.set(pos, docfreq.get(pos)+1);
					}
					
					termfreq.set(pos, termfreq.get(pos)+tw);
				}
				tokenStream.close();
			}
			
			termfreqs.add(termfreq);
			
			for(ArrayList<Double> tf:termfreqs){
				while(tf.size()<termPos.size()){
					tf.add(0.0);
				}
			}
		}
		
		System.out.println("Finished construction of frequency tables.");
		
		
		// Calculate weight of terms for docs
		int numOfDocs = termfreqs.size();
		int numOfTerms = docfreq.size();
		//System.out.println(numOfTerms);
		double weight[][] = new double[numOfDocs][numOfTerms];
		
		for(int j = 0; j < numOfDocs; j++)
			for(int i = 0; i < numOfTerms; i++)
				weight[j][i] = ((double)termfreqs.get(j).get(i)) * Math.log10((double)docs.size()/(double)docfreq.get(i));
		
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
		
		quickSortPos(modifiedQuery, queryPos, 0, numOfTerms-1);
		
		System.out.println("Expansion: ");
		for(int i=0;i<10;i++){
			System.out.println(posTerm.get(queryPos[i])+" "+modifiedQuery[i]);
		}
		
		// Expand query keywords
		int i=0;
		int size = query.keywords.size();
		while(query.keywords.size()<size+2 && i<modifiedQuery.length){
			if(modifiedQuery[i]>MAX_WEIGHT){
				i++;
				continue;
			}
			String newword = posTerm.get(queryPos[i]);
			boolean  exist = false;
			for(String keyword:query.keywords){
				if(keyword.toLowerCase().contains(newword) || newword.contains(keyword)){
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
		for(i=0;i<query.keywords.size();i++){
			keywordPos[i]=i;
		}
		for(int k=0;k<docs.size();k++){
			// initialization
			double[] currentWeight = new double[query.keywords.size()];
			Arrays.fill(currentWeight, Integer.MAX_VALUE);
			int[] currentPos = new int[query.keywords.size()];
			for(i=0;i<query.keywords.size();i++){
				currentPos[i]=i;
			}
			
			boolean found = true;			
			// order terms in a doc to occurrence
			for(int j=0;j<query.keywords.size();j++){
				String keyword = query.keywords.get(j);
				Pattern p = Pattern.compile(keyword.toLowerCase());
				Matcher matcher = p.matcher(docs.get(k).fulltext);
				
				if(matcher.find()){
					found = true;
					currentWeight[j]=matcher.start();
				}
			}
			quickSortPos(currentWeight, currentPos, 0, currentPos.length-1);
			
			// add to cumulative order, later order has small weight;
			if(found==true){
				int count = 1;
				for(int j=0;j<currentPos.length;j++){
					if(currentWeight[currentPos[j]]!=Integer.MAX_VALUE){
						keywordWeight[currentPos[j]] += count;
						count++;
					}
					else
						keywordWeight[currentPos[j]] += 2*currentPos.length;
				}
			}
		}
		quickSortPos(keywordWeight, keywordPos, 0, query.keywords.size()-1);
		
		// Construct new keywords set
		ArrayList<String> keywords= new ArrayList<String>();
		for(i=0;i<query.keywords.size();i++){
			keywords.add(query.keywords.get(keywordPos[i]));
		}
		
		// Update query status
		query.resultCount = docs.size()-1;
		query.keywords = keywords;
		query.precision = (double)numOfRelevDocs/(double)(docs.size()-1);
		
		System.out.println("Finished expanding the query.");
		System.out.println("***************************************");
		System.out.println("Iteration "+query.iteration+" Finished.");
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
            String summary = (String) elist.get(2).getData()+" "+elist.get(1).getData();
            String url = (String) elist.get(4).getData();
            int count = docs.size()+1;
            
            System.out.println("============================================");
            System.out.println("Result "+ count);
            System.out.println("Title: "+title);
            System.out.println("Url: "+ url);
            System.out.println("Summary: "+summary);
            System.out.println("============================================");
            
            boolean  relevant = false;
            System.out.print("Relevant (Y/N)?");
			if ((line = reader.readLine()) != null) {
				if(line.equals("Y")||line.equals("y"))
					relevant = true;
			}
			
			// convert content to lower case
            Doc doc = new Doc(title.toLowerCase(),url,summary.toLowerCase(),relevant);
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
