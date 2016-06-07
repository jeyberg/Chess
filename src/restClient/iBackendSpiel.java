package restClient;
/** @author Frank Dopatka */
public interface iBackendSpiel {
	String getSpielDaten();
	String getAktuelleBelegung();
	String getBelegung(int nummer);
	String getAlleErlaubtenZuege();
	String getFigur(String position);
	String getErlaubteZuege(String position);
	String ziehe(String von,String nach);
	String getZugHistorie();
}