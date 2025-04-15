package com.friendsmap;

import com.friendsmap.messages.LocationUpdate;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.RunnableExceptionLogger;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "FriendsMap"
)
public class FriendsMapPlugin extends Plugin
{

	@Inject
	private Client client;


	@Inject
	private FriendsMapConfig config;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private OverlayManager overlayManager;

	private boolean isLoggedIn;

	@Getter
	private final Map<String, PlayerWorldPoint> friendsMap = Collections.synchronizedMap(new HashMap<>());
	private static final Gson gson = new Gson();
	private WorldPoint lastLocation;

	private  String broker;

	private static final String CHANNEL = "players/location";
	private static final String CLIENT_ID = "test";

	private MqttClient mqttClient;

	private void tick()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;
		try{
			publishMessage();
		} catch (MqttException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	protected void startUp() throws Exception {
		//Should connect to mqtt server here
		executor.scheduleAtFixedRate(RunnableExceptionLogger.wrap(this::tick), 0, 10, TimeUnit.SECONDS);

	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event) throws MqttException {
		switch (event.getGameState()) {
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				disconnectMqtt();
				break;

			case LOGGED_IN:
				if (mqttClient != null && mqttClient.isConnected()) {
					log.info("Already connected to MQTT broker, skipping reconnect.");
					return;
				}

				connectMqtt();
				break;
		}
	}

	private void connectMqtt() throws MqttException {
		disconnectMqtt(); // Clean up first, just in case

		broker = config.MqttServer();
		mqttClient = new MqttClient(broker, CLIENT_ID, null);

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setUserName(config.MqttUserName());
		options.setPassword(config.MqttPassword().getBytes());
		options.setAutomaticReconnect(true);


		mqttClient.setCallback(new MqttCallback() {
			@Override
			public void disconnected(MqttDisconnectResponse response) {
				log.warn("Disconnected from MQTT broker: {}", response.getReasonString());
			}

			@Override
			public void mqttErrorOccurred(MqttException e) {
				log.error("MQTT error occurred", e);
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				if (CHANNEL.equals(topic)) {
					MessageReceived(message);
				}
			}

			@Override
			public void deliveryComplete(IMqttToken token) { }

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				log.info("Connected to MQTT broker at {}", serverURI);
			}

			@Override
			public void authPacketArrived(int reasonCode, MqttProperties props) { }
		});

		mqttClient.connect(options);
		MqttSubscription subscription = new MqttSubscription(CHANNEL, 0);
		subscription.setNoLocal(true);
		mqttClient.subscribe(new MqttSubscription[] { subscription });
	}

	private void disconnectMqtt() {
		if (mqttClient != null) {
			try {
				if (mqttClient.isConnected()) {
					mqttClient.disconnect();
				}
				mqttClient.close();
			} catch (MqttException e) {
				log.warn("Error disconnecting MQTT client", e);
			} finally {
				mqttClient = null;
			}
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (mqttClient != null && mqttClient.isConnected()) {
			mqttClient.disconnect();
			mqttClient.close();
		}

		executor.shutdownNow();
	}

	private void publishMessage() throws MqttException
	{
		if (mqttClient == null || !mqttClient.isConnected())
			return;

		if (client.getLocalPlayer() == null) {
			return;
		}
		String name = client.getLocalPlayer().getName();
		WorldPoint location = client.getLocalPlayer().getWorldLocation();

		if (location.equals(lastLocation))
			return;

		lastLocation = location;


		int regionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		LocationUpdate update = new LocationUpdate(location, name, regionId);

		String msg = gson.toJson(update);

		MqttMessage message = new MqttMessage(msg.getBytes());
		message.setQos(0);

		log.debug("Publishing message to MQTT broker " + broker);
		mqttClient.publish(CHANNEL, message);
	}

	private void MessageReceived(MqttMessage mqttMessage)
	{
		byte[] bytes = mqttMessage.getPayload();
		String message = new String(bytes);
		LocationUpdate event = new Gson().fromJson(message, LocationUpdate.class);

		WorldMapPoint point = getFriendPoint(event.getPlayerName());

		if (point == null)
		{
			return;
		}

		log.debug("Received location update: " + point);
		point.setWorldPoint(event.getWorldPoint());

	}

	@Nullable
	PlayerWorldPoint getFriendPoint(final String name)
	{
		return friendsMap.computeIfAbsent(name, (n) -> {
			final PlayerWorldPoint point = new PlayerWorldPoint(null, name);

			log.info("Adding friend point {}", point);
			worldMapPointManager.add(point);

			return point;
		});
	}


	@Provides
	FriendsMapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendsMapConfig.class);
	}
}
