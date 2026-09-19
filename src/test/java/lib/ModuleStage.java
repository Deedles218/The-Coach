package lib;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Identity within a program; stage numbers alone repeat in every module. */
public final class ModuleStage {
    private static final Pattern STAGE = Pattern.compile("Stage ([1-9][0-9]*) of ([1-9][0-9]*)");
    private static final Pattern MODULE = Pattern.compile("Module ([1-9][0-9]*):\\s*\\S.*");
    public final int module, stage, total;

    public ModuleStage(int module, int stage, int total) {
        if (module < 1 || stage < 1 || stage > total) {
            throw new IllegalArgumentException("Invalid module/stage boundary");
        }
        this.module = module;
        this.stage = stage;
        this.total = total;
    }

    public static ModuleStage parse(String moduleText, String stageText) {
        Matcher module = MODULE.matcher(moduleText == null ? "" : moduleText.trim());
        Matcher stage = STAGE.matcher(stageText == null ? "" : stageText.trim());
        if (!module.matches() || !stage.matches()) {
            throw new AssertionError("Expected a nonempty Module title and Stage X of Y (not Day)");
        }
        return new ModuleStage(Integer.parseInt(module.group(1)), Integer.parseInt(stage.group(1)),
                Integer.parseInt(stage.group(2)));
    }

    public ModuleStage at(int target) { return new ModuleStage(module, target, total); }
    @Override public boolean equals(Object other) {
        if (!(other instanceof ModuleStage)) return false;
        ModuleStage that = (ModuleStage) other;
        return module == that.module && stage == that.stage && total == that.total;
    }
    @Override public int hashCode() { return Objects.hash(module, stage, total); }
    @Override public String toString() { return "Module " + module + "; Stage " + stage + " of " + total; }
}
