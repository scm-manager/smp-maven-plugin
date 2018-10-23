package sonia.scm.maven;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class PluginLinker {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLinker.class);

    private final PluginPathResolver pathResolver;

    public PluginLinker(PluginPathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public void link() throws IOException {
        // create parents
        Files.createDirectories(pathResolver.getInstallationDirectory());

        createLink(pathResolver.getClasses());
        createLink(pathResolver.getWebApp());
        createLink(pathResolver.getMetaInf());
        createLink(pathResolver.getLib());
    }

    private void createLink(PluginPathResolver.PathPair pair) throws IOException {
        createLink(pair.getSource(), pair.getTarget());
    }

    @SuppressWarnings("squid:S3725") // Files.exists is to slow, but this is not critical in this case
    private void createLink(Path source, Path target) throws IOException {
        if (Files.exists(source) && !Files.isSymbolicLink(target)) {
            if (Files.exists(target)) {
                delete(target);
            }
            LOG.debug("link directory {} to {}", source, target);
            Files.createSymbolicLink(target, source);
        }
    }

    @SuppressWarnings("squid:S3725") // Files.isDirectory is to slow, but this is not critical in this case
    private void delete(Path path) throws IOException {
        LOG.debug("delete {}", path);
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            FileUtils.deleteDirectory(path.toFile());
        } else {
            Files.delete(path);
        }
    }

}
