import java.io.IOException;
import java.util.HashSet;

import org.dom4j.DocumentException;


public class Driver {
	public static void main(String[] args) throws IOException, DocumentException{
		if(args.length<3){
			System.out.println("Usage: java FeedbackBing <client-key> <precision> <'query'> <full feedback> <# additional terms>");
			return;
		}
		
		String accountKey = args[0];
		double precision = Double.valueOf(args[1]);
		HashSet<String> keywords = new HashSet<String>();
		for(String str:args[2].split(" ")){
			keywords.add(str);
		}
		Query query = new Query(keywords,0,accountKey);
		
		System.out.println("***************************************");
		System.out.println("Client key: "+query.accountKey);
		System.out.println("Query: "+args[2]);
		System.out.println("Precision: "+precision);
		System.out.println("***************************************");
		
		RetrievalIteration iteration = new RetrievalIteration();		
		if(query.precision<precision){
			iteration.startIteration(query);
		}
	}
}
