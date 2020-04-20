package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Context;
import sonia.scm.maven.doctor.Result;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PackageJsonRuleTest {

  @Mock
  private MavenProject project;

  @Test
  void shouldReturnOkWithoutPackageJson() {
    Result result = new SampleRule().validate(new Context(project));
    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldReturnWarnWithPackageJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode packageJson = mapper.createObjectNode();

    Context context = new Context(project, packageJson);
    Result result = new SampleRule().validate(context);
    assertThat(result.getType()).isEqualTo(Result.Type.WARN);
  }

  static class SampleRule extends PackageJsonRule {
    @Override
    protected Result validate(MavenProject project, ObjectNode jsonNodes) {
      return Result.warn("no").build();
    }
  }

}
