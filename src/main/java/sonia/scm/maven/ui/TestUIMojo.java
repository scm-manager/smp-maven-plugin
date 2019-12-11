package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "test-ui")
public class TestUIMojo extends AbstractUIMojo {
  @Parameter( property = "skipTests")
  private boolean skipTests;
  private static final Logger LOG = LoggerFactory.getLogger(TestUIMojo.class);

  @Override
  public void execute() throws MojoExecutionException {
    if (!skipTests) {
      executeScript("test");
    } else {
      LOG.info("Tests are skipped.");
    }
  }
}
