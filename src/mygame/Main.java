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
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.Display;

public class Main extends SimpleApplication implements ScreenController {

    private GeometrieKonstruktor gKonstruktor = new GeometrieKonstruktor();
    private static BackendSpielAdminStub adminStub = null; // = new BackendSpielAdminStub("http://192.168.56.1:8000")
    private static BackendSpielStub spielStub = null; // = new BackendSpielStub("http://192.168.56.1:8000");
    private Nifty nifty;
    private boolean istFliegend = false;
    private Zugmanager zmngr;
    private int anzahlZeuge = 0;
    private boolean istPausiert = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Display.setResizable(true);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(15f);
        initGui();
        guiNode.attachChild(gKonstruktor.initFadenkreuz(guiFont, this, assetManager, settings));
        initBelegung();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (Display.wasResized()) {
            int neueBreite = Math.max(Display.getWidth(), 1);
            int neueHöhe = Math.max(Display.getHeight(), 1);
            reshape(neueBreite, neueHöhe);
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

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    private void initBelegung() {
        inputManager.addMapping("Klick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Mouse_Mode", new KeyTrigger(KeyInput.KEY_LMENU));
        inputManager.addListener(actionListener, "Klick");
        inputManager.addListener(actionListener, "Mouse_Mode");
    }
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Klick") && !isPressed && !istPausiert && zmngr.getAmZug(spielStub)) {
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                gKonstruktor.schachbrett.collideWith(ray, results);
                if (results.size() > 0) {
                    Geometry g = results.getClosestCollision().getGeometry();
                    String pos = g.getUserData("position");
                    if (g.getUserData("typ").equals("figur")) {
                        if (g.getUserData("farbe").equals("weiss") && zmngr.getIsWeiss()
                                || g.getUserData("farbe").equals("schwarz") && !zmngr.getIsWeiss()) {
                            getErlaubteZeuge(pos);
                        } else {
                            if (gKonstruktor.markierteKacheln.containsKey(pos)) {
                                ziehe(gKonstruktor.gewaehleteKachel, pos);
                            }
                        }
                    } else if (g.getUserData("typ").equals("kachel")) {
                        if (g.getUserData("markiert")) {
                            ziehe(gKonstruktor.gewaehleteKachel, pos);
                        }
                    }
                }
            } else if (name.equals("Mouse_Mode") && !isPressed) {
                flyCam.setDragToRotate(istFliegend);
                istFliegend = !istFliegend;
                System.out.println(istFliegend);
            }

        }
    };

    void initGui() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/screen.xml", "start", this);
        guiViewPort.addProcessor(niftyDisplay);
    }

    public void spielErstellen() {

        Screen scrn = nifty.getCurrentScreen();
        String text = scrn.findNiftyControl("ip", TextField.class).getRealText();
        boolean isWeiss = scrn.findNiftyControl("weiss", RadioButton.class).isActivated();
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

    public void neuesSpiel() {
        System.out.println("starte neues Spiel");
        adminStub.neuesSpiel();
        gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
    }

    public void ladenSpiel() {
        System.out.println("neues Spiel");
    }

    public void speichernSpiel() {
        System.out.println("neues Spiel");
    }

    public void verlassenSpiel() {
        this.stop();
    }

    public void bind(Nifty nifty, Screen screen) {
        //To change body of generated methods, choose Tools | Templates.
    }

    public void onStartScreen() {
        //To change body of generated methods, choose Tools | Templates.
    }

    public void onEndScreen() {
        //To change body of generated methods, choose Tools | Templates.
    }

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

    void getErlaubteZeuge(String pos) {
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

    void ziehe(String from, String to) {
        System.out.println("ziehe");
        String xml = spielStub.ziehe(from, to);
        ArrayList<D> data = Xml.toArray(xml);
        if (data.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
            gKonstruktor.resetKacheln();
            gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
            zmngr.setAmZug(false);
        } else {
            getErlaubteZeuge(to);
        }
    }

    public void setKameraPosition(boolean isWeiss) {
        if (isWeiss) {
            cam.setLocation(new Vector3f(0f, 10f, 25f));
        } else {
            cam.setLocation(new Vector3f(0f, 10f, -25f));
        }
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
    }

    public void aktualisiereHistorie() {
        String xml = spielStub.getZugHistorie();
        if (xml != null) {
            ArrayList<D> daten = Xml.toArray(xml);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBoxWeiss = screen.findNiftyControl("historieW", ListBox.class);
            ListBox listBoxSchwarz = screen.findNiftyControl("historieS", ListBox.class);
            listBoxWeiss.clear();
            listBoxSchwarz.clear(); // listBox.addItem(d.getProperties().getProperty("zug"));
            for (int i = 0; i < daten.size(); i++) {
                if (i % 2 == 0) {
                    listBoxWeiss.addItem(daten.get(i).getProperties().getProperty("zug"));
                } else {
                    listBoxSchwarz.addItem(daten.get(i).getProperties().getProperty("zug"));
                }
            }
        }
    }

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

    @NiftyEventSubscriber(pattern = "historieW")
    public void listBoxWausgewaehlt(final String id, final ListBoxSelectionChangedEvent<String> event) {
        List<Integer> auswahl = event.getSelectionIndices();
        if (auswahl.size() > 0) {
            istPausiert = true;
            int index = auswahl.get(0);
            index += index + 1;
            gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBox = screen.findNiftyControl("historieW", ListBox.class);
            listBox.deselectItemByIndex(auswahl.get(0));
        }
    }

    @NiftyEventSubscriber(pattern = "historieS")
    public void listBoxSausgewaehlt(final String id, final ListBoxSelectionChangedEvent<String> event) {
        List<Integer> auswahl = event.getSelectionIndices();
        if (auswahl.size() > 0) {
            istPausiert = true;
            int index = auswahl.get(0);
            index += index + 2;
            gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
            Screen screen = nifty.getScreen("spiel");
            ListBox listBox = screen.findNiftyControl("historieS", ListBox.class);
            listBox.deselectItemByIndex(auswahl.get(0));
        }
    }

    public void weiterspielen() {
        istPausiert = false;
        gKonstruktor.figuren(spielStub.getAktuelleBelegung(), assetManager, rootNode);
    }
}