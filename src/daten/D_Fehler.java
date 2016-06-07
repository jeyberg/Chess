package daten;

import daten.D;
/** @author Frank Dopatka */
public class D_Fehler extends D {
	public D_Fehler(){
	}
	
	public D_Fehler(String meldung){
		addString("meldung",meldung);
	}
}
