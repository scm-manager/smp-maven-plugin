package sonia.scm.maven.doctor.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionRuleTest extends RuleTestBase {

  @Test
  void shouldReturnOk() {
    when(project.getVersion()).thenReturn("2.0.0");
    packageJson.put("version", "2.0.0");

    Result result = new VersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnOkWithoutPackageJsonVersion() {
    when(project.getVersion()).thenReturn("2.0.0");

    Result result = new VersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldWarnWithFixable() {
    when(project.getVersion()).thenReturn("2.0.0");
    packageJson.put("version", "1.0.0");

    Result result = new VersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.ERROR);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldFix() {
    when(project.getVersion()).thenReturn("2.0.0");
    packageJson.put("version", "1.0.0");

    Result result = new VersionRule().validate(context);
    result.fix();
    assertThat(packageJson.get("version").asText()).isEqualTo("2.0.0");
  }
}
