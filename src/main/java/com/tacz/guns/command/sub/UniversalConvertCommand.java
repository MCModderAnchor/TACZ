package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.convert.UniversalPackConverter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.*;

public final class UniversalConvertCommand {
    private static final Set<Path> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final ExecutorService WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(4), runnable -> {
            Thread thread = new Thread(runnable, "tacz-pack-converter");
            thread.setDaemon(true);
            return thread;
        });

    private UniversalConvertCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("universal")
            .then(Commands.argument("pack", StringArgumentType.string())
                .suggests((context, builder) -> {
                    Path root = FMLPaths.GAMEDIR.get().resolve("tacz");
                    if (Files.isDirectory(root)) {
                        try (var paths = Files.list(root)) {
                            paths.filter(path -> Files.isDirectory(path) || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                                .map(path -> StringArgumentType.escapeIfRequired(path.getFileName().toString()))
                                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                                .sorted().forEach(builder::suggest);
                        } catch (IOException ignored) { }
                    }
                    return builder.buildFuture();
                })
                .executes(context -> convert(context, false))
                .then(Commands.literal("check").executes(context -> convert(context, false)))
                .then(Commands.literal("export").executes(context -> convert(context, true))));
    }

    private static int convert(CommandContext<CommandSourceStack> context, boolean export) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "pack");
        Path path;
        try {
            Path root = FMLPaths.GAMEDIR.get().resolve("tacz").toRealPath();
            path = root.resolve(name).normalize();
            if (!root.equals(path.getParent()) || !path.toRealPath().startsWith(root)
                || !(Files.isDirectory(path) || name.toLowerCase(Locale.ROOT).endsWith(".zip"))) {
                throw new IOException("Select a gun pack directly inside tacz/");
            }
            path = path.toRealPath();
        } catch (IOException | RuntimeException e) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.failed", e.getMessage()));
            return 0;
        }
        if (!ACTIVE.add(path)) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.busy"));
            return 0;
        }
        Path input = path;
        var versions = new HashMap<String, String>();
        ModList.get().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
        var registries = source.registryAccess();
        try {
            WORKER.execute(() -> {
                try {
                    var converter = new UniversalPackConverter(registries, versions);
                    var result = converter.convert(input, FMLPaths.GAMEDIR.get().resolve("tacz-converted"), export);
                    source.getServer().execute(() -> {
                        var report = result.report();
                        if (report.hasErrors()) {
                            source.sendFailure(Component.translatable("message.tacz.converter.universal.rejected", result.reportPath().toString()));
                            report.diagnostics.stream().filter(d -> d.severity().equals("error")).limit(3).forEach(d ->
                                source.sendSystemMessage(Component.literal(d.file() + " " + d.path() + " [" + d.code() + "] " + d.message())));
                        } else {
                            source.sendSuccess(() -> Component.translatable(export ? "message.tacz.converter.universal.exported" : "message.tacz.converter.universal.checked",
                                export ? result.packPath().toString() : result.reportPath().toString(), report.overlayFiles), false);
                        }
                    });
                } catch (Exception e) {
                    GunMod.LOGGER.error("Gun pack conversion failed for {}", input, e);
                    source.getServer().execute(() -> source.sendFailure(Component.translatable("message.tacz.converter.universal.failed", e.getMessage())));
                } finally {
                    ACTIVE.remove(input);
                }
            });
        } catch (RejectedExecutionException e) {
            ACTIVE.remove(input);
            source.sendFailure(Component.translatable("message.tacz.converter.universal.busy"));
            return 0;
        }
        source.sendSystemMessage(Component.translatable("message.tacz.converter.universal.queued", name));
        return Command.SINGLE_SUCCESS;
    }
}
