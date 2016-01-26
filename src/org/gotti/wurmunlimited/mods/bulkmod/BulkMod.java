package org.gotti.wurmunlimited.mods.bulkmod;

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
import javassist.expr.FieldAccess;

public class BulkMod implements WurmMod, Configurable, PreInitable {

	private boolean ignoreTemp = true;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static boolean bDebug = false;
	
	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		if(ignoreTemp) {
			try {
				CtClass ctItem = HookManager.getInstance().getClassPool()
						.getCtClass("com.wurmonline.server.items.Item");
				final CtMethod moveToItem = ctItem.getDeclaredMethod("moveToItem");
				moveToItem.instrument(new ExprEditor() {
					public void edit(FieldAccess f)
							throws CannotCompileException {
						if(f.getClassName().equals("com.wurmonline.server.items.Item")
								&& f.getFieldName().equals("temperature")) {
							Debug("Replacing temperature field access in moveToItem...");
							f.replace("$_ = 0;");
						}
					}
				});
			}
			catch (NotFoundException | CannotCompileException e) {
				logger.severe("Exception in modifying Item.class: " + e);
				throw new HookException(e);
			}
		}

	}

	@Override
	public void configure(Properties properties) {
		ignoreTemp = Boolean.parseBoolean(properties.getProperty("ignoreTemp", Boolean.toString(ignoreTemp)));
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
		logger.log(Level.INFO, "ignoreTemp: " + ignoreTemp);
	}

	private void Debug(String x) {
		if(bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}
}
