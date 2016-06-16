package mygame;

import daten.D;
import restClient.BackendSpielAdminStub;
import restClient.BackendSpielStub;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.RadioButtonGroupStateChangedEvent;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.Display;

/**
 * Interaktion zwischen Spieler und GUI und initialisiert 3D Darstellung.
 * @author Andrejtschik Johann, Watolla Martin
 */
public class Main extends SimpleApplication implements ScreenController {

    private GeometrieKonstruktor gKonstruktor = new GeometrieKonstruktor();
    private static BackendSpielAdminStub adminStub = null;
    private static BackendSpielStub spielStub = null;
    private Nifty nifty;
    private boolean istFliegend = false;
    private Zugmanager zmngr;
    private int anzahlZeuge = 0;
    private boolean istPausiert = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    /**
     * Bildschirmgröße wird auf variabel gestellt, die Kamera wird statisch und
     * ihre Bewegungsgeschwindigkeit wird erhöht, startet Initialisierungen für
     * GUI und Belegung.
     */
    @Override
    public void simpleInitApp() {
        Display.setResizable(true);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(15f);
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White);
        rootNode.addLight(al);
        initGui();
        guiNode.attachChild(gKonstruktor.initFadenkreuz(guiFont, this, assetManager));
        initBelegung();
    }

    /**
     * Aktualisiert den Spielzustand. Bei Änderung der Fenstergröße wird das
     * Fadenkreuz neu gezeichnet, bei Änderung der Spieldaten werden die Figuren
     * und die Historie aktualisiert.
     *
     * @param tpf TimePerFrame, wird hier nicht benötigt.
     */
    @Override
    public void simpleUpdate(float tpf) {
        if (Display.wasResized()) {
            reshape(Display.getWidth(), Display.getHeight());
            guiNode.detachAllChildren();
            guiNode.attachChild(gKonstruktor.initFadenkreuz(guiFont, this, assetManager));
        }
        if (spielStub != null) {
            if (!istPausiert) {
                String xml = spielStub.getSpielDaten();
                ArrayList<D> daten = Xml.toArray(xml);
                String s = daten.get(0).getProperties().getProperty("anzahlZuege");
                String status = daten.get(0).getProperties().getProperty("status");
                if (s != null) {
                    int zeuge = Integer.parseInt(s);
                    if (zeuge > anzahlZeuge || zeuge < anzahlZeuge) {
                        gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
                        aktualisiereHistorie();
                        anzahlZeuge = zeuge;
                        if (!status.equals("") && !status.equals("null")) {
                            aktualisiereNachrichten(status);
                        }
                    }

                }
            }

        }
    }


    /**
     * Es werden Event Trigger für die linke Maustaste und Alt mit einem Key
     * registriert. Anschließend wird ein actionListener für die beiden Events
     * registriert.
     */
    public void initBelegung() {
        inputManager.addMapping("Klick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Mouse_Mode", new KeyTrigger(KeyInput.KEY_LMENU));
        inputManager.addListener(actionListener, "Klick");
        inputManager.addListener(actionListener, "Mouse_Mode");
    }
    
    /**
     * Hört auf "Klick" und "Mouse_Mode" Events. Bei "Klick" wird überprüft ob
     * das Spiel nicht pausiert ist und der Spieler am Zug ist. Wenn ja, wird
     * das geklickte Objekt bestimmt. Bei einer Figur der eigenen Farbe werden
     * die erlaubte Züge abgefragt und die entsprechenden Kacheln markiert. Bei
     * einer gegnerischen Figur oder einer markierten Kachel wird eine
     * Zuganfrage gesendet. Bei dem "Mouse_Mode" Event wird der Flugmodus der
     * Kamera umgeschaltet.
     */
    public ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Klick") && !isPressed && !istPausiert && zmngr.getAmZug(spielStub)) {
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                gKonstruktor.schachbrett.collideWith(ray, results);
                if (results.size() > 0) {
                    //Spatial g = results.getClosestCollision().getGeometry().getParent().getParent().getParent().getParent();
                    Geometry g = results.getClosestCollision().getGeometry();
                    String pos = g.getUserData("position");
                    String typ = g.getUserData("typ");
                    if (typ.equals("figur")) {
                        if (g.getUserData("farbe").equals("weiss") && zmngr.getIsWeiss()
                                || g.getUserData("farbe").equals("schwarz") && !zmngr.getIsWeiss()) {
                            getErlaubteZeuge(pos);
                        } else if (gKonstruktor.markierteKacheln.containsKey(pos)) {
                            ziehe(gKonstruktor.gewaehleteKachel, pos);
                        }
                    } else if (typ.equals("kachel") && g.getUserData("markiert").equals(true)) {
                        ziehe(gKonstruktor.gewaehleteKachel, pos);
                    }
                }
            } else if (name.equals("Mouse_Mode") && !isPressed) {
                flyCam.setDragToRotate(istFliegend);
                istFliegend = !istFliegend;
            }
        }
    };

    /**
     * Initialisiert die GUI, dessen Layout in der screen.xml spezifiziert
     * wurde.
     */
    public void initGui() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/screen.xml", "start", this);
        guiViewPort.addProcessor(niftyDisplay);
    }

    /**
     * Stellt eine Verbindung mit dem Server her, startet ein neues Spiel und
     * startet Initialisierungen von 3D Darstellungen.
     */
    public void spielErstellen() {
        Screen scrn = nifty.getCurrentScreen();
        String text = scrn.findNiftyControl("ip", TextField.class).getRealText();
        boolean isWeiss = scrn.findNiftyControl("weiss", RadioButton.class).isActivated(); // Radio Buttons können nicht abgewählt werden. Es reicht den Wert von einem Button zu erfragen.
        if (!text.equals("")) {
            adminStub = new BackendSpielAdminStub("http://" + text);
            spielStub = new BackendSpielStub("http://" + text);
            this.zmngr = new Zugmanager(isWeiss);
            String s = adminStub.neuesSpiel();
            ArrayList<D> daten = Xml.toArray(s);
            if (daten.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
                gKonstruktor.initPositionen();
                gKonstruktor.initBrett(assetManager, rootNode);
                gKonstruktor.initRandZiffer(guiFont, assetManager, rootNode);
                gKonstruktor.initRandBuchstabe(guiFont, assetManager, rootNode);
                gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
                nifty.gotoScreen("spiel");
                setKameraPosition(zmngr.getIsWeiss());
            }
        }
    }

    /**
     * Stellt eine Verbindung mit dem Server her und startet Initialisierungen
     * von 3D Darstellungen.
     */
    public void spielBeitreten() {
        Screen scrn = nifty.getCurrentScreen();
        String text = scrn.findNiftyControl("ip", TextField.class).getRealText();
        boolean isWeiss = scrn.findNiftyControl("weiss", RadioButton.class).isActivated();
        if (!text.equals("")) {
            adminStub = new BackendSpielAdminStub("http://" + text);
            spielStub = new BackendSpielStub("http://" + text);
            this.zmngr = new Zugmanager(isWeiss);
            gKonstruktor.initPositionen();
            gKonstruktor.initBrett(assetManager, rootNode);
            gKonstruktor.initRandZiffer(guiFont, assetManager, rootNode);
            gKonstruktor.initRandBuchstabe(guiFont, assetManager, rootNode);
            gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
            aktualisiereHistorie();
            nifty.gotoScreen("spiel");
            setKameraPosition(zmngr.getIsWeiss());
        }
    }
    
    @NiftyEventSubscriber(id="skin")
    public void onSkinGoupChanged(final String id, final RadioButtonGroupStateChangedEvent event){
        gKonstruktor.setSkinPfad(event.getSelectedId());
    }

    /**
     * Startet ein neues Spiel und lässt die Figuren neu darstellen.
     */
    public void neuesSpiel() {
        adminStub.neuesSpiel();
        gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
    }

    /**
     * Funktionalität aktuell nicht unterstüzt.
     */
    public void ladenSpiel() {
    }

    /**
     * Funktionalität aktuell nicht unterstüzt.
     */
    public void speichernSpiel() {
    }

    /**
     * Beendet das Spiel.
     */
    public void verlassenSpiel() {
        this.stop();
    }

    /**
     * Methode des ScreenController Interface, welche aktuell nicht erforderlich
     * ist.
     *
     * @param nifty
     * @param screen
     */
    public void bind(Nifty nifty, Screen screen) {
        //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Methode des ScreenController Interface, welche aktuell nicht erforderlich
     * ist.
     */
    public void onStartScreen() {
        //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Methode des ScreenController Interface, welche aktuell nicht erforderlich
     * ist.
     */
    public void onEndScreen() {
        //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Sendet eine Anfrage der bisherigen Züge an den Server und gibt diese als
     * eine Liste von Strings zurück.
     *
     * @return Die String-Liste bereits erfolgter Züge.
     */
    public ArrayList<String> getHistorie() {
        String xml = spielStub.getZugHistorie();
        ArrayList<D> data = Xml.toArray(xml);
        ArrayList<String> historie = new ArrayList<String>();
        for (D d : data) {
            String s = d.getProperties().getProperty("zug");
            historie.add(s);
        }
        return historie;
    }

    /**
     * Sendet eine Anfrage für erlaubte Züge von Position pos aus.
     *
     * @param pos Position in Schachnotation.
     */
    public void getErlaubteZeuge(String pos) {
        String xml = spielStub.getErlaubteZuege(pos);
        List<String> positions = new ArrayList<String>();
        ArrayList<D> data = Xml.toArray(xml);
        for (D d : data) {
            String s = d.getProperties().getProperty("nach");
            positions.add(s);
        }
        gKonstruktor.gewaehleteKachel = pos;
        gKonstruktor.markiereKacheln(positions, assetManager);
    }

    /**
     * Liest die eingegebenen Koordinaten für einen Zug von der GUI aus und
     * sendet eine Zug-Anfrage an den Server. Die eingegebenen Koordinaten
     * werden gegen ein Pattern geprüft, das Strings matched, die der
     * Schachnotation entsprechen. Case-insensitive.
     */
    public void zieheVonGui() {
        String s = "";
        String pattern = "[a-hA-H]{1}[1-8]{1}";
        Pattern r = Pattern.compile(pattern);
        Screen scrn = nifty.getCurrentScreen();
        String von = scrn.findNiftyControl("von", TextField.class).getRealText();
        String nach = scrn.findNiftyControl("nach", TextField.class).getRealText();
        Matcher m = r.matcher(von);
        Matcher m2 = r.matcher(nach);
        if (m.find() && m2.find()) {
            scrn.findNiftyControl("von", TextField.class).setText(s.subSequence(0, 0));
            scrn.findNiftyControl("nach", TextField.class).setText(s.subSequence(0, 0));
            if (zmngr.getAmZug(spielStub)) {
                ziehe(von, nach);
            }
        }

    }

    /**
     * Sendet eine Zug-Anfrage an den Server. Ist der Zug illegal wird eine
     * Anfrage für erlaubte Züge von to aus gesendet.
     *
     * @param from Startkoordinate für den Zug in Schachnotation.
     * @param to Endkoordinaten für den Zug in Schachnotation.
     */
    public void ziehe(String from, String to) {
        System.out.println("ziehe");
        String xml = spielStub.ziehe(from, to);
        ArrayList<D> data = Xml.toArray(xml);
        if (data.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
            gKonstruktor.resetKacheln(assetManager);
            gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
            zmngr.setAmZug(false);
        } else {
            getErlaubteZeuge(to);
        }
    }

    /**
     * Positioniert die Spielerkamera entsprechend seiner Farbe.
     *
     * @param isWeiss true: Spieler spielt Weiss. false: Spieler spielt schwarz.
     */
    public void setKameraPosition(boolean isWeiss) {
        if (isWeiss) {
            cam.setLocation(new Vector3f(0f, 20f, 25f));
        } else {
            cam.setLocation(new Vector3f(0f, 20f, -25f));
        }
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
    }

    /**
     * Sendet eine Anfrage für erfolgte Züge an den Server und aktualisiert die
     * entsprechende ListBox.
     *
     */
    public void aktualisiereHistorie() {
        String xml = spielStub.getZugHistorie();
        if (xml != null) {
            ArrayList<D> daten = Xml.toArray(xml);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBoxWeiss = screen.findNiftyControl("historieW", ListBox.class);
            ListBox listBoxSchwarz = screen.findNiftyControl("historieS", ListBox.class);
            listBoxWeiss.clear();
            listBoxSchwarz.clear();
            for (int i = 0; i < daten.size(); i++) {
                if (i % 2 == 0) {
                    listBoxWeiss.addItem(daten.get(i).getProperties().getProperty("zug"));
                } else {
                    listBoxSchwarz.addItem(daten.get(i).getProperties().getProperty("zug"));
                }
            }
        }
    }

    /**
     * Aktualisiert die "nachrichten" ListBox mit einer passenden Nachricht,
     * wenn einer der Spieler im Schach oder Schach Matt.
     *
     * @param nachricht Die Servernachricht.
     */
    public void aktualisiereNachrichten(String nachricht) {
        if (nachricht.equals("SchwarzSchachMatt")) {
            nachricht = "Schwarz im Schach Matt!";
            if (zmngr.getIsWeiss()) {
                nachricht += " Gewonnen!";
            } else {
                nachricht += " Verloren!";
            }
        } else if (nachricht.equals("SchwarzSchach")) {
            nachricht = "Schwarz im Schach!";
        } else if (nachricht.equals("WeissSchachMatt")) {
            nachricht = "Weiss im Schach Matt!";
            if (zmngr.getIsWeiss()) {
                nachricht += " Verloren!";
            } else {
                nachricht += " Gewonnen!";
            }
        } else if (nachricht.equals("WeissSchach")) {
            nachricht = "Weiss im Schach!";
        }
        Screen screen = nifty.getScreen("spiel");
        ListBox listBox = screen.findNiftyControl("nachrichten", ListBox.class);
        listBox.addItem(nachricht);
    }

    /**
     * Sendet eine Anfrage an den Server für eine vergangene Brettbelegung und
     * zeigt diese an. Dank
     *
     * @NiftyEventSubscriber muss ein Listener von Hand erstellt und registriert
     * werden. Reagiert nur auf Events der Liste für erfolgte Züge von Weiss.
     * @param id ID des Elements, das das Event ausgelöst hat.
     * @param event Das ausgelöste Event. In diesem Fall die Auswahl eines Items
     * aus "historieW".
     */
    @NiftyEventSubscriber(id = "historieW")
    public void listBoxWausgewaehlt(final String id, final ListBoxSelectionChangedEvent<String> event) {
        List<Integer> auswahl = event.getSelectionIndices();
        if (auswahl.size() > 0) {
            istPausiert = true;
            int index = auswahl.get(0);
            index += index + 1;
            gKonstruktor.figuren(spielStub.getBelegung(index), assetManager, rootNode);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBox = screen.findNiftyControl("historieW", ListBox.class);
            listBox.deselectItemByIndex(auswahl.get(0));
        }
    }

    /**
     * Sendet eine Anfrage an den Server für eine vergangene Brettbelegung und
     * zeigt diese an. Dank
     *
     * @NiftyEventSubscriber muss ein Listener von Hand erstellt und registriert
     * werden. Reagiert nur auf Events der Liste für erfolgte Züge von Schwarz.
     * @param id ID des Elements, das das Event ausgelöst hat.
     * @param event Das ausgelöste Event. In diesem Fall die Auswahl eines Items
     * aus "historieW".
     */
    @NiftyEventSubscriber(id = "historieS")
    public void listBoxSausgewaehlt(final String id, final ListBoxSelectionChangedEvent<String> event) {
        List<Integer> auswahl = event.getSelectionIndices();
        if (auswahl.size() > 0) {
            istPausiert = true;
            int index = auswahl.get(0);
            index += index + 2;
            gKonstruktor.figuren(spielStub.getBelegung(index), assetManager, rootNode);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBox = screen.findNiftyControl("historieS", ListBox.class);
            listBox.deselectItemByIndex(auswahl.get(0));
        }
    }

    /**
     * Das Spiel wird fortgeführt und die Figuren aktualisiert.
     */
    public void weiterspielen() {
        istPausiert = false;
        gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
    }
}