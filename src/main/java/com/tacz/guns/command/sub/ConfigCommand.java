package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.command.EnumArgument;

public class ConfigCommand {
    private static final String CONFIG_NAME = "config";
    private static final String KEY = "key";
    private static final String ENABLE = "state";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var config = Commands.literal(CONFIG_NAME);
        var configKey = Commands.argument(KEY, EnumArgument.enumArgument(ConfigKey.class));
        var state = Commands.argument(ENABLE, BoolArgumentType.bool());
        return config.then(configKey.then(state.executes(ConfigCommand::setConfig)));
    }

    private static int setConfig(CommandContext<CommandSourceStack> context) {
        ConfigKey key = context.getArgument(KEY, ConfigKey.class);
        boolean state = BoolArgumentType.getBool(context, ENABLE);

        if (key == null) {
            return 0;
        }
        switch (key) {
            case defaultTableLimit -> {
                SyncConfig.ENABLE_TABLE_FILTER.set(state);
                SyncConfig.ENABLE_TABLE_FILTER.save();
            }
            case serverShootNetworkCheck -> {
                SyncConfig.SERVER_SHOOT_NETWORK_V.set(state);
                SyncConfig.SERVER_SHOOT_NETWORK_V.save();
            }
            case serverShootCooldownCheck -> {
                SyncConfig.SERVER_SHOOT_COOLDOWN_V.set(state);
                SyncConfig.SERVER_SHOOT_COOLDOWN_V.save();
            }
        }
        context.getSource().sendSystemMessage(Component.translatable(key.lang + "." + (state ? "enabled" : "disabled")));

        return Command.SINGLE_SUCCESS;
    }

    public enum ConfigKey {
        defaultTableLimit("commands.tacz.config.default_table_limit"),
        serverShootNetworkCheck("commands.tacz.config.server_shoot_network_check"),
        serverShootCooldownCheck("commands.tacz.config.server_shoot_cooldown_check"),
        ;

        public final String lang;

        ConfigKey(String lang) {
            this.lang = lang;
        }
    }
}
