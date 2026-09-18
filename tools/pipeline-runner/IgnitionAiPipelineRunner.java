package tools.pipeline_runner;

import com.ignitionai.application.*;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.CertificateValidator;
import java.io.File;
import java.nio.file.*;

/** Runs the same service as the native apps. */
public final class IgnitionAiPipelineRunner {
    public static void main(String[] args) throws Exception {
        var messages = args.length == 0
            ? new ObdGenerator(ScenarioFile.parse(ScenarioFile.example())).generate().getPublicStream()
            : SessionFiles.read(Path.of(args[0]));
        var result = new InspectionService(new File("virtual-sensors/src/main/resources/sensors")).assess(messages);
        var validation = new CertificateValidator().validateSnapshot(result.certificate);
        if (!validation.isValid()) throw new IllegalStateException(validation.getErrors().toString());
        Path output = Path.of("out/certificates"); Files.createDirectories(output);
        Files.writeString(output.resolve(result.certificate.getCertificateId() + ".tex"), new LatexGenerator().generateCertificate(result.certificate));
        System.out.println("Assessment: " + result.certificate.getCertificateId() + "; records=" + messages.size() + "; episodes=" + result.episodes.size());
        result.notes.forEach(System.out::println);
    }
}
