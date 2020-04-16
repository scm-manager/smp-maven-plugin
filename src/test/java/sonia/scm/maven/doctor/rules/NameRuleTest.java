package sonia.scm.maven.doctor.rules;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NameRuleTest extends RuleTestBase {

  @Test
  void shouldReturnOk() {
    when(project.getName()).thenReturn("heart-of-gold");
    packageJson.put("name", "heart-of-gold");

    Result result = new NameRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnOkForNameWithScope() {
    when(project.getName()).thenReturn("heart-of-gold");
    packageJson.put("name", "@hitchhiker/heart-of-gold");

    Result result = new NameRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnFixableError() {
    when(project.getName()).thenReturn("heart-of-gold");
    packageJson.put("name", "puzzle-42");

    Result result = new NameRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.ERROR);
    assertThat(result.isFixable()).isTrue();
  }

  @Test
  void shouldFix() {
    when(project.getName()).thenReturn("heart-of-gold");
    packageJson.put("name", "puzzle-42");

    new NameRule().validate(context).fix();
    Assertions.assertThat(packageJson.get("name").asText()).isEqualTo("@scm-manager/heart-of-gold");
  }
}
