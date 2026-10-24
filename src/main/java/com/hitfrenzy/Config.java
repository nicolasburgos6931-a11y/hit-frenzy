package com.hitfrenzy;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Reads config/hitfrenzy.properties (it is created with defaults the first time the game runs). */
final class Config {

	// ---- on / off ----
	boolean damageNumbers = true;
	boolean effectLabels = true;
	boolean combo = true;
	boolean comboRating = true;
	boolean sounds = true;
	boolean particles = true;

	// ---- general ----
	String language = "auto";
	double weakDamage = 2.0;
	double textScale = 1.0;
	double riseBlocks = 0.9;

	// ---- combo ----
	double comboSeconds = 2.0;
	int ratingMinHits = 2;
	int goodHits = 4;
	int greatHits = 10;
	int amazingHits = 15;
	int[] milestones = {5, 10, 20, 30, 50};
	double comboGrowth = 0.08;
	double comboMaxScale = 3.5;

	// ---- colors (names like "red" or hex like "#ff0000") ----
	String[] rainbow = {"#ff3b3b", "#ff9f1c", "#ffe14d", "#4dff6a", "#3bd9ff", "#6a5cff", "#ff4dd2"};
	String colorNormal = "white";
	String colorCrit = "#ffe14d";
	String colorWeak = "gray";
	String colorKill = "#ff3b3b";
	String colorCritLabel = "gold";
	String colorWeakLabel = "dark_gray";
	String colorKillLabel = "dark_red";
	String colorComboMeh = "gray";
	String colorComboGood = "green";
	String colorComboGreat = "gold";
	String colorStats = "gray";

	// ---- custom messages (empty = use the built-in ones) ----
	String[] msgCrit, msgWeak, msgKill, msgComboMeh, msgComboGood, msgComboGreat, msgComboAmazing, msgMilestone;

