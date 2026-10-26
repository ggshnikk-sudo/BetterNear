package gg_tv.betternear.gui;

import gg_tv.betternear.inspect.PlayerInspect;
import gg_tv.betternear.inspect.PlayerSnapshot;
import gg_tv.betternear.util.Fmt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * «Примерный инвентарь» другого игрока. Реально показываются только броня и руки
 * (экипировка приходит клиенту для рендера). Остальные слоты помечены как недоступные:
 * сервер не отправляет клиенту содержимое чужого инвентаря.
 */
public final class InventoryPeekScreen extends Screen {

    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int SLOT = 18;
    private static final int STEP = 20;
    private static final int PANEL_W = 8 * STEP + SLOT + 16;
    private static final int PANEL_H = 182;

    private final UUID target;
    private PlayerSnapshot snap;

    public InventoryPeekScreen(PlayerSnapshot snap) {
        super(Component.literal("Инвентарь: " + snap.name));
        this.snap = snap;
        this.target = snap.uuid;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft mc = this.minecraft;
        if (mc.level != null) {
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p.getUUID().equals(target)) {
                    snap = PlayerInspect.snapshot(mc, p);
                    return;
                }
            }
        }
        snap = snap.offline();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int panelX = (this.width - PANEL_W) / 2;
        int panelY = (this.height - PANEL_H) / 2;
        int armorX = panelX + 8;
        int armorY = panelY + 44;
        int handsX = armorX + 4 * STEP + 10;
        int gridX = panelX + 8;
        int gridY = panelY + 86;
        int hotbarY = gridY + 3 * STEP + 6;

        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_W + 2, panelY + PANEL_H + 2, 0xFF101010);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF0000000);

        graphics.centeredText(this.font, Component.literal(snap.name).withColor(Fmt.WHITE),
                this.width / 2, panelY + 8, Fmt.WHITE);
        graphics.centeredText(this.font, header(), this.width / 2, panelY + 20, Fmt.WHITE);

        graphics.text(this.font, Component.literal("Броня и руки — реальные данные").withColor(Fmt.GRAY),
                armorX, armorY - 12, Fmt.GRAY);
        for (int i = 0; i < 4; i++) {
            drawKnown(graphics, snap.armor.get(i), armorX + i * STEP, armorY, mouseX, mouseY);
        }
        drawKnown(graphics, snap.mainHand, handsX, armorY, mouseX, mouseY);
        drawKnown(graphics, snap.offHand, handsX + STEP, armorY, mouseX, mouseY);

        graphics.text(this.font, Component.literal("Остальной инвентарь недоступен клиенту").withColor(Fmt.GRAY),
                gridX, gridY - 12, Fmt.GRAY);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawUnknown(graphics, gridX + col * STEP, gridY + row * STEP, mouseX, mouseY);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawUnknown(graphics, gridX + col * STEP, hotbarY, mouseX, mouseY);
        }
    }

    private MutableComponent header() {
        MutableComponent sub = Component.literal("Пинг: ").withColor(Fmt.GRAY)
                .append(Component.literal(Fmt.ping(snap.ping)).withColor(Fmt.pingColor(snap.ping)));
        if (snap.distance >= 0) {
            sub.append(Component.literal("   Дистанция: ").withColor(Fmt.GRAY))
                    .append(Component.literal(Fmt.distance(snap.distance) + " m").withColor(Fmt.WHITE));
        }
        if (!snap.online) {
            sub.append(Component.literal("   [вне мира]").withColor(Fmt.RED));
        }
        return sub;
    }

    private void drawKnown(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT, SLOT);
        if (stack != null && !stack.isEmpty()) {
            graphics.item(stack, x + 1, y + 1);
            graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        }
        if (hovered(x, y, mouseX, mouseY)) {
            showItemTooltip(graphics, stack, mouseX, mouseY);
        }
    }

    private void drawUnknown(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT, SLOT);
        graphics.centeredText(this.font, Component.literal("?").withColor(Fmt.DARK_GRAY),
                x + SLOT / 2, y + 5, Fmt.DARK_GRAY);
        if (hovered(x, y, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("Слот недоступен клиенту").withColor(Fmt.GRAY),
                    Component.literal("сервер не отправляет содержимое").withColor(Fmt.DARK_GRAY),
                    Component.literal("чужого инвентаря").withColor(Fmt.DARK_GRAY)), mouseX, mouseY);
        }
    }

    private void showItemTooltip(GuiGraphicsExtractor graphics, ItemStack stack, int mouseX, int mouseY) {
        if (stack == null || stack.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("Пусто").withColor(Fmt.GRAY)), mouseX, mouseY);
            return;
        }
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, stack));
        lines.addAll(PlayerInspect.dumpComponents(stack, 12));
        graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }

    private static boolean hovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }
}
