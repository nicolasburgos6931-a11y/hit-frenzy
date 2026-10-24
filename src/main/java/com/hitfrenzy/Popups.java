package com.hitfrenzy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.minecraft.server.MinecraftServer;

/**
 * Creates and animates the floating texts. They are vanilla "text_display"
 * entities, created and animated with commands, so no rendering code is needed.
 */
final class Popups {

	/** A text waiting to be created at the end of the tick. */
	private static final class Request {
		String dim;
		double x, y, z;
		String text, color;
		boolean bold, rainbow;
		double scale, rise;
		int life;
	}

	/** A text that is already in the world and being animated. */
	private static final class Popup {
		String dim, tag, text;
		boolean bold, rainbow;
		double scale, rise;
		int life, age;
	}

	private static final List<Request> QUEUE = new ArrayList<>();
	private static final List<String> COMMANDS = new ArrayList<>();
	private static final List<Popup> ACTIVE = new ArrayList<>();
	private static long counter = 0;

	private Popups() {
	}

	// ------------------------------------------------------------------
	// API used by the mod
	// ------------------------------------------------------------------

	static void text(String dim, double x, double y, double z, String text, String color,
			boolean bold, boolean rainbow, double scale, double rise, int life) {
		Request r = new Request();
		r.dim = dim;
		r.x = x;
		r.y = y;
		r.z = z;
		r.text = text;
		r.color = color;
		r.bold = bold;
		r.rainbow = rainbow;
		r.scale = scale;
		r.rise = rise;
		r.life = Math.max(12, life);
		QUEUE.add(r);
	}

	/** Queues a command (sound, particle...) to be run at the end of the tick. */
	static void command(String command) {
		COMMANDS.add(command);
	}

	static void sound(String dim, String player, String sound, double x, double y, double z, double pitch) {
		if (!player.matches("[A-Za-z0-9_.]+")) {
			return;
		}
		double p = Math.max(0.0, Math.min(2.0, pitch));
		command("execute in " + dim + " run playsound " + sound + " master " + player + " "
				+ num(x) + " " + num(y) + " " + num(z) + " 1 " + num(p) + " 0.5");
	}

	static void particle(String dim, String particle, double x, double y, double z,
			double spread, double speed, int count) {
		command("execute in " + dim + " run particle " + particle + " "
				+ num(x) + " " + num(y) + " " + num(z) + " "
				+ num(spread) + " " + num(spread) + " " + num(spread) + " " + num(speed) + " " + count);
	}

	// ------------------------------------------------------------------
	// Tick
	// ------------------------------------------------------------------

	static void onTick(MinecraftServer server, String[] palette) {
		// 1) Animate the existing texts (before creating new ones, so at
		//    least one tick always passes between creating and animating).
		Iterator<Popup> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Popup p = it.next();
			p.age++;

			int fadeAt = p.life - 7;
			int riseTicks = p.life - 8;

			if (p.age == 1) {
				// Grows to its real size and floats up.
				run(server, merge(p, "start_interpolation:0,interpolation_duration:" + riseTicks
						+ ",transformation:" + transformation(p.scale, p.rise)));
			} else if (p.age == fadeAt) {
				// Shrinks away while it finishes rising.
				run(server, merge(p, "start_interpolation:0,interpolation_duration:6,transformation:"
						+ transformation(0.05, p.rise + 0.3)));
			} else if (p.age >= p.life) {
				run(server, "execute in " + p.dim + " run kill " + selector(p.tag));
				it.remove();
				continue;
			}

			// Frantic color changes every 2 ticks.
			if (p.rainbow && p.age % 2 == 0 && p.age < p.life - 1) {
				run(server, merge(p, "text:" + component(p.text, "white", p.bold, true, p.age, palette)));
			}
		}

		// 2) Run queued commands (sounds, particles).
		if (!COMMANDS.isEmpty()) {
			for (String c : COMMANDS) {
				run(server, c);
			}
			COMMANDS.clear();
		}

		// 3) Create the new texts.
		if (!QUEUE.isEmpty()) {
			for (Request r : QUEUE) {
				spawn(server, r, palette);
			}
			QUEUE.clear();
		}
	}

	/** Removes every text (used when the server stops or starts). */
	static void clearAll(MinecraftServer server) {
		for (Popup p : ACTIVE) {
			run(server, "execute in " + p.dim + " run kill " + selector(p.tag));
		}
		ACTIVE.clear();
		QUEUE.clear();
		COMMANDS.clear();
	}

	/** Removes leftovers from a crash in the three vanilla dimensions. */
	static void cleanLeftovers(MinecraftServer server) {
		for (String dim : new String[] {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}) {
			run(server, "execute in " + dim + " run kill @e[type=minecraft:text_display,tag=hitfrenzy]");
		}
	}

	// ------------------------------------------------------------------

	private static void spawn(MinecraftServer server, Request r, String[] palette) {
		Popup p = new Popup();
		p.dim = r.dim;
		p.tag = "hf" + (counter++);
		p.text = r.text;
		p.bold = r.bold;
		p.rainbow = r.rainbow;
		p.scale = r.scale;
		p.rise = r.rise;
		p.life = r.life;
		p.age = 0;

		String cmd = "execute in " + r.dim + " run summon minecraft:text_display "
				+ num(r.x) + " " + num(r.y) + " " + num(r.z) + " {"
				+ "Tags:[\"hitfrenzy\",\"" + p.tag + "\"],"
				+ "text:" + component(r.text, r.color, r.bold, r.rainbow, 0, palette) + ","
				+ "billboard:\"center\","
				+ "background:0,"
				+ "shadow:true,"
				+ "see_through:false,"
				+ "brightness:{sky:15,block:15},"
				+ "transformation:" + transformation(r.scale * 0.4, 0.0)
				+ "}";

		run(server, cmd);
		ACTIVE.add(p);
	}

	/** Builds a text component. With rainbow, every letter gets its own color. */
	private static String component(String text, String color, boolean bold, boolean rainbow,
			int phase, String[] palette) {
		if (!rainbow) {
			return "{text:\"" + text + "\",color:\"" + color + "\",bold:" + bold + "}";
		}
		StringBuilder sb = new StringBuilder("{text:\"\",bold:" + bold + ",extra:[");
		for (int i = 0; i < text.length(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			String c = palette[Math.floorMod(i + phase, palette.length)];
			sb.append("{text:\"").append(text.charAt(i)).append("\",color:\"").append(c).append("\"}");
		}
		return sb.append("]}").toString();
	}

	private static String merge(Popup p, String data) {
		return "execute in " + p.dim + " run data merge entity " + selector(p.tag) + " {" + data + "}";
	}

	private static String transformation(double scale, double rise) {
		String s = flt(scale);
		return "{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
				+ "scale:[" + s + "," + s + "," + s + "],"
				+ "translation:[0f," + flt(rise) + ",0f]}";
	}

	private static String selector(String tag) {
		return "@e[type=minecraft:text_display,tag=" + tag + ",limit=1]";
	}

	private static String num(double v) {
		return String.format(Locale.ROOT, "%.3f", v);
	}

	private static String flt(double v) {
		return num(v) + "f";
	}

	/** Runs a command as the server, without chat or console messages. */
	private static void run(MinecraftServer server, String command) {
		try {
			server.getCommands().performPrefixedCommand(
					server.createCommandSourceStack().withSuppressedOutput(), command);
		} catch (Exception e) {
			// A broken text must never crash the server.
		}
	}
}
