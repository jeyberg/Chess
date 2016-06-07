package restClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import daten.D;
import mygame.Xml;
import restClient.iBackendSpielAdmin;
/** @author Frank Dopatka */
public class BackendSpielAdminStub implements iBackendSpielAdmin{
	private static final String urlUnterPfad="schach/spiel/admin/";
	private static final boolean log=true;
	private String url;
	private Client client=ClientBuilder.newClient();
	
	public BackendSpielAdminStub(String url){
		if (url.endsWith("/"))
			this.url=url+urlUnterPfad;
		else
			this.url=url+"/"+urlUnterPfad;
	}
	
	private String getXmlvonRest(String pfad){
		String anfrage=url+pfad;
		if ((log)&&(!anfrage.contains("/update/"))) System.out.println("CLIENT ANFRAGE: "+anfrage);
		String s=client.target(anfrage).request().accept("application/xml").get(String.class);
		if ((log)&&(!anfrage.contains("/update/"))){
			ArrayList<D> daten=Xml.toArray(s);
			//System.out.println(daten);
		}
		return s;
	}

	@Override
	public String neuesSpiel() {
		return getXmlvonRest("neuesSpiel");
	}

	@Override
	public String speichernSpiel(String pfad) {
		try {
			return getXmlvonRest("speichernSpiel"+"/"+URLEncoder.encode(""+pfad,"ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String ladenSpiel(String pfad) {
		try {
			return getXmlvonRest("ladenSpiel"+"/"+URLEncoder.encode(""+pfad,"ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
