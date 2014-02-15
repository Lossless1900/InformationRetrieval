import java.util.HashSet;

public class Query {
	double precision;
	HashSet<String> keywords;
	int iteration;
	String accountKey;

	public Query(HashSet<String> keywords, double precision, String accountKey) {
		this.precision = precision;
		this.keywords = keywords;
		this.iteration = 0;
		this.accountKey = accountKey;
	}
}