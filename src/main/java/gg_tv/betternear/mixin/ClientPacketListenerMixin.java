package gg_tv.betternear.mixin;

import gg_tv.betternear.BetterNearClient;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Клик по нику в чате (ClickEvent.RunCommand) шлёт команду как «unattended»
 * (ClientPacketListener#sendUnattendedCommand), а fabric-перехват клиентских команд
 * обрабатывает только обычные sendCommand. Ловим свои /bn … здесь и выполняем локально.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    private void betternear$unattended(String command, Screen screenAfterCommand, CallbackInfo ci) {
        if (BetterNearClient.handleUnattendedCommand(command)) {
            ci.cancel();
        }
    }
}
