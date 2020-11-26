package sonia.scm.maven.doctor.rules;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.maven.doctor.Context;
import sonia.scm.maven.doctor.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidPluginsJsonRuleTest {

  @Mock
  Context context;
  @Mock
  MavenProject project;
  @Mock
  Resource resource;

  @BeforeEach
  void mockContext(@TempDir Path temp) {
    when(context.getProject()).thenReturn(project);
    when(project.getResources()).thenReturn(singletonList(resource));
    when(resource.getDirectory()).thenReturn(temp.toAbsolutePath().toString());
  }

  @Test
  void shouldFailWithCorruptJson(@TempDir Path temp) throws IOException {
    writeJsonForLanguage(temp, "en", "{\"valid\": true}");
    writeJsonForLanguage(temp, "de", "{invalid: true}");

    Result result = new ValidPluginsJsonRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.ERROR);
  }

  @Test
  void shouldSucceedWithValidJson(@TempDir Path temp) throws IOException {
    writeJsonForLanguage(temp, "en", "{\"valid\": true}");
    writeJsonForLanguage(temp, "de", "{\"valid\": \"too\"}");

    Result result = new ValidPluginsJsonRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldHandleMissingLocalesDirectory() {
    Result result = new ValidPluginsJsonRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldHandleMissingLanguageDirectory(@TempDir Path temp) throws IOException {
    Files.createDirectories(temp.resolve("locales"));

    Result result = new ValidPluginsJsonRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  @Test
  void shouldHandleMissingPluginsJsonFile(@TempDir Path temp) throws IOException {
    Files.createDirectories(temp.resolve("locales").resolve("de"));

    Result result = new ValidPluginsJsonRule().validate(context);

    assertThat(result.getType()).isEqualTo(Result.Type.OK);
  }

  private void writeJsonForLanguage(Path temp, String language, String json) throws IOException {
    Path de = temp.resolve("locales").resolve(language);
    Files.createDirectories(de);
    Path pluginsJson = de.resolve("plugins.json");
    Files.write(pluginsJson, singletonList(json));
  }
}
