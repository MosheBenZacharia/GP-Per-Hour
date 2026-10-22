package com.gpperhour.weaponcharges;

import java.util.regex.Matcher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * Resolves chat messages the same way WeaponChargesManager.onChatMessage does (enum order, first find() wins), so a
 * regex added for one weapon can't silently steal another weapon's messages.
 */
public class ChargedWeaponMessageTest
{
	private static class Resolved
	{
		ChargedWeapon weapon;
		ChargesMessage message;
		Matcher matcher;
	}

	private static Resolved resolve(String chatMessage)
	{
		for (ChargedWeapon weapon : ChargedWeapon.values())
		{
			for (ChargesMessage message : weapon.getCheckChargesRegexes())
			{
				Matcher matcher = message.getPattern().matcher(chatMessage);
				if (matcher.find())
				{
					return resolved(weapon, message, matcher);
				}
			}
			for (ChargesMessage message : weapon.getUpdateMessageChargesRegexes())
			{
				Matcher matcher = message.getPattern().matcher(chatMessage);
				if (matcher.find())
				{
					return resolved(weapon, message, matcher);
				}
			}
		}
		return null;
	}

	private static Resolved resolved(ChargedWeapon weapon, ChargesMessage message, Matcher matcher)
	{
		Resolved resolved = new Resolved();
		resolved.weapon = weapon;
		resolved.message = message;
		resolved.matcher = matcher;
		return resolved;
	}

	private static void assertCharges(ChargedWeapon expected, String chatMessage, int charges)
	{
		Resolved resolved = resolve(chatMessage);
		assertNotNull("no weapon matched: " + chatMessage, resolved);
		assertEquals(chatMessage, expected, resolved.weapon);
		assertEquals(chatMessage, charges, resolved.message.getChargesLeft(resolved.matcher, null));
	}

	// for messages whose handler also stores state in the ConfigManager, which can't be constructed here
	private static void assertMatchesOnly(ChargedWeapon expected, String chatMessage)
	{
		Resolved resolved = resolve(chatMessage);
		assertNotNull("no weapon matched: " + chatMessage, resolved);
		assertEquals(chatMessage, expected, resolved.weapon);
	}

	@Test
	public void eyeOfAyak()
	{
		ChargedWeapon w = ChargedWeapon.EYE_OF_AYAK;
		assertMatchesOnly(w, "The Eye of Ayak has been charged with runes. It currently has 1 charge.");
		assertMatchesOnly(w, "The Eye of Ayak has been charged with demon tears. It currently has 50,000 charges.");
		assertCharges(w, "The Eye of Ayak cannot hold any more charges.", 50_000);
		assertCharges(w, "You uncharge the Eye of Ayak.", 0);
		assertCharges(w, "The Eye of Ayak has run out of charges!", 0);
		assertCharges(w, "The Eye of Ayak only has 100 charges left!", 100);
		assertCharges(w, "The Eye of Ayak has 45,200 charges remaining.", 45_200);
		// renamed from "Eye of ayak" in August 2026
		assertCharges(w, "The Eye of ayak has 200 charges remaining.", 200);
	}

	@Test
	public void arclight()
	{
		ChargedWeapon w = ChargedWeapon.ARCLIGHT;
		assertCharges(w, "Your arclight has 1,234 charges left.", 1234);
		assertCharges(w, "Your arclight can perform 9,000 more attacks.", 9000);
		assertCharges(w, "Your arclight has degraded.", 0);
	}

	@Test
	public void tridents()
	{
		assertCharges(ChargedWeapon.TRIDENT_OF_THE_SEAS, "Your Trident of the seas has 2,500 charges.", 2500);
		assertCharges(ChargedWeapon.TRIDENT_OF_THE_SEAS, "Your Trident of the seas (o) has one charge.", 1);
		assertCharges(ChargedWeapon.TRIDENT_OF_THE_SWAMP, "Your Trident of the Swamp (o) has 1,000 charges.", 1000);
		assertCharges(ChargedWeapon.TRIDENT_OF_THE_SEAS_E, "Your Trident of the seas (e) has 10,000 charges.", 10_000);
		assertCharges(ChargedWeapon.TRIDENT_OF_THE_SWAMP_E, "Your Trident of the swamp (e) (o) has 3 charges.", 3);
	}

	@Test
	public void existingWeaponsStillResolve()
	{
		assertCharges(ChargedWeapon.SANGUINESTI_STAFF, "Your Sanguinesti staff has 1,000 charges remaining.", 1000);
		assertCharges(ChargedWeapon.SANGUINESTI_STAFF, "Your Holy sanguinesti staff only has 100 charges left!", 100);
		assertCharges(ChargedWeapon.TUMEKENS_SHADOW, "Tumeken's shadow has 500 charges remaining.", 500);
		assertCharges(ChargedWeapon.VENATOR_BOW, "Your venator bow has 1,234 charges remaining.", 1234);
		assertCharges(ChargedWeapon.SCYTHE_OF_VITUR, "Your Sanguine scythe of vitur has 20,000 charges remaining.", 20_000);
		assertCharges(ChargedWeapon.IBANS_STAFF, "You have 2500 charges left on the staff.", 2500);
	}

