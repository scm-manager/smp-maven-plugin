package sonia.scm.maven;

import java.io.IOException;
import java.net.URL;

public interface ScmServerListener {

  default void started(URL baseURL) throws IOException {
  }

  default void ready(URL baseURL) throws IOException {
  }

  default void stopped(URL baseURL) throws IOException {
  }

}
