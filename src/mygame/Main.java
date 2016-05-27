package mygame;

import daten.D;
import restClient.BackendSpielAdminStub;
import restClient.BackendSpielStub;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.lwjgl.opengl.Display;

public class Main extends SimpleApplication implements ScreenController {

    private Node chessboard = new Node("chessboard");
    private Node figurenW = new Node("figurenW");
    private Node figurenS = new Node("figurenS");
    private static BackendSpielAdminStub stub = null; // = new BackendSpielAdminStub("http://192.168.56.1:8000")
    private static BackendSpielStub spielStub = null; // = new BackendSpielStub("http://192.168.56.1:8000");
    private Document doc;
    private SAXBuilder builder = new SAXBuilder();
    private Nifty nifty;
    private boolean isFlying = false;
    private Map<String, Vector3f> positions = new HashMap<String, Vector3f>();
    private Map<String, Material> coloredTiles = new HashMap<String, Material>();
    private String selectedTile;
    private Zugmanager zmngr;
    private ArrayList<D> aktuelleBelegung;
    private int anzahlZeuge = 0;
    private Node geschlagenW = new Node("geschlagenW");
    private Node geschlagenS = new Node("geschlagenS");
    private String letzterStatus;
    private boolean istHost = false;

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
        initCrossHairs();
        initKey();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (Display.wasResized()) {
            int neueBreite = Math.max(Display.getWidth(), 1);
            int neueHöhe = Math.max(Display.getHeight(), 1);
            reshape(neueBreite, neueHöhe);
        }
        if (spielStub != null) {
            String xml = spielStub.getSpielDaten();
            ArrayList<D> daten = Xml.toArray(xml);
            String s = daten.get(0).getProperties().getProperty("anzahlZuege");
            String status = daten.get(0).getProperties().getProperty("status");
            if (s != null) {
                int zeuge = Integer.parseInt(s);
                if (zeuge > anzahlZeuge) {
                    figuren();
                    aktualisiereHistorie();
                    anzahlZeuge = zeuge;
                    if (!status.equals("") && !status.equals("null")) {
                        aktualisiereNachrichten(status);
                    }
                }

            }


        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    private void initKey() {
        inputManager.addMapping("Klick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Mouse_Mode", new KeyTrigger(KeyInput.KEY_LMENU));
        inputManager.addListener(actionListener, "Klick");
        inputManager.addListener(actionListener, "Mouse_Mode");
    }
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Klick") && !isPressed && zmngr.getAmZug(spielStub)) {
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                chessboard.collideWith(ray, results);
                if (results.size() > 0) {
                    Geometry g = results.getClosestCollision().getGeometry();
                    String pos = g.getUserData("position");
                    if (g.getUserData("typ").equals("figur")) {
                        if (g.getUserData("farbe").equals("weiss") && zmngr.getIsWeiss()
                                || g.getUserData("farbe").equals("schwarz") && !zmngr.getIsWeiss()) {
                            getLegalPositions(pos);
                        } else {
                            if (coloredTiles.containsKey(pos)) {
                                draw(selectedTile, pos);
                            }
                        }
                    } else if (g.getUserData("typ").equals("kachel")) {
                        if (g.getUserData("markiert")) {
                            draw(selectedTile, pos);
                        }
                    }
                }
            } else if (name.equals("Mouse_Mode") && !isPressed) {
                flyCam.setDragToRotate(isFlying);
                isFlying = !isFlying;
                System.out.println(isFlying);
            }

        }
    };

    void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        ch.setLocalTranslation(settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0); //settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0
        guiNode.attachChild(ch);
    }

    public void initBoard() {
        boolean lastFieldBlack = true;
        int x = -7;
        int y = -7;
        char letter = 'a';
        int number = 8;
        Vector3f pos;
        String name;

        Box box = new Box(1f, 0.01f, 1f);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        Material mat2 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.DarkGray);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                name = letter + "" + number;
                System.out.println(name);
                Geometry geom = new Geometry(name, box);
                if (lastFieldBlack) {
                    geom.setMaterial(mat);
                    lastFieldBlack = false;
                    geom.setUserData("farbe", "weiss");
                } else {
                    geom.setMaterial(mat2);
                    lastFieldBlack = true;
                    geom.setUserData("farbe", "schwarz");
                }
                pos = new Vector3f(x, 0, y);
                geom.setLocalTranslation(pos);
                geom.setUserData("typ", "kachel");
                geom.setUserData("markiert", false);
                geom.setUserData("position", name);
                x += 2;
                letter++;
                chessboard.attachChild(geom);
                positions.put(name, pos);
            }
            y += 2;
            x = -7;
            lastFieldBlack = !lastFieldBlack;
            number--;
            letter = 'a';
        }
        rootNode.attachChild(chessboard);
    }

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
            stub = new BackendSpielAdminStub("http://" + text);
            spielStub = new BackendSpielStub("http://" + text);
            this.zmngr = new Zugmanager(isWeiss);
            String s = stub.neuesSpiel();
            ArrayList<D> daten = Xml.toArray(s);
            if (daten.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
                initPos();
                initBoard();
                initRandZiffer();
                initRandBuchstabe();
                figuren();
                nifty.gotoScreen("spiel");
                setKameraPosition(zmngr.getIsWeiss());
                istHost = true;
            }
        }
    }

    public void spielBeitreten() {
        Screen scrn = nifty.getCurrentScreen();
        String text = scrn.findNiftyControl("ip", TextField.class).getRealText();
        boolean isWeiss = scrn.findNiftyControl("weiss", RadioButton.class).isActivated();
        if (!text.equals("")) {
            stub = new BackendSpielAdminStub("http://" + text);
            spielStub = new BackendSpielStub("http://" + text);
            this.zmngr = new Zugmanager(isWeiss);
            initPos();
            initBoard();
            initRandZiffer();
            initRandBuchstabe();
            figuren();
            aktualisiereHistorie();
            nifty.gotoScreen("spiel");
            setKameraPosition(zmngr.getIsWeiss());
        }
    }

    public void loadGame() {
        String s = stub.ladenSpiel("somepath");
        System.out.println(s);
    }

    public void saveGame() {
        String s = stub.speichernSpiel("somepath");
        System.out.println(s);
    }

    public void quitGame() {
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

    void getLegalPositions(String pos) {
        String xml = spielStub.getErlaubteZuege(pos);
        List<String> positions = new ArrayList<String>();
        ArrayList<D> data = Xml.toArray(xml);
        for (D d : data) {
            String s = d.getProperties().getProperty("nach");
            positions.add(s);
        }
        selectedTile = pos;
        changeTileColor(positions);
    }

    void changeTileColor(List<String> plist) {
        resetTileColor();
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        for (String s : plist) {
            Geometry sp = (Geometry) chessboard.getChild(s);
            coloredTiles.put(s, sp.getMaterial());
            sp.setMaterial(mat);
            sp.getMaterial().setColor("Color", ColorRGBA.Green);
            sp.setUserData("markiert", true);
        }
    }

    void resetTileColor() {
        if (!coloredTiles.isEmpty()) {
            for (Map.Entry<String, Material> entry : coloredTiles.entrySet()) {
                String s = entry.getKey();
                Material mat = entry.getValue();
                Geometry g = (Geometry) chessboard.getChild(s);
                g.setMaterial(mat);
                g.setUserData("markiert", false);
            }
            coloredTiles.clear();
        }
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
                draw(von, nach);
            }
        }

    }

    void draw(String from, String to) {
        System.out.println("ziehe");
        String xml = spielStub.ziehe(from, to);
        ArrayList<D> data = Xml.toArray(xml);
        if (data.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
            resetTileColor();
            figuren();
            zmngr.setAmZug(false);
            //aktualisiereHistorie();
        } else {
            getLegalPositions(to);
        }
    }

    void figuren() {
        String xml = spielStub.getAktuelleBelegung();
        ArrayList<D> data = Xml.toArray(xml);
        ArrayList<Geometry> geschlageneFiguren = new ArrayList<Geometry>();
        if (!data.isEmpty()) {
            chessboard.detachAllChildren();
            geschlagenW.detachAllChildren();
            geschlagenS.detachAllChildren();
            initBoard();
            for (D d : data) {
                if (d.getProperties().getProperty("klasse").equals("D_Figur")) {
                    Geometry g = getGeometry(d.getProperties().getProperty("typ"));
                    if (d.getProperties().getProperty("isWeiss").equals("true")) {
                        Material white = new Material(assetManager,
                                "Common/MatDefs/Misc/Unshaded.j3md");
                        white.setColor("Color", ColorRGBA.White);
                        g.setMaterial(white);
                        g.setUserData("farbe", "weiss");
                    } else {
                        Material black = new Material(assetManager,
                                "Common/MatDefs/Misc/Unshaded.j3md");
                        black.setColor("Color", ColorRGBA.DarkGray);
                        g.setMaterial(black);
                        g.setUserData("farbe", "schwarz");
                    }
                    if (!d.getProperties().getProperty("position").equals("")) {
                        g.setUserData("position", d.getProperties().getProperty("position"));
                        Vector3f position = positions.get(g.getUserData("position"));
                        float offset = g.getUserData("yOffset");
                        position.setY(offset);
                        g.setLocalTranslation(position);
                        g.setUserData("typ", "figur");
                        chessboard.attachChild(g);
                    } else {
                        geschlageneFiguren.add(g);

                    }



                }
            }
            if (geschlageneFiguren.size() != 0) {
                zeigeGeschlageneFigur(geschlageneFiguren);
            }
            rootNode.attachChild(figurenW);
            rootNode.attachChild(figurenS);
        }

    }

    void initPos() {
        char letter = 'a';
        int number = 8;
        int x = -7;
        int y = -7;
        Vector3f pos;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                pos = new Vector3f(x, 0f, y);
                positions.put(letter + "" + number, pos);
                x += 2;
                letter++;
            }
            y += 2;
            x = -7;
            number--;
            letter = 'a';
        }
        System.out.println(positions);
    }

    public void initRandZiffer() {
        float x;
        float z;

        x = -9f;
        z = -8f;
        int nummer = 8;
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                BitmapText ch = new BitmapText(guiFont, false);
                ch.setSize(2f);
                ch.setText(String.valueOf(nummer));
                if (i < 1) {
                    ch.setLocalTranslation(x - 0.5f, 0, z);
                    ch.rotate(-1.5708f, 0, 0);
                } else {

                    ch.setLocalTranslation(x + 0.5f, 0, z + 2.5f);
                    ch.rotate(-1.5708f, 3.14159f, 0);
                }

                rootNode.attachChild(ch);
                z += 2;
                nummer--;
            }

            nummer = 8;
            x *= -1;
            z = -8f;

        }

    }

    public void initRandBuchstabe() {
        float x = -7f;
        float z = -10.5f;
        char buchstabe = 'a';
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                BitmapText ch = new BitmapText(guiFont, false);
                ch.setSize(2f);
                ch.setText(String.valueOf(buchstabe));

                if (i > 0) {
                    ch.setLocalTranslation(x - 0.5f, 0, z);
                    ch.rotate(-1.5708f, 0, 0);
                } else {
                    ch.setLocalTranslation(x + 0.5f, 0, z + 2.5f);
                    ch.rotate(-1.5708f, 3.14159f, 0);

                }
                rootNode.attachChild(ch);
                x += 2;
                buchstabe++;

            }
            buchstabe = 'a';
            x = -7;
            z = 8;
        }
    }

    Geometry getGeometry(String type) {
        Geometry g = null;
        if (type.equals("Turm")) {
            g = new Geometry("Turm", new Box(0.5f, 1.5f, 0.5f));
            g.setUserData("yOffset", 1.5f);
            g.setUserData("rang", "d");
        } else if (type.equals("Springer")) {
            g = new Geometry("Springer", new Dome(Vector3f.ZERO, 2, 4, 1f, false));
            g.setUserData("yOffset", 0f);
            g.setUserData("rang", "b");
        } else if (type.equals("Laeufer")) {
            g = new Geometry("Laeufer", new Dome(Vector3f.ZERO, 2, 32, 1f, false));
            g.setUserData("yOffset", 0f);
            g.setUserData("rang", "c");
        } else if (type.equals("Koenig")) {
            g = new Geometry("Koenig", new Dome(Vector3f.ZERO, 32, 32, 0.6f, false));
            g.setUserData("yOffset", 0f);
            g.setUserData("rang", "f");
        } else if (type.equals("Dame")) {
            g = new Geometry("Dame", new Sphere(32, 32, 0.5f));
            g.setUserData("yOffset", 0.5f);
            g.setUserData("rang", "e");
        } else if (type.equals("Bauer")) {
            g = new Geometry("Bauer", new Box(0.5f, 0.5f, 0.5f));
            g.setUserData("yOffset", 0.5f);
            g.setUserData("rang", "a");
        }
        return g;
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
            ListBox listBox = screen.findNiftyControl("historie", ListBox.class);
            listBox.clear();
            for (D d : daten) {
                listBox.addItem(d.getProperties().getProperty("zug"));
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

    private void zeigeGeschlageneFigur(ArrayList<Geometry> geschlageneFiguren) {
        Collections.sort(geschlageneFiguren, new Comparator<Geometry>() {
            public int compare(Geometry o1, Geometry o2) {
                String s = (String) o1.getUserData("rang");
                String s2 = (String) o2.getUserData("rang");
                return s.compareTo(s2);
            }
        });
        for (Geometry g : geschlageneFiguren) {
            String farbe = g.getUserData("farbe");
            int count = 0;
            float x = 0, z = 0, y = g.getUserData("yOffset");
            if (farbe.equals("weiss")) {
                geschlagenW.attachChild(g);
                count = geschlagenW.getQuantity();
                if (count > 8) {
                    z = -13f;
                    x = -25f + (2 * count);
                } else {
                    z = -11f;
                    x = -9f + (2 * count);
                }

            } else if (farbe.equals("schwarz")) {
                geschlagenS.attachChild(g);
                count = geschlagenS.getQuantity();
                if (count > 8) {
                    z = 13f;
                    x = 25f - (2 * count);
                } else {
                    z = 11f;
                    x = 9f - (2 * count);
                }
            }
            Vector3f position = new Vector3f(x, y, z);
            g.setLocalTranslation(position);
        }
        rootNode.attachChild(geschlagenW);
        rootNode.attachChild(geschlagenS);
    }

    public void revanche() {
        System.out.println("test");
        if (istHost) {
            spielErstellen();
        } else {
            spielBeitreten();
        }
    }
}