package org.gotti.wurmunlimited.mods.sortmod;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.InventoryWindow;
import com.wurmonline.client.renderer.gui.ItemListWindow;
import com.wurmonline.client.renderer.gui.WurmTreeList;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.bytecode.Descriptor;

public class SortMod implements WurmMod, Configurable, Initable {

	private static Logger logger;

	private boolean sortInventoryOnStart = true;
	private boolean autoSortInventory = false;
	private boolean sortContainersOnOpen = true;
	private boolean autoSortContainers = false;
	private boolean autoSortSkills = true;
	private boolean openInventoryOnStart = true;
	private int sortOn = -2;
	private int containerUpdateCutoff = 5;
	private int launchUpdateCutoff = 30;
	private static boolean bDebug = false;
	private static boolean fileLogger = false;
	private static int itemsAddedInUpdate = 0;
	private static int numUpdatesWithoutItemsAdded = 0;
//	private static int numUpdates = 0;
//	private static int updatesBeforeLastItemUpdate = 0;
//	private static int numUpdatesWithSortPending = 0;
//	private static int maxWaitPending = 0;
	private static List<PendingSort> sortsPending;
	private static int maxCutOff = 0;

	public static List<PendingSort> getSortsPending() {
		if(sortsPending == null) {
			sortsPending = new ArrayList<PendingSort>();
		}
		return sortsPending;
	}


	@Override
	public void configure(Properties properties) {
		sortInventoryOnStart = Boolean.parseBoolean(properties.getProperty("sortInventoryOnStart", 
				Boolean.toString(sortInventoryOnStart)));
		autoSortInventory = Boolean.parseBoolean(properties.getProperty("autoSortInventory", 
				Boolean.toString(autoSortInventory)));
		sortContainersOnOpen = Boolean.parseBoolean(properties.getProperty("sortContainersOnOpen", 
				Boolean.toString(sortContainersOnOpen)));
		autoSortContainers = Boolean.parseBoolean(properties.getProperty("autoSortContainers", 
				Boolean.toString(autoSortContainers)));
		openInventoryOnStart = Boolean.parseBoolean(properties.getProperty("openInventoryOnStart", 
				Boolean.toString(openInventoryOnStart)));
		autoSortSkills = Boolean.parseBoolean(properties.getProperty("openInventoryOnStart", 
				Boolean.toString(autoSortSkills)));
		sortOn = Integer.parseInt(properties.getProperty("sortOn", 
				Integer.toString(sortOn)));
		containerUpdateCutoff = Integer.parseInt(properties.getProperty("containerUpdateCutoff", 
				Integer.toString(containerUpdateCutoff)));
		launchUpdateCutoff = Integer.parseInt(properties.getProperty("launchUpdateCutoff", 
				Integer.toString(launchUpdateCutoff)));
		bDebug = Boolean.parseBoolean(properties.getProperty("debug", Boolean.toString(bDebug)));
		Debug("Debugging messages are enabled.");
		getLogger().info("sortInventoryOnStart: " + sortInventoryOnStart);
		getLogger().info("autoSortInventory: " + autoSortInventory);
		getLogger().info("sortContainers: " + sortContainersOnOpen);
		getLogger().info("autoSortContainers: " + autoSortContainers);
		getLogger().info("autoSortSkills: " + autoSortSkills);
		getLogger().info("openInventoryOnStart: " + openInventoryOnStart);
		getLogger().info("sortOn: " + sortOn);
		getLogger().info("containerUpdateCutoff: " + containerUpdateCutoff);
		getLogger().info("launchUpdateCutoff: " + launchUpdateCutoff);
	}
	
	private static Logger getLogger() {
		if (logger == null) {
			@SuppressWarnings("rawtypes")
			Class thisClass = new Object() { }.getClass().getEnclosingClass();
			logger = Logger.getLogger(thisClass.getName());
		}
		return logger;
	}
	