	static Config load() {
		Config c = new Config();
		try {
			Path file = FabricLoader.getInstance().getConfigDir().resolve("hitfrenzy.properties");
			if (!Files.exists(file)) {
				Files.writeString(file, TEMPLATE, StandardCharsets.UTF_8);
				return c;
			}
			Properties p = new Properties();
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				p.load(r);
			}

			c.damageNumbers = bool(p, "damageNumbers", c.damageNumbers);
			c.effectLabels = bool(p, "effectLabels", c.effectLabels);
			c.combo = bool(p, "combo", c.combo);
			c.comboRating = bool(p, "comboRating", c.comboRating);
			c.sounds = bool(p, "sounds", c.sounds);
			c.particles = bool(p, "particles", c.particles);

			c.language = text(p, "language", c.language);
			c.weakDamage = num(p, "weakDamage", c.weakDamage);
			c.textScale = clamp(num(p, "textScale", c.textScale), 0.3, 4.0);
			c.riseBlocks = clamp(num(p, "riseBlocks", c.riseBlocks), 0.0, 5.0);

			c.comboSeconds = clamp(num(p, "comboSeconds", c.comboSeconds), 0.5, 30.0);
			c.ratingMinHits = (int) num(p, "ratingMinHits", c.ratingMinHits);
			c.goodHits = (int) num(p, "goodHits", c.goodHits);
			c.greatHits = (int) num(p, "greatHits", c.greatHits);
			c.amazingHits = (int) num(p, "amazingHits", c.amazingHits);
			c.comboGrowth = clamp(num(p, "comboGrowth", c.comboGrowth), 0.0, 1.0);
			c.comboMaxScale = clamp(num(p, "comboMaxScale", c.comboMaxScale), 1.0, 8.0);
			c.milestones = ints(p, "milestones", c.milestones);

			String[] rb = list(p, "rainbow");
			if (rb != null) {
				List<String> colors = new ArrayList<>();
				for (String s : rb) {
					colors.add(color(s));
				}
				c.rainbow = colors.toArray(new String[0]);
			}

			c.colorNormal = color(text(p, "colorNormal", c.colorNormal));
			c.colorCrit = color(text(p, "colorCrit", c.colorCrit));
			c.colorWeak = color(text(p, "colorWeak", c.colorWeak));
			c.colorKill = color(text(p, "colorKill", c.colorKill));
			c.colorCritLabel = color(text(p, "colorCritLabel", c.colorCritLabel));
			c.colorWeakLabel = color(text(p, "colorWeakLabel", c.colorWeakLabel));
			c.colorKillLabel = color(text(p, "colorKillLabel", c.colorKillLabel));
			c.colorComboMeh = color(text(p, "colorComboMeh", c.colorComboMeh));
			c.colorComboGood = color(text(p, "colorComboGood", c.colorComboGood));
			c.colorComboGreat = color(text(p, "colorComboGreat", c.colorComboGreat));
			c.colorStats = color(text(p, "colorStats", c.colorStats));

			c.msgCrit = list(p, "msg.crit");
			c.msgWeak = list(p, "msg.weak");
			c.msgKill = list(p, "msg.kill");
			c.msgComboMeh = list(p, "msg.combo.meh");
			c.msgComboGood = list(p, "msg.combo.good");
			c.msgComboGreat = list(p, "msg.combo.great");
			c.msgComboAmazing = list(p, "msg.combo.amazing");
			c.msgMilestone = list(p, "msg.milestone");
		} catch (Exception e) {
			// If anything is wrong with the file we just keep the defaults.
		}
		if (c.rainbow.length == 0) {
			c.rainbow = new String[] {"white"};
		}
		return c;
	}

	// ------------------------------------------------------------------

	/** Removes anything that could break a command (quotes, backslashes, control characters). */
	static String clean(String s) {
		return s.replace("\"", "").replace("\\", "").replaceAll("\\p{Cntrl}", "").trim();
	}

	private static String color(String s) {
		String out = s.replaceAll("[^#A-Za-z0-9_]", "");
		return out.isEmpty() ? "white" : out;
	}

	private static String text(Properties p, String key, String def) {
		String v = p.getProperty(key);
		return (v == null || v.trim().isEmpty()) ? def : v.trim();
	}

	private static boolean bool(Properties p, String key, boolean def) {
		String v = p.getProperty(key);
		return (v == null || v.trim().isEmpty()) ? def : Boolean.parseBoolean(v.trim());
	}

	private static double num(Properties p, String key, double def) {
		try {
			String v = p.getProperty(key);
			return (v == null || v.trim().isEmpty()) ? def : Double.parseDouble(v.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	/** Splits a "a|b|c" value. Returns null when the key is missing or empty. */
	private static String[] list(Properties p, String key) {
		String v = p.getProperty(key);
		if (v == null || v.trim().isEmpty()) {
			return null;
		}
		List<String> out = new ArrayList<>();
		for (String part : v.split("\\|")) {
			String s = clean(part);
			if (!s.isEmpty()) {
				out.add(s);
			}
		}
		return out.isEmpty() ? null : out.toArray(new String[0]);
	}

	private static int[] ints(Properties p, String key, int[] def) {
		String[] parts = list(p, key);
		if (parts == null) {
			return def;
		}
		try {
			int[] out = new int[parts.length];
			for (int i = 0; i < parts.length; i++) {
				out[i] = Integer.parseInt(parts[i].trim());
			}
			return out;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static final String TEMPLATE = """
			# ============================================================
			#  Hit Frenzy - configuration
			#  Restart the game (or the server) after changing this file.
			#  Lists are separated with the | character.
			# ============================================================

			# ---------- Turn features on or off (true / false) ----------
			damageNumbers=true
			effectLabels=true
			combo=true
			comboRating=true
			sounds=true
			particles=true

			# ---------- General ----------
			# language: auto, en or es  (auto = your system language)
			language=auto
			# Hits that deal less damage than this show "Meh..." (2.0 = one heart)
			weakDamage=2.0
			# Size of all texts (1.0 = normal)
			textScale=1.0
			# How many blocks the texts float upward
			riseBlocks=0.9

			# ---------- Combo ----------
			# Seconds without hitting before the combo ends
			comboSeconds=2.0
			# Combos with fewer hits than this get no rating at the end
			ratingMinHits=2
			# Hits needed for each rating (below goodHits = "Meh")
			goodHits=4
			greatHits=10
			amazingHits=15
			# Combo counts that trigger a special message while you fight
			milestones=5|10|20|30|50
			# How much the combo number grows with each hit, and its maximum size
			comboGrowth=0.08
			comboMaxScale=3.5

			# ---------- Colors (names like red, gold, dark_gray or hex like #ff8800) ----------
			rainbow=#ff3b3b|#ff9f1c|#ffe14d|#4dff6a|#3bd9ff|#6a5cff|#ff4dd2
			colorNormal=white
			colorCrit=#ffe14d
			colorWeak=gray
			colorKill=#ff3b3b
			colorCritLabel=gold
			colorWeakLabel=dark_gray
			colorKillLabel=dark_red
			colorComboMeh=gray
			colorComboGood=green
			colorComboGreat=gold
			colorStats=gray

			# ---------- Custom messages ----------
			# Leave empty to use the built-in messages (English or Spanish).
			# Put several separated with | and a random one is chosen each time.
			# Example: msg.combo.amazing=AMAZING!!|WOW!!|INSANE!!
			msg.crit=
			msg.weak=
			msg.kill=
			msg.combo.meh=
			msg.combo.good=
			msg.combo.great=
			msg.combo.amazing=
			msg.milestone=
			""";
}
