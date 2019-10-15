package sonia.scm.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class ReadyNotifier implements Runnable {

  private static final int MAX_RETRIES = 40;
  private static final long WAIT_BETWEEN_RETRY = 500L;

  private static final Logger LOG = LoggerFactory.getLogger(ReadyNotifier.class);

  private final Iterable<ScmServerListener> listeners;
  private final URL baseURL;
  private final URL url;

  ReadyNotifier(Iterable<ScmServerListener> listeners, URL baseURL, URL url) {
    this.listeners = listeners;
    this.baseURL = baseURL;
    this.url = url;
  }

  @Override
  public void run() {
    boolean notified = false;
    for (int i=0; i<MAX_RETRIES; i++) {
      if (isAvailable()) {
        notifyListeners();
        notified = true;
        break;
      } else {
        try {
          LOG.trace("scm-server is not ready yet, try later");
          Thread.sleep(WAIT_BETWEEN_RETRY);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    if (!notified) {
      LOG.error("scm-server does not come up, listeners could not be notified");
    }
  }

  private void notifyListeners() {
    LOG.info("scm-server is ready call listeners");
    for (ScmServerListener listener : listeners) {
      try {
        listener.ready(baseURL);
      } catch (IOException ex) {
        LOG.error("listener failed", ex);
      }
    }
  }

  private boolean isAvailable() {
    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(1000);
      connection.connect();
      return connection.getResponseCode() == 200;
    } catch (IOException ex) {
      return false;
    }
  }
}
