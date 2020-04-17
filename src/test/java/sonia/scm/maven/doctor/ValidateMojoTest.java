package sonia.scm.maven.doctor;

import com.google.common.base.Charsets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TempDirectory.class})
class ValidateMojoTest {

  @Mock
  private MavenProject project;

  private File packageJson;

  @BeforeEach
  void createPackageJsonFile(@TempDirectory.TempDir Path tempDir) {
    packageJson = tempDir.resolve("package.json").toFile();
  }

  @Test
  void shouldPassContextWithoutPackageJson() throws MojoFailureException, MojoExecutionException {
    Context context = executeAndCaptureContext();
    assertThat(context.getPackageJson()).isEmpty();
  }

  @Test
  void shouldPassContextWithPackageJson() throws MojoFailureException, MojoExecutionException, IOException {
    Files.write(packageJson.toPath(), "{}".getBytes(Charsets.UTF_8));

    Context context = executeAndCaptureContext();
    assertThat(context.getPackageJson()).isPresent();
  }

  private Context executeAndCaptureContext() throws MojoExecutionException {
    Rule mockedRule = mock(Rule.class);
    when(mockedRule.validate(any())).thenReturn(Result.ok("ok"));
    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

    ValidateMojo mojo = createMojo();
    mojo.setRules(Rules.of(mockedRule));
    mojo.execute();

    verify(mockedRule).validate(contextCaptor.capture());

    return contextCaptor.getValue();
  }

  @Test
  @SuppressWarnings("java:S2699") // should not fail
  void shouldNotFail() throws MojoExecutionException {
    ValidateMojo mojo = createMojo(
      Result.ok("fine")
    );
    mojo.execute();
  }

  @Test
  @SuppressWarnings("java:S2699") // should not fail
  void shouldNotFailForWithWarnings() throws MojoExecutionException {
    ValidateMojo mojo = createMojo(
      Result.ok("fine"),
      Result.warn("not so good").build(),
      Result.warn("not so good, but fixable").withFix(() -> {}).build()
    );
    mojo.execute();
  }

  @Test
  void shouldFailIfOneRuleFails() {
    ValidateMojo mojo = createMojo(
      Result.ok("fine"),
      Result.warn("not ok").build(),
      Result.error("fail").build()
    );

    assertThrows(MojoExecutionException.class, () -> mojo.execute());
  }

  private ValidateMojo createMojo(Result... results) {
    ValidateMojo mojo = new ValidateMojo();
    mojo.setProject(project);
    mojo.setPackageJsonFile(packageJson);
    List<Rule> simpleRules = Arrays.stream(results)
      .map(SimpleRule::new)
      .collect(Collectors.toList());
    mojo.setRules(Rules.of(simpleRules));
    return mojo;
  }

  private static class SimpleRule implements Rule {

    private final Result result;

    private SimpleRule(Result result) {
      this.result = result;
    }

    @Override
    public Result validate(Context context) {
      return result;
    }
  }
}