	@Test
	public void itemIds()
	{
		// full tridents hold charges, so they must not be treated as uncharged
		assertEquals(ChargedWeapon.TRIDENT_OF_THE_SEAS, ChargedWeapon.getChargedWeaponFromId(ItemID.TOTS));
		assertTrue(ChargedWeapon.TRIDENT_OF_THE_SEAS.getItemIds().contains(ItemID.TOTS_ORN));
		assertTrue(ChargedWeapon.TRIDENT_OF_THE_SEAS.getUnchargedIds().contains(ItemID.TOTS_UNCHARGED_ORN));

		assertEquals(ChargedWeapon.IBANS_STAFF, ChargedWeapon.getChargedWeaponFromId(ItemID.IBAN_STAFF_ORN));
		assertEquals(ChargedWeapon.IBANS_STAFF, ChargedWeapon.getChargedWeaponFromId(ItemID.IBAN_STAFF_UPGRADED_ORN));
		assertEquals(ChargedWeapon.VENATOR_BOW, ChargedWeapon.getChargedWeaponFromId(ItemID.VENATOR_BOW_ORNAMENT));
		assertEquals(ChargedWeapon.VENATOR_BOW, ChargedWeapon.getChargedWeaponFromId(ItemID.VENATOR_BOW_ORNAMENT_UNCHARGED));
		assertEquals(ChargedWeapon.ARCLIGHT, ChargedWeapon.getChargedWeaponFromId(ItemID.ARCLIGHT_INACTIVE));
		assertEquals(ChargedWeapon.EYE_OF_AYAK, ChargedWeapon.getChargedWeaponFromId(ItemID.EYE_OF_AYAK));
		assertEquals(ChargedWeapon.EYE_OF_AYAK, ChargedWeapon.getChargedWeaponFromId(ItemID.EYE_OF_AYAK_UNCHARGED));
		assertEquals(ChargedWeapon.OBLIGATOR, ChargedWeapon.getChargedWeaponFromId(ItemID.OBLIGATOR));
		assertEquals(ChargedWeapon.OBLIGATOR, ChargedWeapon.getChargedWeaponFromId(ItemID.OBLIGATOR_UNCHARGED));
		assertEquals(ChargedWeapon.ZORYAS_TOME, ChargedWeapon.getChargedWeaponFromId(ItemID.ZORYAS_TOME));
		assertEquals(ChargedWeapon.ZORYAS_TOME, ChargedWeapon.getChargedWeaponFromId(ItemID.ZORYAS_TOME_UNCHARGED));
		assertEquals(ChargedWeapon.ARCHIVISTS_FOLIO, ChargedWeapon.getChargedWeaponFromId(ItemID.ARCHIVISTS_FOLIO));
		assertEquals(ChargedWeapon.ARCHIVISTS_FOLIO, ChargedWeapon.getChargedWeaponFromId(ItemID.ARCHIVISTS_FOLIO_EMPTY));
	}

	@Test
	public void chargesVarbits()
	{
		assertEquals(Integer.valueOf(VarbitID.CHARGES_OBLIGATOR_QUANTITY), ChargedWeapon.OBLIGATOR.chargesVarbit);
		assertEquals(Integer.valueOf(VarbitID.CHARGES_ZORYAS_TOME_QUANTITY), ChargedWeapon.ZORYAS_TOME.chargesVarbit);
		assertEquals(Integer.valueOf(VarbitID.CHARGES_ARCHIVISTS_FOLIO_QUANTITY), ChargedWeapon.ARCHIVISTS_FOLIO.chargesVarbit);
		// existing weapons keep relying on messages and animations
		assertEquals(null, ChargedWeapon.SANGUINESTI_STAFF.chargesVarbit);
	}

	@Test
	public void noItemIdIsClaimedTwice()
	{
		java.util.Map<Integer, ChargedWeapon> seen = new java.util.HashMap<>();
		for (ChargedWeapon weapon : ChargedWeapon.values())
		{
			for (java.util.List<Integer> ids : java.util.Arrays.asList(weapon.getItemIds(), weapon.getUnchargedIds()))
			{
				for (int id : ids)
				{
					ChargedWeapon previous = seen.put(id, weapon);
					assertTrue(id + " is listed by both " + previous + " and " + weapon, previous == null || previous == weapon);
				}
			}
		}
	}
}
