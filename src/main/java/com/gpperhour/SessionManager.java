/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gpperhour;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//Business logic for managing all the trips within a session
public class SessionManager
{
	private final GPPerHourPlugin plugin;
	private final GPPerHourConfig config;
	private final ScheduledExecutorService executor;
	private final Gson gson;

	@Getter
	private final Map<String, TripData> activeTrips = new HashMap<>();
	@Getter
	private String activeSessionStartId;
	@Getter
	private String activeSessionEndId;

	public SessionManager(GPPerHourPlugin plugin, GPPerHourConfig config, ScheduledExecutorService executor, Gson gson)
	{
		this.plugin = plugin;
		this.config = config;
		this.executor = executor;
		this.gson = gson;
	}

	void refreshSessionTracking()
	{
		if (isTracking())
		{
			startTracking();
		}
		else
		{
			stopTracking();
		}
	}

	boolean isTracking()
	{
		return config.getEnableSessionTracking() && config.enableSessionPanel();
	}

	void startTracking()
	{
		if (plugin.getState() == RunState.BANK)
		{
			return;
		}
		TripData activeTrip = plugin.getRunData();
		if (activeTrip != null && !activeTrip.isBankDelay && !activeTrip.isFirstRun && activeTrip.isInProgress())
		{
			onTripStarted(activeTrip);
		}
	}

	void stopTracking()
	{
		//remove the active trip if there is one
		String activeTripIdentifier = null;
		for (TripData trip : activeTrips.values())
		{
			if (trip.isInProgress())
			{
				activeTripIdentifier = trip.identifier;
			}
		}
		if (activeTripIdentifier != null)
		{
			deleteTrip(activeTripIdentifier);
		}
	}

	private List<TripData> getSortedTrips()
	{
		return activeTrips.values().stream().sorted(Comparator.comparingLong(o -> o.runStartTime))
			.collect(Collectors.toList());
	}

	SessionStats getActiveSessionStats()
	{
		if (activeSessionStartId == null)
		{
			return null;
		}
		List<TripData> runDataSorted = getSortedTrips();

		long gains = 0;
		long losses = 0;
		long tripDurationSum = 0;
		long totalPauseTime = 0;
		boolean foundStart = false;
		int tripCount = 0;
		Map<Integer, Float> initialQtys = new HashMap<>();
		Map<Integer, Float> qtys = new HashMap<>();
		for (TripData runData : runDataSorted)
		{
			foundStart |= runData.identifier.equals(activeSessionStartId);
			if (!foundStart)
			{
				continue;
			}
			for (Integer initialId : runData.initialItemQtys.keySet())
			{
				initialQtys.merge(initialId, runData.initialItemQtys.get(initialId), Float::sum);
			}
			for (Integer itemId : runData.itemQtys.keySet())
			{
				qtys.merge(itemId, runData.itemQtys.get(itemId), Float::sum);
			}

			List<LedgerItem> ledger = GPPerHourPlugin.getProfitLossLedger(runData.initialItemQtys, runData.itemQtys);
			for (LedgerItem item : ledger)
			{
				long value = item.getCombinedValue();
				if (value > 0)
				{
					gains += value;
				}
				else
				{
					losses += value;
				}
			}
			tripDurationSum += runData.getRuntime();
			totalPauseTime += (runData.getEndTime()-runData.runStartTime) - runData.getRuntime();
			tripCount++;

			if (activeSessionEndId != null && activeSessionEndId.equals(runData.identifier))
			{
				break;
			}
		}
		if (!foundStart)
		{
			log.error("couldn't find start session");
			return null;
		}
		long sessionRuntime = 0;
		if (config.ignoreBankTime())
		{
			sessionRuntime = tripDurationSum;
		}
		else
		{
			long sessionStartTime = getSessionStartTime();
			long sessionEndTime = getSessionEndTime();
			sessionRuntime = (sessionEndTime - sessionStartTime) - totalPauseTime;
		}
		long netTotal = gains + losses;
		long avgTripDuration = (long) (tripDurationSum / ((float) tripCount));

		return new SessionStats(getSessionEndTime(), sessionRuntime, gains, losses, netTotal, tripCount, avgTripDuration, initialQtys, qtys);
	}

	long getSessionStartTime()
	{
		if (activeSessionStartId == null)
		{
			return 0;
		}
		return getSessionStartTrip().runStartTime;
	}

	long getSessionEndTime()
	{
		return (activeSessionEndId == null) ? Instant.now().toEpochMilli()
			: (activeTrips.get(activeSessionEndId).isInProgress() ? Instant.now().toEpochMilli()
			: activeTrips.get(activeSessionEndId).runEndTime);
	}

	boolean isTimeInActiveSession(long time)
	{
		long startTime = getSessionStartTime();
		long endTime = getSessionEndTime();
		return time >= startTime && time <= endTime;
	}

	void setSessionStart(String id)
	{
		activeSessionStartId = id;
		if (id != null)
		{
			TripData startTrip = getSessionStartTrip();
			TripData endtrip = getSessionEndTrip();
			// order is messed up, just make end same as start
			if (endtrip != null && endtrip.runStartTime < startTrip.runStartTime)
			{
				setSessionEnd(id);
			}
		}
	}

	void setSessionEnd(String id)
	{
		activeSessionEndId = id;
		if (id != null)
		{
			TripData startTrip = getSessionStartTrip();
			TripData endtrip = getSessionEndTrip();
			// order is messed up, just make start same as end
			if (startTrip != null && startTrip.runStartTime > endtrip.runStartTime)
			{
				setSessionStart(id);
			}
		}
	}

	TripData getSessionStartTrip()
	{
		if (activeSessionStartId == null)
		{
			return null;
		}
		return activeTrips.get(activeSessionStartId);
	}

