package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.convert.GunPackConversionBatch;
import com.tacz.guns.resource.convert.PackConversionRules;
import com.tacz.guns.resource.convert.UniversalPackConverter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class UniversalConvertCommand {
    private static final Set<Path> ACTIVE = new HashSet<>();
    private static final ExecutorService WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(4), runnable -> {
            Thread thread = new Thread(runnable, "tacz-pack-converter");
            thread.setDaemon(true);
            return thread;
        });

    private UniversalConvertCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("universal")
            .then(Commands.literal("all")
                .executes(context -> convert(context, false, true))
                .then(Commands.literal("check").executes(context -> convert(context, false, true)))
                .then(Commands.literal("export").executes(context -> convert(context, true, true))))
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
                .executes(context -> convert(context, false, false))
                .then(Commands.literal("check").executes(context -> convert(context, false, false)))
                .then(Commands.literal("export").executes(context -> convert(context, true, false))));
    }

    private static int convert(CommandContext<CommandSourceStack> context, boolean export, boolean all) {
        CommandSourceStack source = context.getSource();
        String name = all ? "all" : StringArgumentType.getString(context, "pack");
        List<Path> inputs;
        try {
            Path root = FMLPaths.GAMEDIR.get().resolve("tacz");
            inputs = all ? GunPackConversionBatch.discover(root) : List.of(GunPackConversionBatch.select(root, name));
            if (inputs.isEmpty()) {
                source.sendFailure(Component.translatable("message.tacz.converter.universal.batch_empty"));
                return 0;
            }
        } catch (IOException | RuntimeException e) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.failed", e.getMessage()));
            return 0;
        }
        if (!acquire(inputs)) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.busy"));
            return 0;
        }
        Path output = FMLPaths.GAMEDIR.get().resolve("tacz-converted");
        var versions = new HashMap<String, String>();
        ModList.get().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
        var registries = source.registryAccess();
        try {
            WORKER.execute(() -> {
                try {
                    var rules = PackConversionRules.load(FMLPaths.CONFIGDIR.get().resolve("tacz/gunpack-conversion-rules.json"));
                    var converter = new UniversalPackConverter(registries, versions, rules);
                    var summary = GunPackConversionBatch.convert(inputs, converter, output, export, outcome -> {
                        if (outcome.failure() != null) GunMod.LOGGER.error("Gun pack conversion failed for {}", outcome.source(), outcome.failure());
                        source.getServer().execute(() -> report(source, outcome, export));
                    });
                    if (all) source.getServer().execute(() -> source.sendSuccess(() ->
                        Component.translatable(export ? "message.tacz.converter.universal.batch_exported" : "message.tacz.converter.universal.batch_checked",
                            summary.total(), summary.complete(), summary.partial(), summary.failed(), output.toString())
                            .withStyle(summary.failed() > 0 || summary.partial() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false));
                } catch (Exception e) {
                    GunMod.LOGGER.error("Gun pack conversion job failed for {}", inputs, e);
                    source.getServer().execute(() -> source.sendFailure(Component.translatable("message.tacz.converter.universal.failed", e.getMessage())));
                } finally {
                    release(inputs);
                }
            });
        } catch (RejectedExecutionException e) {
            release(inputs);
            source.sendFailure(Component.translatable("message.tacz.converter.universal.busy"));
            return 0;
        }
        source.sendSystemMessage(all
            ? Component.translatable("message.tacz.converter.universal.batch_queued", inputs.size())
            : Component.translatable("message.tacz.converter.universal.queued", name));
        return Command.SINGLE_SUCCESS;
    }

    private static boolean acquire(List<Path> inputs) {
        synchronized (ACTIVE) {
            if (inputs.stream().anyMatch(ACTIVE::contains)) return false;
            ACTIVE.addAll(inputs);
            return true;
        }
    }

    private static void release(List<Path> inputs) {
        synchronized (ACTIVE) {
            ACTIVE.removeAll(inputs);
        }
    }

    private static void report(CommandSourceStack source, GunPackConversionBatch.Outcome outcome, boolean export) {
        if (outcome.failure() != null) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.failed",
                outcome.source().getFileName() + ": " + outcome.failure().getMessage()));
            return;
        }
        var result = outcome.result();
        var report = result.report();
        if (report.hasErrors()) {
            source.sendFailure(Component.translatable("message.tacz.converter.universal.rejected", result.reportPath().toString()));
            report.diagnostics.stream().filter(d -> d.severity().equals("error")).limit(3).forEach(d ->
                source.sendSystemMessage(Component.literal(d.file() + " " + d.path() + " [" + d.code() + "] " + d.message())));
        } else if (report.isPartial()) {
            source.sendSuccess(() -> Component.translatable(export ? "message.tacz.converter.universal.partial_exported" : "message.tacz.converter.universal.partial_checked",
                export ? result.packPath().toString() : result.reportPath().toString(), report.skippedRecipes, result.reportPath().toString())
                .withStyle(ChatFormatting.YELLOW), false);
        } else {
            source.sendSuccess(() -> Component.translatable(export ? "message.tacz.converter.universal.exported" : "message.tacz.converter.universal.checked",
                export ? result.packPath().toString() : result.reportPath().toString(), report.overlayFiles), false);
        }
        if (!report.missingItemIds.isEmpty()) {
            source.sendSystemMessage(Component.translatable("message.tacz.converter.universal.deferred_items", report.missingItemIds.size()));
        }
    }
}
