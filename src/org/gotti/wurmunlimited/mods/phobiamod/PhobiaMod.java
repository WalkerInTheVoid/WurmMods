package org.gotti.wurmunlimited.mods.phobiamod;

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
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

public class PhobiaMod implements WurmMod, PreInitable, Configurable {
	
	private boolean replaceHugeSpidersWithBlackBears = true;
	private boolean replaceLavaSpidersWithLavaFiends = true;
	private static boolean bDebug = false;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Override
	public void configure(Properties properties) {
		replaceHugeSpidersWithBlackBears = Boolean.valueOf(properties.getProperty("replaceHugeSpidersWithBlackBears", Boolean.toString(replaceHugeSpidersWithBlackBears)));
		replaceLavaSpidersWithLavaFiends = Boolean.valueOf(properties.getProperty("replaceLavaSpidersWithLavaFiends", Boolean.toString(replaceLavaSpidersWithLavaFiends)));
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
		logger.log(Level.INFO, "replaceHugeSpidersWithBlackBears: " + replaceHugeSpidersWithBlackBears);
		logger.log(Level.INFO, "replaceLavaSpidersWithLavaFiends: " + replaceLavaSpidersWithLavaFiends);
	}

	@Override
	public void preInit() {
		if (replaceHugeSpidersWithBlackBears || replaceLavaSpidersWithLavaFiends) {
			try {
				CtClass[] paramTypes = {
						CtPrimitiveType.intType
				};
				CtClass ctCreatureTemplateFactory = HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.CreatureTemplateFactory");
//				ctCaveTileBehaviour.addField(CtField.make("private static final com.wurmonline.server.behaviours.TileRockBehaviour surfaceRockBehaviour = new com.wurmonline.server.behaviours.TileRockBehaviour();", ctCaveTileBehaviour));
				CtMethod ctGetTemplate = ctCreatureTemplateFactory.getDeclaredMethod("getTemplate", paramTypes);
				String toInsert = "{";
				if (replaceHugeSpidersWithBlackBears) {
					toInsert += "if (id == com.wurmonline.server.creatures.CreatureTemplateIds.SPIDER_CID) {";
					if (bDebug) {
					}
					toInsert += "id = com.wurmonline.server.creatures.CreatureTemplateIds.BEAR_BLACK_CID;}";
				}
				if (replaceLavaSpidersWithLavaFiends) {
					toInsert += "if (id == com.wurmonline.server.creatures.CreatureTemplateIds.LAVA_SPIDER_CID) {";
					if (bDebug) {
					}
					toInsert += "id = com.wurmonline.server.creatures.CreatureTemplateIds.LAVA_CREATURE_CID;}";
				}
				toInsert += "}";
				ctGetTemplate.insertBefore(toInsert);
				
				CtClass[] paramStringTypes = {
						HookManager.getInstance().getClassPool().get("java.lang.String")
				};
				CtMethod ctGetTemplateByString = ctCreatureTemplateFactory.getDeclaredMethod("getTemplate", paramStringTypes );
				toInsert = "{";
				if (replaceHugeSpidersWithBlackBears) {
					toInsert += "if (name.equals(\"Huge spider\")) {";
					if (bDebug) {
					}
					toInsert += "name = \"Black bear\";}";
				}
				if (replaceLavaSpidersWithLavaFiends) {
					toInsert += "if (name.equals(\"Lava spider\")) {";
					if (bDebug) {
					}
					toInsert += "name = \"Lava fiend\";}";
				}
				toInsert += "}";
				ctGetTemplateByString.insertBefore(toInsert);
				
				CtClass ctCreature = HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature");
				CtMethod ctSetName = ctCreature.getDeclaredMethod("setName", paramStringTypes);
				toInsert = "{";
				if (replaceHugeSpidersWithBlackBears) {
					toInsert += "if (_name.equals(\"Huge spider\")) {";
					if (bDebug) {
					}
					toInsert += "_name = \"Black bear\";}";
				}
				if (replaceLavaSpidersWithLavaFiends) {
					toInsert += "if (_name.equals(\"Lava spider\")) {";
					if (bDebug) {
					}
					toInsert += "_name = \"Lava fiend\";}";
				}
				toInsert += "}";
				ctSetName.insertBefore(toInsert);
			} catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
	}

	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}

}
