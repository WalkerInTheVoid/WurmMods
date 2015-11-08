package org.gotti.wurmunlimited.mods.bountymod;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Features;
import com.wurmonline.server.Server;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

public class BountyMod implements WurmMod, Configurable, PreInitable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private boolean skillGainForBred = true;
	private boolean increasedBounties = true;
	private static boolean bDebug = false;

	@Override
	public void configure(Properties properties) {
		skillGainForBred = Boolean.parseBoolean(properties.getProperty("skillGainForBred", Boolean.toString(skillGainForBred)));
		logger.log(Level.INFO, "skillGainForBred: " + skillGainForBred);
		increasedBounties = Boolean.parseBoolean(properties.getProperty("increasedBounties", Boolean.toString(increasedBounties)));
		logger.log(Level.INFO, "increasedBounties: " + increasedBounties);
		bDebug = Boolean.parseBoolean(properties.getProperty("debug", Boolean.toString(bDebug)));
		Debug("Debug is on.");
	}
	
	@Override
	public void preInit() {
		Debug("Running preInit");
		if (skillGainForBred || increasedBounties) {
			try {
				CtClass ctCreature = ClassPool.getDefault().get("com.wurmonline.server.creatures.Creature");
				if(increasedBounties){
					Debug("Injecting increased bounty code...");
					ClassPool myPool = new ClassPool(ClassPool.getDefault());
					myPool.appendClassPath("./mods/bountymod/bountymod.jar");
					CtClass ctTHIS = myPool.get(this.getClass().getName());
					ctCreature.addField(CtField.make("private static final boolean bDebug = " + Boolean.toString(bDebug) + ";", ctCreature));
					ctCreature.addMethod(CtNewMethod.copy(ctTHIS.getDeclaredMethod("DebugLog"), ctCreature, null));
					ctCreature.addMethod(CtNewMethod.copy(ctTHIS.getDeclaredMethod("checkCoinBounty"), ctCreature, null));
					ctCreature.getDeclaredMethod("modifyFightSkill").instrument(new ExprEditor() {
						public void edit(MethodCall m)
								throws CannotCompileException {
							if (m.getClassName().equals("com.wurmonline.server.players.Player")
									&& m.getMethodName().equals("checkCoinAward")) {
								String debugString = "";
								if (bDebug)
									debugString = "java.util.logging.Logger.getLogger(\"org.gotti.wurmunlimited.mods.bountymod.BountyMod"
											+ "\").log(java.util.logging.Level.INFO, \"Overriding checkCoinAward to checkCoinBounty\");\n";
								m.replace(debugString + "$_ = checkCoinBounty(player, this.isDomestic(), this.isBred());");
							}
						}
					});
				}
				if(skillGainForBred) {
					Debug("Injecting skillgain for bred code...");
					ctCreature.getDeclaredMethod("isNoSkillgain").instrument(new ExprEditor() {
						public void edit(MethodCall m)
								throws CannotCompileException {
							if (m.getClassName().equals("com.wurmonline.server.creatures.Creature")
									&& m.getMethodName().equals("isBred")) {
								String debugString = "";
								if(bDebug) debugString = "System.out.println(\"isBred suppressed to false\");\n" + 
										"System.out.flush();\n";
								m.replace(debugString + "$_ = false;");
							}
						}
					});
				}
			}
			catch (NotFoundException | CannotCompileException e) {
				logger.log(Level.SEVERE, "Error editing functions:", e);
				e.printStackTrace();
			}
		}
	}
	
	public boolean checkCoinBounty(Player thisPlayer, boolean isDomestic, boolean isBred) {

//		int chance = (int)args[0];
		DebugLog("checkCoinBounty running...");
		try {
	        if (Features.Feature.COIN_KICKBACK.isEnabled()) {
	            final Shop kingsMoney = Economy.getEconomy().getKingsShop();
	            if (kingsMoney.getMoney() > 100000L) {
	                int coin = 50;
	                String coinMessage = "a copper coin.";
                	final int coinRand = Math.max(Server.rand.nextInt(10), (isDomestic && !isBred) ? Server.rand.nextInt(10) : 0 );
	                if(isBred) {
		                //If it's a bred creature, reduced coinage rate.
	                	switch (coinRand) {
	                	case 0: {
	                		//1 iron coin
	                		DebugLog("Awarding 1 iron");
	                		coin = 51;
	                		coinMessage = "an iron coin.";
	                		break;
	                	}
	                	case 1:
	                	case 2:
	                	case 3:
	                	case 4: {
	                		//5 iron coin
	                		DebugLog("Awarding 5 iron");
	                		coinMessage = "five irons.";
	                		coin = 55;
	                		break;
	                	}
	                	case 5:
	                	case 6:
	                	case 7:
	                	case 8: {
	                		//20 copper coin
	                		DebugLog("Awarding 20 iron");
	                		coinMessage = "twenty irons.";
	                		coin = 59;
	                		break;
	                	}
	                	case 9: {
	                		//1 copper coin
	                		DebugLog("Awarding 1 copper");
	                		coinMessage = "a copper coin.";
	                		coin = 50;
	                		break;
	                	}
	                	default: {
	                		//1 copper coin
	                		DebugLog("Awarding 1 iron");
	                		coinMessage = "an iron coin.";
	                		coin = 51;
	                		break;
	                	}
                	}
                }
	                else {
	                	switch (coinRand) {
		                	case 0: {
		                		//1 copper coin
		                		DebugLog("Awarding 1 copper");
		                		coinMessage = "a copper coin.";
		                		coin = 50;
		                		break;
		                	}
		                	case 1:
		                	case 2:
		                	case 3:
		                	case 4: {
		                		//5 copper coin
		                		DebugLog("Awarding 5 copper");
		                		coinMessage = "five coppers.";
		                		coin = 54;
		                		break;
		                	}
		                	case 5:
		                	case 6:
		                	case 7:
		                	case 8: {
		                		//20 copper coin
		                		DebugLog("Awarding 20 copper");
		                		coinMessage = "twenty coppers.";
		                		coin = 58;
		                		break;
		                	}
		                	case 9: {
		                		//1 silver coin
		                		DebugLog("Awarding 1 silver");
		                		coinMessage = "a silver coin.";
		                		coin = 52;
		                		break;
		                	}
		                	default: {
		                		//1 copper coin
		                		DebugLog("Awarding 1 copper");
		                		coinMessage = "a copper coin.";
		                		coin = 50;
		                		break;
		                	}
	                	}
	                }
	                try {
	                    final Item coinItem = ItemFactory.createItem(coin, 60 + Server.rand.nextInt(20), thisPlayer.getRarity(), "");
	                    thisPlayer.getInventory().insertItem(coinItem, true);
	                    kingsMoney.setMoney(kingsMoney.getMoney() - Economy.getValueFor(coin));
	                    thisPlayer.getCommunicator().sendNormalServerMessage("You receive a bounty of " + coinMessage);
	                    return true;
	                }
	                catch (NoSuchTemplateException nst) {
	                	((Logger)ReflectionUtil.getPrivateField(
	                			null, ReflectionUtil.getField(thisPlayer.getClass(), "logger"))).log(Level.WARNING, "No template for item coin");
	                }
	                catch (FailedException fe) {
	                	((Logger)ReflectionUtil.getPrivateField(
	                    		null, ReflectionUtil.getField(thisPlayer.getClass(), "logger"))).log(Level.WARNING, String.valueOf(fe.getMessage()) + ": coin");
	                }
	            } 
	            else {
	            	thisPlayer.getCommunicator().sendNormalServerMessage("There are apparently no coins in the coffers to pay out a bounty at the moment.");
	            }
	        }
	        return false;
		}
		catch (Exception e) {
			java.util.logging.Logger.getLogger("org.gotti.wurmunlimited.mods.bountymod.BountyMod").log(Level.SEVERE, "checkCoinBounty went Kablooie", e);
			return false;
		}
	}
	
	private void DebugLog(String x) {
		if (bDebug) {
			java.util.logging.Logger.getLogger("org.gotti.wurmunlimited.mods.bountymod.BountyMod").log(Level.INFO, x);
		}
	}
	
	
	private void Debug(String x) {
		if (bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			logger.log(Level.INFO, x);
		}
	}

}
