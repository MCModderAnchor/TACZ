package com.tacz.guns.compat.cloth.widget;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class OpenGunPackDirEntry extends AbstractConfigListEntry<Boolean> {
    private final Button button = Button.builder(Component.translatable("config.tacz.open_gunpack_folder"), button -> {
        Util.getPlatform().openUri(FMLPaths.GAMEDIR.get().resolve("tacz").toUri());
        button.setFocused(false);
    }).bounds(0, 0, 150, 20).build();

    public OpenGunPackDirEntry(Component name) {
        super(name, true);
    }

    @Override
    @NotNull
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }

    @Override
    public Boolean getValue() {
        return true;
    }

    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.of(true);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        button.setX(x + entryWidth - 150);
        button.setY(y);
        button.render(graphics, mouseX, mouseY, delta);
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }
}
