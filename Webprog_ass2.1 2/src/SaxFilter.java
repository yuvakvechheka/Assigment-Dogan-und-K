import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import com.sun.xml.internal.bind.marshaller.*;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import jaxb.script.*;

public class SaxFilter extends DefaultHandler{
	static XMLReader xmlReader;
	// ObjectFactory Erzeugen
	private static ObjectFactory objectFactory;

	// Variable für aller Ergebnisse bzw. gefilterter Nachrichten
	private static FilteredNews filteredNews;

	// Zwischenspeicher fuer gefilterte Nachrichten
	private static ItemType itemType;

	// currentNews BufferArray für die Inhalte der Tags,
	private static String[] currentNews;
	
	// filterCrit Array für Filterkriterien
	private static String[] filterCrit;

	// boolean für den XML-Tag,
	private static boolean item;
	
	// boolean für den Filter
	private static boolean filter;
	
	static String newFeedData="";
	
	//	 Aktuelle Position im Item von XML Element
	//	 Fuer die Indizes von currentNews und position gilt:
	//	 0 - Title, 1 - Description, 2 - GUID, 3 - Category, 4 - PubDate,
	//	 -1 - ArrayIndexOutOfBoundsException
	private static int cursor;

	// Klassen Konstruktur
	public SaxFilter() {
		objectFactory = new ObjectFactory();
		filteredNews = objectFactory.createFilteredNews();

		currentNews = new String[5];
		for (int i = 0; i < currentNews.length; i++)
			currentNews[i] = "";

		filterCrit = new String[5];
		filterCrit[0] = "Android";
		filterCrit[1] = "Java";
		filterCrit[2] = "Welt";
		filterCrit[3] = "Mensch";
		filterCrit[4] = "Berlin";

		filter = false;
		item = false;
		cursor = -1;
	}
	
	// get method für filterNews
	public FilteredNews getFilteredNews() {
		return filteredNews;
	}
	// Start_Element fang hier an!
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) {
		if (qName.equals("item"))
			item = true;
		else if (!filter && item) {
			if (qName.equals("title"))
				cursor = 0;
			else if (qName.equals("description"))
				cursor = 1;
		} else if (filter && item) {
			if (qName.equals("guid"))
				cursor = 2;
			else if (qName.equals("category"))
				cursor = 3;
			else if (qName.equals("pubDate"))
				cursor = 4;
		}
	}
	// Start-Element ended hier!
	
	// End-Element fang hier an!
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals("item")) {
			if (filter)
				filteredNews.getItem().add(itemType);
			found();
		} else if (cursor == 1 && qName.equals("description")) {
			String temp;
			for (String fc : filterCrit)
				// Filter Kriterien auf Description suchen
				if (currentNews[cursor].contains(fc)){
					filter = true;
					break;
				}
			if (filter) {
				// wenn stimmt, neu Item einzufugen, 
				//Title und Description Speichen
				temp=currentNews[cursor].replaceAll("&", "&amp;");
				temp=temp.replaceAll("\"", "&quot;");
				temp=temp.replaceAll(">", "&gt;");
				temp=temp.replaceAll("'", "&apos;");
				itemType = objectFactory.createItemType();
				itemType.setTitle(currentNews[cursor - 1]);
				itemType.setDescription(temp);
			}
		} else if (cursor == 2 && qName.equals("guid")) {
			// wenn GUID gibt, GUID speichen
			itemType.setGuid(currentNews[cursor]);
			for (ItemType it : filteredNews.getItem())
				// Falls die GUID schon in der Liste vorhanden ist,
				// Filter wieder schliessen
				if (it.getGuid().equals(currentNews[cursor])){
					filter = false;
					break;
				}
		} else if (cursor == 3 && qName.equals("category"))
			// wenn category gibt, category speichen
			itemType.setCategory(currentNews[cursor]);
		else if (cursor == 4 && qName.equals("pubDate"))
			// wenn pubDate gibt, pubDate speichen
			itemType.setPubDate(currentNews[cursor]);
		cursor = -1;
	}
	// End_Element ended hier!
	
	// Characters fang hier an!
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// Daten zu speichern!
		try {
			currentNews[cursor] += new String(ch, start, length);
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}
	// Characters ended hier!
	
	// wenn filter zutriff!
	private void found() {
		if (filter) {
			filter = false;
			System.out.println("Criteria met, and news added to the list");
		}
		for (int i = 0; i < currentNews.length; i++)
			currentNews[i] = "";
		item = false;
	}
	
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}
	
	// Datei um zu lesen, egal online oder offline, boolean true bedeuted ist es online datei
	static void read(String feed, boolean online) throws MalformedURLException, IOException{
		if (online){
			newFeedData = new Scanner(new URL(feed).openStream(), "UTF-8").useDelimiter("\\A").next();
		} else{
			newFeedData = readFile(feed, StandardCharsets.UTF_8);
		}
		String newFeed="src/newTestSax.rss";
		if(newFeedData.contains("& ")){
			// wenn "&" ist found, tausch das mit "&amp;"
			System.out.println("\nContains '&' character, changing now...");
			newFeedData = newFeedData.replaceAll("&", "&amp;");
			System.out.println("All '&' has been changed to &amp;. \n");
		}
		else{
			// wenn nicht gefunden
			System.out.println("\nDoes not contain '&', move to the next process!\n");
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(newFeed))) {
			// speichen die geandert Datei in unsere neu File (newFeed)
			out.print(newFeedData);
		}
		System.out.println("Start Phasing Feed...\n");
		try {
			xmlReader.parse(newFeed);
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws Exception{
		System.out.println("SaxFilter Initializing...");
		//1. Instanz des ContentHandler erzeugen
		SaxFilter contentHandler = new SaxFilter();
		//2. SAX Treiber auswählen
		xmlReader = XMLReaderFactory.createXMLReader();
		//3. call-back für ContentHandler setzen
		xmlReader.setContentHandler(contentHandler);
		//4. Verarbeitung starten
		String feed = "src/test.rss";
		String spiegel = "http://www.spiegel.de/schlagzeilen/index.rss";
		String heise = "https://www.heise.de/newsticker/heise.rdf";
		String tagesschau = "http://www.tagesschau.de/xml/rss2";
		// read File (true for online, false for offline)
		read(feed,false);
		read(spiegel,true);
		read(heise,true);
		read(tagesschau,true);
		
		//5. JAXB initialisieren
		String jaxb = "jaxb.script";
		System.out.println("\n"+"Initialising JAXB Script...");
		System.out.println("JAXB Location: "+ jaxb);
		JAXBContext jc = JAXBContext.newInstance(jaxb);
		// instanz für Marshaller erstellen 
		Marshaller m = jc.createMarshaller();
		// XML Indentation Format
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		// Marschaller UTF-8 Setzen
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		
		//6. Schema Setzen
		String sh = "src/news.xsd";
		System.out.println("\n"+"Binding Schema...");
		System.out.println("Schema Location: "+ sh +"\n");
		
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(new File(sh));
		m.setSchema(schema);
		
		//7. Output XML Daten
		String output = "ErgebnisSAX.xml";	
		System.out.println("Generating result as xml in path: " + output);
		OutputStream os = new FileOutputStream("ErgebnisSax.xml");
        PrintWriter printWriter = new PrintWriter(os);
        DataWriter dataWriter = new DataWriter(printWriter, "UTF-8", new JaxbCharacterEscapeHandler());
		m.marshal(contentHandler.getFilteredNews(), dataWriter);
	}
}

