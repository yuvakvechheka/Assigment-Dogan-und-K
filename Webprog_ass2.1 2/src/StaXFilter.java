import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.sun.xml.internal.bind.marshaller.DataWriter;

import jaxb.script.*;

public class StaXFilter {
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
	
	//	 Aktuelle Position im Item von XML Element
	//	 Fuer die Indizes von currentNews und position gilt:
	//	 0 - Title, 1 - Description, 2 - GUID, 3 - Category, 4 - PubDate,
	//	 -1 - ArrayIndexOutOfBoundsException
	private static int cursor;

	// Klassen Konstruktur
	public StaXFilter() {
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
	
	//reset wenn item ist found
	private static void found() {
		if (filter) {
			filter = false;
			System.out.println("Filtered news found and added to the list");
		}
		for (int i = 0; i < currentNews.length; i++)
			currentNews[i] = "";
		item = false;
	}
	
	// neu Method so man Byte by Byte lesen kann
	public static String readFile(String path, Charset encoding) throws IOException {
		System.out.println(path);
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	public static void StaXReader(String feed, boolean online) throws Exception {
		// neu file path erstellen
		String newFeed = "src/newTestStax.rss";
		// file Datei von source speichen (Byte by Byte)
		String newFeedData ="";
		if (online){
			newFeedData = new Scanner(new URL(feed).openStream(), "UTF-8").useDelimiter("\\A").next();
		} else{
			newFeedData = readFile(feed, StandardCharsets.UTF_8);
		}
	
		if(newFeedData.contains("& ")){
			// wenn "&" ist found, tausch das mit "&amp;"
			System.out.println("Contains '&' character, changing now...");
			newFeedData = newFeedData.replaceAll("&", "&amp;");
			System.out.println("All '&' has been changed to &amp;. \n");
		}
		else{
			// wenn nicht gefunden
			System.out.println("Does not contain '&', move to the next process!");
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(newFeed))) {
			// speichen die geandert Datei in unsere neu File (newFeed)
			out.print(newFeedData);
		}
		System.out.println("Start Phasing Feed...\n");
		try {
			// instanz für XMLInputFactory
			XMLInputFactory factory = XMLInputFactory.newInstance();
			// XMLEventReader erstellen
			
			XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(newFeed));
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				switch (event.getEventType()) {
				
				// Start_Element fang hier an!
				case XMLStreamConstants.START_ELEMENT:
					StartElement startElement = event.asStartElement();
					String qNameS = startElement.getName().getLocalPart();
					if (qNameS.equals("item")) {
						item = true;
					}
					else if (!filter && item) {
						if (qNameS.equals("title"))
							cursor = 0;
						else if (qNameS.equals("description"))
							cursor = 1;
					} else if (filter && item) {
						if (qNameS.equals("guid"))
							cursor = 2;
						else if (qNameS.equals("category"))
							cursor = 3;
						else if (qNameS.equals("pubDate"))
							cursor = 4;
					}
				// Start_Element ended hier!
				
					break;
				// Characters fang hier an!
				case XMLStreamConstants.CHARACTERS:
					Characters characters = event.asCharacters();
					try {
						currentNews[cursor] += characters.toString();
					} catch (ArrayIndexOutOfBoundsException e) {
					}
				// Characters ended hier!
					
					break;
				// End_Element fang hier an!
				case XMLStreamConstants.END_ELEMENT:
					EndElement endElement = event.asEndElement();
					String qNameE = endElement.getName().getLocalPart();
					if (qNameE.equals("item")) {
						if (filter)
							filteredNews.getItem().add(itemType);
						found();
					} else if (cursor == 1 && qNameE.equals("description")) {
						String temp;
						for (String fc : filterCrit)
							// Filter Kriterien auf Description suchen
							if (currentNews[cursor].contains(fc)) {
								filter = true;
								break;
							}
						if (filter) {
							// wenn stimmt, neu Item einzufugen, 
							//Title und Description Speichen
							temp = currentNews[cursor].replaceAll("&", "&amp;");
							temp = temp.replaceAll("\"", "&quot;");
							temp = temp.replaceAll(">", "&gt;");
							temp = temp.replaceAll("'", "&apos;");
							itemType = objectFactory.createItemType();
							itemType.setTitle(currentNews[cursor - 1]);
							itemType.setDescription(temp);
						}
					} else if (cursor == 2 && qNameE.equals("guid")) {
							// wenn GUID gibt, GUID speichen
						itemType.setGuid(currentNews[cursor]);
						for (ItemType it : filteredNews.getItem())
							// Falls die GUID schon in der Liste vorhanden ist,
							// Filter wieder schliessen
							if (it.getGuid().equals(currentNews[cursor])) {
								filter = false;
								break;
							}
					} else if (cursor == 3 && qNameE.equals("category"))
						// wenn category gibt, category speichen
						itemType.setCategory(currentNews[cursor]);
					else if (cursor == 4 && qNameE.equals("pubDate"))
						// wenn pubDate gibt, pubDate speichen
						itemType.setPubDate(currentNews[cursor]);

					cursor = -1;
				// End_Element ended hier!
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("StaXFilter Initialing...\n");
		
	// 1. Instanz des ContentHandlers erzeugen (Class Construktur)
		StaXFilter contentHandler = new StaXFilter();
		
	// 2. Feed Path setzen und StaXReader aufrufen!
		String feed = "src/test.rss";
		String spiegel = "http://www.spiegel.de/schlagzeilen/index.rss";
		String heise = "https://www.heise.de/newsticker/heise.rdf";
		String tagesschau = "http://www.tagesschau.de/xml/rss2";
		
		StaXReader(feed,false);
//		StaXReader(spiegel, true);
//		StaXReader(heise,true);
		StaXReader(tagesschau,true);

		
	// 3. Jaxb script initialisieren
		// Jaxb script path
		String jaxb = "jaxb.script";								
		System.out.println("\n" + "Initialising JAXB Script...");
		System.out.println("JAXB Location: " + jaxb);
		JAXBContext jc = JAXBContext.newInstance(jaxb);
		// instanz für Marshaller erstellen 
		Marshaller m = jc.createMarshaller();	
		// XML Indentation Format
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		// Marschaller UTF-8 Setzen
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");			
		

	// 4. Schema Setzen
		// Schema Path
		String sh = "src/news.xsd";									
		System.out.println("\n" + "Binding Schema...");
		System.out.println("Schema Location: " + sh + "\n");
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(new File(sh));
		m.setSchema(schema);

	// 5. Output XML Daten
		// output path
		String output = "ErgebnisSTAX.xml";						
		System.out.println("Generating result as xml in path: " + output);
		// create new File
		OutputStream os = new FileOutputStream(output);	
		PrintWriter printWriter = new PrintWriter(os);	
		
		// Unsere programmierte JaxbCharacterEscapeHandler Benutzen 
		//bzw. NoEscapeHandler
		DataWriter dataWriter = new DataWriter(printWriter, "UTF-8", new JaxbCharacterEscapeHandler());
		//Start Marschalling
		m.marshal(contentHandler.getFilteredNews(), dataWriter);
	}
}
