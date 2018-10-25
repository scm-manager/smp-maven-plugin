package sonia.scm.maven.lr;

import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.maven.PluginPathResolver;
import sonia.scm.maven.ScmServerListener;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class LiveReloadLifeCycleListener implements ScmServerListener {

    private static final Logger LOG = LoggerFactory.getLogger(LiveReloadLifeCycleListener.class);

    private final PluginPathResolver pathResolver;

    private DirectoryWatcher watcher;

    public LiveReloadLifeCycleListener(PluginPathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public void started(URL baseURL) throws IOException {
        Path source = pathResolver.getWebApp().getSource();

        LOG.info("start livereload watcher for {}", source);

        watcher = DirectoryWatcher.builder()
                .path(source)
                .listener(new LiveReloadDirectoryListener())
                .build();

        watcher.watchAsync();
    }

    @Override
    public void stopped(URL baseURL) throws IOException {

        LOG.info("stop livereload watcher");

        watcher.close();
    }
}
