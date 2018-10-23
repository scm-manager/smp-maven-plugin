package sonia.scm.maven;

import java.io.IOException;
import java.net.URL;

public interface ScmServerListener {

    void started(URL baseURL) throws IOException;

    void stopped(URL baseURL) throws IOException;

}
