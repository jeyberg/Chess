package restClient;

import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import daten.D;
import mygame.Xml;
import mygame.Xml;
import restClient.iBackendSpiel;

/** @author Frank Dopatka */
public class BackendSpielStub implements iBackendSpiel{
	private static final String urlUnterPfad="schach/spiel/";
	private static final boolean log=false;
	private String url;
	private Client client=ClientBuilder.newClient();
	
	public BackendSpielStub(String url){
		if (url.endsWith("/"))
			this.url=url+urlUnterPfad;
		else
			this.url=url+"/"+urlUnterPfad;
	}
	
	private String getXmlvonRest(String pfad){
		String anfrage=url+pfad;
		if (log) System.out.println("CLIENT ANFRAGE: "+anfrage);
		String s=client.target(anfrage).request().accept("application/xml").get(String.class);
		if (log){
			ArrayList<D> daten=Xml.toArray(s);
			System.out.println(daten);
		}
		return s;
	}
	
	@Override
	public String getSpielDaten() {
		return getXmlvonRest("getSpielDaten/");
	}

	@Override
	public String getAktuelleBelegung() {
		return getXmlvonRest("getAktuelleBelegung/");
	}
	
	@Override
	public String getBelegung(int nummer) {
		return getXmlvonRest("getBelegung/"+nummer);
	}
	
	@Override
	public String getAlleErlaubtenZuege() {
		return getXmlvonRest("getAlleErlaubtenZuege/");
	}

	@Override
	public String getFigur(String position) {
		return getXmlvonRest("getFigur/"+position);
	}

	@Override
	public String getErlaubteZuege(String position) {
		return getXmlvonRest("getErlaubteZuege/"+position);
	}

	@Override
	public String ziehe(String von,String nach) {
		return getXmlvonRest("ziehe/"+von+"/"+nach);
	}

	@Override
	public String getZugHistorie() {
		return getXmlvonRest("getZugHistorie/");
	}
}
