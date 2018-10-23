package sonia.scm.maven;

import java.nio.file.Path;

public final class PluginPathResolver {

    private static final String DIRECTORY_CLASSES = "classes";
    private static final String DIRECTORY_LIB = "lib";
    private static final String DIRECTORY_WEBAPP = "webapp";
    private static final String DIRECTORY_METAINF = "META-INF";

    private final Path classesCompileDirectory;
    private final Path packageDirectory;
    private final Path installationDirectory;

    public PluginPathResolver(Path classesCompileDirectory, Path packageDirectory, Path installationDirectory) {
        this.classesCompileDirectory = classesCompileDirectory;
        this.packageDirectory = packageDirectory;
        this.installationDirectory = installationDirectory;
    }

    /**
     * Returns the target directory of the maven compiler normally target/classes.
     *
     * @return classes compile directory
     */
    public Path getClassesCompileDirectory() {
        return classesCompileDirectory;
    }

    /**
     * Returns the target directory maven, before the artifact gets packages.
     *
     * @return package directory
     */
    public Path getPackageDirectory() {
        return packageDirectory;
    }

    /**
     * Returns the target plugin installation directory.
     *
     * @return installation directory
     */
    public Path getInstallationDirectory() {
        return installationDirectory;
    }

    /**
     * Returns the source and target directory for classes.
     *
     * @return path pair for classes
     */
    public PathPair getClasses() {
        return new PathPair(classesCompileDirectory, installationDirectory.resolve(DIRECTORY_CLASSES));
    }

    /**
     * Returns the source and target directory for web resources.
     *
     * @return path pair for web resources
     */
    public PathPair getWebApp() {
        return new PathPair(packageDirectory.resolve(DIRECTORY_WEBAPP), installationDirectory.resolve(DIRECTORY_WEBAPP));
    }

    /**
     * Returns the source and target directory for meta information.
     *
     * @return path pair for META-INF
     */
    public PathPair getMetaInf() {
        return new PathPair(packageDirectory.resolve(DIRECTORY_METAINF), installationDirectory.resolve(DIRECTORY_METAINF));
    }

    /**
     * Returns the source and target directory for libraries.
     *
     * @return path pair for lib
     */
    public PathPair getLib() {
        return new PathPair(packageDirectory.resolve(DIRECTORY_LIB), installationDirectory.resolve(DIRECTORY_LIB));
    }

    public static class PathPair {

        private final Path source;
        private final Path target;

        private PathPair(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        /**
         * Returns the source of the pair.
         *
         * @return source
         */
        public Path getSource() {
            return source;
        }

        /**
         * Returns the target path of the pair.
         *
         * @return target
         */
        public Path getTarget() {
            return target;
        }
    }
}
