package gg_tv.betternear.inspect;

import gg_tv.betternear.util.Fmt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Всё, что клиент может вытащить о другом игроке: пинг, дистанция, броня и руки
 * (полные ItemStack с NBT-компонентами). Содержимое остального инвентаря сервер
 * клиентам не отправляет — его прочитать нельзя.
 */
public final class PlayerInspect {

    private static final String[] ARMOR_LABELS = {"Шлем", "Нагрудник", "Штаны", "Ботинки"};

    private PlayerInspect() {
    }

    public static AbstractClientPlayer byName(Minecraft mc, String name) {
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }
        for (AbstractClientPlayer p : level.players()) {
            if (p.getName().getString().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public static AbstractClientPlayer nearest(Minecraft mc, double radius) {
        ClientLevel level = mc.level;
        LocalPlayer self = mc.player;
        if (level == null || self == null) {
            return null;
        }
        AbstractClientPlayer best = null;
        double bestSq = radius * radius;
        for (AbstractClientPlayer p : level.players()) {
            if (p == self) {
                continue;
            }
            double distSq = self.distanceToSqr(p);
            if (distSq <= bestSq) {
                bestSq = distSq;
                best = p;
            }
        }
        return best;
    }

    /** Пинг из tab-list (latency, мс). -1 — неизвестно. */
    public static int ping(Minecraft mc, UUID uuid) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(uuid);
        return info == null ? -1 : info.getLatency();
    }

    public static PlayerSnapshot snapshot(Minecraft mc, AbstractClientPlayer p) {
        LocalPlayer self = mc.player;
        double distance = self == null ? -1.0 : Math.sqrt(self.distanceToSqr(p));
        List<ItemStack> armor = List.of(
                p.getItemBySlot(EquipmentSlot.HEAD),
                p.getItemBySlot(EquipmentSlot.CHEST),
                p.getItemBySlot(EquipmentSlot.LEGS),
                p.getItemBySlot(EquipmentSlot.FEET));
        return new PlayerSnapshot(p.getUUID(), p.getName().getString(), ping(mc, p.getUUID()),
                distance, p.isAlive(), armor, p.getMainHandItem(), p.getOffhandItem());
    }

    /** Развёрнутый дамп NBT-компонентов стака (для тултипа в GUI). */
    public static List<Component> dumpComponents(ItemStack stack, int max) {
        List<Component> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return out;
        }
        out.add(Component.literal("NBT / компоненты:").withColor(Fmt.GRAY));
        int shown = 0;
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (shown++ >= max) {
                out.add(Component.literal("  …").withColor(Fmt.DARK_GRAY));
                break;
            }
            Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            String value = String.valueOf(component.value());
            if (value.length() > 200) {
                value = value.substring(0, 200) + "…";
            }
            out.add(Component.literal("  " + (key == null ? "?" : key.toString()) + " = " + value)
                    .withColor(Fmt.WHITE));
        }
        return out;
    }

    /** Многострочный тултип для наведения на ник в чате. */
    public static Component hoverComponent(Minecraft mc, AbstractClientPlayer p) {
        PlayerSnapshot s = snapshot(mc, p);
        MutableComponent t = Component.literal(s.name).withColor(Fmt.WHITE);
        t.append("\n");
        t.append(Component.literal("Пинг: ").withColor(Fmt.GRAY))
                .append(Component.literal(Fmt.ping(s.ping)).withColor(Fmt.pingColor(s.ping)));
        if (s.distance >= 0) {
            t.append("\n");
            t.append(Component.literal("Дистанция: ").withColor(Fmt.GRAY))
                    .append(Component.literal(Fmt.distance(s.distance) + " m").withColor(Fmt.WHITE));
        }
        for (int i = 0; i < 4; i++) {
            t.append("\n");
            t.append(gearLine(ARMOR_LABELS[i], s.armor.get(i)));
        }
        t.append("\n");
        t.append(gearLine("В руке", s.mainHand));
        t.append("\n");
        t.append(gearLine("Офф-хенд", s.offHand));
        t.append("\n");
        t.append(Component.literal("ЛКМ — открыть примерный инвентарь").withColor(Fmt.YELLOW));
        return t;
    }

    /** "Шлем: Diamond Helmet  Protection IV". */
    public static MutableComponent gearLine(String label, ItemStack stack) {
        MutableComponent m = Component.literal(label + ": ").withColor(Fmt.GRAY);
        if (stack == null || stack.isEmpty()) {
            return m.append(Component.literal("—").withColor(Fmt.DARK_GRAY));
        }
        m.append(stack.getHoverName().copy());
        ItemEnchantments enchantments = stack.getEnchantments();
        if (!enchantments.isEmpty()) {
            for (var entry : enchantments.entrySet()) {
                m.append(" ");
                m.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()));
            }
        }
        return m;
    }
}
