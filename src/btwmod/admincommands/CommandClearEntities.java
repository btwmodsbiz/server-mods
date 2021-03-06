package btwmod.admincommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import btwmods.Util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityBat;
import net.minecraft.src.EntityGhast;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntitySquid;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.World;
import net.minecraft.src.WrongUsageException;

public class CommandClearEntities extends CommandBase {
	
	private Map<String,Integer> worldNames = null;

	@Override
	public String getCommandName() {
		return "clearentities";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + getCommandName() + " <dimension>";
	}
	
	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			getWorldNames();
			Set keys = worldNames.keySet();
			return getListOfStringsMatchingLastWord(args, (String[])keys.toArray(new String[keys.size()]));
		}
		
		return super.addTabCompletionOptions(sender, args);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		getWorldNames();
		
		if (args.length != 1 || worldNames.get(args[0]) == null)
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);

		World world = MinecraftServer.getServer().worldServers[worldNames.get(args[0]).intValue()];
		List<Entity> toRemove = new ArrayList<Entity>();
		Map<Class, Integer> counts = new HashMap<Class, Integer>();
		
		int count = 0;
		long startTime = System.currentTimeMillis();
		Iterator iterator = world.loadedEntityList.iterator();
		while (iterator.hasNext()) {
			Object obj = iterator.next();
			if (obj instanceof EntityMob || obj instanceof EntityGhast || obj instanceof EntitySquid || obj instanceof EntityBat) {
				if (!((EntityLiving)obj).isPersistenceRequired()) {
					Integer classCount = counts.get(obj.getClass());
					counts.put(obj.getClass(), Integer.valueOf((classCount == null ? 0 : classCount.intValue()) + 1));
					
					toRemove.add((Entity)obj);
					count++;
				}
			}
		}
		
		for (Entity entity : toRemove) {
			world.removeEntity(entity);
		}
		
		List<String> countStrings = new ArrayList<String>();
		countStrings.add("");
		for (Entry<Class, Integer> entry : counts.entrySet()) {
			countStrings.add(entry.getKey().getSimpleName() + "x" + entry.getValue().intValue());
		}
		
		countStrings.set(0, "Removed " + count + " entities in " + (System.currentTimeMillis() - startTime) + "ms:");
		
		for (String message : Util.combineIntoMaxLengthMessages(countStrings, Packet3Chat.maxChatLength, " ", false)) {
			sender.sendChatToPlayer(message);
		}
	}
	
	private void getWorldNames() {
		if (worldNames == null) {
			worldNames = new HashMap<String, Integer>();
			World[] worlds = MinecraftServer.getServer().worldServers;
			for (int i = 0; i < worlds.length; i++) {
				worldNames.put(worlds[i].provider.getDimensionName().replaceAll("[ \\t]+", ""), new Integer(i));
			}
		}
	}
}
