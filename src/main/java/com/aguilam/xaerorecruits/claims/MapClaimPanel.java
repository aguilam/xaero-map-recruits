package com.aguilam.xaerorecruits.claims;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.client.gui.worldmap.claim.ClaimInfoMenu;
import com.talhanation.recruits.world.RecruitsClaim;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;
import xaero.map.gui.GuiMap;

public final class MapClaimPanel {
    private static final float PANEL_DEPTH = 1000.0F;

    private static ClaimInfoMenu menu;
    private static GuiMap owner;
    private static List<RecruitsClaim> claims;
    private static ChunkPos chunk;

    private MapClaimPanel() {
    }

    public static void open(GuiMap map, RecruitsClaim claim, int blockX, int blockZ, int mouseX, int mouseY) {
        if (menu == null) {
            Minecraft minecraft = Minecraft.getInstance();
            WorldMapScreen parent = new WorldMapScreen() {
                @Override
                protected void init() {
                }
            };
            parent.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            menu = new ClaimInfoMenu(parent);
            menu.init();
        }
        owner = map;
        claims = ClientManager.recruitsClaims;
        chunk = new ChunkPos(blockX >> 4, blockZ >> 4);
        menu.openForClaim(claim, mouseX + 12, mouseY + 12);
    }

    public static void render(GuiMap map, GuiGraphics graphics) {
        if (menu == null || owner != map) {
            return;
        }
        if (ClientManager.recruitsClaims != claims) {
            claims = ClientManager.recruitsClaims;
            RecruitsClaim claim = claims == null ? null : MapClaimActions.findClaimAt(chunk);
            if (claim == null) {
                close();
                return;
            }
            menu.setClaim(claim);
        }
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, PANEL_DEPTH);
        menu.render(graphics);
        graphics.pose().popPose();
        graphics.flush();
    }

    public static void close() {
        if (menu != null) {
            menu.close();
        }
        owner = null;
        claims = null;
    }
}