	TripData getSessionEndTrip()
	{
		if (activeSessionEndId == null)
		{
			if (activeTrips.size() == 0)
			{
				return null;
			}
			else
			{
				List<TripData> sortedData = getSortedTrips();
				return sortedData.get(sortedData.size() - 1);
			}
		}
		return activeTrips.get(activeSessionEndId);
	}

	void deleteAllTrips()
	{
		List<TripData> allTrips = new LinkedList<>(activeTrips.values());
		activeSessionStartId = null;
		activeSessionEndId = null;
		for(TripData trip : allTrips)
		{
			if (trip.isInProgress())
			{
				activeSessionStartId = trip.identifier;
			}
			else
			{
				activeTrips.remove(trip.identifier);
			}
		}
	}

	//Deletes all trips that started before the specified time.
	void deleteAllTripsBefore(long time)
	{
		List<TripData> allTrips = new LinkedList<>(activeTrips.values());
		for(TripData trip : allTrips)
		{
			if (trip.runStartTime < time)
			{
				deleteTrip(trip.identifier);
			}
		}
	}

	void deleteTrip(String id)
	{
		activeTrips.remove(id);
		if (activeSessionStartId == id)
		{
			if (activeTrips.size() == 0)
			{
				activeSessionStartId = null;
			}
			else
			{
				activeSessionStartId = getSortedTrips().get(0).identifier;
			}
		}
		if (activeSessionEndId == id)
		{
			activeSessionEndId = null;
		}
	}

	void onTripStarted(TripData runData)
	{
		if (!isTracking())
		{
			return;
		}
		activeTrips.put(runData.identifier, runData);
		if (activeSessionStartId == null)
		{
			activeSessionStartId = runData.identifier;
		}
	}

	void onTripCompleted(TripData runData)
	{
		if (!isTracking())
		{
			return;
		}
		// don't care about trips where nothing happened, can remove it from the history
		if (!tripHadChange(runData.initialItemQtys, runData.itemQtys))
		{
			log.debug("nothing changed, ignoring trip");
			deleteTrip(runData.identifier);
			return;
		}
	}

	boolean tripHadChange(Map<Integer, Float> tripStart, Map<Integer, Float> tripEnd)
	{
		List<LedgerItem> ledger = GPPerHourPlugin.getProfitLossLedger(tripStart, tripEnd);
		return !ledger.isEmpty();
	}

	List<SessionStats> sessionHistory = new LinkedList<>();
	List<String> savedSessionIdentifiers = null;
	boolean sessionHistoryDirty;

	void saveNewSession(String name)
	{
		if (savedSessionIdentifiers == null)
		{
			log.error("can't save session, hasn't loaded sessions yet.");
			return;
		}
		executor.execute(()->
		{
			SessionStats statsToSave = getActiveSessionStats();
			if (statsToSave == null)
			{
				return;
			}
			statsToSave.sessionName = name;
			statsToSave.sessionID = UUID.randomUUID().toString();
			sessionHistory.add(statsToSave);

			String json = gson.toJson(statsToSave);
			plugin.saveData(GPPerHourConfig.getSessionKey(statsToSave.sessionID), json);

			savedSessionIdentifiers.add(statsToSave.sessionID);
			saveSessionIdentifiers();
			sessionHistoryDirty = true;
		});
	}

	//assume already exists
	void overwriteSession(SessionStats sessionStats)
	{
		if(sessionStats == null)
		{
			return;
		}
		executor.execute(()->
		{
			String json = gson.toJson(sessionStats);
			plugin.saveData( GPPerHourConfig.getSessionKey(sessionStats.sessionID), json);
			sessionHistoryDirty = true;
		});
	}

	void deleteSession(SessionStats sessionStats)
	{
		if (savedSessionIdentifiers == null)
		{
			log.error("can't delete session, hasn't loaded sessions yet.");
			return;
		}
		if (sessionStats == null)
		{
			return;
		}
		executor.execute(()->
		{
			sessionHistory.remove(sessionStats);
			plugin.deleteData(GPPerHourConfig.getSessionKey(sessionStats.sessionID));

			savedSessionIdentifiers.remove(sessionStats.sessionID);
			saveSessionIdentifiers();
			sessionHistoryDirty = true;
		});
	}

	void saveSessionIdentifiers()
	{
		String json = gson.toJson(savedSessionIdentifiers);
		plugin.saveData(GPPerHourConfig.sessionIdentifiersKey, json);
	}

	void reloadSessions()
	{
		sessionHistory.clear();
		savedSessionIdentifiers = null;
		executor.execute(()->
		{
			Type listType = new com.google.gson.reflect.TypeToken<List<String>>() {}.getType();
			String keysJSON = plugin.readData(GPPerHourConfig.sessionIdentifiersKey);
			try 
			{
				savedSessionIdentifiers = gson.fromJson(keysJSON, listType);
			}
			catch(Exception e)
			{
				log.error("Failed to load session identifiers from json: " + keysJSON, e);
				return;
			}
			if (savedSessionIdentifiers == null)
			{
				savedSessionIdentifiers = new LinkedList<>();
			}
			for (String sessionIdentifier : savedSessionIdentifiers)
			{
				String json = plugin.readData(GPPerHourConfig.getSessionKey(sessionIdentifier));
				if	(json == null)
				{
					continue;
				}
				try 
				{
					SessionStats sessionStats = gson.fromJson(json, SessionStats.class);
					sessionHistory.add(sessionStats);
				}
				catch(Exception e)
				{
					log.error("Failed to load session data from json: " + json, e);
					return;
				}
			}
			sessionHistoryDirty = true;
		});
	}
}
