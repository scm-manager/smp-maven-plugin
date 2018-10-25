package sonia.scm.maven.lr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.Collection;

public final class LiveReloadServer extends LiveReloadHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LiveReloadServer.class);

    /** registered client sessions */
    private final Collection<Session> sessions;

    public LiveReloadServer(Collection<Session> sessions) {
        this.sessions = sessions;
    }

    /**
     * Registers a new client session.
     *
     * @param session client session
     */
    public void addSession(Session session) {
        LOG.debug("session {} connected", session.getId());
        sessions.add(session);
    }

    /**
     * Handle received messages from client sessions.
     *
     *
     * @param session client session
     * @param message received message
     */
    public void receiveMessage(Session session, String message) {
        if (LiveReloadProtocol.isHello(message)) {
            LOG.debug("received hello from {}", session.getId());
            sendMessage(session, LiveReloadProtocol.hello());
        } else if (LiveReloadProtocol.isInfo(message)) {
            LOG.debug("received info from {}", session.getId());
            // info does not need an answer
        } else {
            LOG.warn("received unknown message {} from {}", message, session.getId());
            sendMessage(session, LiveReloadProtocol.alert("could not handle message"));
        }
    }

    /**
     * Remove client session.
     *
     * @param session client session
     */
    public void removeSession(Session session) {
        LOG.debug("session {} disconnected", session.getId());
        sessions.remove(session);
    }

}
