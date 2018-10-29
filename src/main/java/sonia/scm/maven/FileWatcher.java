package sonia.scm.maven;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Watches directories for changes and notifies registered listeners.
 */
public class FileWatcher implements DirectoryChangeListener, ScmServerListener {

  private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

  private final Set<Listener> listeners = Sets.newHashSet();
  private DirectoryWatcher watcher;

  /**
   * Register new directory change listener.
   *
   * @param changeListener directory change listener
   * @param path path to watch
   * @param paths additional paths to watch
   *
   * @return {@code this}
   */
  public FileWatcher register(DirectoryChangeListener changeListener, Path path, Path... paths) {
    listeners.add(new Listener(changeListener, Lists.asList(path, paths)));
    return this;
  }

  @Override
  public void started(URL baseURL) throws IOException {
    checkState(watcher == null, "file watcher is already started");

    List<Path> paths = listeners.stream()
      .flatMap(listener -> listener.paths.stream())
      .collect(Collectors.toList());

    paths.stream().forEach(p -> LOG.info("wautch directory {} for changes", p));

    watcher = DirectoryWatcher.builder()
      .paths(paths)
      .listener(this)
      .build();

    watcher.watchAsync();
  }

  @Override
  public void stopped(URL baseURL) throws IOException {
    checkState(watcher != null, "file watcher was not started");
    closeListeners();
    watcher.close();
    watcher = null;
  }

  private void closeListeners() {
    for (Listener listener : listeners) {
      closeIfRequired(listener);
    }
  }

  private void closeIfRequired(Listener listener) {
    if (listener.changeListener instanceof Closeable) {
      try {
        ((Closeable) listener.changeListener).close();
      } catch (IOException ex) {
        LOG.warn("failed to close changeListener " + listener.getName(), ex);
      }
    }
  }

  @Override
  public void onEvent(DirectoryChangeEvent event) throws IOException {
    LOG.info("received change event {} for path {}", event.eventType(), event.path());
    for (Listener listener : listeners) {
      callListenerIfPathMatches(listener, event);
    }
  }

  private void callListenerIfPathMatches(Listener listener, DirectoryChangeEvent event) throws IOException {
    Path changedPath = event.path();
    if (isAnyStaringWith(listener.paths, changedPath)) {
      LOG.info("call changeListener {}, because path {} changed", listener.getName(), changedPath);
      listener.changeListener.onEvent(event);
    }
  }

  private boolean isAnyStaringWith(Iterable<Path> paths, Path path) {
    for (Path p : paths) {
      if (path.startsWith(p)) {
        return true;
      }
    }
    return false;
  }

  private static class Listener {

    private Collection<Path> paths;
    private DirectoryChangeListener changeListener;

    private Listener(DirectoryChangeListener changeListener, Collection<Path> paths) {
      this.changeListener = changeListener;
      this.paths = paths;
    }

    public String getName() {
      return changeListener.getClass().toString();
    }
  }

}
