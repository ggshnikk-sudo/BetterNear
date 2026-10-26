package gg_tv.betternear.mixin;

import gg_tv.betternear.BetterNearClient;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Подсветка игроков.
 *
 * Vanilla-контур (glow outline) рисуется тогда и только тогда, когда
 * Minecraft#shouldEntityAppearGlowing -> Entity#isCurrentlyGlowing() вернул true.
 * На клиенте isCurrentlyGlowing() читает общий флаг из SynchedEntityData, который
 * сервер регулярно перезаписывает своими пакетами (бег/присед/огонь), из-за чего
 * выставление флага каждый тик давало мерцание/пропадание контура.
 *
 * Поэтому контур включается НАПРЯМУЮ: для entity из нашего набора
 * isCurrentlyGlowing() принудительно возвращает true. Работает только на клиенте.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void betternear$forceGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() && BetterNearClient.isMarkedGlowing(self.getId())) {
            cir.setReturnValue(true);
        }
    }
}
