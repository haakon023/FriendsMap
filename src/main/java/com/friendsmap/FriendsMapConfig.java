package com.friendsmap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface FriendsMapConfig extends Config
{
	@ConfigItem(
		keyName = "mqttServer",
		name = "Mqtt server",
		description = "Type in the mqtt server to be used"
	)

	default String MqttServer(){
		return "0.0.0.0";
	}

	@ConfigItem(
			keyName = "mqttUserName",
			name = "Mqtt username",
			description = "The username to connect to the mqtt broker"
	)
	default String MqttUserName(){
		return "name";
	}

	@ConfigItem(
			keyName = "mqttPassword",
			name = "Mqtt password",
			description = "The password to connect to the mqtt broker"
	)
	default String MqttPassword(){
		return "password";
	}

}
