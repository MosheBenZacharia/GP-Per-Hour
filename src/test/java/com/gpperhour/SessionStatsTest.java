package com.gpperhour;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SessionStatsTest
{
	@Test
	public void oldSessionsWithoutItemQuantitiesLoad()
	{
		String json = "{\"sessionID\":\"abc\",\"sessionName\":\"old\",\"sessionSaveTime\":1,\"sessionRuntime\":2,"
			+ "\"totalGain\":3,\"totalLoss\":-1,\"netTotal\":2,\"tripCount\":1,\"avgTripDuration\":2,\"deltaQtys\":{\"4151\":1.0}}";
		SessionStats stats = new Gson().fromJson(json, SessionStats.class);
		assertEquals(2, stats.getNetTotal());
		assertTrue(stats.getInitialQtys().isEmpty());
		assertTrue(stats.getQtys().isEmpty());
		assertTrue(GPPerHourPlugin.getProfitLossLedger(stats.getInitialQtys(), stats.getQtys()).isEmpty());
	}
}
