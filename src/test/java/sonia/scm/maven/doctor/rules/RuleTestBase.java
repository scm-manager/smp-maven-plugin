package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import sonia.scm.maven.doctor.Context;

public class RuleTestBase {

  protected final ObjectMapper mapper = new ObjectMapper();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  protected MavenProject project;
  protected ObjectNode packageJson;
  protected Context context;

  @BeforeEach
  void setupContext() {
    packageJson = mapper.createObjectNode();
    context = new Context(project, packageJson);
  }

}
