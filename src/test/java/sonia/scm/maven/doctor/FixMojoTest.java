package sonia.scm.maven.doctor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.xml.bind.v2.runtime.reflect.Lister;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.rules.PackageJsonRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, TempDirectory.class})
class FixMojoTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MavenProject project;

  @Test
  void shouldFixAll(@TempDirectory.TempDir Path tempDirectory) throws IOException, MojoExecutionException {
    Path packageJsonPath = tempDirectory.resolve("package.json");
    Files.write(packageJsonPath, "{}".getBytes(StandardCharsets.UTF_8));

    FixMojo mojo = new FixMojo();
    mojo.setProject(project);
    mojo.setPackageJsonFile(packageJsonPath.toFile());
    mojo.setRules(Rules.of(new SpaceshipRule(), new Version42Rule()));
    mojo.execute();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(packageJsonPath.toFile());
    assertThat(node.get("name").asText()).isEqualTo("spaceship");
    assertThat(node.get("version").asText()).isEqualTo("42");
  }

  private static class SpaceshipRule extends PackageJsonRule {

    @Override
    protected Result validate(MavenProject project, ObjectNode packageJson) {
      return Result.warn("name is not spaceship").withFix(() -> packageJson.put("name", "spaceship")).build();
    }
  }

  private static class Version42Rule extends PackageJsonRule {

    @Override
    protected Result validate(MavenProject project, ObjectNode packageJson) {
      return Result.warn("name is not 42").withFix(() -> packageJson.put("version", "42")).build();
    }
  }

}
