package sonia.scm.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith({MockitoExtension.class, TempDirectory.class})
class CopyPluginXmlMojoTest {

  @Mock
  private MavenProject project;

  @Mock
  private MavenSession session;

  @Mock
  private MavenFileFilter fileFilter;

  private final List<String> filters = Collections.emptyList();

  @Test
  void shouldCopyPluginXml(@TempDirectory.TempDir Path tempPath) throws IOException, MojoExecutionException, MavenFilteringException {
    CopyPluginXmlMojo mojo = createMojo(tempPath);
    mojo.getSource().createNewFile();
    mojo.execute();

    assertThat(mojo.getOutputDirectory()).exists();
    assertCopied(mojo);
  }

  @Test
  void shouldNotCopyPluginXml(@TempDirectory.TempDir Path tempPath) throws IOException, MojoExecutionException {
    CopyPluginXmlMojo mojo = createMojo(tempPath);
    mojo.getOutputDirectory().mkdirs();
    mojo.getTarget().createNewFile();
    mojo.getSource().createNewFile();

    mojo.execute();
    verifyZeroInteractions(fileFilter);
  }

  @Test
  void shouldCopyPluginXmlAndDeleteOutputDirectory(@TempDirectory.TempDir Path tempPath) throws IOException, MojoExecutionException, MavenFilteringException {
    CopyPluginXmlMojo mojo = createMojo(tempPath);

    File outputDirectory = mojo.getOutputDirectory();
    outputDirectory.mkdirs();

    File other = new File(outputDirectory, "other");
    other.createNewFile();

    File target = mojo.getTarget();
    target.createNewFile();
    target.setLastModified(42L);

    mojo.getSource().createNewFile();

    mojo.execute();

    assertThat(outputDirectory).exists();
    assertThat(other).doesNotExist();
    assertCopied(mojo);
  }

  private void assertCopied(CopyPluginXmlMojo mojo) throws MavenFilteringException {
    verify(fileFilter).copyFile(mojo.getSource(), mojo.getTarget(), true, project, filters, true, "UTF-8", session);
  }

  private CopyPluginXmlMojo createMojo(Path tempPath) {
    CopyPluginXmlMojo mojo = new CopyPluginXmlMojo();
    mojo.setBuildFilters(Collections.emptyList());
    mojo.setFileFilter(fileFilter);

    File tempDir = tempPath.toFile();

    File outputDirectory = new File(tempDir, "out");
    mojo.setOutputDirectory(outputDirectory);

    File target = new File(outputDirectory, "plugin.xml");
    mojo.setTarget(target);

    File source = new File(tempDir, "plugin.xml");
    mojo.setSource(source);

    mojo.setProject(project);
    mojo.setSession(session);

    return mojo;
  }

}
