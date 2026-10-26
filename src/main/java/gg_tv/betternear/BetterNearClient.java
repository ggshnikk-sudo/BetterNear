package gg_tv.betternear;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import gg_tv.betternear.config.BetterNearConfig;
import gg_tv.betternear.config.ConfigManager;
import gg_tv.betternear.gui.InventoryPeekScreen;
import gg_tv.betternear.inspect.PlayerInspect;
import gg_tv.betternear.inspect.PlayerSnapshot;
import gg_tv.betternear.util.Fmt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BetterNear — клиентский мод для MC 26.2.
 *
 * /bn — РАЗОВАЯ подсветка всех игроков в радиусе (vanilla glow-контур, виден сквозь
 * стены) прямо сейчас + список в чат. Никакой «подписки»/постоянного режима:
 * подсветка живёт highlightSeconds секунд и гаснет сама.
 * /bn info — только список игроков в радиусе, без подсветки.
 * /bn inv — разведка игрока (пинг, броня, руки, примерный инвентарь).
 */
public final class BetterNearClient implements ClientModInitializer {

    public static final String MOD_ID = "betternear";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static BetterNearConfig CONFIG = new BetterNearConfig();

    /** Entity id, которым сейчас рисуется glow-контур (читается из EntityMixin). */
    private static final Set<Integer> glowing = new HashSet<>();
    private static long pulseUntilMillis;
    private static double radiusOverride = -1.0;
    private static ClientLevel currentLevel;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** true — этому entity сейчас нужно рисовать glow-контур (см. EntityMixin). */
    public static boolean isMarkedGlowing(int entityId) {
        return !glowing.isEmpty() && glowing.contains(entityId);
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommands.literal("bn")
                    .requires(FabricClientCommandSource::attended)
                    .executes(ctx -> highlightNow(ctx.getSource()))
                    .then(ClientCommands.literal("info")
                            .executes(ctx -> info(ctx.getSource())))
                    .then(ClientCommands.literal("clear")
                            .executes(ctx -> clearPulse(ctx.getSource())))
                    .then(ClientCommands.literal("radius")
                            .executes(ctx -> reportRadius(ctx.getSource()))
                            .then(ClientCommands.argument("blocks", DoubleArgumentType.doubleArg(1.0, 5000.0))
                                    .executes(ctx -> setRadius(ctx.getSource(),
                                            DoubleArgumentType.getDouble(ctx, "blocks")))))
                    .then(ClientCommands.literal("inv")
                            .executes(ctx -> openInspect(ctx.getSource(), null))
                            .then(ClientCommands.argument("player", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        Minecraft mc = Minecraft.getInstance();
                                        if (mc.level != null) {
                                            for (AbstractClientPlayer p : mc.level.players()) {
                                                builder.suggest(p.getName().getString());
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> openInspect(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "player")))))
                    .then(ClientCommands.literal("reload")
                            .executes(ctx -> reload(ctx.getSource())))
                    .then(ClientCommands.literal("status")
                            .executes(ctx -> status(ctx.getSource()))));
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level != currentLevel) {
            currentLevel = level;
            glowing.clear();
            pulseUntilMillis = 0;
            return;
        }
        LocalPlayer self = mc.player;
        if (level == null || self == null || pulseUntilMillis == 0) {
            return;
        }
        if (System.currentTimeMillis() >= pulseUntilMillis) {
            pulseUntilMillis = 0;
            glowing.clear();
            return;
        }
        glowing.clear();
        for (AbstractClientPlayer p : collectNearby(mc)) {
            glowing.add(p.getId());
        }
    }

    private static List<AbstractClientPlayer> collectNearby(Minecraft mc) {
        List<AbstractClientPlayer> out = new ArrayList<>();
        ClientLevel level = mc.level;
        LocalPlayer self = mc.player;
        if (level == null || self == null) {
            return out;
        }
        double radius = currentRadius();
        for (AbstractClientPlayer p : level.players()) {
            if (p == self) {
                continue;
            }
            double distSq = self.distanceToSqr(p);
            if (NearLogic.shouldHighlight(distSq, radius,
                    p.hasEffect(MobEffects.INVISIBILITY), p.isSpectator(), CONFIG)) {
                out.add(p);
            }
        }
        return out;
    }

    private int highlightNow(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            src.sendFeedback(msg(colored("Ты не в мире.", Fmt.RED)));
            return 0;
        }
        List<AbstractClientPlayer> nearby = collectNearby(mc);
        pulseUntilMillis = System.currentTimeMillis() + (long) (CONFIG.highlightSeconds * 1000.0);
        glowing.clear();
        for (AbstractClientPlayer p : nearby) {
            glowing.add(p.getId());
        }
        src.sendFeedback(msg(
                colored("Подсветил ", Fmt.GRAY),
                colored(String.valueOf(nearby.size()), Fmt.WHITE),
                colored(" игроков на ", Fmt.GRAY),
                colored(Fmt.distance(CONFIG.highlightSeconds) + " сек", Fmt.GREEN),
                colored(" (радиус " + Fmt.distance(currentRadius()) + ").", Fmt.GRAY)));
        printPlayers(src, nearby);
        return 1;
    }

    private int info(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            src.sendFeedback(msg(colored("Ты не в мире.", Fmt.RED)));
            return 0;
        }
        List<AbstractClientPlayer> nearby = collectNearby(mc);
        src.sendFeedback(msg(
                colored("Игроки в радиусе ", Fmt.GRAY),
                colored(Fmt.distance(currentRadius()), Fmt.GREEN),
                colored(": ", Fmt.GRAY),
                colored(String.valueOf(nearby.size()), Fmt.WHITE)));
        printPlayers(src, nearby);
        return 1;
    }

    private void printPlayers(FabricClientCommandSource src, List<AbstractClientPlayer> players) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || players.isEmpty()) {
            src.sendFeedback(msg(colored("  никого рядом.", Fmt.GRAY)));
            return;
        }
        for (AbstractClientPlayer p : players) {
            double dist = Math.sqrt(mc.player.distanceToSqr(p));
            int ping = PlayerInspect.ping(mc, p.getUUID());
            MutableComponent name = Component.literal(p.getName().getString())
                    .withColor(Fmt.WHITE)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent.RunCommand("/bn inv " + p.getName().getString()))
                            .withHoverEvent(new HoverEvent.ShowText(PlayerInspect.hoverComponent(mc, p))));
            src.sendFeedback(line(
                    colored("  • ", Fmt.GRAY),
                    name,
                    colored(" — " + Fmt.distance(dist) + " m ", Fmt.GRAY),
                    colored("[" + Fmt.ping(ping) + "]", Fmt.pingColor(ping))));
        }
        src.sendFeedback(msg(colored("ЛКМ по нику — пинг, броня, руки и примерный инвентарь.", Fmt.GRAY)));
    }

    private int clearPulse(FabricClientCommandSource src) {
        pulseUntilMillis = 0;
        glowing.clear();
        src.sendFeedback(msg(colored("Подсветка снята.", Fmt.GRAY)));
        return 1;
    }

    private int openInspect(FabricClientCommandSource src, String name) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer target = name == null
                ? PlayerInspect.nearest(mc, currentRadius())
                : PlayerInspect.byName(mc, name);
        if (target == null) {
            src.sendFeedback(msg(colored(name == null
                    ? "Рядом никого нет."
                    : "Игрок не найден: " + name, Fmt.RED)));
            return 0;
        }
        PlayerSnapshot snapshot = PlayerInspect.snapshot(mc, target);
        showPeek(mc, target);
        src.sendFeedback(msg(
                colored("Открываю: ", Fmt.GRAY),
                colored(snapshot.name, Fmt.WHITE),
                colored("  пинг ", Fmt.GRAY),
                colored(Fmt.ping(snapshot.ping), Fmt.pingColor(snapshot.ping))));
        return 1;
    }

    private static void showPeek(Minecraft mc, AbstractClientPlayer target) {
        PlayerSnapshot snapshot = PlayerInspect.snapshot(mc, target);
        mc.execute(() -> mc.setScreenAndShow(new InventoryPeekScreen(snapshot)));
    }

    /**
     * Обработка команд, пришедших из клика по нику в чате (unattended).
     * true — команда наша и уже выполнена, пакет на сервер отправлять не нужно.
     */
    public static boolean handleUnattendedCommand(String rawCommand) {
        if (rawCommand == null) {
            return false;
        }
        String cmd = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        if (!cmd.equals("bn inv") && !cmd.startsWith("bn inv ")) {
            return false;
        }
        String name = cmd.length() > 6 ? cmd.substring(6).trim() : "";
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer target = name.isEmpty()
                ? PlayerInspect.nearest(mc, currentRadius())
                : PlayerInspect.byName(mc, name);
        if (target != null) {
            showPeek(mc, target);
        }
        return true;
    }

    private int setRadius(FabricClientCommandSource src, double r) {
        radiusOverride = r;
        src.sendFeedback(msg(
                colored("Радиус подсветки: ", Fmt.GRAY),
                colored(Fmt.distance(r), Fmt.GREEN),
                colored(" блоков.", Fmt.GRAY)));
        return 1;
    }

    private int reportRadius(FabricClientCommandSource src) {
        src.sendFeedback(msg(
                colored("Текущий радиус: ", Fmt.GRAY),
                colored(Fmt.distance(currentRadius()), Fmt.GREEN),
                colored(" блоков (конфиг: " + Fmt.distance(CONFIG.radius) + ").", Fmt.GRAY)));
        return 1;
    }

    private int reload(FabricClientCommandSource src) {
        ConfigManager.load();
        radiusOverride = -1.0;
        pulseUntilMillis = 0;
        glowing.clear();
        src.sendFeedback(msg(
                colored("Конфиг перезагружен. Радиус: ", Fmt.GRAY),
                colored(Fmt.distance(CONFIG.radius), Fmt.GREEN),
                colored(" блоков, подсветка: ", Fmt.GRAY),
                colored(Fmt.distance(CONFIG.highlightSeconds) + " сек", Fmt.GREEN),
                colored(".", Fmt.GRAY)));
        return 1;
    }

    private int status(FabricClientCommandSource src) {
        long leftMillis = pulseUntilMillis - System.currentTimeMillis();
        boolean active = leftMillis > 0;
        src.sendFeedback(msg(
                colored("Радиус: ", Fmt.GRAY),
                colored(Fmt.distance(currentRadius()), Fmt.GREEN),
                colored(" блоков, подсветка: ", Fmt.GRAY),
                colored(active ? "активна (" + ((leftMillis + 999) / 1000) + " сек)" : "выключена",
                        active ? Fmt.GREEN : Fmt.RED),
                colored(". Длительность: " + Fmt.distance(CONFIG.highlightSeconds) + " сек.", Fmt.GRAY)));
        return 1;
    }

    private static MutableComponent prefix() {
        return Component.literal("[BN] ").withColor(Fmt.GREEN);
    }

    private static Component colored(String s, int rgb) {
        return Component.literal(s).withColor(rgb);
    }

    private static MutableComponent msg(Component... parts) {
        MutableComponent m = prefix();
        for (Component p : parts) {
            m.append(p);
        }
        return m;
    }

    private static MutableComponent line(Component... parts) {
        MutableComponent m = Component.empty();
        for (Component p : parts) {
            m.append(p);
        }
        return m;
    }

    private static double currentRadius() {
        return radiusOverride > 0 ? radiusOverride : CONFIG.radius;
    }
}
