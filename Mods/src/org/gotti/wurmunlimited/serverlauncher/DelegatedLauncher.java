package org.gotti.wurmunlimited.serverlauncher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.ServerHook;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import com.wurmonline.server.gui.WurmServerGuiMainDeferred;

public class DelegatedLauncher {
	
	public static void main(String[] args) {
		
		try {
			List<WurmMod> wurmMods = new ModLoader().loadModsFromModDir(Paths.get("mods"));
			ServerHook.createServerHook().addMods(wurmMods);
			WurmServerGuiMainDeferred.main(args);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
