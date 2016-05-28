/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import daten.D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ivan
 */
public class GeometrieKonstruktor {

    Node geschlagenS = new Node("geschlagenS");
    String gewaehleteKachel;
    Map<String, Material> markierteKacheln = new HashMap<String, Material>();
    Node figurenS = new Node("figurenS");
    Map<String, Vector3f> positionen = new HashMap<String, Vector3f>();
    Node geschlagenW = new Node("geschlagenW");
    Node figurenW = new Node("figurenW");
    Node schachbrett = new Node("chessboard");

    void zeigeGeschlageneFiguren(ArrayList<Geometry> geschlageneFiguren, Node rootNode) {
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
            float x = 0;
            float z = 0;
            float y = g.getUserData("yOffset");
            if (farbe.equals("weiss")) {
                geschlagenW.attachChild(g);
                count = geschlagenW.getQuantity();
                if (count > 8) {
                    z = -13.0F;
                    x = -25.0F + (2 * count);
                } else {
                    z = -11.0F;
                    x = -9.0F + (2 * count);
                }
            } else if (farbe.equals("schwarz")) {
                geschlagenS.attachChild(g);
                count = geschlagenS.getQuantity();
                if (count > 8) {
                    z = 13.0F;
                    x = 25.0F - (2 * count);
                } else {
                    z = 11.0F;
                    x = 9.0F - (2 * count);
                }
            }
            Vector3f position = new Vector3f(x, y, z);
            g.setLocalTranslation(position);
        }
        rootNode.attachChild(geschlagenW);
        rootNode.attachChild(geschlagenS);
    }

    public void initBrett(AssetManager assetManager, Node rootNode) {
        boolean lastFieldBlack = true;
        int x = -7;
        int y = -7;
        char letter = 'a';
        int number = 8;
        Vector3f pos;
        String name;
        Box box = new Box(1.0F, 0.01F, 1.0F);
        Material weiss = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        weiss.setColor("Color", ColorRGBA.White);
        Material schwarz = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        schwarz.setColor("Color", ColorRGBA.DarkGray);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                name = letter + "" + number;
                System.out.println(name);
                Geometry geom = new Geometry(name, box);
                if (lastFieldBlack) {
                    geom.setMaterial(weiss);
                    lastFieldBlack = false;
                    geom.setUserData("farbe", "weiss");
                } else {
                    geom.setMaterial(schwarz);
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
                schachbrett.attachChild(geom);
                positionen.put(name, pos);
            }
            y += 2;
            x = -7;
            lastFieldBlack = !lastFieldBlack;
            number--;
            letter = 'a';
        }
        rootNode.attachChild(schachbrett);
    }

    BitmapText initFadenkreuz(BitmapFont font, Main main, AssetManager assetManager, AppSettings settings) {
        main.setDisplayStatView(false);
        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(font, false);
        ch.setSize(font.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        ch.setLocalTranslation(settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        return ch;
    }

    Geometry getGeometry(String type) {
        Geometry g = null;
        if (type.equals("Turm")) {
            g = new Geometry("Turm", new Box(0.5F, 1.5F, 0.5F));
            g.setUserData("yOffset", 1.5F);
            g.setUserData("rang", "d");
        } else if (type.equals("Springer")) {
            g = new Geometry("Springer", new Dome(Vector3f.ZERO, 2, 4, 1.0F, false));
            g.setUserData("yOffset", 0.0F);
            g.setUserData("rang", "b");
        } else if (type.equals("Laeufer")) {
            g = new Geometry("Laeufer", new Dome(Vector3f.ZERO, 2, 32, 1.0F, false));
            g.setUserData("yOffset", 0.0F);
            g.setUserData("rang", "c");
        } else if (type.equals("Koenig")) {
            g = new Geometry("Koenig", new Dome(Vector3f.ZERO, 32, 32, 0.6F, false));
            g.setUserData("yOffset", 0.0F);
            g.setUserData("rang", "f");
        } else if (type.equals("Dame")) {
            g = new Geometry("Dame", new Sphere(32, 32, 0.5F));
            g.setUserData("yOffset", 0.5F);
            g.setUserData("rang", "e");
        } else if (type.equals("Bauer")) {
            g = new Geometry("Bauer", new Box(0.5F, 0.5F, 0.5F));
            g.setUserData("yOffset", 0.5F);
            g.setUserData("rang", "a");
        }
        return g;
    }

    void markiereKacheln(List<String> plist, AssetManager assetManager) {
        resetKacheln();
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        for (String s : plist) {
            Geometry sp = (Geometry) schachbrett.getChild(s);
            markierteKacheln.put(s, sp.getMaterial());
            sp.setMaterial(mat);
            sp.getMaterial().setColor("Color", ColorRGBA.Green);
            sp.setUserData("markiert", true);
        }
    }

    void resetKacheln() {
        if (!markierteKacheln.isEmpty()) {
            for (Map.Entry<String, Material> entry : markierteKacheln.entrySet()) {
                String s = entry.getKey();
                Material mat = entry.getValue();
                Geometry g = (Geometry) schachbrett.getChild(s);
                g.setMaterial(mat);
                g.setUserData("markiert", false);
            }
            markierteKacheln.clear();
        }
    }

    public void initRandZiffer(BitmapFont guiFont, AssetManager assetManager, Node rootNode) {
        float x = -9.0F;
        float z = -8.0F;
        int nummer = 8;
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                BitmapText ch = new BitmapText(guiFont, false);
                ch.setSize(2.0F);
                ch.setText(String.valueOf(nummer));
                if (i < 1) {
                    ch.setLocalTranslation(x - 0.5F, 0, z);
                    ch.rotate(-1.5708F, 0, 0);
                } else {
                    ch.setLocalTranslation(x + 0.5F, 0, z + 2.5F);
                    ch.rotate(-1.5708F, 3.14159F, 0);
                }
                rootNode.attachChild(ch);
                z += 2;
                nummer--;
            }
            nummer = 8;
            x *= -1;
            z = -8.0F;
        }
    }

    void initPositionen() {
        char letter = 'a';
        int number = 8;
        int x = -7;
        int y = -7;
        Vector3f pos;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                pos = new Vector3f(x, 0.0F, y);
                positionen.put(letter + "" + number, pos);
                x += 2;
                letter++;
            }
            y += 2;
            x = -7;
            number--;
            letter = 'a';
        }
    }

    public void initRandBuchstabe(BitmapFont guiFont, AssetManager assetManager, Node rootNode) {
        float x = -7.0F;
        float z = -10.5F;
        char buchstabe = 'a';
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                BitmapText ch = new BitmapText(guiFont, false);
                ch.setSize(2.0F);
                ch.setText(String.valueOf(buchstabe));
                if (i > 0) {
                    ch.setLocalTranslation(x - 0.5F, 0, z);
                    ch.rotate(-1.5708F, 0, 0);
                } else {
                    ch.setLocalTranslation(x + 0.5F, 0, z + 2.5F);
                    ch.rotate(-1.5708F, 3.14159F, 0);
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

    void figuren(String xml, AssetManager assetManager, Node rootNode) {
        ArrayList<D> data = Xml.toArray(xml);
        ArrayList<Geometry> geschlageneFiguren = new ArrayList<Geometry>();
        if (!data.isEmpty()) {
            schachbrett.detachAllChildren();
            geschlagenW.detachAllChildren();
            geschlagenS.detachAllChildren();
            initBrett(assetManager, rootNode);
            for (D d : data) {
                if (d.getProperties().getProperty("klasse").equals("D_Figur")) {
                    Geometry g = getGeometry(d.getProperties().getProperty("typ"));
                    if (d.getProperties().getProperty("isWeiss").equals("true")) {
                        Material white = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                        white.setColor("Color", ColorRGBA.White);
                        g.setMaterial(white);
                        g.setUserData("farbe", "weiss");
                    } else {
                        Material black = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                        black.setColor("Color", ColorRGBA.DarkGray);
                        g.setMaterial(black);
                        g.setUserData("farbe", "schwarz");
                    }
                    if (!d.getProperties().getProperty("position").equals("")) {
                        g.setUserData("position", d.getProperties().getProperty("position"));
                        Vector3f position = positionen.get(g.getUserData("position"));
                        float offset = g.getUserData("yOffset");
                        position.setY(offset);
                        g.setLocalTranslation(position);
                        g.setUserData("typ", "figur");
                        schachbrett.attachChild(g);
                    } else {
                        geschlageneFiguren.add(g);
                    }
                }
            }
            if (!geschlageneFiguren.isEmpty()) {
                zeigeGeschlageneFiguren(geschlageneFiguren, rootNode);
            }
            rootNode.attachChild(figurenW);
            rootNode.attachChild(figurenS);
        }
    }
}
