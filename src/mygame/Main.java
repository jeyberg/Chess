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
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

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

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(15f);
        initGui();
        initCrossHairs();
        initKey();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (spielStub != null) {
            String xml = spielStub.getSpielDaten();
            ArrayList<D> daten = Xml.toArray(xml);
            String s = daten.get(0).getProperties().getProperty("anzahlZuege");
            if (s != null) {
                int zeuge = Integer.parseInt(s);
                if (zeuge > anzahlZeuge) {
                    figuren();
                    anzahlZeuge = zeuge;
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
                    String gName = g.getName();
                    gName = gName.substring(gName.length() - 2);
                    if (g.getUserData("typ").equals("figur")) {
                        if (g.getUserData("farbe").equals("weiss") && zmngr.getIsWeiss()
                                || g.getUserData("farbe").equals("schwarz") && !zmngr.getIsWeiss()) {
                            gName = g.getName();
                            gName = gName.substring(gName.length() - 2);
                            getLegalPositions(gName);
                        }
                    } else if (g.getUserData("typ").equals("kachel")) {
                        if (g.getUserData("markiert")) {
                            draw(selectedTile, gName);
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
        ch.setLocalTranslation(
                settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
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
                figuren();
                nifty.gotoScreen("spiel");
                setKameraPosition(zmngr.getIsWeiss());
            }
            System.out.println(daten);
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
            figuren();
            nifty.gotoScreen("spiel");
            setKameraPosition(zmngr.getIsWeiss());
        }
    }
    
    public void loadGame() {
        String s = getXmlMessage(stub.ladenSpiel("somepath"));
        System.out.println(s);
    }

    public void saveGame() {
        String s = getXmlMessage(stub.speichernSpiel("somepath"));
        System.out.println(s);
    }

    public void quitGame() {
        this.stop();
    }

    public String getXmlMessage(String msg) {
        String s = null;
        try {
            doc = builder.build(new StringReader(msg));
            Element root = doc.getRootElement();
            List<Element> entry = root.getChildren("entry");
            s = entry.get(0).getText();
        } catch (JDOMException ex) {
            Logger.getLogger(MyStartScreen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MyStartScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
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

    void draw(String from, String to) {
        String xml = spielStub.ziehe(from, to);
        ArrayList<D> data = Xml.toArray(xml);
        if (data.get(0).getProperties().getProperty("klasse").equals("D_OK")) {
            resetTileColor();
            figuren();
            zmngr.setAmZug(false);
        } else {
            getLegalPositions(to);
        }
    }

    void figuren() {
        String xml = spielStub.getAktuelleBelegung();
        ArrayList<D> data = Xml.toArray(xml);
        if (!data.isEmpty()) {
            chessboard.detachAllChildren();
            initBoard();
            for (D d : data) {
                if (d.getProperties().getProperty("klasse").equals("D_Figur") && !d.getProperties().getProperty("position").equals("")) {
                    Geometry g = getGeometry(d.getProperties().getProperty("typ"), d.getProperties().getProperty("position"));
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
                    g.setUserData("typ", "figur");
                    chessboard.attachChild(g);

                }
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

    Geometry getGeometry(String type, String pos) {
        Geometry g = null;
        if (type.equals("Turm")) {
            g = new Geometry("Turm" + pos, new Box(0.5f, 1.5f, 0.5f));
            g.setLocalTranslation(positions.get(pos).setY(1.5f));
        } else if (type.equals("Springer")) {
            g = new Geometry("Springer" + pos, new Dome(Vector3f.ZERO, 2, 4, 1f, false));
            g.setLocalTranslation(positions.get(pos));
        } else if (type.equals("Laeufer")) {
            g = new Geometry("Laeufer" + pos, new Dome(Vector3f.ZERO, 2, 32, 1f, false));
            g.setLocalTranslation(positions.get(pos));
        } else if (type.equals("Koenig")) {
            g = new Geometry("Koenig" + pos, new Dome(Vector3f.ZERO, 32, 32, 0.6f, false));
            g.setLocalTranslation(positions.get(pos));
        } else if (type.equals("Dame")) {
            g = new Geometry("Dame" + pos, new Sphere(32, 32, 0.5f));
            g.setLocalTranslation(positions.get(pos).setY(0.5f));
        } else if (type.equals("Bauer")) {
            g = new Geometry("Bauer" + pos, new Box(0.5f, 0.5f, 0.5f));
            g.setLocalTranslation(positions.get(pos).setY(0.5f));
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
}
