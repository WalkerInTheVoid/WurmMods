package org.gotti.wurmunlimited.mods.meditatemod;

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
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class MeditateMod implements WurmMod, Configurable, PreInitable {
	boolean autoPassSkillChecks = true;
	boolean noMeditateDelay = true;
	boolean noMeditateDistance = true;
	boolean unlimitedMeditations = false;
	boolean useCustomTime = false;
	float pathLevelDelayMultiplier = 1.0f;
	int customTime = 300;
	boolean bDebug = false;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public void configure(Properties properties) {
		autoPassSkillChecks = Boolean.parseBoolean(properties.getProperty("autoPassSkillChecks", Boolean.toString(autoPassSkillChecks)));
		noMeditateDelay = Boolean.parseBoolean(properties.getProperty("noMeditateDelay", Boolean.toString(noMeditateDelay)));
		noMeditateDistance = Boolean.parseBoolean(properties.getProperty("noMeditateDistance", Boolean.toString(noMeditateDistance)));
		unlimitedMeditations = Boolean.parseBoolean(properties.getProperty("unlimitedMeditations", Boolean.toString(unlimitedMeditations)));
		useCustomTime = Boolean.parseBoolean(properties.getProperty("useCustomTime", Boolean.toString(useCustomTime)));
		pathLevelDelayMultiplier = Float.parseFloat(properties.getProperty("pathLevelDelayMultiplier", Float.toString(pathLevelDelayMultiplier)));
		customTime = Integer.parseInt(properties.getProperty("customTime", Integer.toString(customTime)));
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
		}
		logger.log(Level.INFO, "autoPassSkillChecks: " + autoPassSkillChecks);
		logger.log(Level.INFO, "noMeditateDelay: " + noMeditateDelay);
		logger.log(Level.INFO, "noMeditateDistance: " + noMeditateDistance);
		logger.log(Level.INFO, "unlimitedMeditations: " + unlimitedMeditations);
		logger.log(Level.INFO, "pathLevelDelayMultiplier: " + pathLevelDelayMultiplier);
		logger.log(Level.INFO, "useCustomTime: " + useCustomTime);
		logger.log(Level.INFO, "customTime: " + customTime);
		Debug("Debugging messages are enabled.");
		
	}

	private void Debug(String x) {
		if (bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}

	@Override
	public void preInit() {
		if (autoPassSkillChecks || noMeditateDistance || noMeditateDelay 
				|| unlimitedMeditations || useCustomTime ) {
			try {
				CtClass ctCults = HookManager.getInstance().getClassPool().get("com.wurmonline.server.players.Cults");
				ctCults.getDeclaredMethod("meditate").instrument(new ExprEditor() {
					public void edit(MethodCall m) throws CannotCompileException {
						if (autoPassSkillChecks) {
							if (m.getClassName().equals("com.wurmonline.server.skills.Skill")
									&& m.getMethodName().equals("skillCheck")) {
								m.replace("$proceed($$);$_ = 1.0f;");
								Debug("Replaced skillCheck.");
								return;
							} else if (m.getClassName().equals("com.wurmonline.server.creatures.CreatureStatus")
									&& m.getMethodName().equals("refresh")) {
								/*
								 * So. In making all skill checks pass, we'll
								 * accidentally trigger a semi-refresh on every
								 * meditation. This suppresses that unwanted
								 * side effect.
								 */
								m.replace("$_ = false;");
								Debug("Replaced refresh");
								return;
							}
						}
						if (noMeditateDistance) {
							if (m.getClassName().equals("com.wurmonline.server.creatures.Creature")
									&& m.getMethodName().equals("getMeditateX")) {
								/*
								 * The code takes the absolute value of
								 * getMeditateX - currentTileX then checks if
								 * that's less than 10. That's just one of the
								 * conditions (there's a similar check for the
								 * Y-coordinate), but all we need to do is
								 * change ONE of them and we pass the distance
								 * check. So if the getMeditateX returns -10,
								 * then no matter what, the distance check will
								 * pass and override the need to move. Which is
								 * exactly what we want.
								 */
								m.replace("$_ = -10;");
								Debug("Replaced getMeditateX");
								return;
							}
						}
						if (m.getClassName().equals("com.wurmonline.server.players.Cultist")) {
							if (noMeditateDelay) {
								if (m.getMethodName().equals("getLastMeditated")) {
									/*
									 * Normally, the game requires 30 minutes
									 * between meditations for skill. Once past
									 * skill 20 anyway. It calls this function
									 * to get the last time the player
									 * meditated.
									 */
									m.replace(
											"$_ = Math.min(cultist.getLastMeditated(), System.currentTimeMillis() - 1800001L);");
									Debug("Replaced getLastMeditated");
									return;
									/*
									 * The above means the game will think the
									 * player last meditated either when they
									 * actually meditated, or 30 minutes ago,
									 * whichever is earlier. This way it doesn't
									 * interfere if the configuration specifies
									 * no delay but not unlimited meditations
									 * per day. (In which case the player will
									 * get 5 skill gains with no delay, then 3
									 * hrs between skill gains after that till
									 * the next day)
									 */
								}
							}
							if (unlimitedMeditations) {
								if (m.getMethodName().equals("getSkillgainCount")) {
									/*
									 * If we don't want to limit the number of
									 * meditations for skill gain... why should
									 * this function ever return more than zero?
									 */
									m.replace("$_ = 0;");
									Debug("Replaced getSkillgainCount");
									return;
								}
							}
						}
						if (useCustomTime) {
							/*
							 * These two functions are where the action duration, once calculated,
							 * is communicated/stored.  By replacing the time variable with
							 * customTime, we'll set it to the duration we want.  The first one is 
							 * the server-side memory of the action time.  It's the one that determines
							 * when the action is over/complete, but not what is shown to the user's
							 * action timer.
							 */
							if (m.getClassName().equals("com.wurmonline.server.behaviours.Action")
									&& m.getMethodName().equals("setTimeLeft")) {
								m.replace("$_ = $proceed(" + customTime + ");");
								return;
							}
							/*
							 * This is what is passed to the user's action timer.
							 */
							if (m.getClassName().equals("com.wurmonline.server.creatures.Creature")
									&& m.getMethodName().equals("sendActionControl")) {
								m.replace("$_ = $proceed($1, $2, " + customTime + ");");
								return;
							}
						}
					}
				});
			} catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
		if (pathLevelDelayMultiplier != 1.0f) {
			try {
				CtClass ctCultist = HookManager.getInstance().getClassPool()
						.get("com.wurmonline.server.players.Cultist");
				String toInsert = "{\r\n";
				if (bDebug) {
					toInsert += "	java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.meditatemod.MeditateMod\").info(\"Original time left: \" + $_);\r\n";
					toInsert += "	System.out.println(\"Original time left: \" + $_);\r\n";
					toInsert += "	java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.meditatemod.MeditateMod\").info(\"Original Needed Time: \" + ($_  + (currentTime - this.lastReceivedLevel)));\r\n";
					toInsert += "	System.out.println(\"Original Needed Time: \" + ($_  + (currentTime - this.lastReceivedLevel)));\r\n";
				}
				toInsert += "	$_ = (($_ + (currentTime - this.lastReceivedLevel)) * " + pathLevelDelayMultiplier + ");\r\n";
				if (bDebug) {
					toInsert += "	java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.meditatemod.MeditateMod\").info(\"New Needed Time: \" + $_);\r\n";
					toInsert += "	System.out.println(\"New Needed Time: \" + $_);\r\n";
				}
				toInsert += "	$_ = $_  - (currentTime - this.lastReceivedLevel);";
				if (bDebug) {
					toInsert += "	java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.meditatemod.MeditateMod\").info(\"New time left: \" + $_);\r\n";
					toInsert += "	System.out.println(\"New time left: \" + $_);\r\n";
				}
				toInsert += "}";
				ctCultist.getDeclaredMethod("getTimeLeftToIncreasePath").insertAfter(toInsert);
			} catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}

	}

}
