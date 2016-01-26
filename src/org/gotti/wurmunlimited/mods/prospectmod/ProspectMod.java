package org.gotti.wurmunlimited.mods.prospectmod;

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
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

public class ProspectMod implements WurmMod, Configurable, PreInitable {
	private boolean canFindOreUnderground = true;
	private static boolean bDebug = false;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static com.wurmonline.server.behaviours.TileRockBehaviour surfaceRockBehaviour;
	
	@Override
	public void preInit() {
		if (canFindOreUnderground) {
			try {
				CtClass[] paramTypes = {
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
						CtPrimitiveType.intType,
						CtPrimitiveType.intType,
						CtPrimitiveType.booleanType,
						CtPrimitiveType.intType,
						CtPrimitiveType.intType,
						CtPrimitiveType.shortType,
						CtPrimitiveType.floatType
				};
				CtClass ctCaveTileBehaviour = HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.CaveTileBehaviour");
				//CaveTileBehavior doesn't have a good way to call non-static methods on TileRockBehavior.
				//So the following line GIVES it one, by creating a static TileRockBehavior that can then 
				//be used in the code that will be inserted into the "action" method.
				ctCaveTileBehaviour.addField(CtField.make("private static final com.wurmonline.server.behaviours.TileRockBehaviour surfaceRockBehaviour = new com.wurmonline.server.behaviours.TileRockBehaviour();", ctCaveTileBehaviour));
				CtMethod ctAction = ctCaveTileBehaviour.getDeclaredMethod("action", paramTypes);
				String toInsert = "if (source.isMiningtool() "
						+ "&& action == com.wurmonline.server.behaviours.Actions.PROSPECT) {";
				if(bDebug) {
					Debug("Inserting debugging statement into prospecting detection code...");
					toInsert += "System.out.println(\"Running Prospecting override...\");System.out.flush();";
				}
				toInsert += "return surfaceRockBehaviour.action(act, performer, source, tilex, tiley, "
						+ "onSurface, heightOffset, tile, action, counter);}";
				ctAction.insertBefore(toInsert);
				
			} catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
	}

	@Override
	public void configure(Properties properties) {
		canFindOreUnderground = Boolean.valueOf(properties.getProperty("canFindOreUnderground", Boolean.toString(canFindOreUnderground)));
		logger.log(Level.INFO, "canFindOreUnderground: " + canFindOreUnderground);
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
		Debug("Debugging messages enabled.");
	}
	
	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}

}
