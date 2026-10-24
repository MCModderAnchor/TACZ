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
    public final boolean gameValidated = false;
    public String output;
    public final List<Diagnostic> diagnostics = new ArrayList<>();
    public final List<ChangedField> changedFields = new ArrayList<>();
    public final List<String> generatedFiles = new ArrayList<>();
    private transient String currentFile = "";
    private final transient Set<String> legacyTags = new TreeSet<>();

    public PackConversionPlan(String source) {
        this.source = source;
    }

    public void error(String file, String path, String code, String message) {
        diagnostics.add(new Diagnostic("error", file, path, code, message));
    }

    public void warn(String file, String path, String code, String message) {
        diagnostics.add(new Diagnostic("warning", file.isEmpty() ? currentFile : file, path, code, message));
    }

    public void beginFile(String file) { currentFile = file; }

    public void converted(String path, String action) {
        convertedFields++;
        changedFields.add(new ChangedField(currentFile, path, action));
    }

    public void requireLegacyTag(String id) { legacyTags.add(id); }
    public Set<String> legacyTags() { return Set.copyOf(legacyTags); }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity.equals("error"));
    }

    public record Diagnostic(String severity, String file, String path, String code, String message) { }
    public record ChangedField(String file, String path, String action) { }
}
