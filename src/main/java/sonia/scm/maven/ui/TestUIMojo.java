package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "test-ui")
public class TestUIMojo extends AbstractUIMojo {
  @Override
  public void execute() throws MojoExecutionException {
    executeScript("test");
  }
}
