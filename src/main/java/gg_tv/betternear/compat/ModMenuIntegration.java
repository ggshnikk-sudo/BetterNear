package gg_tv.betternear.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Интеграция с ModMenu: кнопка «Настроить» у BetterNear в списке модов.
 * Класс грузится только когда установлен ModMenu (entrypoint "modmenu"),
 * поэтому без ModMenu мод работает как обычно.
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> null;
        }
        return ClothConfigScreen::create;
    }
}
