package gg_tv.betternear.inspect;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** Снимок всего, что клиент реально знает о другом игроке. */
public final class PlayerSnapshot {

    public static final int HELMET = 0;
    public static final int CHESTPLATE = 1;
    public static final int LEGGINGS = 2;
    public static final int BOOTS = 3;

    public final UUID uuid;
    public final String name;
    public final int ping;
    public final double distance;
    public final boolean online;
    public final List<ItemStack> armor;
    public final ItemStack mainHand;
    public final ItemStack offHand;

    public PlayerSnapshot(UUID uuid, String name, int ping, double distance, boolean online,
                          List<ItemStack> armor, ItemStack mainHand, ItemStack offHand) {
        this.uuid = uuid;
        this.name = name;
        this.ping = ping;
        this.distance = distance;
        this.online = online;
        this.armor = armor;
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

    public PlayerSnapshot offline() {
        return new PlayerSnapshot(uuid, name, ping, distance, false, armor, mainHand, offHand);
    }
}
