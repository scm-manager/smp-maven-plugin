package sonia.scm.maven;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URL;
import java.util.Deque;
import java.util.LinkedList;

public class RestartNotificationDirectoryListener implements DirectoryChangeListener, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RestartNotificationDirectoryListener.class);

    private final Deque<DirectoryChangeEvent> eventQueue = new LinkedList<>();
    private final RestartNotifier restartNotifier;

    public RestartNotificationDirectoryListener(URL restartUrl, long waitTimeout) {
        this.restartNotifier = new RestartNotifier(eventQueue, restartUrl, waitTimeout);
        new Thread(restartNotifier).start();
    }

    @Override
    public void onEvent(DirectoryChangeEvent event) {
        LOG.info("received directory change event: {} of {}", event.path(), event.eventType());
        synchronized (eventQueue) {
            eventQueue.push(event);
            eventQueue.notifyAll();
        }
    }

  @Override
  public void close() {
    restartNotifier.stop();
    synchronized (eventQueue) {
      eventQueue.notifyAll();
    }
  }
}
