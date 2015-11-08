package org.gotti.wurmunlimited.mods.meditatemod;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.JavassistUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
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
	boolean noPathLevelDelay = false;
	boolean bDebug = false;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public void configure(Properties properties) {
		autoPassSkillChecks = Boolean.parseBoolean(properties.getProperty("autoPassSkillChecks", Boolean.toString(autoPassSkillChecks)));
		noMeditateDelay = Boolean.parseBoolean(properties.getProperty("noMeditateDelay", Boolean.toString(noMeditateDelay)));
		noMeditateDistance = Boolean.parseBoolean(properties.getProperty("noMeditateDistance", Boolean.toString(noMeditateDistance)));
		unlimitedMeditations = Boolean.parseBoolean(properties.getProperty("unlimitedMeditations", Boolean.toString(unlimitedMeditations)));
		noPathLevelDelay = Boolean.parseBoolean(properties.getProperty("noPathLevelDelay", Boolean.toString(noPathLevelDelay)));
		bDebug = Boolean.parseBoolean(properties.getProperty("debug", Boolean.toString(bDebug)));

		logger.log(Level.INFO, "autoPassSkillChecks: " + autoPassSkillChecks);
		logger.log(Level.INFO, "noMeditateDelay: " + noMeditateDelay);
		logger.log(Level.INFO, "noMeditateDistance: " + noMeditateDistance);
		logger.log(Level.INFO, "unlimitedMeditations: " + unlimitedMeditations);
		logger.log(Level.INFO, "noPathLevelDelay: " + noPathLevelDelay);
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
				|| unlimitedMeditations || noPathLevelDelay ) {
			try {
				CtClass ctCults = JavassistUtil.getCtClass("com.wurmonline.server.players.Cults");
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
							if (noPathLevelDelay) {
								if (m.getMethodName().equals("getTimeLeftToIncreasePath")) {
									/*
									 * This function returns how much time from
									 * now the player needs to wait until the
									 * next path question. If it is negative,
									 * the time is in the past. Thus, to ensure
									 * there is NO delay, we override it to
									 * return -1L.
									 */
									m.replace("$_ = -1L;");
									Debug("Replaced getTimeLeftToIncreasePath");
									return;
								}
							}
						}
					}
				});
			} catch (NotFoundException | CannotCompileException e) {
				throw new HookException(e);
			}
		}
	}

}
