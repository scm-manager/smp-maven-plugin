package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "watch-ui")
public class WatchUIMojo extends AbstractUIMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    executeScript("watch", true);
  }

}
