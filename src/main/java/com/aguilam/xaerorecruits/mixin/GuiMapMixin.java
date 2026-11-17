package com.aguilam.xaerorecruits.mixin;

import com.aguilam.xaerorecruits.claims.MapClaimActions;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void xaerorecruits$addClaimOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> callbackInfo) {
        ArrayList<RightClickOption> options = callbackInfo.getReturnValue();
        if (options == null) {
            return;
        }
        GuiMap guiMap = (GuiMap) (Object) this;
        MapClaimActions.addOptions(options, guiMap, this.rightClickX, this.rightClickZ, this.rightClickDim);
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
