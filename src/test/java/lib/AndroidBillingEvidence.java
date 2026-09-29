package lib;

import org.junit.Assert;
import org.openqa.selenium.json.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** UID-bound before/after entitlement checks for test-store purchases. */
public final class AndroidBillingEvidence {
    private final String uid;
    public AndroidBillingEvidence() throws Exception {
        Map<String,Object> baseline=read(null);
        uid=(String)baseline.get("uid");
        Assert.assertEquals("Purchase fixture must initially have no subscription",Boolean.FALSE,baseline.get("active"));
    }
    public void assertActivated() throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
        do {
            Map<String,Object> result=read(uid);
            if(Boolean.TRUE.equals(result.get("active"))) {
                io.qameta.allure.Allure.addAttachment("Android billing entitlement","Same Firebase UID; subscription.active false -> true");
                return;
            }
            Thread.sleep(1000);
        } while(System.nanoTime()<deadline);
        Assert.fail("Test transaction did not activate this account's subscription");
    }
    private Map<String,Object> read(String expected) throws Exception {
        Path output=Files.createTempFile("coach-billing-", ".json");
        List<String> command=new ArrayList<String>(Arrays.asList("python3","scripts/read_android_subscription.py","--serial",AndroidDevice.serial()));
        if(expected!=null) {command.add("--uid");command.add(expected);}
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            Assert.assertTrue("Entitlement reader timed out",process.waitFor(20,TimeUnit.SECONDS));
            Assert.assertEquals("Cannot verify billing entitlement",0,process.exitValue());
            return new Json().toType(new String(Files.readAllBytes(output),StandardCharsets.UTF_8),Map.class);
        } finally {
            if(process.isAlive())process.destroyForcibly();Files.deleteIfExists(output);
        }
    }
}
