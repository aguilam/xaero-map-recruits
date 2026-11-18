package com.aguilam.xaerorecruits.claims;

import com.aguilam.xaerorecruits.XaeroRecruitsMod;
import com.aguilam.xaerorecruits.config.XRConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

public final class RecruitsMapReplacer {
    private RecruitsMapReplacer() {
    }

    public static boolean openXaeroMap() {
        if (!XRConfig.CLIENT.replaceRecruitsMap.get()) {
            return false;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.level == null
                    || !Level.OVERWORLD.equals(minecraft.level.dimension())) {
                return false;
            }
            WorldMapSession session = WorldMapSession.getForPlayer(player);
            MapProcessor processor = session == null ? null : session.getMapProcessor();
            if (processor == null) {
                return false;
            }
            minecraft.setScreen(new GuiMap(null, null, processor, minecraft.getCameraEntity()));
            return true;
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not open Xaero's world map instead of the Recruits map", throwable);
            return false;
        }
    }
}
