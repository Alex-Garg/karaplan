package me.crespel.karaplan.service.impl;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.crespel.karaplan.config.KarafunConfig.KarafunProperties;
import me.crespel.karaplan.domain.Playlist;
import me.crespel.karaplan.model.exception.TechnicalException;
import me.crespel.karaplan.service.ExportService;

@Slf4j
@Service("karafunExport")
public class KarafunExportServiceImpl implements ExportService {

	public static final String EVENT_AUTHENTICATE = "authenticate";
	public static final String EVENT_PERMISSIONS = "permissions";
	public static final String EVENT_PREFERENCES = "preferences";
	public static final String EVENT_STATUS = "status";
	public static final String EVENT_QUEUE = "queue";
	public static final String EVENT_QUEUE_ADD = "queueAdd";

	@Autowired
	protected KarafunProperties karafunProperties;

	@Override
	public void exportPlaylist(Playlist playlist, String target) {
		if (playlist.getSongs() != null && !playlist.getSongs().isEmpty()) {
			Set<Long> songIds = playlist.getSongs().stream().map(it -> it.getCatalogId()).collect(Collectors.toSet());
			try {
				Socket socket = buildSocket(target);
				socket.on(Socket.EVENT_CONNECT, new ConnectEventListener(socket, target, new AuthenticateAckListener(socket, songIds)))
						.on(Socket.EVENT_CONNECT_ERROR, new LoggingListener(Socket.EVENT_CONNECT_ERROR))
						.on(Socket.EVENT_CONNECT_TIMEOUT, new LoggingListener(Socket.EVENT_CONNECT_ERROR))
						.on(Socket.EVENT_ERROR, new ErrorEventListener(socket))
						.on(Socket.EVENT_MESSAGE, new LoggingListener(Socket.EVENT_CONNECT_ERROR))
						.on(Socket.EVENT_DISCONNECT, new LoggingListener(Socket.EVENT_CONNECT_ERROR))
						.on(EVENT_PERMISSIONS, new LoggingListener(EVENT_PERMISSIONS))
						.on(EVENT_PREFERENCES, new LoggingListener(EVENT_PREFERENCES))
						.on(EVENT_STATUS, new LoggingListener(EVENT_STATUS))
						.on(EVENT_QUEUE, new LoggingListener(EVENT_QUEUE));
				log.debug("Connecting to Karafun Remote {}", target);
				socket.connect();
			} catch (Exception e) {
				throw new TechnicalException(e);
			}
		}
	}

	protected Socket buildSocket(String remoteId) throws URISyntaxException {
		IO.Options opts = new IO.Options();
		opts.forceNew = true;
		opts.reconnection = false;
		opts.query = "remote=kf" + remoteId;
		return IO.socket(karafunProperties.getEndpoint(), opts);
	}

	protected JSONObject buildAuthenticateEvent(String remoteId) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("login", "Karaplan");
			obj.put("channel", remoteId);
			obj.put("role", "participant");
			obj.put("app", "karafun");
			obj.put("socket_id", JSONObject.NULL);
			return obj;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected JSONObject buildQueueAddEvent(Long songId) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("songId", songId);
			obj.put("post", 9999);
			obj.put("singer", "");
			return obj;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected void logSendEvent(String eventName, Object... args) {
		log.debug("Sending event {}: {}", eventName, args);
	}

	protected void logReceivedEvent(String eventName, Object... args) {
		log.debug("Received event {}: {}", eventName, args);
	}

	protected void logReceivedAck(String eventName, Object... args) {
		log.debug("Received ack for {}: {}", eventName, args);
	}

	@AllArgsConstructor
	public class LoggingListener implements Emitter.Listener {

		private final String eventName;

		@Override
		public void call(Object... args) {
			logReceivedEvent(eventName, args);
		}

	}

	@AllArgsConstructor
	public class ConnectEventListener implements Emitter.Listener {

		private final Socket socket;
		private final String remoteId;
		private final Ack authenticateListener;

		@Override
		public void call(Object... args) {
			logReceivedEvent(Socket.EVENT_CONNECT, args);
			JSONObject eventData = buildAuthenticateEvent(remoteId);
			logSendEvent(EVENT_AUTHENTICATE, eventData);
			socket.emit(EVENT_AUTHENTICATE, eventData, authenticateListener);
		}

	}

	@AllArgsConstructor
	public class ErrorEventListener implements Emitter.Listener {

		private final Socket socket;

		@Override
		public void call(Object... args) {
			logReceivedEvent(Socket.EVENT_ERROR, args);
			socket.disconnect();
		}

	}

	@AllArgsConstructor
	public class AuthenticateAckListener implements Ack {

		private final Socket socket;
		private final Collection<Long> songIds;

		@Override
		public void call(Object... args) {
			logReceivedAck(EVENT_AUTHENTICATE, args);
			for (Long songId : songIds) {
				if (songId != null) {
					JSONObject eventData = buildQueueAddEvent(songId);
					logSendEvent(EVENT_QUEUE_ADD, eventData);
					socket.emit(EVENT_QUEUE_ADD, eventData);
				}
			}
			log.debug("Disconnecting from Karafun session");
			socket.disconnect();
		}

	}

}
