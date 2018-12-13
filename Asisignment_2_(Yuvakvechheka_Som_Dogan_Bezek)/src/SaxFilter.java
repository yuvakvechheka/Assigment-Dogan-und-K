
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class SaxFilter extends DefaultHandler {

	static String tmpFeed;
	public void startDocument() throws SAXException {
		
		
	}

	public void endDocument() throws SAXException {
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		System.out.println();
	}
	public void check(String feed, boolean online) throws IOException {
		
		
	}
	public static void main(String[] args) throws Exception {
		// 1. Instanz des ContentHandlers erzeugen
		ContentHandler contentHandler = new SaxFilter();
		// 2. SAX-Treiber erzeugen
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);
		SAXParser parser = saxParserFactory.newSAXParser();
		XMLReader xmlReader = parser.getXMLReader();
		// 3. Call-back für ContentHandler setzen
		xmlReader.setContentHandler(contentHandler);
		// 4. Verarbeitung starten
		xmlReader.parse("http://www.spiegel.de/schlagzeilen/index.rss");
		
		
	}

}