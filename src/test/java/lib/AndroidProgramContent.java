package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.json.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class AndroidProgramContent {
    private AndroidProgramContent() { }
    public static Map<String,Object> read(String program,long boundary) throws Exception {
        Path folder=Files.createTempDirectory(Paths.get("target"),"android-selector-content-");
        Path output=folder.resolve("content.json"), log=folder.resolve("reader.log");
        Process reader=new ProcessBuilder("python3","scripts/read_android_program_content.py","--serial",AndroidDevice.serial(),
                "--program",program,"--boundary",Long.toString(boundary),"--output",output.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            Assert.assertTrue("Android catalog reader timed out",reader.waitFor(40,TimeUnit.SECONDS));
            Assert.assertEquals("Android catalog verification failed; see "+log,0,reader.exitValue());
            String json=new String(Files.readAllBytes(output),StandardCharsets.UTF_8);
            Allure.addAttachment("Android destination catalog", "application/json",json,"json");
            return new Json().toType(json,Map.class);
        } finally { if(reader.isAlive()) reader.destroyForcibly(); }
    }
}
