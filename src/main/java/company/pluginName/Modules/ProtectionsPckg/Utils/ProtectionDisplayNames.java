package company.pluginName.Modules.ProtectionsPckg.Utils;

import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;

public final class ProtectionDisplayNames {

	private static final Pattern AUTOMATIC_NAME = Pattern.compile("^(.+)_(\\d+)$");

	private ProtectionDisplayNames() {
	}

	public static String automaticName(String ownerName, int number) {
		return ownerName.toLowerCase(Locale.ROOT) + "_" + number;
	}

	public static int nextAutomaticNumber(Iterable<? extends IProtection> protections) {
		Set<Integer> used = new HashSet<>();
		for (IProtection protection : protections) {
			Integer number = automaticNumber(protection);
			if (number != null) {
				used.add(number);
			}
		}

		int next = 1;
		while (used.contains(next)) {
			next++;
		}
		return next;
	}

	public static boolean isLegacyAutomaticName(Protection protection) {
		return protection.getDisplayName() != null
				&& protection.getDisplayNameWithoutFormat().equalsIgnoreCase(protection.getProtectionId());
	}

	public static Integer automaticNumber(IProtection protection) {
		String displayName = protection.getDisplayName();
		String ownerName = protection.getOwnerName();
		if (displayName == null || ownerName == null) {
			return null;
		}

		Matcher matcher = AUTOMATIC_NAME.matcher(displayName);
		if (!matcher.matches() || !matcher.group(1).equalsIgnoreCase(ownerName)) {
			return null;
		}

		try {
			return Integer.valueOf(matcher.group(2));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	public static String forGui(IProtection protection) {
		return forGui(protection.getDisplayName(), protection.getOwnerName(), protection.getProtectionId());
	}

	static String forGui(String displayName, String ownerName, String fallback) {
		if (displayName != null && ownerName != null) {
			Matcher matcher = AUTOMATIC_NAME.matcher(displayName);
			if (matcher.matches() && matcher.group(1).equalsIgnoreCase(ownerName)) {
				return ownerName + " #" + matcher.group(2);
			}
		}
		return displayName != null ? displayName : fallback;
	}
}
