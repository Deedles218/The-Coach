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
                "total_program_stages", "progress_percent")) {
            Assert.assertTrue("Progress field is missing: " + key, row.containsKey(key)
                    || "total_program_stages".equals(key));
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

    public void assertDisplayedPercent(String id, String displayed) {
        Assert.assertTrue("Displayed progress is not a percentage", displayed != null
                && displayed.trim().matches("[0-9]+(?:\\.[0-9]+)?%?"));
        Assert.assertEquals("Displayed percentage differs from server: " + id,
                ((Number) program(id).get("progress_percent")).doubleValue(),
                Double.parseDouble(displayed.replace("%", "").trim()), .01);
    }

    public void assertExactlyEquals(ProgramSelectionState before) {
        Assert.assertEquals("Program state changed unexpectedly", before.value, value);
    }
}
