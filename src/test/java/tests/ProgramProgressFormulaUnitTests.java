package tests;

import lib.ProgramSelectionState;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;

/** User formula from 25.11.2026: min(round(100 * (day - 1) / total_days), 99). */
public class ProgramProgressFormulaUnitTests {
    private static final Object[][] PROGRAMS = {
        {"sex_skill_app",35,97}, {"sexting_woman",16,94}, {"menopause_for_her",16,94},
        {"art_of_attraction",12,92}, {"kegel_journey",60,98}, {"sex_skill_forher_lgbt",30,97},
        {"for_her",35,97}, {"sex_skill_man",30,97}, {"navigating_arguments_man",20,95},
        {"sexting_man",14,93}, {"keep_it_hard",60,98}, {"kegel_only",70,99},
        {"overall_health",15,93}, {"last_longer",60,98}
    };
    private Map<String,Object> row(String id,int day,int total,String status,int percent) {
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("program_id",id);row.put("current_program_day",day);row.put("total_program_stages",total);
        row.put("status",status);row.put("progress_percent",percent);
        row.put("current_stage",1);row.put("total_stages",1);
        return row;
    }
    private ProgramSelectionState state(Map<String,Object> main,List<Map<String,Object>> secondary) {
        Map<String,Object> state=new LinkedHashMap<>();state.put("main",main);state.put("secondary",secondary);
        return new ProgramSelectionState(state);
    }
    private void rejected(Runnable check,String message) {
        try {check.run();Assert.fail("Invalid progress was accepted");}
        catch(AssertionError failure) {Assert.assertTrue(failure.getMessage(),failure.getMessage().contains(message));}
    }
    @Test public void allFourteenProgramsHaveZeroOnDayOneAndTheirCalculatedFinalDayPercentage() {
        for(Object[] p:PROGRAMS) {
            String id=(String)p[0];int total=(Integer)p[1],last=(Integer)p[2];
            Assert.assertEquals(id,0,ProgramSelectionState.calculateProgressPercent(1,total));
            Assert.assertEquals(id,last,ProgramSelectionState.calculateProgressPercent(total,total));
            state(row(id,total,total,"in_progress",last),Collections.emptyList()).assertAllProgressFormulas();
        }
    }
    @Test public void halfValuesUsePythonTiesToEvenInsteadOfMathRound() {
        Assert.assertEquals(0,ProgramSelectionState.calculateProgressPercent(2,200));
        Assert.assertEquals(2,ProgramSelectionState.calculateProgressPercent(2,40));
        Assert.assertEquals(8,ProgramSelectionState.calculateProgressPercent(4,40));
        Assert.assertEquals(12,ProgramSelectionState.calculateProgressPercent(6,40));
        Assert.assertEquals(18,ProgramSelectionState.calculateProgressPercent(8,40));
    }
    @Test public void unfinishedProgressIsCappedAtNinetyNine() {
        Assert.assertEquals(99,ProgramSelectionState.calculateProgressPercent(61,60));
        Assert.assertEquals(99,ProgramSelectionState.calculateProgressPercent(200,60));
        Assert.assertEquals(99,ProgramSelectionState.calculateProgressPercent(2,1));
    }
    @Test public void completedProgramsShowOneHundredAndDoNotRequireAModuleCounter() {
        for(Object[] p:PROGRAMS) {
            String id=(String)p[0];int total=(Integer)p[1];Map<String,Object> row=row(id,total,total,"completed",100);
            row.put("current_stage",null);row.put("total_stages",null);
            ProgramSelectionState state=state(row,Collections.emptyList());state.assertAllProgressFormulas();
            Assert.assertEquals("Completed",state.progressLabel(id));
            state.assertDisplayedPercent(id,"100 %");
        }
    }
    @Test public void moduleStageCannotReplaceTheAbsoluteDayForEitherRole() {
        ProgramSelectionState state=state(row("main",60,60,"in_progress",98),
                Arrays.asList(row("secondary",15,15,"in_progress",93),row("done",14,14,"completed",100)));
        state.assertAllProgressFormulas();state.assertDisplayedPercent("main","98 %");
        state.program("secondary").put("progress_percent",99);
        rejected(state::assertAllProgressFormulas,"differs from day formula: secondary");
    }
    @Test public void comparingUiWithTheSameIncorrectApiValueDoesNotProduceAPass() {
        ProgramSelectionState state=state(row("kegel_journey",60,60,"in_progress",99),Collections.emptyList());
        rejected(()->state.assertDisplayedPercent("kegel_journey","99%"),"differs from day formula");
        state.program("kegel_journey").put("status","completed");
        rejected(state::assertAllProgressFormulas,"differs from day formula");
    }
    @Test public void missingFractionalAndNonpositiveDayCountsAreRejected() {
        for(Object invalid:Arrays.asList(null,0,1.5)) {
            Map<String,Object> row=row("main",2,60,"in_progress",2);row.put("current_program_day",invalid);
            rejected(()->state(row,Collections.emptyList()).assertAllProgressFormulas(),
                    invalid==null?"Absolute day":"positive integral");
        }
        rejected(()->ProgramSelectionState.calculateProgressPercent(1,0),"total_days");
    }
}
