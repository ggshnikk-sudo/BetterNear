package gg_tv.betternear.compat;

import gg_tv.betternear.BetterNearClient;
import gg_tv.betternear.config.BetterNearConfig;
import gg_tv.betternear.config.ConfigManager;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран настроек на Cloth Config. Класс грузится ТОЛЬКО если cloth-config установлен
 * (проверка в {@link ModMenuIntegration#getModConfigScreenFactory()}), поэтому отсутствие
 * Cloth не роняет мод.
 */
final class ClothConfigScreen {

    private ClothConfigScreen() {
    }

    static Screen create(Screen parent) {
        BetterNearConfig cfg = BetterNearClient.CONFIG;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("BetterNear"))
                .setSavingRunnable(ConfigManager::save);
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory main = builder.getOrCreateCategory(Component.literal("Основное"));

        main.addEntry(entries.startIntSlider(
                        Component.literal("Радиус подсветки, блоки"),
                        (int) Math.round(cfg.radius), 1, 5000)
                .setDefaultValue(150)
                .setTooltip(Component.literal("Игроки ближе этого расстояния подсвечиваются и попадают в список."))
                .setSaveConsumer(v -> cfg.radius = v)
                .build());

        main.addEntry(entries.startIntSlider(
                        Component.literal("Длительность подсветки, сек"),
                        (int) Math.round(cfg.highlightSeconds), 1, 120)
                .setDefaultValue(5)
                .setTooltip(Component.literal("Сколько держится контур после команды /bn."))
                .setSaveConsumer(v -> cfg.highlightSeconds = v)
                .build());

        main.addEntry(entries.startBooleanToggle(
                        Component.literal("Скрывать невидимых"), cfg.hideInvisible)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Не подсвечивать игроков под зельем невидимости."))
                .setSaveConsumer(v -> cfg.hideInvisible = v)
                .build());

        main.addEntry(entries.startBooleanToggle(
                        Component.literal("Скрывать спектаторов"), cfg.hideSpectators)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Не подсвечивать игроков в режиме наблюдателя."))
                .setSaveConsumer(v -> cfg.hideSpectators = v)
                .build());

        return builder.build();
    }
}
