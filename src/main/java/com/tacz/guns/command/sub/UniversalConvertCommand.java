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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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
                source.sendFailure(message("batch_empty"));
                return 0;
            }
        } catch (IOException | RuntimeException e) {
            source.sendFailure(message("failed", e.getMessage()));
            return 0;
        }
        if (!acquire(inputs)) {
            source.sendFailure(message("busy"));
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
                    source.getServer().execute(() -> {
                        if (all) source.sendSuccess(() ->
                            message(export ? "batch_exported" : "batch_checked",
                                summary.total(), summary.complete(), summary.partial(), summary.skipped(), summary.failed(), "tacz-converted/")
                                .withStyle(summary.failed() > 0 || summary.partial() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
                        else source.sendSystemMessage(message("reports_directory", "tacz-converted/"));
                        if (summary.complete() + summary.partial() > 0) {
                            source.sendSystemMessage(export ? message("install_hint") : message("export_hint",
                                suggestCommand("/tacz convert universal " + (all ? "all" : StringArgumentType.escapeIfRequired(name)) + " export")));
                        }
                    });
                } catch (Exception e) {
                    GunMod.LOGGER.error("Gun pack conversion job failed for {}", inputs, e);
                    source.getServer().execute(() -> source.sendFailure(message("failed", e.getMessage())));
                } finally {
                    release(inputs);
                }
            });
        } catch (RejectedExecutionException e) {
            release(inputs);
            source.sendFailure(message("busy"));
            return 0;
        }
        source.sendSystemMessage(all
            ? message(export ? "batch_export_queued" : "batch_queued", inputs.size())
            : message(export ? "export_queued" : "queued", name));
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
        String name = outcome.source().getFileName().toString();
        if (outcome.failure() != null) {
            source.sendFailure(message("pack_failed", name, outcome.failure().getMessage()));
            return;
        }
        var result = outcome.result();
        var report = result.report();
        if (report.hasErrors()) {
            source.sendFailure(message(export ? "rejected" : "check_rejected", name));
        } else if (report.isSkipped()) {
            source.sendSystemMessage(message("skipped", name).withStyle(ChatFormatting.GRAY));
        } else if (report.isPartial()) {
            source.sendSuccess(() -> message(export ? "partial_exported" : "partial_checked", name,
                export ? displayPath(result.packPath()) : report.overlayFiles, report.skippedRecipes)
                .withStyle(ChatFormatting.YELLOW), false);
        } else {
            source.sendSuccess(() -> message(export ? "exported" : "checked", name,
                export ? displayPath(result.packPath()) : report.overlayFiles)
                .withStyle(ChatFormatting.GREEN), false);
        }
        if (!report.missingItemIds.isEmpty()) {
            source.sendSystemMessage(message("deferred_items", name, report.missingItemIds.size()));
        }
    }

    private static String displayPath(Path path) {
        return "tacz-converted/" + path.getFileName();
    }

    private static MutableComponent message(String key, Object... arguments) {
        return Component.translatable("message.tacz.pre")
            .append(Component.translatable("message.tacz.converter.universal." + key, arguments));
    }

    private static Component suggestCommand(String command) {
        return Component.literal(command).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.translatable("message.tacz.pack_compatibility.command.hover"))));
    }
}
