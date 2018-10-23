package sonia.scm.maven;

import com.google.common.collect.Lists;
import io.methvin.watcher.DirectoryWatcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class RestartNotificationLifeCycleListener implements ScmServerListener {

    private final PluginPathResolver pathResolver;
    private final String restartPath;
    private final long waitTimeout;

    private DirectoryWatcher watcher;
    private RestartNotificationDirectoryListener directoryListener;


    public RestartNotificationLifeCycleListener(PluginPathResolver pathResolver, String restartPath, long waitTimeout) {
        this.pathResolver = pathResolver;
        this.restartPath = restartPath;
        this.waitTimeout = waitTimeout;
    }

    @Override
    public void started(URL baseURL) throws IOException {
        checkState(watcher == null, "restart notifier is already started");

        // TODO restart on descriptor change
        List<Path> paths = Lists.newArrayList(
                pathResolver.getClasses().getSource(),
                pathResolver.getLib().getSource()
        );

        directoryListener = new RestartNotificationDirectoryListener(createRestartURL(baseURL), waitTimeout);

        watcher = DirectoryWatcher.builder()
                .paths(paths)
                .listener(directoryListener)
                .build();

        watcher.watchAsync();
    }

    private URL createRestartURL(URL baseURL) throws IOException {
        try {
            return baseURL.toURI().resolve(restartPath).toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new IOException("failed to create restart url", ex);
        }
    }

    @Override
    public void stopped(URL baseURL) throws IOException {
        checkState(watcher != null, "restart notifier was not started");
        directoryListener.stop();
        watcher.close();
        watcher = null;
    }
}
