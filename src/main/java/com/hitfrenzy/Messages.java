package com.hitfrenzy;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Pools of messages (English / Spanish). Anything set in the config file overrides these. */
final class Messages {

	final String[] crit, weak, kill, comboMeh, comboGood, comboGreat, comboAmazing, milestone;
	final String hitsWord, damageWord;

	Messages(Config c) {
		String lang = c.language.toLowerCase(Locale.ROOT);
		if (lang.equals("auto")) {
			lang = Locale.getDefault().getLanguage().equals("es") ? "es" : "en";
		}
		boolean es = lang.equals("es");

		crit = pick(c.msgCrit, es
				? new String[] {"¡CRÍTICO!", "¡CRIT!", "¡BOOM!", "¡PUM!"}
				: new String[] {"CRITICAL!", "CRIT!", "BOOM!", "SMASH!"});
		weak = pick(c.msgWeak, es
				? new String[] {"Meh...", "Flojito...", "Cosquillas"}
				: new String[] {"Meh...", "Weak...", "Tickle"});
		kill = pick(c.msgKill, es
				? new String[] {"¡KILL!!", "¡ELIMINADO!!", "¡K.O.!!", "¡FUERA!!"}
				: new String[] {"KILL!!", "SLAIN!!", "DESTROYED!!", "K.O.!!"});
		comboMeh = pick(c.msgComboMeh, es
				? new String[] {"Meh...", "¿Eso fue todo?", "Ni cosquillas", "Calentando..."}
				: new String[] {"Meh...", "Is that all?", "Barely a combo", "Warming up..."});
		comboGood = pick(c.msgComboGood, es
				? new String[] {"¡Bien!", "¡Buen combo!", "¡Nada mal!", "¡Golpes limpios!"}
				: new String[] {"Nice!", "Good combo!", "Not bad!", "Clean hits!"});
		comboGreat = pick(c.msgComboGreat, es
				? new String[] {"¡GENIAL!", "¡FANTÁSTICO!", "¡BRUTAL!", "¡SALVAJE!"}
				: new String[] {"GREAT!", "FANTASTIC!", "BRUTAL!", "SAVAGE!"});
		comboAmazing = pick(c.msgComboAmazing, es
				? new String[] {"¡ASOMBROSO!!", "¡LEGENDARIO!!", "¡IMPARABLE!!", "¡FRENESÍ TOTAL!!", "¡DIVINO!!"}
				: new String[] {"AMAZING!!", "LEGENDARY!!", "UNSTOPPABLE!!", "TOTAL FRENZY!!", "GODLIKE!!"});
		milestone = pick(c.msgMilestone, es
				? new String[] {"¡EN RACHA!", "¡EN LLAMAS!", "¡IMPARABLE!", "¡FRENESÍ!", "¡MODO BESTIA!"}
				: new String[] {"ON A ROLL!", "ON FIRE!", "UNSTOPPABLE!", "FRENZY!", "BEAST MODE!"});
		hitsWord = es ? "golpes" : "hits";
		damageWord = es ? "de daño" : "damage";
	}

	private static String[] pick(String[] custom, String[] builtIn) {
		return (custom != null && custom.length > 0) ? custom : builtIn;
	}

	static String random(String[] pool) {
		return pool[ThreadLocalRandom.current().nextInt(pool.length)];
	}
}
