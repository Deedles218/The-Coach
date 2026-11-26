package lib;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server oracle; switching may change the active flag but must preserve per-program progress. */
public final class ProgramSelectionState {
    private final Map<String, Object> value;

    public ProgramSelectionState(Map<String, Object> value) {
        Assert.assertNotNull("Program selection observation is absent", value);
        this.value = value;
    }

    public Map<String, Object> main() { return (Map<String, Object>) value.get("main"); }
    public List<Map<String, Object>> secondary() { return (List<Map<String, Object>>) value.get("secondary"); }
    public String active() { return (String) value.get("active"); }
    public String mainId() { return main() == null ? null : (String) main().get("program_id"); }
    public Map<String, Object> raw() { return value; }

    public Set<String> secondaryIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : secondary()) {
            Assert.assertTrue("Duplicate Secondary program_id", ids.add((String) row.get("program_id")));
        }
        return ids;
    }

    public List<Map<String, Object>> programs() {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (main() != null) rows.add(main());
        rows.addAll(secondary());
        return rows;
    }

    public Map<String, Object> program(String id) {
        return programs().stream().filter(p -> id.equals(p.get("program_id"))).findFirst()
                .orElseThrow(() -> new AssertionError("Expected program is absent: " + id));
    }

    public void assertRoles(String main, List<String> secondary) {
        Assert.assertEquals("Wrong Main Journey", main, mainId());
        Assert.assertEquals("Wrong Secondary Programs", new LinkedHashSet<>(secondary), secondaryIds());
        Assert.assertEquals("Expected Secondary IDs must be unique", secondary.size(), secondaryIds().size());
        Assert.assertFalse("Main Journey must not be a Secondary", secondaryIds().contains(main));
        if (main != null) Assert.assertEquals("Wrong Main membership", "main_journey", main().get("membership_status"));
        for (Map<String, Object> row : secondary()) {
            Assert.assertEquals("Wrong Secondary membership", "secondary", row.get("membership_status"));
        }
    }

    public void assertActive(String id) {
        Assert.assertEquals("Wrong server Active Program", id, active());
        int marked = 0;
        for (Map<String, Object> row : programs()) {
            boolean selected = Boolean.TRUE.equals(row.get("is_active"));
            Assert.assertEquals("Server active flag disagrees with program ID", id.equals(row.get("program_id")), selected);
            if (selected) marked++;
        }
        Assert.assertEquals("Exactly one program must be active", 1, marked);
    }

    public void assertSameRoles(ProgramSelectionState other) {
        assertRoles(other.mainId(), new ArrayList<>(other.secondaryIds()));
    }

    public Map<String, Object> progress(String id) {
        Map<String, Object> row = program(id), result = new LinkedHashMap<>();
        for (String key : Arrays.asList("status", "current_module_name", "current_stage", "total_stages",
                "total_program_stages", "current_program_day", "progress_percent")) {
            Assert.assertTrue("Progress field is missing: " + key, row.containsKey(key)
                    || "total_program_stages".equals(key) || "current_program_day".equals(key));
            if (row.containsKey(key)) result.put(key, row.get(key));
        }
        return Collections.unmodifiableMap(result);
    }

    public void assertSameProgress(ProgramSelectionState before) {
        for (Map<String, Object> row : before.programs()) {
            String id = (String) row.get("program_id");
            Assert.assertEquals("Switching changed progress: " + id, before.progress(id), progress(id));
        }
    }

    public void assertOnlyProgramRestarted(ProgramSelectionState before, String id) {
        Assert.assertEquals("Reset changed the backend account", before.value.get("backend_identity"),
                value.get("backend_identity"));
        assertSameRoles(before);
        assertActive(before.active());
        Map<String, Object> restarted = program(id);
        Assert.assertEquals("Reset did not return the program to in_progress", "in_progress", restarted.get("status"));
        Assert.assertEquals("Reset changed program duration", before.program(id).get("total_program_stages"),
                restarted.get("total_program_stages"));
        Assert.assertTrue("Reset did not return to absolute Day 1", restarted.get("current_program_day") instanceof Number
                && ((Number) restarted.get("current_program_day")).doubleValue() == 1);
        Assert.assertTrue("Reset did not clear percentage", restarted.get("progress_percent") instanceof Number
                && ((Number) restarted.get("progress_percent")).doubleValue() == 0);
        for (Map<String, Object> row : before.programs()) {
            String other = (String) row.get("program_id");
            if (!id.equals(other)) Assert.assertEquals("Reset changed another program: " + other,
                    before.progress(other), progress(other));
        }
        assertAllProgressFormulas();
    }

    public void assertDisplayedPercent(String id, String displayed) {
        assertProgressFormula(id);
        displayed = displayed == null ? null : displayed.replaceAll("[\\s\u00a0]+", "");
        Assert.assertTrue("Displayed progress is not a percentage", displayed != null
                && displayed.trim().matches("[0-9]+(?:\\.[0-9]+)?%?"));
        Assert.assertEquals("Displayed percentage differs from server: " + id,
                ((Number) program(id).get("progress_percent")).doubleValue(),
                Double.parseDouble(displayed.replace("%", "").trim()), .01);
    }

    /** Python round uses ties-to-even; Math.round would incorrectly round 2.5 to 3. */
    public static int calculateProgressPercent(int day, int totalDays) {
        Assert.assertTrue("Program day must be positive", day >= 1);
        Assert.assertTrue("Program total_days must be positive", totalDays >= 1);
        return Math.min((int) Math.rint(100.0 * (day - 1) / totalDays), 99);
    }

    public void assertProgressFormula(String id) {
        Map<String, Object> row = program(id);
        Object percent = row.get("progress_percent");
        Assert.assertTrue("Program progress_percent must be numeric: " + id, percent instanceof Number);
        int expected;
        if ("completed".equals(row.get("status"))) {
            expected = 100;
        } else {
            Object day = row.get("current_program_day"), total = row.get("total_program_stages");
            Assert.assertTrue("Absolute day and total_days are required for progress calculation: " + id,
                    day instanceof Number && total instanceof Number);
            double current = ((Number) day).doubleValue(), count = ((Number) total).doubleValue();
            Assert.assertTrue("Progress calculation requires positive integral day counts: " + id,
                    current == Math.rint(current) && count == Math.rint(count)
                            && current >= 1 && current <= Integer.MAX_VALUE
                            && count >= 1 && count <= Integer.MAX_VALUE);
            expected = calculateProgressPercent((int) current, (int) count);
        }
        Assert.assertEquals("Program percentage differs from day formula: " + id,
                expected, ((Number) percent).doubleValue(), .001);
    }

    public void assertAllProgressFormulas() {
        Assert.assertFalse("Prepare at least one saved program for progress checks", programs().isEmpty());
        for (Map<String, Object> row : programs()) assertProgressFormula((String) row.get("program_id"));
    }

    /** Absolute day remains a progress invariant even for programs displayed as stages. */
    public String dayLabel(String id) {
        Map<String, Object> row = program(id);
        Object day = row.get("current_program_day"), total = row.get("total_program_stages");
        Assert.assertTrue("current_program_day and total_program_stages are required for UI v3: " + id,
                day instanceof Number && total instanceof Number);
        double current = ((Number) day).doubleValue(), count = ((Number) total).doubleValue();
        Assert.assertTrue("Invalid program day: " + id, current == Math.rint(current)
                && count == Math.rint(count) && current >= 1 && current <= count);
        return "Day " + (int) current + " of " + (int) count;
    }

    /** The reviewed fixture declares the unit; module metadata alone cannot identify it. */
    public static String progressUnit(String id) {
        String unit = System.getProperty("coach.programProgress.unit." + id);
        Assert.assertTrue("Configure coach.programProgress.unit." + id + " as Day or Stage",
                "Day".equalsIgnoreCase(unit) || "Stage".equalsIgnoreCase(unit));
        return "Day".equalsIgnoreCase(unit) ? "Day" : "Stage";
    }

    public String progressLabel(String id) {
        return "completed".equals(program(id).get("status")) ? "Completed" : progressLabel(id, progressUnit(id));
    }

    public String progressLabel(String id, String unit) {
        if ("completed".equals(program(id).get("status"))) return "Completed";
        if ("Day".equalsIgnoreCase(unit)) return dayLabel(id);
        Assert.assertTrue("Program progress unit must be Day or Stage", "Stage".equalsIgnoreCase(unit));
        Object stage = program(id).get("current_stage"), total = program(id).get("total_stages");
        Assert.assertTrue("current_stage and total_stages are required: " + id,
                stage instanceof Number && total instanceof Number);
        double current = ((Number) stage).doubleValue(), count = ((Number) total).doubleValue();
        Assert.assertTrue("Invalid program stage: " + id, current == Math.rint(current)
                && count == Math.rint(count) && current >= 1 && current <= count);
        return "Stage " + (int) current + " of " + (int) count;
    }

    public void assertExactlyEquals(ProgramSelectionState before) {
        Assert.assertEquals("Program state changed unexpectedly", before.value, value);
    }
}
