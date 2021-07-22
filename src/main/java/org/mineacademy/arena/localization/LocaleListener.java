package org.mineacademy.arena.localization;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public class LocaleListener implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
	}

	@EventHandler
	public void onLocaleChange(PlayerLocaleChangeEvent event){
		/*Player player = event.getPlayer();

		String newLocale = Remain.getLocale(player);

		if(newLocale.equals("en_gb"))
			Common.tell(player,"You're locale is now english.");

		if(newLocale.equals("de_de"))
			Common.tell(player,"Dein Locale ist nun deutsch.");*/
	}
}
