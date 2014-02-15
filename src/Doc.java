public class Doc {
	String title;
	String url;
	String summary;
	boolean relevant;

	public Doc(String title, String url, String summary, boolean relevant) {
		this.title = title;
		this.url = url;
		this.summary = summary;
		this.relevant = relevant;
	}
}