import java.io.IOException;
import java.util.ArrayList;

import org.dom4j.DocumentException;


public class Driver {
	public static void main(String[] args) throws IOException, DocumentException{
		if(args.length<3){
			System.out.println("Usage: java FeedbackBing <client-key> <precision> <'query'> <full feedback> <# additional terms>");
			return;
		}
		
//		args[2] = "columbia";
		String accountKey = args[0];
		double precision = Double.valueOf(args[1]);
		ArrayList<String> keywords = new ArrayList<String>();
		for(String str:args[2].split(" ")){
			keywords.add(str);
		}
		Query query = new Query(keywords,0,precision,accountKey);		
		RetrievalIteration iteration = new RetrievalIteration();		
		do{
			iteration.startIteration(query);
		}while(query.precision>0 && query.precision<query.goalprecision && query.resultCount>0);
		
		if(query.precision>=query.goalprecision){
			System.out.println("Desired precision reached, done.");
		}
		else if(query.resultCount==0){
			System.out.println("No result found, done.");
		}
		else if(query.precision==0){
			System.out.println("No result relevant, done.");
		}
	}
}
