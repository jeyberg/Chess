package restClient;
/** @author Frank Dopatka */
public interface iBackendSpielAdmin {
	String neuesSpiel();
	String speichernSpiel(String pfad);
	String ladenSpiel(String pfad);
}
