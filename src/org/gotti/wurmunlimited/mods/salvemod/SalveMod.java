package org.gotti.wurmunlimited.mods.salvemod;

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

public class SalveMod implements WurmMod, Configurable, PreInitable {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static boolean bDebug = false;
	private boolean nameByPower = true;

	@Override
	public void preInit() {
		if (nameByPower) {
			try {
				CtMethod createSalve = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.MethodsItems").getDeclaredMethod("createSalve");
				createSalve.instrument(new ExprEditor() {
					public void edit(MethodCall m)
							throws CannotCompileException {
						if (m.getClassName().equals("com.wurmonline.server.items.Item")
								&& m.getMethodName().equals("setDescription")) {
							/*The objective here is to add a prefix to the 
							 * description that lists the healing cover's power.
							 */
							m.replace("java.lang.String sPower = java.lang.String.valueOf(source.getAlchemyType() * target.getAlchemyType());\r\n" + 
									"$_ = $proceed(\"00\".substring(sPower.length()) + sPower + \" - \" + $1);");
						}
					}
				});
			} catch (NotFoundException | CannotCompileException e) {
				e.printStackTrace();
				throw new HookException(e);
			}
		}
	}

	@Override
	public void configure(Properties properties) {
		nameByPower = Boolean.parseBoolean(properties.getProperty("nameByPower", Boolean.toString(nameByPower)));
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
		logger.log(Level.INFO, "nameByPower: " + nameByPower);
	}

	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}
}
