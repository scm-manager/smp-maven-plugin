package sonia.scm.maven;

import com.google.common.base.Throwables;
import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Deque;

public class RestartNotifier implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RestartNotifier.class);

    private boolean stopped = false;

    private final Deque<DirectoryChangeEvent> eventQueue;
    private final URL restartUrl;
    private final long waitTimeout;

    public RestartNotifier(Deque<DirectoryChangeEvent> eventQueue, URL restartUrl, long waitTimeout) {
        this.eventQueue = eventQueue;
        this.restartUrl = restartUrl;
        this.waitTimeout = waitTimeout;
    }

    void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        while (!stopped) {
            synchronized (eventQueue) {
                handleRestartNotify();
            }
        }
    }

    @SuppressWarnings({"squid:S2274", "squid:S2273"}) // move wait into a synchronized while loop, both are done
    private void handleRestartNotify() {
        try {
            eventQueue.wait();

            DirectoryChangeEvent event = null;
            while (!eventQueue.isEmpty()) {
                event = eventQueue.pop();
                eventQueue.clear();
                eventQueue.wait(waitTimeout);
            }

            if (event != null){
                sendNotification(event);
            }
        } catch (InterruptedException e) {
            LOG.error("notifier thread was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendNotification(DirectoryChangeEvent event)  {
        LOG.info(
                "send restart notification to {}, because of {} has been {}",
                restartUrl, event.path(), event.eventType()
        );
        try {

            JsonObject content = createContent(event);
            HttpURLConnection connection = openConnection();
            writeContent(connection, content);

            if (isSuccessful(connection)) {
                LOG.info("successfully send restart notification to scm-manager");
            } else {
                LOG.warn("failed to send restart notification to scm-manager");
            }

        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private boolean isSuccessful(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        return responseCode == 202;
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) restartUrl.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();
        return connection;
    }

    private void writeContent(HttpURLConnection connection, JsonObject content) throws IOException {
        try (JsonWriter writer = Json.createWriter(connection.getOutputStream())) {
            writer.writeObject(content);
        }
    }

    private JsonObject createContent(DirectoryChangeEvent directoryChangeEvent) {
        String message = String.format("received directory change event %s for path %s",
                directoryChangeEvent.path(), directoryChangeEvent.eventType()
        );
        return Json.createObjectBuilder()
                .add("message", message)
                .build();
    }

}
