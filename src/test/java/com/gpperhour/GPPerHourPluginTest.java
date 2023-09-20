package com.gpperhour;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GPPerHourPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GPPerHourPlugin.class);
		RuneLite.main(args);
	}
}