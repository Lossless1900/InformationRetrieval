import java.util.ArrayList;

public class Query {
	double precision;
	double goalprecision;
	ArrayList<String> keywords;
	int iteration;
	String accountKey;
	int resultCount = 0;

	public Query(ArrayList<String> keywords, double precision, double goalprecision, String accountKey) {
		this.precision = precision;
		this.keywords = keywords;
		this.accountKey = accountKey;
		this.goalprecision = goalprecision;
		this.iteration = 0;
		this.resultCount = 0;
	}
}