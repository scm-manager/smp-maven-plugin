package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Result;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MissingPostInstallRuleTest extends RuleTestBase {

  @Test
  void shouldReturnWarnWithoutScripts() {
    Result result = new MissingPostInstallRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.WARN);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldReturnWarnWithoutPostInstall() {
    packageJson.set("scripts", mapper.createObjectNode());

    Result result = new MissingPostInstallRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.WARN);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldReturnWarnWithOtherPostInstall() {
    ObjectNode scripts = mapper.createObjectNode();
    scripts.put("postinstall", "awesome");
    packageJson.set("scripts", scripts);

    Result result = new MissingPostInstallRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.WARN);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldReturnOk() {
    ObjectNode scripts = mapper.createObjectNode();
    scripts.put("postinstall", "ui-plugins postinstall");
    packageJson.set("scripts", scripts);

    Result result = new MissingPostInstallRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldFix() {
    ObjectNode scripts = mapper.createObjectNode();
    scripts.put("postinstall", "awesome");
    packageJson.set("scripts", scripts);

    Result result = new MissingPostInstallRule().validate(context);
    result.fix();
    assertThat(scripts.get("postinstall").asText()).isEqualTo("ui-plugins postinstall");
  }

  @Test
  void shouldFixWithoutScripts() {
    Result result = new MissingPostInstallRule().validate(context);
    result.fix();
    assertThat(packageJson.get("scripts").get("postinstall").asText()).isEqualTo("ui-plugins postinstall");
  }

}
