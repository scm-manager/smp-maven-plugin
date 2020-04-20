package sonia.scm.maven.doctor.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UiPluginsVersionRuleTest extends RuleTestBase {

  @BeforeEach
  void setParentVersion() {
    when(project.getParent().getVersion()).thenReturn("2.0.0");
  }

  @Test
  void shouldReturnOkWithoutDependencies() {
    Result result = new UiPluginsVersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnOkWithoutUiPlugins() {
    packageJson.with("dependencies");
    Result result = new UiPluginsVersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnOkWithEqualVersion() {
    packageJson.with("dependencies").put("@scm-manager/ui-plugins", "2.0.0");

    Result result = new UiPluginsVersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnOkForSnapshotVersion() {
    when(project.getParent().getVersion()).thenReturn("2.0.0-SNAPSHOT");
    packageJson.with("dependencies").put("@scm-manager/ui-plugins", "latest");

    Result result = new UiPluginsVersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnFixableWarn() {
    packageJson.with("dependencies").put("@scm-manager/ui-plugins", "1.0.0");

    Result result = new UiPluginsVersionRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.WARN);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldFix() {
    packageJson.with("dependencies").put("@scm-manager/ui-plugins", "1.0.0");

    Result result = new UiPluginsVersionRule().validate(context);
    result.fix();

    assertThat(packageJson.get("dependencies").get("@scm-manager/ui-plugins").asText()).isEqualTo("2.0.0");
  }
}
