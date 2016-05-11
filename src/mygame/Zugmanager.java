/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import daten.D;
import restClient.BackendSpielStub;
import java.util.ArrayList;

/**
 *
 * @author ivan
 */
public class Zugmanager {

    private boolean isWeiss;
    private boolean amZug;

    public Zugmanager(boolean isWeiss) {
        if (isWeiss) {
            this.amZug = true;
        }
        this.isWeiss = isWeiss;
    }

    public void setIsWeiss(boolean isWeiss) {
        this.isWeiss = isWeiss;
    }

    public boolean getIsWeiss() {
        return this.isWeiss;
    }

    public void setAmZug(boolean amZug) {
        this.amZug = amZug;
    }

    public boolean getAmZug() {
        return this.amZug;
    }

    boolean getAmZug(BackendSpielStub spielStub) {
        boolean amZug = false;
        String xml = spielStub.getSpielDaten();
        int anzahlZeuge;
        ArrayList<D> daten = Xml.toArray(xml);
        String s = daten.get(0).getProperties().getProperty("anzahlZuege");
        if (s != null) {
            anzahlZeuge = Integer.parseInt(s);
            if (((anzahlZeuge & 1) == 0 && this.isWeiss) || ((anzahlZeuge & 1) != 0 && !this.isWeiss)) {
                amZug = true;
            }
        }else{
            amZug = true;
        }
        return amZug;
    }
}
