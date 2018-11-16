package de.timmyrs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.Calendar;
import java.util.GregorianCalendar;

@SuppressWarnings("unused")
public class LobbyUtils extends JavaPlugin implements Listener
{
	private Team team;
	private static World world;

	@Override
	public void onEnable()
	{
		getConfig().addDefault("worldName", "world");
		getConfig().addDefault("realTimeToInGameTime", true);
		getConfig().addDefault("disableJoinAndQuitMessages", true);
		getConfig().addDefault("forceSpawnPosition", true);
		getConfig().addDefault("playersAreInvincible", true);
		getConfig().addDefault("playerCollision", false);
		getConfig().addDefault("playerVisibility", 2);
		getConfig().addDefault("giveNightVision", true);
		getConfig().addDefault("vectorBorder", 0);
		getConfig().options().copyDefaults(true);
		saveConfig();
		reloadConfig();
		final Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();
		team = scoreboard.getTeam("lobbyPlayers");
		if(team == null)
		{
			team = scoreboard.registerNewTeam("lobbyPlayers");
		}
		team.setCanSeeFriendlyInvisibles(getConfig().getInt("playerVisibility") == 1);
		getServer().dispatchCommand(getServer().getConsoleSender(), "scoreboard teams option lobbyPlayers collisionRule " + (getConfig().getBoolean("playerCollision") ? "always" : "never"));
		world = getServer().getWorld(getConfig().getString("worldName"));
		if(getConfig().getBoolean("realTimeToInGameTime"))
		{
			world.setGameRuleValue("doDaylightCycle", "false");
			getServer().getScheduler().scheduleSyncRepeatingTask(this, LobbyUtils::realTimeToInGameTime, 0, 100);
		}
		getServer().getPluginManager().registerEvents(this, this);
		for(Player p : getServer().getOnlinePlayers())
		{
			resetPlayer(p);
		}
	}

	private void resetPlayer(Player p)
	{
		p.setHealth(20);
		p.setExhaustion(0);
		p.setFoodLevel(20);
		p.setLevel(0);
		p.setExp(0);
		p.setFireTicks(0);
		for(PotionEffect pe : p.getActivePotionEffects())
		{
			p.removePotionEffect(pe.getType());
		}
		if(!team.getEntries().contains(p.getName()))
		{
			team.addEntry(p.getName());
		}
		if(getConfig().getBoolean("giveNightVision"))
		{
			p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100000, 0), true);
		}
		final int playerVisibility = getConfig().getInt("playerVisibility");
		if(playerVisibility == 0)
		{
			for(Player p_ : getServer().getOnlinePlayers())
			{
				p.hidePlayer(this, p);
				p_.hidePlayer(this, p_);
			}
		}
		else if(playerVisibility == 1)
		{
			p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 0), true);
		}
	}

	private Location getSpawnLocation()
	{
		final Location spawnLocation = world.getSpawnLocation();
		return new Location(world, spawnLocation.getBlockX() + 0.5D, (double) spawnLocation.getBlockY(), spawnLocation.getBlockZ() + 0.5D);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		resetPlayer(e.getPlayer());
		if(getConfig().getBoolean("forceSpawnPosition"))
		{
			e.getPlayer().teleport(getSpawnLocation());
		}
		if(getConfig().getBoolean("disableJoinAndQuitMessages"))
		{
			e.setJoinMessage("");
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		if(getConfig().getBoolean("disableJoinAndQuitMessages"))
		{
			e.setQuitMessage("");
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e)
	{
		if(e.getEntity() instanceof Player)
		{
			final Player p = (Player) e.getEntity();
			if(getConfig().getBoolean("playersAreInvincible"))
			{
				e.setCancelled(true);
			}
			else if(p.getHealth() - e.getFinalDamage() <= 0)
			{
				e.setCancelled(true);
				resetPlayer(p);
				p.teleport(getSpawnLocation());
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e)
	{
		if(getConfig().getInt("vectorBorder") == 0)
		{
			return;
		}
		if((Math.abs(e.getTo().getX() - getSpawnLocation().getX()) > getConfig().getInt("vectorBorder")) || (Math.abs(e.getTo().getZ() - getSpawnLocation().getZ()) > getConfig().getInt("vectorBorder")))
		{
			if((Math.abs(e.getTo().getX() - getSpawnLocation().getX()) > getConfig().getInt("vectorBorder") + 16) || (Math.abs(e.getTo().getZ() - getSpawnLocation().getZ()) > getConfig().getInt("vectorBorder") + 16))
			{
				e.setTo(getSpawnLocation());
				e.getPlayer().sendMessage("This is why we can't have nice things.");
				return;
			}
			final Vector v = e.getFrom().toVector().subtract(e.getTo().toVector()).multiply(2);
			if(v.getY() < 0.5)
			{
				v.setY(2);
			}
			e.getPlayer().setVelocity(v);
		}
	}

	private static void realTimeToInGameTime()
	{
		final GregorianCalendar c = new GregorianCalendar();
		world.setTime((1000 * c.get(Calendar.HOUR_OF_DAY)) + ((100 / 6) * c.get(Calendar.MINUTE)) + 18000);
	}
}
