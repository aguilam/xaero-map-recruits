package com.aguilam.xaerorecruits.mixin;

import com.aguilam.xaerorecruits.claims.MapClaimActions;
import com.aguilam.xaerorecruits.claims.MapClaimPanel;
import com.talhanation.recruits.world.RecruitsClaim;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin {
    @Shadow
    private int rightClickX;

    @Shadow
    private int rightClickZ;

    @Shadow
    private ResourceKey<Level> rightClickDim;

    @Shadow
    private int mouseBlockPosX;

    @Shadow
    private int mouseBlockPosZ;

    @Shadow
    private ResourceKey<Level> mouseBlockDim;

    @Shadow
    private GuiRightClickMenu rightClickMenu;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void xaerorecruits$addClaimOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> callbackInfo) {
        ArrayList<RightClickOption> options = callbackInfo.getReturnValue();
        if (options == null) {
            return;
        }
        MapClaimPanel.close();
        GuiMap guiMap = (GuiMap) (Object) this;
        MapClaimActions.addOptions(options, guiMap, this.rightClickX, this.rightClickZ, this.rightClickDim);
    }

    @Inject(method = "m_88315_", at = @At("RETURN"))
    private void xaerorecruits$renderClaimPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                                CallbackInfo callbackInfo) {
        MapClaimPanel.render((GuiMap) (Object) this, graphics);
    }

    @Inject(method = "m_6375_", at = @At("HEAD"))
    private void xaerorecruits$openClaimPanel(double mouseX, double mouseY, int button,
                                              CallbackInfoReturnable<Boolean> callbackInfo) {
        if (button != 0 || this.rightClickMenu != null || !Level.OVERWORLD.equals(this.mouseBlockDim)) {
            return;
        }
        RecruitsClaim claim = MapClaimActions.findClaimAt(
                new ChunkPos(this.mouseBlockPosX >> 4, this.mouseBlockPosZ >> 4));
        if (claim == null) {
            MapClaimPanel.close();
            return;
        }
        MapClaimPanel.open((GuiMap) (Object) this, claim, this.mouseBlockPosX, this.mouseBlockPosZ,
                (int) mouseX, (int) mouseY);
    }

    @Redirect(method = "handleRightClick", at = @At(
            value = "INVOKE",
            target = "Lxaero/map/gui/dropdown/rightclick/GuiRightClickMenu;getMenu(Lxaero/map/gui/IRightClickableElement;Lxaero/map/gui/GuiMap;III)Lxaero/map/gui/dropdown/rightclick/GuiRightClickMenu;"
    ), remap = false)
    private GuiRightClickMenu xaerorecruits$widenForPrice(IRightClickableElement target, GuiMap screen,
                                                          int x, int y, int width) {
        int required = 0;
        ArrayList<RightClickOption> options = target.getRightClickOptions();
        if (options == null) {
            return GuiRightClickMenu.getMenu(target, screen, x, y, width);
        }
        for (RightClickOption option : options) {
            if (MapClaimActions.hasPrice(option)) {
                required = Math.max(required, Minecraft.getInstance().font.width(option.getDisplayName()));
            }
        }
        return GuiRightClickMenu.getMenu(target, screen, x, y, Math.max(width, required + 4));
    }
}
