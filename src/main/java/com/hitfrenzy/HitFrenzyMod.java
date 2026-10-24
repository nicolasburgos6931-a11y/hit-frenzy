package com.hitfrenzy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hit Frenzy - by ElBatzyMC (with Claude, Anthropic).
 *
 * Floating damage numbers, a frantic rainbow combo counter and end-of-combo ratings.
 */
public class HitFrenzyMod implements ModInitializer {

	private enum HitType { NORMAL, CRIT, WEAK, KILL }

	/** The combo of one player. */
	private static final class Combo {
		int hits;
		double total;
		long lastTick;
		String dim = "minecraft:overworld";
		String player = "";
		double x, y, z; // where the last hit landed (top of the mob)
	}

	private static Config cfg = new Config();
	private static Messages msg = new Messages(cfg);

	private static long tick = 0;
	private static final Map<UUID, Combo> COMBOS = new HashMap<>();

	// Health of the mob right before it was hit (to calculate the real damage).
	private static LivingEntity healthEntity = null;
	private static float healthBefore = 0f;

	@Override
	public void onInitialize() {
		cfg = Config.load();
		msg = new Messages(cfg);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			healthEntity = entity;
			healthBefore = entity.getHealth();
			return true; // we never cancel anything
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			float dealt = (healthEntity == entity) ? (healthBefore - entity.getHealth()) : damageTaken;
			healthEntity = null;

			if (blocked || dealt < 0.05f) {
				return;
			}

			boolean melee = source.getDirectEntity() == player;
			HitType type;
			if (melee && isCritical(player)) {
				type = HitType.CRIT;
			} else if (dealt < cfg.weakDamage) {
				type = HitType.WEAK;
			} else {
				type = HitType.NORMAL;
			}
			onHit(player, entity, dealt, type);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}
			float dealt = (healthEntity == entity) ? healthBefore : 0f;
			healthEntity = null;
			onHit(player, entity, dealt, HitType.KILL);
		});

		ServerTickEvents.END_SERVER_TICK.register(HitFrenzyMod::onTick);

		ServerLifecycleEvents.SERVER_STARTED.register(Popups::cleanLeftovers);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Popups.clearAll(server);
			COMBOS.clear();
		});
	}

	/** Approximation of the vanilla critical hit: falling, not sprinting, not in water. */
	private static boolean isCritical(ServerPlayer player) {
		return player.fallDistance > 0.0
				&& !player.onGround()
				&& !player.isInWater()
				&& !player.isSprinting()
				&& !player.isPassenger();
	}

	// ------------------------------------------------------------------
	// A hit happened
	// ------------------------------------------------------------------

	private static void onHit(ServerPlayer player, LivingEntity target, float dealt, HitType type) {
		if (!(target.level() instanceof ServerLevel level)) {
			return;
		}

		String dim = level.dimension().identifier().toString();
		String name = player.getName().getString();
		ThreadLocalRandom rnd = ThreadLocalRandom.current();

		double x = target.getX() + (rnd.nextDouble() - 0.5) * 0.7;
		double z = target.getZ() + (rnd.nextDouble() - 0.5) * 0.7;
		double top = target.getY() + target.getBbHeight();
		double mid = target.getY() + target.getBbHeight() * 0.5;

		Combo combo = cfg.combo ? registerHit(player, name, dim, x, top, z, dealt) : null;
		int hits = (combo == null) ? 1 : combo.hits;

		Stack stack = new Stack(dim, x, top + 0.2, z);
		double ts = cfg.textScale;
		String number = String.format(Locale.ROOT, "%.1f", dealt);

		// 1) The damage number.
		if (cfg.damageNumbers && dealt > 0.05f) {
			switch (type) {
				case CRIT -> stack.add(number, cfg.colorCrit, true, false, 1.4 * ts, 20);
				case WEAK -> stack.add(number, cfg.colorWeak, false, false, 0.8 * ts, 20);
				case KILL -> stack.add(number, cfg.colorKill, true, false, 1.6 * ts, 20);
				default -> stack.add(number, cfg.colorNormal, false, false, 1.0 * ts, 20);
			}
		}

		// 2) The effect label: CRITICAL! / Meh... / KILL!!
		if (cfg.effectLabels) {
			switch (type) {
				case CRIT -> stack.add(Messages.random(msg.crit), cfg.colorCritLabel, true, false, 1.0 * ts, 20);
				case WEAK -> stack.add(Messages.random(msg.weak), cfg.colorWeakLabel, false, false, 0.7 * ts, 20);
				case KILL -> stack.add(Messages.random(msg.kill), cfg.colorKillLabel, true, false, 1.3 * ts, 22);
				default -> { }
			}
		}

		// 3) The combo counter: bigger and more frantic with every hit.
		if (combo != null && hits >= 2) {
			double growth = Math.min(cfg.comboMaxScale, 1.0 + hits * cfg.comboGrowth);
			stack.add("x" + hits, "white", true, true, growth * ts, 22);
		}

		// 4) Milestone message in the middle of the combo.
		int milestoneIndex = milestoneIndex(hits);
		if (combo != null && milestoneIndex >= 0) {
			String text = msg.milestone[milestoneIndex % msg.milestone.length];
			stack.add(text, "gold", true, true, 1.3 * ts, 26);
		}

		// Sounds and particles.
		if (cfg.sounds) {
			if (hits >= 2) {
				Popups.sound(dim, name, "minecraft:block.note_block.bell", x, mid, z, 0.7 + hits * 0.05);
			}
			if (type == HitType.KILL) {
				Popups.sound(dim, name, "minecraft:entity.experience_orb.pickup", x, mid, z, 0.8);
			}
			if (milestoneIndex >= 0) {
				Popups.sound(dim, name, "minecraft:entity.player.levelup", x, mid, z, 1.0 + milestoneIndex * 0.1);
			}
		}
		if (cfg.particles) {
			if (type == HitType.CRIT) {
				Popups.particle(dim, "minecraft:crit", x, mid, z, 0.3, 0.3, 15);
			} else if (type == HitType.KILL) {
				Popups.particle(dim, "minecraft:totem_of_undying", x, mid, z, 0.3, 0.4, 25);
			}
		}
	}

	private static Combo registerHit(ServerPlayer player, String name, String dim,
			double x, double top, double z, float dealt) {
		Combo c = COMBOS.computeIfAbsent(player.getUUID(), id -> new Combo());
		c.hits++;
		c.total += dealt;
		c.lastTick = tick;
		c.dim = dim;
		c.player = name;
		c.x = x;
		c.y = top;
		c.z = z;
		return c;
	}

	/** Position of this hit count in the milestones list, or -1. */
	private static int milestoneIndex(int hits) {
		for (int i = 0; i < cfg.milestones.length; i++) {
			if (cfg.milestones[i] == hits) {
				return i;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------
	// Tick: end combos that ran out of time
	// ------------------------------------------------------------------

	private static void onTick(MinecraftServer server) {
		tick++;

		long timeout = Math.max(1, Math.round(cfg.comboSeconds * 20.0));
		Iterator<Combo> it = COMBOS.values().iterator();
		while (it.hasNext()) {
			Combo c = it.next();
			if (tick - c.lastTick >= timeout) {
				it.remove();
				endCombo(c);
			}
		}

		Popups.onTick(server, cfg.rainbow);
	}

	private static void endCombo(Combo c) {
		if (!cfg.comboRating || c.hits < cfg.ratingMinHits) {
			return;
		}

		double ts = cfg.textScale;
		Stack stack = new Stack(c.dim, c.x, c.y + 0.3, c.z);

		// Small line with the totals.
		String stats = c.hits + " " + msg.hitsWord + " - "
				+ String.format(Locale.ROOT, "%.1f", c.total) + " " + msg.damageWord;
		stack.add(stats, cfg.colorStats, false, false, 0.7 * ts, 34);

		// The rating itself.
		String text;
		String color;
		boolean rainbow = false;
		double scale;
		int life = 36;
		String sound;
		double pitch;

		if (c.hits >= cfg.amazingHits) {
			text = Messages.random(msg.comboAmazing);
			color = "white";
			rainbow = true;
			scale = 2.6;
			life = 44;
			sound = "minecraft:ui.toast.challenge_complete";
			pitch = 1.0;
		} else if (c.hits >= cfg.greatHits) {
			text = Messages.random(msg.comboGreat);
			color = cfg.colorComboGreat;
			scale = 1.9;
			sound = "minecraft:entity.player.levelup";
			pitch = 1.2;
		} else if (c.hits >= cfg.goodHits) {
			text = Messages.random(msg.comboGood);
			color = cfg.colorComboGood;
			scale = 1.4;
			sound = "minecraft:entity.experience_orb.pickup";
			pitch = 1.0;
		} else {
			text = Messages.random(msg.comboMeh);
			color = cfg.colorComboMeh;
			scale = 1.0;
			life = 28;
			sound = "minecraft:block.note_block.bass";
			pitch = 0.6;
		}

		stack.add(text, color, true, rainbow, scale * ts, life);

		if (cfg.sounds) {
			Popups.sound(c.dim, c.player, sound, c.x, c.y, c.z, pitch);
		}
		if (cfg.particles && c.hits >= cfg.amazingHits) {
			Popups.particle(c.dim, "minecraft:firework", c.x, c.y + 0.5, c.z, 0.5, 0.2, 40);
		}
	}

	// ------------------------------------------------------------------

	/** Stacks several texts one above the other. */
	private static final class Stack {
		private final String dim;
		private final double x, z;
		private double cursor;

		Stack(String dim, double x, double y, double z) {
			this.dim = dim;
			this.x = x;
			this.cursor = y;
			this.z = z;
		}

		void add(String text, String color, boolean bold, boolean rainbow, double scale, int life) {
			String clean = Config.clean(text);
			if (clean.isEmpty()) {
				return;
			}
			double height = 0.22 * scale;
			Popups.text(dim, x, cursor + height / 2.0, z, clean, color, bold, rainbow,
					scale, cfg.riseBlocks, life);
			cursor += height + 0.1;
		}
	}
}
