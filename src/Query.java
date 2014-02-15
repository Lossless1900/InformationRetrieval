public class Query {
	double precision;
	String keywords;
	int iteration;
	String accountKey;

	public Query(String keywords, double precision, String accountKey) {
		this.precision = precision;
		this.keywords = keywords.replace(" ", "%20");
		this.iteration = 0;
		this.accountKey = accountKey;
	}
}