import java.io.File;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class testXML {
	public static void main(String[] args) throws DocumentException{
		SAXReader reader = new SAXReader();
		File xml = new File("result.xml");
	    Document document = reader.read(xml);
		Element root = document.getRootElement();
		
		for ( Iterator i = root.elementIterator("entry"); i.hasNext(); ) {
            Element entry = (Element) i.next();
            System.out.println(entry.element("content").element("properties").element("ID").getData());
//            if((line = reader.readLine())!=null){
//    			
//    		}
        }
	}
}
