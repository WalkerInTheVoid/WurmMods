package org.gotti.wurmunlimited.mods.diglikeminingmod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class DigLikeMiningMod implements WurmMod, Configurable, PreInitable {

	private boolean digLikeMining = true;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static boolean bDebug = false;
	
	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		if(digLikeMining) {
			try {
				CtMethod dig = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Terraforming").getDeclaredMethod("dig");
				String checkGroundClutter = "{"
						+ "final com.wurmonline.server.zones.VolaTile tempTile = " 
						+ 		"com.wurmonline.server.zones.Zones.getTileOrNull(performer.getTileX(), performer.getTileY(), performer.isOnSurface());";
				if(bDebug) checkGroundClutter += "if(tempTile != null) {System.out.println(\"NumItems: \" + tempTile.getNumberOfItems(performer.getFloorLevel()));"
						+ "} else { System.out.println(\"tempTile is null, posX: \" + performer.getTileX() + \" " 
						+ "posY: \" + performer.getTileY() + \" IsOnSurface: \" + performer.isOnSurface()); }";
				checkGroundClutter += "if (tempTile != null && tempTile.getNumberOfItems(performer.getFloorLevel()) > 99) {\r\n"
						+ 		"performer.getCommunicator().sendNormalServerMessage(\"There is no space to dig here. Clear the area first.\");\r\n" 
						+ 		"return true;\r\n" 
						+ 	"}"
						+ "}";
				dig.instrument(new InsertToGround());
				
				dig.insertBefore(checkGroundClutter);
				CtMethod flatten = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Flattening").getDeclaredMethod("flatten");
				CtMethod getDirt = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Flattening").getDeclaredMethod("getDirt");
				getDirt.instrument(new InsertToGround());
				flatten.insertBefore(checkGroundClutter);
			}
			catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
	}
	
	class InsertToGround extends ExprEditor {
		public void edit(MethodCall m)
				throws CannotCompileException {
			if (m.getClassName().equals("com.wurmonline.server.items.Item")
					&& m.getMethodName().equals("insertItem")) {
				String debugString = "";
				Debug("Inserting insertItem override");
				if (bDebug)
					debugString = "java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.diglikeminingmod.DigLikeMiningMod"
							+ "\").log(java.util.logging.Level.INFO, \"Overriding insertItem\");\n";
				String toInsert = "$1.setLastOwnerId($0.getOwnerId());"
				+ "$1.setPosXY($0.getPosX(),$0.getPosY());"
				+ "com.wurmonline.server.zones.Zone zTemp = "
				+		"com.wurmonline.server.zones.Zones.getZone((int)$1.getPosX() >> 2, (int)$1.getPosY() >> 2, $1.isOnSurface());"
				+ "zTemp.addItem($1);";
				m.replace(debugString + toInsert + "$_ = true;");
			}
		}
	}

	@Override
	public void configure(Properties properties) {
		digLikeMining = Boolean.parseBoolean(properties.getProperty("ignoreTemp", Boolean.toString(digLikeMining)));
		bDebug = Boolean.parseBoolean(properties.getProperty("debug", Boolean.toString(bDebug)));
		try {
			final String logsPath = Paths.get("mods") + "/logs/";
			final File newDirectory = new File(logsPath);
			if (!newDirectory.exists()) {
				newDirectory.mkdirs();
			}
			final FileHandler fh = new FileHandler(String.valueOf(logsPath) + "mods.log", 10240000, 200, true);
			if (bDebug) {
				fh.setLevel(Level.INFO);
			}
			else {
				fh.setLevel(Level.WARNING);
			}
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
		}
		catch (IOException ie) {
			System.err.println(this.getClass().getName() + ": Unable to add file handler to logger");
			logger.log(Level.WARNING, this.getClass().getName() + ": Unable to add file handler to logger");
		}
		Debug("Debugging messages are enabled.");
		logger.log(Level.INFO, "digLikeMining: " + digLikeMining);
	}

	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}
}
