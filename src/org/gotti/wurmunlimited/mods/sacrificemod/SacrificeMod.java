package org.gotti.wurmunlimited.mods.sacrificemod;

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
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class SacrificeMod implements WurmMod, PreInitable, Configurable {

	private static Logger logger;
	private static boolean bDebug = false;
	private static int boneChance = 0;

	@Override
	public void configure(Properties properties) {
		logger = Logger.getLogger(this.getClass().getName());
		boneChance = Integer.parseInt(properties.getProperty("boneChance", Integer.toString(boneChance)));
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
		logger.log(Level.INFO, "boneChance: " + boneChance);
	}

	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		if (boneChance > 0) {
			try {
				CtClass[] paramTypes = {
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action"),
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
						HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item")
				};
				CtMethod sacrifice = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.MethodsReligion").getDeclaredMethod("sacrifice", paramTypes);
				sacrifice.instrument(new ExprEditor() {
					int i = 0;
					public void edit(MethodCall m)
							throws CannotCompileException {
						if (m.getClassName().equals("com.wurmonline.server.items.Item")
								&& m.getMethodName().equals("getRarity")) {
							if(i > 0) {
								return;
							}
							/*
byte temp = $proceed($$);
$_ = temp;
if (temp > 0) {
	if (com.wurmonline.server.Server.rand.nextInt(boneChance) == 0) {
	    final float ql2 = Math.max(Math.min((float)(com.wurmonline.server.Server.rand.nextFloat() * 100), 100.0f), 1.0f);
	    com.wurmonline.server.items.Item bone;
		try {
			bone = com.wurmonline.server.items.ItemFactory.createItem(867, ql2, null);
	        bone.setRarity(temp);
	        performer.getInventory().insertItem(bone, true);
	        performer.getCommunicator().sendNormalServerMessage(altar.getBless().name + " gives you a reward for your sacrifice.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		$_ = 0;
	}
}
*/
							m.replace("byte temp = $proceed($$);\r\n" + 
									"$_ = temp;\r\n" + 
									"if (temp > 0) {\r\n" + 
									"	if (com.wurmonline.server.Server.rand.nextInt("+boneChance+") == 0) {\r\n" + 
									"	    final float ql2 = Math.max(Math.min((float)(com.wurmonline.server.Server.rand.nextFloat() * 100), 100.0f), 1.0f);\r\n" + 
									"	    com.wurmonline.server.items.Item bone;\r\n" + 
									"		try {\r\n" + 
									"			bone = com.wurmonline.server.items.ItemFactory.createItem(867, ql2, null);\r\n" + 
									"	        bone.setRarity(temp);\r\n" + 
									"	        performer.getInventory().insertItem(bone, true);\r\n" + 
									"	        performer.getCommunicator().sendNormalServerMessage(altar.getBless().name + \" gives you a reward for your sacrifice.\");\r\n" + 
									"		} catch (Exception e) {\r\n" + 
									"			// TODO Auto-generated catch block\r\n" + 
									"			e.printStackTrace();\r\n" + 
									"		}\r\n" + 
									"		$_ = 0;\r\n" + 
									"	}\r\n" + 
									"}\r\n");
							i++;
						}
					}
				});
			} catch (NotFoundException | CannotCompileException e) {
				e.printStackTrace();
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
