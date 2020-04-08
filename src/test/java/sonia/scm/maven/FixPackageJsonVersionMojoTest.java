package sonia.scm.maven;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(TempDirectory.class)
class FixPackageJsonVersionMojoTest {

  Path packageJson;

  @BeforeEach
  void initPackageJson(@TempDirectory.TempDir Path temp) {
    packageJson = temp.resolve("package.json");
  }

  @Test
  void shouldDoNothingWithoutPackageJson(@TempDirectory.TempDir Path temp) throws MojoFailureException, IOException {
    FixPackageJsonVersionMojo mojo = new FixPackageJsonVersionMojo();
    MavenProject project = new MavenProject();
    project.setFile(temp.resolve("pom.xml").toFile());
    project.setVersion("2.1.0");
    mojo.setProject(project);
    mojo.execute();

    Assertions.assertThat(Files.exists(packageJson)).isFalse();
  }

  @Test
  void shouldIgnoreFileWithCorrectVersion(@TempDirectory.TempDir Path temp) throws MojoFailureException, IOException {
    URL resource = Resources.getResource("package.json.correctVersion");
    ByteSource byteSource = Resources.asByteSource(resource);
    byteSource.copyTo(Files.newOutputStream(packageJson));
    String originalString = Resources.toString(resource, Charset.defaultCharset());
    FixPackageJsonVersionMojo mojo = new FixPackageJsonVersionMojo();
    MavenProject project = new MavenProject();
    project.setFile(temp.resolve("pom.xml").toFile());
    project.setVersion("2.1.0");
    mojo.setProject(project);
    mojo.execute();

    Assertions.assertThat(packageJson).hasContent(originalString);
  }

  @Test
  void shouldSetCorrectVersion(@TempDirectory.TempDir Path temp) throws MojoFailureException, IOException {
    ByteSource byteSource = Resources.asByteSource(Resources.getResource("package.json.incorrectVersion"));
    byteSource.copyTo(Files.newOutputStream(packageJson));
    FixPackageJsonVersionMojo mojo = new FixPackageJsonVersionMojo();
    MavenProject project = new MavenProject();
    project.setFile(temp.resolve("pom.xml").toFile());
    project.setVersion("2.1.0");
    mojo.setProject(project);
    mojo.execute();

    Assertions.assertThat(Files.readAllLines(packageJson)).contains("  \"version\" : \"2.1.0\",");
  }
}
