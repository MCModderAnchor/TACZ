package com.tacz.guns.resource.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Serializable report. Game validation is explicitly separate from format validation. */
public final class PackConversionPlan {
    public final String sourceVersion = "1.20.1";
    public final List<String> targetVersions = List.of("1.20.1", "1.21.1");
    public final String source;
    public int baseFiles;
    public int overlayFiles;
    public int convertedFields;
    public boolean targetFormatValidated;
    public boolean registryValidationDeferred;
    public String status = "checking";
    public String rulesSource;
    public int convertedRecipes;
    public int skippedRecipes;
    public final Set<String> missingItemIds = new TreeSet<>();
    public final List<SkippedFile> skippedFiles = new ArrayList<>();
    public final boolean gameValidated = false;
    public String output;
    public final List<Diagnostic> diagnostics = new ArrayList<>();
    public final List<ChangedField> changedFields = new ArrayList<>();
    public final List<String> generatedFiles = new ArrayList<>();
    private transient String currentFile = "";
    private final transient Set<String> deferredWarnings = new TreeSet<>();
    private transient int fileChangeStart;

    public PackConversionPlan(String source) {
        this.source = source;
    }

    public void error(String file, String path, String code, String message) {
        diagnostics.add(new Diagnostic("error", file, path, code, message));
    }

    public void warn(String file, String path, String code, String message) {
        diagnostics.add(new Diagnostic("warning", file.isEmpty() ? currentFile : file, path, code, message));
    }

    public void beginFile(String file) {
        currentFile = file;
        fileChangeStart = changedFields.size();
    }

    public void converted(String path, String action) {
        convertedFields++;
        changedFields.add(new ChangedField(currentFile, path, action));
    }

    public void skipRecipe(String file, String path, String code, String message) {
        convertedFields -= changedFields.size() - fileChangeStart;
        changedFields.subList(fileChangeStart, changedFields.size()).clear();
        skippedRecipes++;
        skippedFiles.add(new SkippedFile(file, path, code, message));
        diagnostics.add(new Diagnostic("skipped", file, path, code, message));
    }

    public void deferItem(String id, String path) {
        registryValidationDeferred = true;
        missingItemIds.add(id);
        if (deferredWarnings.add(currentFile + "\n" + path + "\n" + id)) {
            warn("", path, "MISSING_ITEM_DEFERRED", "Preserved item ID not installed in this environment: " + id);
        }
    }

    public boolean isPartial() { return skippedRecipes > 0; }

    public void finish() {
        status = hasErrors() ? "failed" : isPartial() ? "partial" : "complete";
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity.equals("error"));
    }

    public record Diagnostic(String severity, String file, String path, String code, String message) { }
    public record ChangedField(String file, String path, String action) { }
    public record SkippedFile(String file, String path, String code, String message) { }
}
