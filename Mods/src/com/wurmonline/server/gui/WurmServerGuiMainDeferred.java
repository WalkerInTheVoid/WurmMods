package com.wurmonline.server.gui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import com.wurmonline.server.Servers;
import com.wurmonline.server.utils.SimpleArgumentParser;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WurmServerGuiMainDeferred extends Application {
    private static final Logger logger;
    private static final Set<String> ACCEPTED_ARGS;
    public static final String ARG_START = "start";
    public static final String ARG_QUERY_PORT = "queryport";
    public static final String ARG_INTERNAL_PORT = "internalport";
    public static final String ARG_EXTERNAL_PORT = "externalport";
    public static final String ARG_IP_ADDR = "ip";
    public static final String ARG_RMI_REG = "rmiregport";
    public static final String ARG_RMI_PORT = "rmiport";
    public static final String ARG_SERVER_PASS = "serverpassword";
    public static final String ARG_PLAYER_NUM = "maxplayers";
    public static final String ARG_LOGIN_SERVER = "loginserver";
    public static final String ARG_PVP = "pvp";
    public static final String ARG_HOME_SERVER = "homeserver";
    public static final String ARG_HOME_KINGDOM = "homekingdom";
    public static final String ARG_EPIC_SETTINGS = "epicsettings";
    public static final String ARG_SERVER_NAME = "servername";
    public static final String ADMIN_PWD = "adminpwd";
    
    static {
        logger = Logger.getLogger(WurmServerGuiMain.class.getName());
        final HashSet<String> acceptedArgs = new HashSet<String>(1);
        acceptedArgs.add("start");
        acceptedArgs.add("internalport");
        acceptedArgs.add("externalport");
        acceptedArgs.add("ip");
        acceptedArgs.add("rmiregport");
        acceptedArgs.add("rmiport");
        acceptedArgs.add("serverpassword");
        acceptedArgs.add("queryport");
        acceptedArgs.add("maxplayers");
        acceptedArgs.add("loginserver");
        acceptedArgs.add("pvp");
        acceptedArgs.add("homeserver");
        acceptedArgs.add("homekingdom");
        acceptedArgs.add("epicsettings");
        acceptedArgs.add("servername");
        acceptedArgs.add("adminpwd");
        ACCEPTED_ARGS = Collections.unmodifiableSet((Set<? extends String>)acceptedArgs);
    }
    
	public static void main(final String[] args) {
        WurmServerGuiMainDeferred.logger.info("WurmServerGuiMainDeferred starting");
        final SimpleArgumentParser parser = new SimpleArgumentParser(args, WurmServerGuiMainDeferred.ACCEPTED_ARGS);
        String dbToStart = "";
        if (parser.hasOption("start")) {
            dbToStart = parser.getOptionValue("start");
            if (dbToStart == null || dbToStart.isEmpty()) {
                System.err.println("Start param needs to be followed by server dir: Start=<ServerDir>");
                System.exit(1);
            }
        }
        Servers.argumets = parser;
        String adminPass = "";
        if (parser.hasOption("adminpwd")) {
            adminPass = parser.getOptionValue("adminpwd");
            if (adminPass == null || adminPass.isEmpty()) {
                System.err.println("The admin password needs to be set or it will not be possible to change the settings within the game.");
            }
            else {
                WurmServerGuiController.adminPassword = adminPass;
            }
        }
        if (!dbToStart.isEmpty()) {
            System.out.println("Should start without GUI here!");
            WurmServerGuiController.startDB(dbToStart);
        }
        else {
            Application.launch(WurmServerGuiMainDeferred.class, (String[])null);
        }
        WurmServerGuiMainDeferred.logger.info("WurmServerGuiMainDeferred finished");
    }
    
    public void start(final Stage primaryStage) {
        try {
            final FXMLLoader loader = new FXMLLoader(WurmServerGuiMain.class.getResource("WurmServerGui.fxml"));
            final TabPane page = (TabPane)loader.load();
            final Scene scene = new Scene((Parent)page);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Wurm Unlimited Server");
            final List<Image> iconsList = new ArrayList<Image>();
            iconsList.add(new Image("com/wurmonline/server/gui/img/icon2_16.png"));
            iconsList.add(new Image("com/wurmonline/server/gui/img/icon2_32.png"));
            iconsList.add(new Image("com/wurmonline/server/gui/img/icon2_64.png"));
            iconsList.add(new Image("com/wurmonline/server/gui/img/icon2_128.png"));
            primaryStage.getIcons().addAll(iconsList);
            primaryStage.show();
            final Object controller = loader.getController();
            try {
            	Method shutdown = ReflectionUtil.getMethod(controller.getClass(), "shutdown");
            	scene.getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
            		public void handle(final WindowEvent ev) {
            			try {
            				if (!(boolean)shutdown.invoke(controller)) {
            					ev.consume();
            				}
            			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            				e.printStackTrace();
            			}
            		}
            	});
            } catch (NoSuchMethodException e) { 
            	e.printStackTrace();
            }
        }
        catch (IOException ex) {
        	WurmServerGuiMainDeferred.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
