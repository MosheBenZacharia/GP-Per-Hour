package com.gpperhour;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Pins down the numbers the session panel and overlay show, so caching them can't silently change them.
 */
public class SessionManagerTest
{
	private static final int SHARK = 1;
	private static final int SHARK_NOTED = 2;
	private static final int RUNE = 3;
	private static final int BONES = 4;
	private static final int UNNAMED = 5;
	private static final int FREE = 6;

	private static final long MINUTE = 60_000L;

	private boolean ignoreBankTime;
	private SessionManager sessionManager;

	@Before
	public void setUp() throws Exception
	{
		for (String name : new String[]{"itemPrices", "itemNames"})
		{
			Field field = GPPerHourPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			((Map<?, ?>) field.get(null)).clear();
		}
		GPPerHourPlugin.putItemName(SHARK, "Shark");
		GPPerHourPlugin.putItemName(SHARK_NOTED, "Shark");
		GPPerHourPlugin.putItemName(RUNE, "Rune");
		GPPerHourPlugin.putItemName(BONES, "Bones");
		GPPerHourPlugin.putItemName(FREE, "Free");
		GPPerHourPlugin.putItemPrice(SHARK, 1000f);
		GPPerHourPlugin.putItemPrice(SHARK_NOTED, 1000f);
		GPPerHourPlugin.putItemPrice(RUNE, 200f);
		GPPerHourPlugin.putItemPrice(BONES, 100f);
		GPPerHourPlugin.putItemPrice(UNNAMED, 5000f);
		GPPerHourPlugin.putItemPrice(FREE, 0f);

		ignoreBankTime = true;
		GPPerHourConfig config = (GPPerHourConfig) Proxy.newProxyInstance(GPPerHourConfig.class.getClassLoader(),
			new Class<?>[]{GPPerHourConfig.class}, (proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "ignoreBankTime":
						return ignoreBankTime;
					case "getEnableSessionTracking":
					case "enableSessionPanel":
						return true;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
		sessionManager = new SessionManager(null, config, null, null);
	}

	// trip i starts at i*10 minutes, runs for 5 minutes of tracked time and banks 6 minutes in (so 1 minute paused)
	private TripData trip(int i, boolean inProgress, Object... itemChanges)
	{
		TripData trip = new TripData();
		trip.identifier = "trip" + i;
		trip.runStartTime = i * 10 * MINUTE;
		trip.runEndTime = inProgress ? null : trip.runStartTime + 6 * MINUTE;
		trip.runtime = 5 * MINUTE;
		for (int c = 0; c < itemChanges.length; c += 3)
		{
			int itemId = (Integer) itemChanges[c];
			float before = ((Number) itemChanges[c + 1]).floatValue();
			float after = ((Number) itemChanges[c + 2]).floatValue();
			trip.initialItemQtys.put(itemId, before);
			trip.itemQtys.put(itemId, after);
		}
		sessionManager.onTripStarted(trip);
		return trip;
	}

	// the plugin always passes the trip's own identifier around, which SessionManager compares by reference
	private String id(int i)
	{
		return sessionManager.getActiveTrips().get("trip" + i).identifier;
	}

	// trip 0: ate 2 sharks (-2000), got 5 bones (+500)
	// trip 1: used 10 runes (-2000), got 30 bones (+3000)
	// trip 2: got 1 shark (+1000)
	private void threeCompletedTrips()
	{
		trip(0, false, SHARK, 10, 8, BONES, 0, 5);
		trip(1, false, RUNE, 100, 90, BONES, 0, 30);
		trip(2, false, SHARK, 0, 1);
	}

	@Test
	public void noTripsMeansNoStats()
	{
		assertNull(sessionManager.getActiveSessionStats());
	}

	@Test
	public void totalsAcrossAllTrips()
	{
		threeCompletedTrips();
		SessionStats stats = sessionManager.getActiveSessionStats();
		assertNotNull(stats);
		assertEquals(3, stats.getTripCount());
		assertEquals(4500, stats.getTotalGain());
		assertEquals(-4000, stats.getTotalLoss());
		assertEquals(500, stats.getNetTotal());
		assertEquals(15 * MINUTE, stats.getSessionRuntime());
		assertEquals(5 * MINUTE, stats.getAvgTripDuration());
		assertEquals(Float.valueOf(10), stats.getInitialQtys().get(SHARK));
		assertEquals(Float.valueOf(9), stats.getQtys().get(SHARK));
		assertEquals(Float.valueOf(35), stats.getQtys().get(BONES));
	}

	@Test
	public void sessionRuntimeIncludesBankTimeButNotPauses()
	{
		ignoreBankTime = false;
		threeCompletedTrips();
		sessionManager.setSessionEnd(id(2));
		// trip 0 starts at 0, trip 2 ends at 26 minutes, each trip has 1 paused minute
		assertEquals(23 * MINUTE, sessionManager.getActiveSessionStats().getSessionRuntime());
	}

	@Test
	public void startAndEndLimitTheSession()
	{
		threeCompletedTrips();
		sessionManager.setSessionStart(id(1));
		SessionStats stats = sessionManager.getActiveSessionStats();
		assertEquals(2, stats.getTripCount());
		assertEquals(3000 + 1000, stats.getTotalGain());
		assertEquals(-2000, stats.getTotalLoss());

		sessionManager.setSessionEnd(id(1));
		stats = sessionManager.getActiveSessionStats();
		assertEquals(1, stats.getTripCount());
		assertEquals(1000, stats.getNetTotal());

		sessionManager.setSessionEnd(null);
		assertEquals(2, sessionManager.getActiveSessionStats().getTripCount());
	}

	@Test
	public void startAfterEndMovesEnd()
	{
		threeCompletedTrips();
		sessionManager.setSessionEnd(id(0));
		sessionManager.setSessionStart(id(2));
		assertEquals("trip2", sessionManager.getActiveSessionEndId());
		assertEquals(1, sessionManager.getActiveSessionStats().getTripCount());
	}

	@Test
	public void inProgressTripChangesShowUp()
	{
		threeCompletedTrips();
		TripData active = trip(3, true, RUNE, 50, 50);
		assertEquals(500, sessionManager.getActiveSessionStats().getNetTotal());

		// the plugin rebuilds the active trip's item map every tick
		active.itemQtys.put(RUNE, 40f);
		assertEquals(500 - 2000, sessionManager.getActiveSessionStats().getNetTotal());
		active.itemQtys.put(BONES, 10f);
		assertEquals(500 - 2000 + 1000, sessionManager.getActiveSessionStats().getNetTotal());
		assertEquals(4, sessionManager.getActiveSessionStats().getTripCount());
	}

	@Test
	public void repeatedCallsGiveTheSameAnswer()
	{
		threeCompletedTrips();
		for (int i = 0; i < 5; i++)
		{
			assertEquals(500, sessionManager.getActiveSessionStats().getNetTotal());
		}
	}

	@Test
	public void priceChangesShowUpInCompletedTrips()
	{
		threeCompletedTrips();
		assertEquals(500, sessionManager.getActiveSessionStats().getNetTotal());
		GPPerHourPlugin.putItemPrice(BONES, 200f);
		assertEquals(500 + 3500, sessionManager.getActiveSessionStats().getNetTotal());
		GPPerHourPlugin.removeItemPrice(BONES);
		// items without a cached price count as 1gp
		assertEquals(500 - 3500 + 35, sessionManager.getActiveSessionStats().getNetTotal());
	}

	@Test
	public void nameChangesShowUpInCompletedTrips()
	{
		trip(0, false, UNNAMED, 0, 1, SHARK, 0, 1);
		// items without a name are left out
		assertEquals(1000, sessionManager.getActiveSessionStats().getNetTotal());
		GPPerHourPlugin.putItemName(UNNAMED, "Unnamed");
		assertEquals(6000, sessionManager.getActiveSessionStats().getNetTotal());
	}

	@Test
	public void ledgerMergesItemsWithTheSameName()
	{
		// banked 5 noted sharks, withdrew them unnoted: no change
		trip(0, false, SHARK, 0, 5, SHARK_NOTED, 5, 0, FREE, 0, 100);
		List<LedgerItem> ledger = GPPerHourPlugin.getProfitLossLedger(
			sessionManager.getActiveTrips().get("trip0").initialItemQtys, sessionManager.getActiveTrips().get("trip0").itemQtys);
		assertTrue(ledger.isEmpty());
		assertEquals(0, sessionManager.getActiveSessionStats().getNetTotal());
	}

	@Test
	public void deletingTripsMovesStartAndEnd()
	{
		threeCompletedTrips();
		sessionManager.setSessionStart(id(1));
		sessionManager.setSessionEnd(id(2));

		sessionManager.deleteTrip(id(1));
		assertEquals("trip0", sessionManager.getActiveSessionStartId());
		assertEquals(2, sessionManager.getActiveSessionStats().getTripCount());
		assertEquals(-2000 + 500 + 1000, sessionManager.getActiveSessionStats().getNetTotal());

		sessionManager.deleteTrip(id(2));
		assertNull(sessionManager.getActiveSessionEndId());
		assertEquals(-1500, sessionManager.getActiveSessionStats().getNetTotal());

		sessionManager.deleteTrip(id(0));
		assertNull(sessionManager.getActiveSessionStartId());
		assertNull(sessionManager.getActiveSessionStats());
	}

	@Test
	public void deleteAllKeepsTheActiveTrip()
	{
		threeCompletedTrips();
		trip(3, true, RUNE, 50, 40);
		sessionManager.deleteAllTrips();
		assertEquals(1, sessionManager.getActiveTrips().size());
		assertEquals("trip3", sessionManager.getActiveSessionStartId());
		assertEquals(-2000, sessionManager.getActiveSessionStats().getNetTotal());
	}

	@Test
	public void deleteTripsBefore()
	{
		threeCompletedTrips();
		sessionManager.deleteAllTripsBefore(20 * MINUTE);
		assertEquals(1, sessionManager.getActiveTrips().size());
		assertEquals(1000, sessionManager.getActiveSessionStats().getNetTotal());
	}

	@Test
	public void identicalTripsHaveMatchingLedgers()
	{
		TripData a = trip(0, false, SHARK, 10, 8, BONES, 0, 5, RUNE, 50, 40);
		TripData b = trip(1, false, SHARK, 10, 8, BONES, 0, 5, RUNE, 50, 40);
		TripData c = trip(2, false, SHARK, 10, 7, BONES, 0, 5, RUNE, 50, 40);
		List<LedgerItem> ledgerA = UI.sortLedger(GPPerHourPlugin.getProfitLossLedger(a.initialItemQtys, a.itemQtys));
		List<LedgerItem> ledgerB = UI.sortLedger(GPPerHourPlugin.getProfitLossLedger(b.initialItemQtys, b.itemQtys));
		List<LedgerItem> ledgerC = UI.sortLedger(GPPerHourPlugin.getProfitLossLedger(c.initialItemQtys, c.itemQtys));
		assertTrue(UI.ledgersMatch(ledgerA, ledgerB));
		assertFalse(UI.ledgersMatch(ledgerA, ledgerC));
	}
}
