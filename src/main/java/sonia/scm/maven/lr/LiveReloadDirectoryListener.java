package sonia.scm.maven.lr;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveReloadDirectoryListener implements DirectoryChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(LiveReloadDirectoryListener.class);

    @Override
    public void onEvent(DirectoryChangeEvent event) {
        LOG.info("send reload event");
        LiveReloadContext.getClient().reload(event.path().toString());
    }
}
