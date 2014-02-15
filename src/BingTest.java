import java.io.BufferedInputStream;
import java.io.BufferedReader; 
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;


//Download and add this library to the build path.
import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class BingTest {

	public static void main(String[] args) throws IOException, DocumentException {
		String bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27gates%27&$top=10&$format=Atom";
		//Provide your account key here. 
		String accountKey = "PYQaQKNv704nTLnBiMQ7gSNZyYWaoD4uhm2Tk/qmN50";
		
		byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
		String accountKeyEnc = new String(accountKeyBytes);

		URL url = new URL(bingUrl);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
				
		InputStream inputStream = (InputStream) urlConnection.getContent();		
		byte[] contentRaw = new byte[urlConnection.getContentLength()];
		inputStream.read(contentRaw);
		String content = new String(contentRaw);
		
		Document document = DocumentHelper.parseText(content);
		Element root = document.getRootElement();
		BufferedInputStream inStream = new BufferedInputStream(System.in);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		String line = "";
		// iterate through child elements of root with element name "foo"
		for ( Iterator<Element> i = root.elementIterator("entry"); i.hasNext(); ) {
            Element entry = (Element) i.next();
            System.out.println(entry.asXML());
        }

		//The content string is the xml/json output from Bing.
		System.out.println(content);
	}

}
