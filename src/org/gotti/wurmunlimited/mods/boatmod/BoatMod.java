package org.gotti.wurmunlimited.mods.boatmod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;
import org.gotti.wurmunlimited.modsupport.vehicles.ModVehicleBehaviour;
import org.gotti.wurmunlimited.modsupport.vehicles.ModVehicleBehaviours;

import com.wurmonline.server.behaviours.Seat;
import com.wurmonline.server.behaviours.Vehicle;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class BoatMod implements WurmMod, Configurable, PreInitable, Initable {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static boolean bDebug = false;
	private boolean cancelShipWindEffect = true;
	private boolean allSwim = true;
	private boolean leadThroughEmbark = true;
	
	//DISABLED, not ready for prime time
	private boolean animalSeats = false;
	private boolean orderPassengersByDistance = false;
	
	@Override
	public void preInit() {
		if(cancelShipWindEffect) {
			try {
				/* This here is a forcible replacement of the boat speed code,
				 * so as to add one last little wrinkle: Capping the speed at 
				 * the maximum value of a byte BEFORE converting to a byte, 
				 * thus preventing overflow. */
				CtMethod calcNewBoatSpeed = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.Vehicle").getDeclaredMethod("calculateNewBoatSpeed");
				calcNewBoatSpeed.setBody("{        int numsOccupied = 0;\r\n" + 
						"        float qlMod = 0.0f;\r\n" + 
						"        for (int x = 0; x < this.seats.length; ++x) {\r\n" + 
						"            if (this.seats[x].isOccupied()) {\r\n" + 
						"                ++numsOccupied;\r\n" + 
						"            }\r\n" + 
						"        }\r\n" + 
						"        try {\r\n" + 
						"            final com.wurmonline.server.items.Item itemVehicle = com.wurmonline.server.Items.getItem(this.wurmid);\r\n" + 
						"            numsOccupied = Math.min(this.seats.length, numsOccupied + itemVehicle.getRarity());\r\n" + 
						"            qlMod = Math.max(0.0f, itemVehicle.getCurrentQualityLevel() - 10.0f) / 90.0f;\r\n" + 
						"            if (qlMod > 0.0f) {\r\n" + 
						"                ++qlMod;\r\n" + 
						"            }\r\n" + 
						"        }\r\n" + 
						"        catch (com.wurmonline.server.NoSuchItemException nsi) {\r\n" + 
						"            return 0;\r\n" + 
						"        }\r\n" + 
						"        if ($1) {\r\n" + 
						"            --numsOccupied;\r\n" + 
						"        }\r\n" + 
						"        float percentOccupied = 1.0f;\r\n" + 
						"        percentOccupied = 1.0f + numsOccupied / this.seats.length;\r\n" + 
						" float toReturn = percentOccupied * 10.0f * this.maxSpeed + qlMod * this.maxSpeed;\r\n" +
						"        return (byte)Math.min(toReturn, (float)Byte.MAX_VALUE);\r\n" + 
						"}");
				
			} catch (NotFoundException | CannotCompileException e) {
				e.printStackTrace();
				throw new HookException(e);
			}
		}
		if (orderPassengersByDistance) {
//			if(bDebug) {
//				try {
//					CtMethod occupy = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.Seat").getDeclaredMethod("occupy");
//					occupy.insertAfter("System.out.println(\"This seat is \" + (float) Math.sqrt(Math.pow((double)offx, (double)2) + Math.pow((double)offy, (double)2)) + \" from the driver.\");");
//					CtMethod setMountAction = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.creatures.Creature").getDeclaredMethod("setMountAction");
//					setMountAction.insertAfter("if(act != null) { System.out.println(\"The mounting seat is \" + (float) Math.sqrt(Math.pow((double)act.getVehicle().seats[act.getSeatNum()].offx, (double)2) + Math.pow((double)act.getVehicle().seats[act.getSeatNum()].offy, (double)2)) + \" from the driver.\");}");
////					CtMethod getSeatFor = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.Vehicle").getDeclaredMethod("getSeatFor");
////					getSeatFor.insertAfter("System.out.println(\"The located seat is \" + (float) Math.sqrt(Math.pow((double)$_.offx, (double)2) + Math.pow((double)$_.offy, (double)2)) + \" from the driver.\");");
//					CtMethod setVehicle = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.players.Player").getDeclaredMethod("setVehicle");
//					setVehicle.insertAfter("            final com.wurmonline.server.behaviours.Vehicle vehic = com.wurmonline.server.behaviours.Vehicles.getVehicleForId(this.vehicle);\r\n" + 
//							"            if (vehic != null) {\r\n" + 
//							"                float offx = 0.0f;\r\n" + 
//							"                float offy = 0.0f;\r\n" + 
//							"                for (int x = 0; x < vehic.seats.length; ++x) {\r\n" + 
//							"                    if (vehic.seats[x].occupant == this.getWurmId()) {\r\n" + 
//							"                        offx = vehic.seats[x].offx;\r\n" + 
//							"                        offy = vehic.seats[x].offy;\r\n" + 
//							"                        break;\r\n" + 
//							"                    }\r\n" + 
//							"                }\r\n" + 
//							"System.out.println(\"This seat is \" + (float) Math.sqrt(Math.pow((double)offx, (double)2) + Math.pow((double)offy, (double)2)) + \" from the driver.\");}");
//					CtMethod sendAttachCreature = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.zones.VolaTile").getDeclaredMethod("sendAttachCreature");
//					sendAttachCreature.insertAfter("System.out.println(\"The attached seat is \" + (float) Math.sqrt(Math.pow((double)offx, (double)2) + Math.pow((double)offy, (double)2)) + \" from the driver.\");");
//					CtMethod setPositionXYZ = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.creatures.CreaturePos").getDeclaredMethod("setPosY");
////					setPositionXYZ.insertAfter("if(isPlayer) {System.out.println(\"New Pos(y): \"+ posY + \" isPlayer: \" + isPlayer);new Exception().printStackTrace();}");
//					CtMethod communicateAttachCreature = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.creatures.Communicator").getDeclaredMethod("attachCreature");
//					communicateAttachCreature.insertAfter("System.out.println(\"The communicated seat is \" + (float) Math.sqrt(Math.pow((double)offx, (double)2) + Math.pow((double)offy, (double)2)) + \" from the driver.\");");
//					
//				} catch (NotFoundException | CannotCompileException e) {
//					e.printStackTrace();
//					throw new HookException(e);
//				}
//			}
		}
		if (animalSeats) {
			try {
				CtMethod strongEnoughToDrag = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.VehicleBehaviour").getDeclaredMethod("isStrongEnoughToDrag");
				strongEnoughToDrag.insertBefore("if(aVehicle.isBoat()) return true;");
				CtMethod positionDragger = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.Vehicle").getDeclaredMethod("positionDragger");
				positionDragger.instrument(new ExprEditor() {
					public void edit(MethodCall m)
							throws CannotCompileException {
						if (m.getClassName().equals("com.wurmonline.server.zones.Zones")
								&& m.getMethodName().equals("calculateHeight")) {
							m.replace("if (this.windImpact == 0 ) { $_ = $proceed($$); } else {" + 
									"	for (int temp = 0; temp < this.hitched.length; ++temp) {"
									+ "		if (this.hitched[temp].type == 2 && this.hitched[temp].getOccupant() == dragger.getWurmId()) {"
									+ "			$_ = this.hitched[temp].offz;"
									+ "		}"
									+ "}" + 
									"}");
						}
					}
				});
				CtMethod calcNewVehicleSpeed = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.behaviours.Vehicle").getDeclaredMethod("calculateNewVehicleSpeed");
				calcNewVehicleSpeed.insertBefore("if(this.windImpact > 0) return calculateNewBoatSpeed($$);");
			} catch (NotFoundException | CannotCompileException e) {
				e.printStackTrace();
				throw new HookException(e);
			}
		}
		if (allSwim) {
			try {
				CtMethod embark = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.creatures.Creature").getDeclaredMethod("isSwimming");
				embark.insertBefore("if(this.leader != null) return true;");
			} catch (NotFoundException | CannotCompileException e) {
				e.printStackTrace();
				throw new HookException(e);
			}
		}
		if (leadThroughEmbark) {
			try {
				CtMethod embark = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.server.players.Player").getDeclaredMethod("embark");
				embark.instrument(new ExprEditor() {
					public void edit(MethodCall m)
							throws CannotCompileException {
						if (m.getClassName().equals("com.wurmonline.server.players.Player")
								&& m.getMethodName().equals("stopLeading")) {
							//The whole point here is to simply suppress the "stop leading all animals on embark
							m.replace("$_ = null;");
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
		cancelShipWindEffect = Boolean.parseBoolean(properties.getProperty("cancelShipWindEffect", Boolean.toString(cancelShipWindEffect)));
//		animalSeats = Boolean.parseBoolean(properties.getProperty("animalSeats", Boolean.toString(animalSeats)));
//		orderPassengersByDistance = Boolean.parseBoolean(properties.getProperty("orderPassengersByDistance", Boolean.toString(orderPassengersByDistance)));
		allSwim = Boolean.parseBoolean(properties.getProperty("allSwim", Boolean.toString(allSwim)));
		leadThroughEmbark = Boolean.parseBoolean(properties.getProperty("leadThroughEmbark", Boolean.toString(leadThroughEmbark)));
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
		logger.log(Level.INFO, "cancelShipWindEffect: " + cancelShipWindEffect);
		logger.log(Level.INFO, "allSwim: " + allSwim);
		logger.log(Level.INFO, "leadThroughEmbark: " + leadThroughEmbark);
//		logger.log(Level.INFO, "animalSeats: " + animalSeats);
//		logger.log(Level.INFO, "orderPassengersByDistance: " + orderPassengersByDistance);
	}

	@Override
	public void init() {
		if (cancelShipWindEffect || animalSeats || orderPassengersByDistance) {
			ModVehicleBehaviours.init();
			ModBoats modBoats = new ModBoats();
			ModVehicleBehaviours.addItemVehicle(ItemList.cog, modBoats);
			ModVehicleBehaviours.addItemVehicle(ItemList.knarr, modBoats);
			ModVehicleBehaviours.addItemVehicle(ItemList.caravel, modBoats);
			ModVehicleBehaviours.addItemVehicle(ItemList.corbita, modBoats);
			ModVehicleBehaviours.addItemVehicle(ItemList.boatSailing, modBoats);
		}
	}
	
	class ModBoats extends ModVehicleBehaviour {

		@Override
		public void setSettingsForVehicle(Creature creature, Vehicle vehicle) {
			// Do nothing cause don't care about mounts
			
		}

		@Override
		public void setSettingsForVehicle(Item item, Vehicle vehicle) {
			if (cancelShipWindEffect) {
				if (vehicle.getWindImpact() > 10) {
					float windMultiplier = vehicle.getWindImpact() / 10;
					try {
						float speed = ReflectionUtil.getPrivateField(vehicle,
								ReflectionUtil.getField(vehicle.getClass(), "maxSpeed"));
						Debug("Old Max Speed: " + speed);
						Debug("Item: " + item.getTemplateId());
						Debug("Wind Multiplier: " + windMultiplier);
						ReflectionUtil.setPrivateField(vehicle, ReflectionUtil.getField(vehicle.getClass(), "maxSpeed"),
								speed * windMultiplier);
						Debug("New Max Speed: " + ReflectionUtil.getPrivateField(vehicle,
								ReflectionUtil.getField(vehicle.getClass(), "maxSpeed")));
						ReflectionUtil.callPrivateMethod(vehicle,
								ReflectionUtil.getMethod(vehicle.getClass(), "setWindImpact"), (byte) 10);
					} catch (Exception e) {
						e.printStackTrace();
						throw new HookException(e);
					}
				}
			}
			if(orderPassengersByDistance) {
				try {
				Seat[] originalSeats = vehicle.seats;
				Seat[] newSeats = new Seat[originalSeats.length];
				newSeats[0] = originalSeats[0];
				ArrayList<Seat> seatList = new ArrayList<Seat>(originalSeats.length - 1);
				for (int i = 1; i < originalSeats.length; i++) {
					seatList.add(originalSeats[i]);
				}
				seatList.sort(new Comparator<Seat>() {
					public int compare(Seat seat1, Seat seat2) {
				        return (int) Math.ceil((float) Math.sqrt(Math.pow(seat1.offx, 2) + Math.pow(seat1.offy, 2))
				        - (float) Math.sqrt(Math.pow(seat2.offx, 2) + Math.pow(seat2.offy, 2)));
				    }
				});
				Iterator<Seat> distanceIterator = seatList.iterator();
				int count = 0;
				for (int i = 1; i < newSeats.length; i++) {
					newSeats[i] = distanceIterator.next();
					Debug("iterations: " + count++);
					Debug("Distance: " + (float) Math.sqrt(Math.pow(newSeats[i].offx, 2) + Math.pow(newSeats[i].offy, 2)));
				}
				vehicle.seats = newSeats;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(animalSeats) {
				Seat[] originalSeats = vehicle.seats;
				Debug("Total seats = " + originalSeats.length);
				int numPassengers = originalSeats.length - 1;
				Seat[] newSeats = new Seat[((numPassengers + 1) / 2) + 1];
				Debug("New seats = " + newSeats.length);
				Seat[] hitchSeats = new Seat[(numPassengers / 2)];
				Debug("Hitched Seats: " + hitchSeats.length);
				
				for(int i = 0; i < newSeats.length; i++) {
					newSeats[i] = originalSeats[i];
				}
				Debug("Now for hitched seats");
				try {
				for (int i = 0; i < hitchSeats.length; i++) {
					Seat s = originalSeats[i + newSeats.length];
					Seat hitchSeat = createSeat(Seat.TYPE_HITCHED);
					hitchSeat.offx = s.offx;
					hitchSeat.offy = s.offy;
					hitchSeat.offz = s.offz;
					hitchSeats[i] = hitchSeat;
				}
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					ReflectionUtil.setPrivateField(vehicle, ReflectionUtil.getField(vehicle.getClass(), "maxPassengers"),
							(byte)(newSeats.length - 1));
				} catch (IllegalArgumentException | IllegalAccessException | ClassCastException
						| NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new HookException(e);
				}
				vehicle.seats = newSeats;
				vehicle.addHitchSeats(hitchSeats);
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
