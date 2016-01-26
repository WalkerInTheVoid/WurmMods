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

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class DigLikeMiningMod implements WurmMod, Configurable, PreInitable {

	private boolean digLikeMining = true;
	private boolean dredgeToShip = true;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static boolean bDebug = false;
	
	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		if(digLikeMining) {
			try {
				CtMethod dig = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Terraforming").getDeclaredMethod("dig");
				
				dig.instrument(new InsertToGround(true));
				
				CtMethod getDirt = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Flattening").getDeclaredMethod("getDirt");
				CtMethod checkUseDirt = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Flattening").getDeclaredMethod("checkUseDirt");
				CtMethod useDirt = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.behaviours.Flattening").getDeclaredMethod("useDirt");
				getDirt.instrument(new InsertToGround());
				checkUseDirt.instrument(new FillFromGround());
				useDirt.instrument(new FillFromGround());
			}
			catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
	}
	
	public Item findItemInInventoryOrTile(Creature $0, int $1) {
		Item $_ = $0.getCarriedItem($1);
		if ($_ == null) {
			com.wurmonline.server.zones.VolaTile tempTile = 
					com.wurmonline.server.zones.Zones.getTileOrNull($0.getTileX(), $0.getTileY(), $0.isOnSurface());
			if (tempTile != null) {
				com.wurmonline.server.items.Item[] itemArray = tempTile.getItems();
				for (int i = 0; i < itemArray.length; i++) {
					if (itemArray[i].getTemplateId() == $1) {
						$_ = itemArray[i];
					}
				}
			}
		}
		return $_;
	}
	
	class FillFromGround extends ExprEditor {
		public void edit(MethodCall m)
				throws CannotCompileException {
			if (m.getClassName().equals("com.wurmonline.server.creatures.Creature")
					&& m.getMethodName().equals("getCarriedItem")) {
				m.replace("$_ = $proceed($$);" + 
						"if($_ == null) {" + 
						"	com.wurmonline.server.zones.VolaTile tempTile = " + 
						"			com.wurmonline.server.zones.Zones.getTileOrNull($0.getTileX(), $0.getTileY(), $0.isOnSurface());" + 
						"	if (tempTile != null) {" + 
						"		com.wurmonline.server.items.Item[] itemArray = tempTile.getItems();" +
						"		for (int i = 0; i < itemArray.length; i++) {\r\n" + 
						"			if (itemArray[i].getTemplateId() == $1) {\r\n" + 
						"				$_ = itemArray[i];\r\n" + 
						"				break;\r\n" + 
						"			}\r\n" + 
						"		}" + 
						"	}" + 
						"}");
			}
		}
	}
	
	class InsertToGround extends ExprEditor {
		int i = 0;
		boolean dredge = false;
		
		public InsertToGround (boolean bDredge) {
			super();
			dredge = bDredge;
		}
		
		public InsertToGround () {
			super();
			dredge = false;
		}
		
		public void edit(MethodCall m)
				throws CannotCompileException {
			if (m.getClassName().equals("com.wurmonline.server.items.Item")
					&& m.getMethodName().equals("insertItem")) {
				String debugString = "";
				Debug("Inserting insertItem override");
				if (bDebug)
					debugString = "java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.diglikeminingmod.DigLikeMiningMod"
							+ "\").log(java.util.logging.Level.INFO, \"Overriding insertItem\");\n";
				String escapeString = "if($1.getTemplateId() == com.wurmonline.server.items.ItemList.emerald "
						+ " || $1.getTemplateId() == com.wurmonline.server.items.ItemList.emeraldStar "
						+ "|| $1.getTemplateId() == com.wurmonline.server.items.ItemList.boneCollar)"
						+ " { ";
				if (bDebug) { 
					escapeString += "java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.diglikeminingmod.DigLikeMiningMod" 
							+ "\").log(java.util.logging.Level.INFO, \"Cancelling override as item is emerald or bone\");\n"; 
				}
				escapeString += "$_ = $proceed($$); } else {";
				String insertToGround = "$1.setLastOwnerId($0.getOwnerId());"
				+ "$1.setPosXY($0.getPosX(),$0.getPosY());"
				+ "com.wurmonline.server.zones.Zone zTemp = "
				+		"com.wurmonline.server.zones.Zones.getZone((int)$1.getPosX() >> 2, (int)$1.getPosY() >> 2, $0.isOnSurface());"
				+ "zTemp.addItem($1);";
				String toInsert = "";
				if(dredge) {
					toInsert = "if ($0.isDredgingTool() && ($0.getTemplateId() != 176 || performer.getPositionZ() < 0.0f)) {\r\n"
							+ "			boolean bInserted = false;\r\n" + "			try {\r\n"
							+ "				if (performer.getVehicle() != -10) {\r\n"
							+ "					com.wurmonline.server.items.Item veh = com.wurmonline.server.Items.getItem(performer.getVehicle());\r\n"
							+ "					if (veh != null) {\r\n"
							+ "						if (veh.isHollow()) {\r\n"
							+ "							if (veh.getNumItemsNotCoins() < 100 && veh.getFreeVolume() >= $1.getVolume()) {\r\n"
							+ "								veh.insertItem($1, true);\r\n"
							+ "								bInserted = true;\r\n"
							+ "							} else {\r\n"
							+ "								performer.getCommunicator().sendNormalServerMessage(\"The \" + $1.getName() + \" spills out of the \" + veh.getName() + \" and tumbles to the ground.\");\r\n"
							+ "							}\r\n" + "						}\r\n"
							+ "					}\r\n" + "				}\r\n"
							+ "			} catch (com.wurmonline.server.NoSuchItemException e) {\r\n"
							+ "				java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.diglikeminingmod.DigLikeMiningMod\")\r\n"
							+ "						.log(java.util.logging.Level.SEVERE, \"Unable to get vehicle ID \" + performer.getVehicle());\r\n"
							+ "				e.printStackTrace();\r\n" + "			}\r\n"
							+ "			if (!bInserted) {\r\n" 
							+ insertToGround
							+ "			}\r\n"
							+ "		} else { "
							+ insertToGround
							+ "		}";
				} else {
					toInsert = insertToGround;
				}
				m.replace(debugString + escapeString + toInsert + "$_ = true;}");
			} else if (m.getClassName().equals("com.wurmonline.server.items.Item") && m.getMethodName().equals("getNumItemsNotCoins")) {
				m.replace("$_ = 0;");
			} else if (m.getClassName().equals("com.wurmonline.server.creatures.Creature") && m.getMethodName().equals("canCarry")) {
				String checkGroundClutter = "{$_ = true;"
						+ "final com.wurmonline.server.zones.VolaTile tempTile = " 
						+ 		"com.wurmonline.server.zones.Zones.getTileOrNull($0.getTileX(), $0.getTileY(), $0.isOnSurface());";
//				if(bDebug) checkGroundClutter += "if(tempTile != null) {System.out.println(\"NumItems: \" + tempTile.getNumberOfItems(performer.getFloorLevel()));"
//						+ "} else { System.out.println(\"tempTile is null, posX: \" + performer.getTileX() + \" " 
//						+ "posY: \" + performer.getTileY() + \" IsOnSurface: \" + performer.isOnSurface()); }";
				checkGroundClutter += "if (tempTile != null && tempTile.getNumberOfItems($0.getFloorLevel()) > 99) {\r\n"
						+ 		"$0.getCommunicator().sendNormalServerMessage(\"There is no space to dig here. Clear the area first.\");\r\n" 
						+ 		"$_ = false;\r\n" 
						+ 	"}"
						+ "}";
				m.replace(checkGroundClutter);
			} else if (m.getClassName().equals("com.wurmonline.server.items.Item") && m.getMethodName().equals("getFreeVolume")) {
				m.replace("$_ = 1000;");
			} else if (m.getClassName().equals("com.wurmonline.server.creatures.Communicator") && m.getMethodName().equals("sendNormalServerMessage")) {
				m.replace("if (!($1.equals(\"You are not strong enough to carry one more dirt pile.\") " 
					+ "|| $1.equals(\"You would not be able to carry all the heavy dirt you dig. You need to drop some things first.\") )) "
					+ "{ $_ = $proceed($$); } else { $_ = null; }");
			}
		}
	}

	@Override
	public void configure(Properties properties) {
		digLikeMining = Boolean.parseBoolean(properties.getProperty("ignoreTemp", Boolean.toString(digLikeMining)));
		dredgeToShip = Boolean.parseBoolean(properties.getProperty("dredgeToShip", String.valueOf(dredgeToShip)));
		
		bDebug = Boolean.parseBoolean(properties.getProperty("debug", Boolean.toString(bDebug)));
		try {
			final String logsPath = Paths.get("mods") + "/logs/";
			final File newDirectory = new File(logsPath);
			if (!newDirectory.exists()) {
				newDirectory.mkdirs();
			}
			final FileHandler fh = new FileHandler(String.valueOf(logsPath) + this.getClass().getSimpleName() + ".log", 10240000, 200, true);
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
		logger.log(Level.INFO, "dredgeToShip: " + dredgeToShip);
	}

	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}
}
