/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import restClient.BackendSpielAdminStub;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author ivan
 */
public class MyStartScreen extends AbstractAppState implements ScreenController{
    BackendSpielAdminStub stub;
    private SimpleApplication app;
    SAXBuilder builder;
    Document doc;
    
    public MyStartScreen(){
        builder = new SAXBuilder();
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        //TODO: initialize your AppState, e.g. attach spatials to rootNode
        //this is called on the OpenGL thread after the AppState has been attached
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        //TODO: clean up what you initialized in the initialize method,
        //e.g. remove all spatials from rootNode
        //this is called on the OpenGL thread after the AppState has been detached
    }

    public void bind(Nifty nifty, Screen screen) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void onStartScreen() {
  //      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void onEndScreen() {
    //    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void newGame(){
        String s;
        
        s = stub.neuesSpiel();
        try {
            doc = builder.build(new StringReader(s));
            Element root = doc.getRootElement();
            List<Element> entry = root.getChildren("entry");
            if(entry.get(1).getText() == "D_OK") ;
        } catch (JDOMException ex) {
            Logger.getLogger(MyStartScreen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MyStartScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(s);
    }
}
