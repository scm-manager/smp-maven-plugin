package sonia.scm.maven;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Synchronizes two directories on every change.
 */
public class StaticFileListener implements DirectoryChangeListener {

  private static final Logger LOG = LoggerFactory.getLogger(StaticFileListener.class);

  private Path sourceDirectory;
  private Path targetDirectory;

  public StaticFileListener(Path sourceDirectory, Path targetDirectory) {
    this.sourceDirectory = sourceDirectory;
    this.targetDirectory = targetDirectory;
  }

  @Override
  public void onEvent(DirectoryChangeEvent event) throws IOException {
    Path source = event.path();
    Path target = targetPath(source);

    switch (event.eventType()) {
      case CREATE:
        create(source, target);
        break;
      case MODIFY:
        modify(source, target);
        break;
      case DELETE:
        delete(target);
        break;
      case OVERFLOW:
        LOG.warn("received overflow event for path {}", source);
        break;
    }
  }

  private void delete(Path target) throws IOException {
    Files.delete(target);
  }

  private void modify(Path source, Path target) throws IOException {
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  private void create(Path source, Path target) throws IOException {
    Files.copy(source, target);
  }

  private Path targetPath(Path source) {
    return targetDirectory.resolve(sourceDirectory.relativize(source));
  }
}