	private static Logger getFileLogger() {
		if(!fileLogger) {
			@SuppressWarnings("rawtypes")
			Class thisClass = new Object() { }.getClass().getEnclosingClass();
			try {
				final String logsPath = Paths.get("mods") + "/logs/";
				final File newDirectory = new File(logsPath);
				if (!newDirectory.exists()) {
					newDirectory.mkdirs();
				}
				final FileHandler fh = new FileHandler(String.valueOf(logsPath) + 
						thisClass.getSimpleName() + ".log", 10240000, 200);
				if (bDebug) {
					fh.setLevel(Level.INFO);
				}
				else {
					fh.setLevel(Level.WARNING);
				}
				fh.setFormatter(new SimpleFormatter());
				getLogger().addHandler(fh);
				fileLogger = true;
			}
			catch (IOException ie) {
				System.err.println(thisClass.getName() + ": Unable to add file handler to logger");
				getLogger().log(Level.WARNING, thisClass.getName() + ": Unable to add file handler to logger");
			}
		}
		return getLogger();
	}

	private void Debug(String x) {
		if (bDebug) {
			System.out.println(this.getClass().getSimpleName() + ": " + x);
			System.out.flush();
			getFileLogger().log(Level.INFO, x);
		}
	}

	@Override
	public void init() {
		if(sortInventoryOnStart || autoSortInventory || openInventoryOnStart || autoSortSkills) {
			try {
				CtClass[] paramTypes = {
						HookManager.getInstance().getClassPool().get(
								"com.wurmonline.client.settings.Profile$PlayerProfile"),
						HookManager.getInstance().getClassPool().get(
								"com.wurmonline.client.resources.Resources"),
						CtPrimitiveType.booleanType
				};
				HookManager.getInstance().registerHook(
						"com.wurmonline.client.WurmClientBase", 
						"launch", 
						Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes),
						new InvocationHandlerFactory() {
							@Override
							public InvocationHandler createInvocationHandler() {
								return new InvocationHandler() {

									@Override
									public Object invoke(Object object, Method method, Object[] args) 
											throws Throwable {
										Debug("Launch called...");
										method.invoke(object, args);
										Debug("Launch finished.");
										try {
											WurmClientBase base = (WurmClientBase)ReflectionUtil.getPrivateField(
													null, 
													ReflectionUtil.getField(
															WurmClientBase.class,
															"clientObject"));
											Debug("Launch: got base");
											HeadsUpDisplay hud = (HeadsUpDisplay)ReflectionUtil.getPrivateField(
													base, ReflectionUtil.getField(WurmClientBase.class, "hud"));
											Debug("Launch: got hud");
											if(autoSortSkills) {
												setWurmTreeListAutoSort(getPrivateField(
														getPrivateField(hud, "skillWindow"),
														"skillList"));											}
											if(sortInventoryOnStart || autoSortInventory || openInventoryOnStart) {
												com.wurmonline.client.renderer.gui.InventoryWindow invWindow = 
														(com.wurmonline.client.renderer.gui.InventoryWindow)
														ReflectionUtil.getPrivateField(
																hud, 
																ReflectionUtil.getField(
																		HeadsUpDisplay.class, "inventoryWindow"));
												com.wurmonline.client.renderer.gui.InventoryListComponent component =
														(com.wurmonline.client.renderer.gui.InventoryListComponent)
														ReflectionUtil.getPrivateField(
																invWindow, 
																ReflectionUtil.getField(
																		InventoryWindow.class,
																		"component"));
												Debug("Launch: got InventoryListComponent");
												/* The Last-In-First-Out nature of the SortsPending processing
												 * means we need to add the PendingNodeOpen first so it runs after
												 * any PendingSorts we add further down in this method. */
												if(openInventoryOnStart) {
													maxCutOff = Math.max(maxCutOff, launchUpdateCutoff);
													getSortsPending().add(new PendingNodeOpen(
															component,
															"inventory"));
												}
												if (autoSortInventory) {
													setInventoryListComponentAutoSort(component);
												}
												else {
													sortInventoryListComponent(
															component, 
															"launch", 
															launchUpdateCutoff);
												}
											}
										}
										catch (Exception e) {
											logException(e);
										}
										return null;
									}
								};
							}
						});
				Debug("launch hooked");
			} catch (Exception e) {
				logException(e);
			}

		}
		if(sortContainersOnOpen || autoSortContainers) {
			try {
				Debug("Beginning sorting container window hooks...");
				CtClass[] paramTypes = {
						HookManager.getInstance().getClassPool().get(
								"com.wurmonline.client.game.inventory.InventoryMetaWindowView"),
				};
				HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", 
						"addInventoryWindow", Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes),
						new InvocationHandlerFactory() {

							@Override
							public InvocationHandler createInvocationHandler() {
								return new InvocationHandler() {@SuppressWarnings("unchecked")
								@Override
								public Object invoke(Object object, Method method, Object[] args) throws Throwable {
									method.invoke(object, args);
									Debug("Running addInventoryWindow hook...");
									HeadsUpDisplay hud = (HeadsUpDisplay)object;
									InventoryMetaWindowView newWindow = (InventoryMetaWindowView)args[0];
									Map<InventoryMetaWindowView, ItemListWindow> invWindowMap = 
											(Map<InventoryMetaWindowView, ItemListWindow>)
											ReflectionUtil.getPrivateField(
													hud, 
													ReflectionUtil.getField(HeadsUpDisplay.class, "inventoryWindows"));
									ItemListWindow itemListWindow = invWindowMap.get(newWindow);
									com.wurmonline.client.renderer.gui.InventoryListComponent component = 
											(com.wurmonline.client.renderer.gui.InventoryListComponent)
											ReflectionUtil.getPrivateField(
													itemListWindow, 
													ReflectionUtil.getField(ItemListWindow.class, "component"));
									if (autoSortContainers) {
										setInventoryListComponentAutoSort(component);
									} 
									else {
										sortInventoryListComponent(
												component, 
												"addInventoryWindow", 
												containerUpdateCutoff);
									}
									return null;
								}};
							}
						});
				Debug("addInventoryWindow hooked");
			}
			catch (Exception e) {
				logException(e);
			}

		}
		if (sortInventoryOnStart || sortContainersOnOpen ){
			try {
				HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.InventoryListComponent", 
						"addInventoryItem", 
						Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {
								HookManager.getInstance().getClassPool().get(
										"com.wurmonline.client.game.inventory.InventoryMetaItem")}),
						new InvocationHandlerFactory() {

					@Override
					public InvocationHandler createInvocationHandler() {
						return new InvocationHandler() {
							@Override
							public Object invoke(Object object, Method method, Object[] args) throws Throwable {
								itemsAddedInUpdate++;
								if (((com.wurmonline.client.game.inventory.InventoryMetaWindowView)
										ReflectionUtil.getPrivateField(
											object, 
											ReflectionUtil.getField(
												com.wurmonline.client.renderer.gui.InventoryListComponent.class,
												"inventoryWindow"))
									).getWindowId() == -1) {
									Debug("addInventoryItem called on inventory.");
								}
								else {
									Debug("addInventoryItem called on non-inventory.");
								}
								return method.invoke(object, args);
							}};
					}
				});
				HookManager.getInstance().registerHook("com.wurmonline.client.WurmClientBase", 
						"serverUpdate", Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] {}),
						new InvocationHandlerFactory() {

					@Override
					public InvocationHandler createInvocationHandler() {
						return new InvocationHandler() {
							@Override
							public Object invoke(Object object, Method method, Object[] args) throws Throwable {
//								numUpdates++;
//								Debug("Server update: " + numUpdates);
//								Debug("Items added since last update: " + itemsAddedInUpdate);
//								Debug("Number of updates before receiving the most recent item: " + 
//										updatesBeforeLastItemUpdate);
//								Debug("Longest duration before final item message while sort pending:" + 
//										maxWaitPending);
//								if (numUpdatesWithSortPending > 0) {
//									numUpdatesWithSortPending++;
//									Debug("Updates with the current sort pending: " + numUpdatesWithSortPending);
//								}
								int numPending = getSortsPending().size();
//								if (numPending > 0 && numUpdatesWithSortPending == 0) {
//									numUpdatesWithSortPending = 1;
//									Debug("Updates with the current sort pending: " + numUpdatesWithSortPending);
//								}
//								Debug("Sorts Pending: " + numPending);
								if(itemsAddedInUpdate == 0) {
									numUpdatesWithoutItemsAdded++;
									if(numPending >0 && numUpdatesWithoutItemsAdded >= maxCutOff) {
//										numUpdatesWithSortPending = 0;
										maxCutOff = 0;
										for(int i = sortsPending.size() - 1; i >= 0; i--) {
											Debug("Running Pending Sorts...");
											sortsPending.get(i).doSort();
											sortsPending.remove(i);
										}
									}
								} else {
									Debug("Received " + itemsAddedInUpdate + " items.");
//									maxWaitPending = Math.max(maxWaitPending, numUpdatesWithSortPending);
//									updatesBeforeLastItemUpdate = numUpdatesWithoutItemsAdded;
									numUpdatesWithoutItemsAdded = 0;
								}
//								Debug("Number of updates without items: " + numUpdatesWithoutItemsAdded);
								itemsAddedInUpdate = 0;
								return method.invoke(object, args);
							}};
					}
				});

			}
			catch (Exception e) {
				logException(e);
			}
		}
		if(autoSortContainers || autoSortInventory) {
			try {
				HookManager.getInstance().registerHook(
						"com.wurmonline.client.renderer.gui.InventoryListComponent", 
						"updateInventoryItem", 
						null,
						new InvocationHandlerFactory() {

					@Override
					public InvocationHandler createInvocationHandler() {
						return new InvocationHandler() {
							@SuppressWarnings("rawtypes")
							@Override
							public Object invoke(Object object, Method method, Object[] args) throws Throwable {
								if(bDebug) {
									Debug("updateInventoryItem called on " + getPrivateField(
											args[0], 
											"displayName"));
								}
								method.invoke(object, args);
								((WurmTreeList)getPrivateField(object, "itemList")).recalcLines();
								return null;
							}};
					}
				});
			}
			catch (Exception e) {
				logException(e);
			}
		}
	}


	private void sortInventoryListComponent(
			com.wurmonline.client.renderer.gui.InventoryListComponent component, String methodName, 
			int cutOff)
					throws IllegalAccessException, NoSuchFieldException,
					InvocationTargetException, NoSuchMethodException {
		Debug("sortInventoryListComponent called.");
		@SuppressWarnings("rawtypes")
		WurmTreeList itemList = 
				(WurmTreeList)ReflectionUtil.getPrivateField(
						component, 
						ReflectionUtil.getField(
								com.wurmonline.client.renderer.gui.InventoryListComponent.class,
								"itemList"));
		itemList.setKeepAutoSorted(true);
		try {
			ReflectionUtil.callPrivateMethod(
					itemList, 
					ReflectionUtil.getMethod(
							com.wurmonline.client.renderer.gui.WurmTreeList.class,
							"toggleAllNodes"),
					true);
			ReflectionUtil.callPrivateMethod(
					itemList, 
					ReflectionUtil.getMethod(
							com.wurmonline.client.renderer.gui.WurmTreeList.class,
							"setSortOn"),
					sortOn);
			ReflectionUtil.callPrivateMethod(
					itemList, 
					ReflectionUtil.getMethod(
							com.wurmonline.client.renderer.gui.WurmTreeList.class,
							"toggleAllNodes"),
					false);
		} catch (Exception e) {
			logException(e);
		}
		maxCutOff = Math.max(maxCutOff, cutOff);
		getSortsPending().add(new PendingSort(methodName, itemList));
	}
	
	private void setInventoryListComponentAutoSort(
			com.wurmonline.client.renderer.gui.InventoryListComponent component)
					throws IllegalAccessException, NoSuchFieldException,
					IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		@SuppressWarnings("rawtypes")
		WurmTreeList itemList = (WurmTreeList)ReflectionUtil.getPrivateField(
				component, 
				ReflectionUtil.getField(
						com.wurmonline.client.renderer.gui.InventoryListComponent.class,
						"itemList"));
		setWurmTreeListAutoSort(itemList);
	}
	
	private void setWurmTreeListAutoSort(WurmTreeList itemList)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		itemList.setKeepAutoSorted(true);
		ReflectionUtil.callPrivateMethod(
				itemList, 
				ReflectionUtil.getMethod(
						com.wurmonline.client.renderer.gui.WurmTreeList.class,
						"setSortOn"),
				sortOn);
	}
	
	private class PendingSort {
		String methodName;
		@SuppressWarnings("rawtypes")
		WurmTreeList itemList;
		
		@SuppressWarnings("rawtypes")
		public PendingSort(String sMethod, WurmTreeList list) {
			this.methodName = sMethod;
			this.itemList = list;
		}
		
		private PendingSort(){
			
		}
		
		public void doSort () {
			Debug(methodName + ": sorting....");
			try {
				ReflectionUtil.setPrivateField(
						itemList,
						ReflectionUtil.getField(
								com.wurmonline.client.renderer.gui.WurmTreeList.class,
								"lastSortOn"),
						-1);
				ReflectionUtil.callPrivateMethod(
						itemList, 
						ReflectionUtil.getMethod(
								com.wurmonline.client.renderer.gui.WurmTreeList.class,
								"toggleAllNodes"),
						true);
				ReflectionUtil.callPrivateMethod(
						itemList, 
						ReflectionUtil.getMethod(
								com.wurmonline.client.renderer.gui.WurmTreeList.class,
								"setSortOn"),
						sortOn);
				ReflectionUtil.callPrivateMethod(
						itemList, 
						ReflectionUtil.getMethod(
								com.wurmonline.client.renderer.gui.WurmTreeList.class,
								"toggleAllNodes"),
						false);
				itemList.setKeepAutoSorted(false);
				ReflectionUtil.setPrivateField(
						itemList,
						ReflectionUtil.getField(
								com.wurmonline.client.renderer.gui.WurmTreeList.class,
								"lastSortOn"),
						-1);
			} catch (Exception e) {
				// Not the end of the world, so log it and move on.
				logNonCriticalException(e);
			}
		}
	}
	
	private class PendingNodeOpen extends PendingSort {
		com.wurmonline.client.renderer.gui.InventoryListComponent component;
		public PendingNodeOpen(com.wurmonline.client.renderer.gui.InventoryListComponent newComponent
				, String nodeName) {
			component = newComponent;
			methodName = nodeName;
		}
		
		@SuppressWarnings("rawtypes")
		public void doSort () {
			com.wurmonline.client.game.inventory.InventoryMetaItem item = 
					component.getItemWithName(methodName, 0);
			try {
				Object treeItem = ((Map)getPrivateField(component, "inventoryTreeListItems")).get(item.getId());
				itemList = 
						(WurmTreeList)ReflectionUtil.getPrivateField(
								component, 
								ReflectionUtil.getField(
										com.wurmonline.client.renderer.gui.InventoryListComponent.class,
										"itemList"));
				callPrivateMethod(callPrivateMethod(itemList, "getNode", treeItem), "setOpen", true);
				boolean isOpen = callPrivateMethod(callPrivateMethod(itemList, "getNode", treeItem), "isOpen");
				Debug("Node " + item.getDisplayName() + "'s open state is: " + isOpen);
				itemList.recalcLines();
			} catch (Exception e) {
				// Not the end of the world, so log it and move on.
				logNonCriticalException(e);
			}
		}
	}

	public static <T> T getPrivateField(Object object, String fieldName) 
			throws IllegalArgumentException, IllegalAccessException, ClassCastException, NoSuchFieldException {
		return ReflectionUtil.getPrivateField(object, ReflectionUtil.getField(object.getClass(), fieldName));
	}
	
	public static <T> T callPrivateMethod(Object object, String methodName, Object... args) 
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		return ReflectionUtil.callPrivateMethod(object, ReflectionUtil.getMethod(object.getClass(), methodName), args);
	}
	
	private static void logException (Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		getFileLogger().log(Level.SEVERE, sw.toString(), e);
		e.printStackTrace();
		throw new HookException(e);
	}

	private static void logNonCriticalException (Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		getFileLogger().log(Level.WARNING, sw.toString(), e);
		e.printStackTrace();
	}
	
	


}
