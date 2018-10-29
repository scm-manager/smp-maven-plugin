package sonia.scm.maven;

import com.google.common.base.Charsets;
import io.methvin.watcher.DirectoryChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TempDirectory.class)
class StaticFileListenerTest {

  private Path sourceDirectory;
  private Path targetDirectory;
  private StaticFileListener listener;

  @BeforeEach
  public void setUpObjectUnderTest(@TempDirectory.TempDir Path directory) throws IOException {
    sourceDirectory = directory.resolve("source");
    targetDirectory = directory.resolve("target");

    Files.createDirectories(sourceDirectory);
    Files.createDirectories(targetDirectory);

    listener = new StaticFileListener(sourceDirectory, targetDirectory);
  }

  @Test
  public void fileAdded() throws IOException {
    DirectoryChangeEvent event = create(sourceDirectory, "hello.txt", "Hello");
    listener.onEvent(event);

    Path target = targetDirectory.resolve("hello.txt");
    assertThat(target).hasContent("Hello");
  }

  @Test
  public void fileModified() throws IOException {
    create(sourceDirectory, "hello.txt", "Hello");
    create(targetDirectory, "hello.txt", "Hello");

    DirectoryChangeEvent event = modify(sourceDirectory, "hello.txt", "Hello again");
    listener.onEvent(event);

    Path target = targetDirectory.resolve("hello.txt");
    assertThat(target).hasContent("Hello again");
  }

  @Test
  public void fileDeleted() throws IOException {
    create(sourceDirectory, "hello.txt", "Hello");
    create(targetDirectory, "hello.txt", "Hello");

    DirectoryChangeEvent event = remove(sourceDirectory, "hello.txt");
    listener.onEvent(event);

    Path target = targetDirectory.resolve("hello.txt");
    assertThat(target).doesNotExist();
  }

  private DirectoryChangeEvent remove(Path parent, String name) throws IOException {
    Path file = parent.resolve(name);
    Files.delete(file);
    return new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, file, 1);
  }

  private DirectoryChangeEvent create(Path parent, String name, String content) throws IOException {
    Path file = parent.resolve(name);
    Files.write(file, content.getBytes(Charsets.UTF_8));
    return new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, file, 1);
  }

  private DirectoryChangeEvent modify(Path parent, String name, String content) throws IOException {
    Path file = parent.resolve(name);
    Files.write(file, content.getBytes(Charsets.UTF_8));
    return new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, file, 1);
  }
}
