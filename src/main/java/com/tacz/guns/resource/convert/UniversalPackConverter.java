package com.tacz.guns.resource.convert;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.GunPackOverlayResolver;
import com.tacz.guns.resource.PackMeta;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class UniversalPackConverter {
    public static final String OVERLAY = "compat_1_21_1";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_JSON_BYTES = 16 * 1024 * 1024;
    private static final Map<String, String> DIRECTORIES = Map.ofEntries(
        Map.entry("recipes", "recipe"), Map.entry("loot_tables", "loot_table"),
        Map.entry("advancements", "advancement"), Map.entry("predicates", "predicate"),
        Map.entry("item_modifiers", "item_modifier"), Map.entry("functions", "function"));
    private static final Map<String, String> TAG_DIRECTORIES = Map.of(
        "items", "item", "blocks", "block", "fluids", "fluid", "entity_types", "entity_type",
        "game_events", "game_event", "functions", "function");
    private static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final Map<String, String> COMMON_TAGS = commonTags();
    private static final Map<String, List<String>> LEGACY_COMMON_TAGS = legacyCommonTags();
    private static final Set<String> EMBEDDED_ATTACHMENTS = java.util.Arrays.stream(AttachmentType.values())
        .filter(type -> type != AttachmentType.NONE).map(type -> "Attachment" + type.name()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Set<String> VANILLA_NBT = Set.of("Damage", "RepairCost", "Unbreakable", "HideFlags", "display",
        "Enchantments", "StoredEnchantments", "AttributeModifiers", "CustomModelData", "CanDestroy", "CanPlaceOn",
        "BlockEntityTag", "BlockStateTag", "EntityTag", "Potion", "CustomPotionEffects", "CustomPotionColor",
        "SkullOwner", "Fireworks", "Explosion", "Trim", "Charged", "ChargedProjectiles", "Items", "map",
        "LodestonePos", "LodestoneDimension", "LodestoneTracked", "pages", "filtered_pages", "author", "title",
        "resolved", "generation", "Decorations", "DebugProperty", "instrument", "Recipes", "ForgeCaps", "BucketVariantTag");
    private static final Set<String> VANILLA_RECIPES = Set.of("minecraft:crafting_shaped", "minecraft:crafting_shapeless",
        "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
        "minecraft:stonecutting", "minecraft:smithing_transform", "minecraft:smithing_trim");
    private final LegacyItemStackUpgrader items;
    private final Map<String, String> installedVersions;

    public UniversalPackConverter(HolderLookup.Provider registries, Map<String, String> installedVersions) {
        items = new LegacyItemStackUpgrader(registries);
        this.installedVersions = Map.copyOf(installedVersions);
    }

    public record Result(PackConversionPlan report, Path reportPath, Path packPath) { }

    public Result convert(Path source, Path outputDirectory, boolean export) throws IOException {
        Path realSource = source.toRealPath();
        if (Files.isDirectory(realSource) && outputDirectory.toAbsolutePath().normalize().startsWith(realSource)) {
            throw new IOException("Conversion output must be outside the source gun pack");
        }
        PackConversionPlan plan = new PackConversionPlan(source.getFileName().toString());
        Map<String, byte[]> generated = new TreeMap<>();
        String stem = source.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
        Files.createDirectories(outputDirectory);
        if (Files.isDirectory(realSource) && outputDirectory.toRealPath().startsWith(realSource)) {
            throw new IOException("Conversion output resolves inside the source gun pack");
        }
        Path reportPath = outputDirectory.resolve(stem + "-universal.report.json");
        Path packPath = outputDirectory.resolve(stem + "-universal.zip");
        if (export && Files.exists(packPath)) {
            plan.error("", "$", "OUTPUT_EXISTS", "Output already exists: " + packPath.getFileName());
            return finish(plan, reportPath, null);
        }
        try (GunPackInput input = new GunPackInput(source)) {
            plan.baseFiles = input.files.size();
            JsonObject meta = readObject(required(input, "gunpack.meta.json"));
            Set<String> existingOverlays = prepareMetadata(meta, input, plan);
            if (!plan.hasErrors()) {
                for (var entry : input.files.entrySet()) {
                    String name = entry.getKey();
                    if (name.equals("gunpack.meta.json") || existingOverlays.contains(name.split("/", 2)[0])) continue;
                    if (name.startsWith(OVERLAY + "/")) {
                        plan.error(name, "$", "OVERLAY_CONFLICT", "Target overlay directory already exists");
                        continue;
                    }
                    try {
                        plan.beginFile(name);
                        String target = targetPath(name);
                        if (!target.equals(name) && input.files.containsKey(target)) {
                            throw fail("$", "OUTPUT_COLLISION", "Both old and new resource paths exist: " + target);
                        }
                        convertFile(name, entry.getValue(), generated, plan);
                    } catch (MigrationException e) {
                        plan.error(name, e.path, e.code, e.getMessage());
                    } catch (Exception e) {
                        plan.error(name, "$", "INVALID_DATA", message(e));
                    }
                }
            }
            for (String id : plan.legacyTags()) {
                String path = OVERLAY + "/data/forge/tags/item/" + id.substring("forge:".length()) + ".json";
                if (!generated.containsKey(path)) {
                    JsonObject definition = new JsonObject();
                    definition.add("values", GSON.toJsonTree(LEGACY_COMMON_TAGS.get(id)));
                    generated.put(path, GSON.toJson(definition).getBytes(StandardCharsets.UTF_8));
                }
            }
            if (generated.isEmpty() && !plan.hasErrors()) {
                JsonObject overlays = meta.getAsJsonObject("overlays");
                JsonArray entries = overlays.getAsJsonArray("entries");
                entries.remove(entries.size() - 1);
                if (entries.isEmpty()) meta.remove("overlays");
            }
            plan.overlayFiles = generated.size();
            plan.generatedFiles.addAll(generated.keySet());
            plan.targetFormatValidated = !plan.hasErrors();
            if (export && !plan.hasErrors()) {
                writePack(input, meta, generated, packPath);
                plan.output = packPath.getFileName().toString();
                return finish(plan, reportPath, packPath);
            }
        } catch (Exception e) {
            plan.targetFormatValidated = false;
            plan.error("gunpack.meta.json", "$", "PACK_INPUT", message(e));
        }
        return finish(plan, reportPath, null);
    }

    private Set<String> prepareMetadata(JsonObject meta, GunPackInput input, PackConversionPlan plan) throws Exception {
        if (!meta.has("namespace") || meta.get("namespace").getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing gun pack name in namespace metadata");
        }
        if (!ResourceLocation.isValidNamespace(meta.get("namespace").getAsString())) {
            plan.warn("gunpack.meta.json", "$.namespace", "PACK_NAME_LABEL", "Preserved legacy pack name; resource namespaces are validated independently");
        }
        Set<String> existing = new HashSet<>();
        if (meta.has("overlays")) {
            PackMeta parsed = GSON.fromJson(meta, PackMeta.class);
            GunPackOverlayResolver.resolve(parsed, "1.20.1");
            for (var overlay : parsed.getOverlays().entries()) {
                existing.add(overlay.directory());
                if (overlay.directory().equals(OVERLAY) || GunPackOverlayResolver.matches(overlay.minecraft(), "1.21.1")) {
                    plan.error("gunpack.meta.json", "$.overlays", "OVERLAY_CONFLICT", "Pack already has a 1.21.1 overlay; refusing to overwrite it");
                }
            }
        }
        if (input.files.keySet().stream().anyMatch(name -> name.startsWith(OVERLAY + "/"))) {
            plan.error("gunpack.meta.json", "$.overlays", "OVERLAY_CONFLICT", "Target overlay directory already exists");
        }
        // A source-version overlay would have to be flattened before converting the effective old pack.
        if (meta.has("overlays")) {
            PackMeta parsed = GSON.fromJson(meta, PackMeta.class);
            if (!GunPackOverlayResolver.resolve(parsed, "1.20.1").isEmpty()) {
                plan.error("gunpack.meta.json", "$.overlays", "SOURCE_OVERLAY", "Flatten 1.20.1 overlays before converting this pack");
            }
        }
        JsonObject dependencies = meta.has("dependencies") ? meta.getAsJsonObject("dependencies") : new JsonObject();
        for (var dependency : dependencies.entrySet()) {
            if (dependency.getKey().equals("minecraft")) {
                if (!GunPackOverlayResolver.matches(dependency.getValue().getAsString(), "1.20.1")) {
                    plan.error("gunpack.meta.json", "$.dependencies.minecraft", "SOURCE_VERSION", "Pack does not declare support for 1.20.1");
                }
                continue;
            }
            String installed = installedVersions.get(dependency.getKey());
            VersionRange range = VersionRange.createFromVersionSpec(dependency.getValue().getAsString());
            if (installed == null || !range.containsVersion(new DefaultArtifactVersion(installed))) {
                plan.error("gunpack.meta.json", "$.dependencies." + dependency.getKey(), "DEPENDENCY", "Dependency is unavailable or incompatible on 1.21.1");
            }
        }
        dependencies.addProperty("minecraft", "[1.20.1],[1.21.1]");
        meta.add("dependencies", dependencies);
        JsonObject overlays = meta.has("overlays") ? meta.getAsJsonObject("overlays") : new JsonObject();
        JsonArray entries = overlays.has("entries") ? overlays.getAsJsonArray("entries") : new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("directory", OVERLAY);
        entry.addProperty("minecraft", "[1.21.1]");
        entries.add(entry);
        overlays.add("entries", entries);
        meta.add("overlays", overlays);
        return existing;
    }

    private void convertFile(String name, GunPackInput.Opener source, Map<String, byte[]> generated, PackConversionPlan plan) throws IOException {
        String target = targetPath(name);
        if (!name.endsWith(".json")) {
            if (name.endsWith(".lua") || name.endsWith(".js")) {
                String script = new String(readLimited(source), StandardCharsets.UTF_8);
                if (Pattern.compile("[.:](?:getTag|getOrCreateTag)\\s*\\(|[a-z0-9_.-]+:[a-z0-9_./-]+\\{").matcher(script).find()) {
                    throw fail("$", "SCRIPT_API", "Script uses old item NBT APIs or commands and requires manual migration");
                }
                plan.warn(name, "$", "SCRIPT_PRESERVED", "Script copied unchanged; review cross-version API compatibility in game");
            }
            if (!target.equals(name)) {
                byte[] bytes = readLimited(source);
                String text = new String(bytes, StandardCharsets.UTF_8);
                if (Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+\\{|\\b(tag|SelectedItem)\\s*:\\s*\\{").matcher(text).find()) {
                    throw fail("$", "COMMAND_SYNTAX", "Old item NBT command syntax requires manual migration");
                }
                putGenerated(generated, target, bytes);
            }
            return;
        }
        if (!name.startsWith("data/") && !name.startsWith("assets/")) return;
        String[] segments = name.split("/", 4);
        if (segments.length < 4) return;
        String local = segments[2] + "/" + segments[3];
        if (name.startsWith("data/") && !Set.of("recipes", "loot_tables", "tacz_loot_injectors", "tags", "advancements", "predicates", "item_modifiers").contains(segments[2])
                && !local.startsWith("data/blocks/")) return;
        JsonElement original;
        try (InputStream stream = source.open(); JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(readLimited(stream)), StandardCharsets.UTF_8))) {
            original = JsonParser.parseReader(reader);
            try {
                if (reader.peek() != JsonToken.END_DOCUMENT) plan.warn(name, "$", "TRAILING_JSON_CONTENT", "Trailing source content preserved; only the first JSON value is used, as by the legacy loader");
            } catch (IOException e) {
                plan.warn(name, "$", "TRAILING_JSON_CONTENT", "Trailing source content preserved; only the first JSON value is used, as by the legacy loader");
            }
        }
        JsonElement converted = original.deepCopy();
        if (name.startsWith("data/")) {
            if (segments[2].equals("recipes")) {
                convertRecipe(converted.getAsJsonObject(), plan);
            } else if (local.startsWith("data/blocks/")) {
                convertTabs(converted.getAsJsonObject(), plan);
            } else if (segments[2].equals("loot_tables") || segments[2].equals("tacz_loot_injectors")) {
                rejectOldPredicates(converted, "$");
                convertLoot(converted, null, "$", plan);
                LootTable.DIRECT_CODEC.parse(items.ops(), converted).getOrThrow(IllegalArgumentException::new);
            } else if (segments[2].equals("tags")) {
                convertTagFile(converted.getAsJsonObject(), plan);
            } else if (segments[2].equals("advancements")) {
                JsonObject advancement = converted.getAsJsonObject();
                if (advancement.has("display") && advancement.getAsJsonObject("display").has("icon")) {
                    JsonObject display = advancement.getAsJsonObject("display");
                    display.add("icon", items.upgrade(display.getAsJsonObject("icon")));
                    plan.converted("$.display.icon", "upgrade_item_stack");
                }
                rejectOldPredicates(converted, "$");
                Advancement.CODEC.parse(items.ops(), converted).getOrThrow(IllegalArgumentException::new);
            } else if (segments[2].equals("predicates")) {
                rejectOldPredicates(converted, "$");
                LootItemCondition.DIRECT_CODEC.parse(items.ops(), converted).getOrThrow(IllegalArgumentException::new);
            } else if (segments[2].equals("item_modifiers")) {
                rejectOldPredicates(converted, "$");
                JsonObject wrapper = new JsonObject();
                JsonArray functions = converted.isJsonArray() ? converted.getAsJsonArray() : new JsonArray();
                if (!converted.isJsonArray()) functions.add(converted);
                wrapper.add("functions", functions);
                convertLoot(wrapper, null, "$", plan);
                converted = wrapper.get("functions");
                LootItemFunctions.ROOT_CODEC.parse(items.ops(), converted).getOrThrow(IllegalArgumentException::new);
            }
            if (converted.isJsonObject()) convertConditions(converted.getAsJsonObject(), "$", plan);
            rejectUnconvertedForge(converted, "$");
        } else {
            // No current client POJO declares ItemStack fields. Do not rewrite unrelated objects by shape.
            if (containsField(converted, "nbt")) {
                throw fail("$", "CLIENT_ITEM_ADAPTER", "Client resource contains NBT; its field requires a schema-specific adapter");
            }
        }
        if (!target.equals(name) || !converted.equals(original)) {
            putGenerated(generated, target, GSON.toJson(converted).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void convertRecipe(JsonObject recipe, PackConversionPlan plan) {
        String type = recipe.get("type").getAsString();
        convertConditions(recipe, "$", plan);
        if (type.equals("tacz:gun_smith_table_crafting")) {
            JsonArray materials = recipe.getAsJsonArray("materials");
            for (int i = 0; i < materials.size(); i++) {
                JsonObject material = materials.get(i).getAsJsonObject();
                normalizeLegacyCount(material, "$.materials[" + i + "].count", plan);
                material.add("item", ingredient(material.get("item"), "$.materials[" + i + "].item", plan));
            }
            JsonObject result = recipe.getAsJsonObject("result");
            String resultType = result.get("type").getAsString();
            if (resultType.equals("custom")) {
                result.add("item", items.upgrade(result.getAsJsonObject("item")));
                plan.converted("$.result.item", "upgrade_item_stack");
                if (result.remove("nbt") != null) {
                    plan.warn("", "$.result.nbt", "IGNORED_LEGACY_FIELD", "Removed outer custom-result NBT which 1.20.1 did not apply");
                }
            } else if (Set.of("gun", "ammo", "attachment").contains(resultType)) {
                normalizeLegacyCount(result, "$.result.count", plan);
                if (result.has("components")) throw fail("$.result.components", "SOURCE_COMPONENTS", "Source recipe already contains 1.21.1 component data");
                if (result.has("nbt")) {
                    String itemId = switch (resultType) {
                        case "gun" -> "tacz:modern_kinetic_gun";
                        case "ammo" -> "tacz:ammo";
                        default -> "tacz:attachment";
                    };
                    result.add("components", items.upgradeComponents(itemId, result.remove("nbt")));
                    plan.converted("$.result.components", "upgrade_typed_result_components");
                }
            } else throw fail("$.result.type", "RESULT_TYPE", "Unknown TACZ recipe result type: " + resultType);
            TableRecipe.CODEC.codec().parse(items.ops(), recipe).getOrThrow(IllegalArgumentException::new);
        } else if (VANILLA_RECIPES.contains(type) || type.startsWith("minecraft:crafting_special_")) {
            if (recipe.has("key")) {
                JsonObject key = recipe.getAsJsonObject("key");
                for (String symbol : new ArrayList<>(key.keySet())) key.add(symbol, ingredient(key.get(symbol), "$.key." + symbol, plan));
            }
            if (recipe.has("ingredients")) {
                JsonArray array = recipe.getAsJsonArray("ingredients");
                for (int i = 0; i < array.size(); i++) array.set(i, ingredient(array.get(i), "$.ingredients[" + i + "]", plan));
            }
            for (String field : List.of("ingredient", "template", "base", "addition")) {
                if (recipe.has(field)) recipe.add(field, ingredient(recipe.get(field), "$." + field, plan));
            }
            if (recipe.has("result")) {
                JsonElement result = recipe.get("result");
                JsonObject oldItem = result.isJsonObject() ? result.getAsJsonObject() : new JsonObject();
                if (result.isJsonPrimitive()) oldItem.addProperty("item", result.getAsString());
                recipe.add("result", items.upgrade(oldItem));
                plan.converted("$.result", "upgrade_item_stack");
            }
            Recipe.CODEC.parse(items.ops(), recipe).getOrThrow(IllegalArgumentException::new);
        } else throw fail("$.type", "RECIPE_ADAPTER", "Recipe serializer requires an adapter: " + type);
    }

    private JsonElement ingredient(JsonElement input, String path, PackConversionPlan plan) {
        if (input == null) throw fail(path, "INGREDIENT", "Missing ingredient");
        if (input.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (int i = 0; i < input.getAsJsonArray().size(); i++) out.add(ingredient(input.getAsJsonArray().get(i), path + "[" + i + "]", plan));
            return out;
        }
        JsonObject object = input.getAsJsonObject().deepCopy();
        String type = object.has("type") ? object.get("type").getAsString() : "minecraft:item";
        if (type.equals("forge:nbt")) throw fail(path, "STRICT_NBT", "Strict Forge share-tag matching has no verified component equivalent");
        if (type.equals("forge:partial_nbt")) {
            CompoundTag nbt = LegacyItemStackUpgrader.readNbt(object.get("nbt"));
            for (String key : nbt.getAllKeys()) {
                if (VANILLA_NBT.contains(key) || EMBEDDED_ATTACHMENTS.contains(key)) {
                    throw fail(path + ".nbt." + key, "NBT_PREDICATE", "This NBT predicate requires a component predicate adapter");
                }
            }
            JsonObject out = new JsonObject();
            out.addProperty("type", "tacz:nbt");
            JsonArray accepted = new JsonArray();
            if (object.has("item")) accepted.add(object.get("item"));
            else if (object.has("items")) accepted = object.getAsJsonArray("items");
            else throw fail(path, "INGREDIENT_ITEMS", "Missing item/items for partial NBT ingredient");
            out.add("items", accepted);
            out.addProperty("nbt", nbt.toString());
            out.addProperty("partial", true);
            object = out;
            plan.converted(path, "convert_partial_nbt_predicate");
        } else if (type.equals("forge:intersection") || type.equals("forge:difference")) {
            object.addProperty("type", type.replace("forge:", "neoforge:"));
            if (object.has("children")) {
                JsonArray children = object.getAsJsonArray("children");
                for (int i = 0; i < children.size(); i++) children.set(i, ingredient(children.get(i), path + ".children[" + i + "]", plan));
            }
            for (String field : List.of("base", "subtracted")) if (object.has(field)) object.add(field, ingredient(object.get(field), path + "." + field, plan));
        } else if (!type.equals("minecraft:item")) {
            throw fail(path + ".type", "INGREDIENT_ADAPTER", "Ingredient requires an adapter: " + type);
        }
        if (object.has("tag")) {
            String before = object.get("tag").getAsString();
            String after = tag(before, path + ".tag", plan);
            object.addProperty("tag", after);
            if (!before.equals(after)) plan.converted(path + ".tag", "map_common_tag");
        }
        Ingredient.CODEC.parse(items.ops(), object).getOrThrow(IllegalArgumentException::new);
        return object;
    }

    private void convertTabs(JsonObject block, PackConversionPlan plan) {
        if (!block.has("tabs")) return;
        JsonArray tabs = block.getAsJsonArray("tabs");
        for (int i = 0; i < tabs.size(); i++) {
            JsonObject entry = tabs.get(i).getAsJsonObject();
            entry.add("icon", items.upgrade(entry.getAsJsonObject("icon")));
            plan.converted("$.tabs[" + i + "].icon", "upgrade_item_stack");
        }
    }

    private static void normalizeLegacyCount(JsonObject object, String path, PackConversionPlan plan) {
        if (object.has("count") && object.get("count").getAsInt() < 1) {
            object.addProperty("count", 1);
            plan.converted(path, "preserve_legacy_minimum_count");
        }
    }

    private void convertLoot(JsonElement element, String inheritedItem, String path, PackConversionPlan plan) {
        if (element.isJsonArray()) {
            for (int i = 0; i < element.getAsJsonArray().size(); i++) convertLoot(element.getAsJsonArray().get(i), inheritedItem, path + "[" + i + "]", plan);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String item = inheritedItem;
            if (object.has("type") && object.get("type").getAsString().equals("minecraft:item") && object.has("name")) item = object.get("name").getAsString();
            for (String key : new ArrayList<>(object.keySet())) {
                if (key.equals("functions")) {
                    JsonArray converted = new JsonArray();
                    for (JsonElement function : object.getAsJsonArray(key)) {
                        JsonObject fn = function.getAsJsonObject();
                        String functionType = fn.get("function").getAsString();
                        if (functionType.equals("minecraft:set_nbt")) {
                            CompoundTag oldTag = LegacyItemStackUpgrader.readNbt(fn.get("tag"));
                            if (oldTag.getAllKeys().stream().anyMatch(VANILLA_NBT::contains)) {
                                throw fail(path + ".functions", "LOOT_NBT_SEMANTICS", "set_nbt with vanilla fields requires a context-preserving component adapter");
                            }
                            JsonObject components = items.upgradeComponents(item == null ? "minecraft:stick" : item, fn.get("tag"));
                            JsonElement custom = components.remove("minecraft:custom_data");
                            if (!components.isEmpty()) {
                                JsonObject output = fn.deepCopy();
                                output.remove("tag");
                                output.addProperty("function", "minecraft:set_components");
                                output.add("components", components);
                                converted.add(output);
                            }
                            if (custom != null) {
                                JsonObject output = fn.deepCopy();
                                output.addProperty("function", "minecraft:set_custom_data");
                                output.add("tag", custom);
                                converted.add(output);
                            }
                            plan.converted(path + ".functions", "convert_custom_nbt_loot_function");
                        } else {
                            if (functionType.equals("minecraft:copy_nbt")) throw fail(path, "LOOT_FUNCTION", "copy_nbt requires a component-aware adapter");
                            convertLoot(fn, item, path + ".functions", plan);
                            converted.add(fn);
                        }
                    }
                    object.add(key, converted);
                } else convertLoot(object.get(key), item, path + "." + key, plan);
            }
        }
    }

    private void convertConditions(JsonObject object, String path, PackConversionPlan plan) {
        if (!object.has("forge:conditions")) return;
        if (object.has("neoforge:conditions")) throw fail(path, "CONDITIONS", "Both Forge and NeoForge conditions are present");
        JsonArray conditions = object.remove("forge:conditions").getAsJsonArray();
        for (JsonElement condition : conditions) convertCondition(condition.getAsJsonObject(), path + ".forge:conditions", plan);
        object.add("neoforge:conditions", conditions);
        ICondition.LIST_CODEC.parse(items.ops(), conditions).getOrThrow(IllegalArgumentException::new);
    }

    private void convertCondition(JsonObject condition, String path, PackConversionPlan plan) {
        String type = condition.get("type").getAsString();
        if (!Set.of("forge:mod_loaded", "forge:item_exists", "forge:tag_empty", "forge:not", "forge:or", "forge:and", "forge:true", "forge:false").contains(type)) {
            throw fail(path, "CONDITION_ADAPTER", "Condition requires an adapter: " + type);
        }
        condition.addProperty("type", type.replace("forge:", "neoforge:"));
        if (type.equals("forge:mod_loaded") && condition.has("modid") && condition.get("modid").getAsString().equals("forge")) {
            throw fail(path + ".modid", "LOADER_CONDITION", "Forge-specific loader condition requires manual migration");
        }
        if (condition.has("tag")) condition.addProperty("tag", tag(condition.get("tag").getAsString(), path + ".tag", plan));
        if (condition.has("value")) convertCondition(condition.getAsJsonObject("value"), path + ".value", plan);
        for (String key : List.of("values", "children")) {
            if (condition.has(key)) for (JsonElement child : condition.getAsJsonArray(key)) convertCondition(child.getAsJsonObject(), path + "." + key, plan);
        }
    }

    private static void convertTagFile(JsonObject object, PackConversionPlan plan) {
        JsonArray values = object.getAsJsonArray("values");
        if (values == null) throw fail("$.values", "TAG_VALUES", "Tag file is missing values");
        for (int i = 0; i < values.size(); i++) {
            JsonElement entry = values.get(i);
            if (entry.isJsonPrimitive()) values.set(i, new JsonPrimitive(tagReference(entry.getAsString(), plan)));
            else {
                JsonObject reference = entry.getAsJsonObject();
                reference.addProperty("id", tagReference(reference.get("id").getAsString(), plan));
            }
        }
    }

    private static String tagReference(String value, PackConversionPlan plan) {
        return value.startsWith("#") ? "#" + tag(value.substring(1), "$.values", plan) : value;
    }

    private static String tag(String id, String path, PackConversionPlan plan) {
        if (!id.startsWith("forge:")) return id;
        String mapped = COMMON_TAGS.get(id);
        if (mapped == null && LEGACY_COMMON_TAGS.containsKey(id)) {
            plan.requireLegacyTag(id);
            return id;
        }
        if (mapped == null) throw fail(path, "COMMON_TAG", "No verified common-tag mapping for " + id);
        return mapped;
    }

    private static Map<String, String> commonTags() {
        Map<String, String> tags = new HashMap<>();
        for (String metal : List.of("iron", "gold", "copper", "netherite")) {
            for (String form : List.of("ingots", "storage_blocks")) tags.put("forge:" + form + "/" + metal, "c:" + form + "/" + metal);
        }
        for (String metal : List.of("iron", "gold")) tags.put("forge:nuggets/" + metal, "c:nuggets/" + metal);
        tags.put("forge:rods/blaze", "c:rods/blaze");
        tags.put("forge:rods/wooden", "c:rods/wooden");
        for (String gem : List.of("diamond", "emerald", "quartz", "amethyst", "lapis", "prismarine")) tags.put("forge:gems/" + gem, "c:gems/" + gem);
        tags.put("forge:dusts/redstone", "c:dusts/redstone");
        tags.put("forge:dusts/glowstone", "c:dusts/glowstone");
        tags.put("forge:gunpowder", "c:gunpowders");
        tags.put("forge:leather", "c:leathers");
        tags.put("forge:string", "c:strings");
        tags.put("forge:glass", "c:glass_blocks");
        tags.put("forge:glass/colorless", "c:glass_blocks/colorless");
        tags.put("forge:slimeballs", "c:slime_balls");
        tags.put("forge:obsidian", "c:obsidians/normal");
        tags.put("forge:nether_stars", "c:nether_stars");
        tags.put("forge:feathers", "c:feathers");
        tags.put("forge:sandstone", "c:sandstone/blocks");
        tags.put("forge:ores/coal", "c:ores/coal");
        tags.put("forge:tools/crossbows", "c:tools/crossbow");
        for (String material : List.of("coal", "diamond", "emerald", "lapis", "redstone", "raw_iron", "raw_gold", "raw_copper", "slime", "wheat", "bone_meal", "dried_kelp")) {
            tags.put("forge:storage_blocks/" + material, "c:storage_blocks/" + material);
        }
        for (String color : COLORS) tags.put("forge:dyes/" + color, "c:dyes/" + color);
        return Map.copyOf(tags);
    }

    private static Map<String, List<String>> legacyCommonTags() {
        Map<String, List<String>> tags = new HashMap<>();
        tags.put("forge:storage_blocks/amethyst", List.of("minecraft:amethyst_block"));
        for (String color : COLORS) tags.put("forge:glass/" + color, List.of("minecraft:" + color + "_stained_glass"));
        return Map.copyOf(tags);
    }

    public static String targetPath(String source) {
        String[] parts = source.split("/", 5);
        if (parts.length >= 4 && parts[0].equals("data")) {
            parts[2] = DIRECTORIES.getOrDefault(parts[2], parts[2]);
            if (parts[2].equals("tags") && parts.length == 5) parts[3] = TAG_DIRECTORIES.getOrDefault(parts[3], parts[3]);
        }
        return String.join("/", parts);
    }

    private static void rejectOldPredicates(JsonElement json, String path) {
        if (json.isJsonArray()) {
            for (int i = 0; i < json.getAsJsonArray().size(); i++) rejectOldPredicates(json.getAsJsonArray().get(i), path + "[" + i + "]");
        } else if (json.isJsonObject()) {
            for (var field : json.getAsJsonObject().entrySet()) {
                if (Set.of("nbt", "durability", "potion", "stored_enchantments").contains(field.getKey())
                    || (Set.of("tag", "enchantments").contains(field.getKey()) && !json.getAsJsonObject().has("function"))) {
                    throw fail(path + "." + field.getKey(), "VANILLA_PREDICATE", "Legacy item predicate requires a component-aware adapter");
                }
                if (!field.getKey().equals("components")) rejectOldPredicates(field.getValue(), path + "." + field.getKey());
            }
        }
    }

    private static void rejectUnconvertedForge(JsonElement json, String path) {
        if (json.isJsonArray()) {
            for (int i = 0; i < json.getAsJsonArray().size(); i++) rejectUnconvertedForge(json.getAsJsonArray().get(i), path + "[" + i + "]");
        } else if (json.isJsonObject()) {
            for (var field : json.getAsJsonObject().entrySet()) {
                if (field.getKey().startsWith("forge:") || (Set.of("type", "condition", "function", "tag").contains(field.getKey())
                    && field.getValue().isJsonPrimitive() && field.getValue().getAsString().startsWith("forge:")
                    && !(field.getKey().equals("tag") && LEGACY_COMMON_TAGS.containsKey(field.getValue().getAsString())))) {
                    throw fail(path + "." + field.getKey(), "FORGE_ADAPTER", "Unconverted Forge data requires an adapter");
                }
                rejectUnconvertedForge(field.getValue(), path + "." + field.getKey());
            }
        }
    }

    private static boolean containsField(JsonElement json, String key) {
        if (json.isJsonArray()) {
            for (JsonElement child : json.getAsJsonArray()) if (containsField(child, key)) return true;
        } else if (json.isJsonObject()) {
            if (json.getAsJsonObject().has(key)) return true;
            for (JsonElement child : json.getAsJsonObject().asMap().values()) if (containsField(child, key)) return true;
        }
        return false;
    }

    private static void putGenerated(Map<String, byte[]> generated, String target, byte[] bytes) {
        if (generated.putIfAbsent(OVERLAY + "/" + target, bytes) != null) {
            throw fail("$", "OUTPUT_COLLISION", "Multiple source files map to " + target);
        }
    }

    private static GunPackInput.Opener required(GunPackInput input, String name) throws IOException {
        GunPackInput.Opener opener = input.files.get(name);
        if (opener == null) throw new IOException("Missing " + name + " at pack root");
        return opener;
    }

    private static JsonObject readObject(GunPackInput.Opener source) throws IOException {
        return JsonParser.parseString(new String(readLimited(source), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static byte[] readLimited(GunPackInput.Opener source) throws IOException {
        try (InputStream stream = source.open()) { return readLimited(stream); }
    }

    private static byte[] readLimited(InputStream source) throws IOException {
        byte[] bytes = source.readNBytes(MAX_JSON_BYTES + 1);
        if (bytes.length > MAX_JSON_BYTES) throw new IOException("Data file exceeds 16 MiB conversion limit");
        return bytes;
    }

    private static void writePack(GunPackInput input, JsonObject meta, Map<String, byte[]> generated, Path output) throws IOException {
        for (String name : generated.keySet()) if (input.files.containsKey(name)) throw new IOException("Generated file conflicts with source: " + name);
        Path temporary = Files.createTempFile(output.getParent(), ".universal-", ".zip.tmp");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                for (var entry : input.files.entrySet()) {
                    ZipEntry file = new ZipEntry(entry.getKey());
                    file.setTime(0);
                    zip.putNextEntry(file);
                    if (entry.getKey().equals("gunpack.meta.json")) zip.write(GSON.toJson(meta).getBytes(StandardCharsets.UTF_8));
                    else try (InputStream stream = entry.getValue().open()) { stream.transferTo(zip); }
                    zip.closeEntry();
                }
                for (var entry : generated.entrySet()) {
                    ZipEntry file = new ZipEntry(entry.getKey());
                    file.setTime(0);
                    zip.putNextEntry(file);
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            // No REPLACE_EXISTING: an output created concurrently must also remain untouched.
            Files.move(temporary, output);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Result finish(PackConversionPlan plan, Path report, Path pack) throws IOException {
        // Reports are snapshots; choose a new filename so check/export never destroy an earlier report.
        Path destination = report;
        int sequence = 1;
        while (Files.exists(destination)) destination = report.resolveSibling(report.getFileName().toString().replace(".json", "-" + sequence++ + ".json"));
        Files.writeString(destination, GSON.toJson(plan), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return new Result(plan, destination, pack);
    }

    private static String message(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private static MigrationException fail(String path, String code, String message) { return new MigrationException(path, code, message); }
    private static final class MigrationException extends IllegalArgumentException {
        final String path;
        final String code;
        MigrationException(String path, String code, String message) { super(message); this.path = path; this.code = code; }
    }
}
