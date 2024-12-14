package com.tacz.guns.compat.cloth.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.Util;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class OpenGunPackDirEntry extends AbstractConfigListEntry<Boolean> {
    private final Button button = new Button(0, 0, 150, 20, new TranslatableComponent("config.tacz.open_gunpack_folder"), button -> {
        Util.getPlatform().openUri(FMLPaths.GAMEDIR.get().resolve("tacz").toUri());
    });

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
    public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        button.x = (x + entryWidth - 150);
        button.y = (y);
        button.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }
}
