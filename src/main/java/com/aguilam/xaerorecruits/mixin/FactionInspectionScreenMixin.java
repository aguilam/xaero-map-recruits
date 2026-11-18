package com.aguilam.xaerorecruits.mixin;

import com.aguilam.xaerorecruits.claims.RecruitsMapReplacer;
import com.talhanation.recruits.client.gui.faction.FactionInspectionScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactionInspectionScreen.class, remap = false)
public abstract class FactionInspectionScreenMixin {
    @Inject(method = "lambda$postInit$2", at = @At("HEAD"), cancellable = true)
    private void xaerorecruits$replaceClaimMap(Button button, CallbackInfo callbackInfo) {
        if (RecruitsMapReplacer.openXaeroMap()) {
            callbackInfo.cancel();
        }
    }
}
