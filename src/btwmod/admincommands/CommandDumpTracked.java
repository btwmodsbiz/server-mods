package btwmod.admincommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import btwmods.ModLoader;
import btwmods.ReflectionAPI;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityTracker;
import net.minecraft.src.EntityTrackerEntry;
import net.minecraft.src.EntityXPOrb;
import net.minecraft.src.ICommandSender;

public class CommandDumpTracked extends CommandBase {
	
	private static Set[] trackedEntities = null;
	
	public CommandDumpTracked() {
		Field trackedEntitiesField = ReflectionAPI.getPrivateField(EntityTracker.class, "trackedEntities");
		
		if (trackedEntitiesField == null) {
			ModLoader.outputError(CommandDumpTracked.class.getSimpleName() + " failed to get " + EntityTracker.class.getName() + "#trackedEntitySet Field");
		}
		else {
			try {
				MinecraftServer server = MinecraftServer.getServer();
				
				trackedEntities = new Set[server.worldServers.length];
				for (int i = 0; i < server.worldServers.length; i++) {
					trackedEntities[i] = (Set)trackedEntitiesField.get(server.worldServers[i].getEntityTracker());
				}
			} catch (IllegalAccessException e) {
				ModLoader.outputError(e, CommandDumpTracked.class.getSimpleName() + " failed to get trackedEntitySet instance: " + e.getMessage());
			}
		}
	}

	@Override
	public String getCommandName() {
		return "dumptracked";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (trackedEntities == null)
			return;
		
		int trackedEntityCount = 0;
		File dump = new File(new File("."), "trackeddump.txt");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < trackedEntities.length; i++) {
			sb.append("World ").append(i).append(":\n");
			Iterator trackedIterator = trackedEntities[i].iterator();
			while (trackedIterator.hasNext()) {
				trackedEntityCount++;
				Object obj = ((EntityTrackerEntry)trackedIterator.next()).trackedEntity;
				if (obj == null) {
					sb.append("Tracking null!\n");
				}
				else if (obj instanceof Entity) {
					Entity entity = (Entity)obj;
					sb.append("ID ")
						.append(entity.entityId).append(": ")
						.append(entity instanceof EntityItem ? ((EntityItem)entity).getEntityItem().getItemName() : entity.getEntityName())
						.append(" (").append(entity.getClass().getSimpleName()).append(") in chunk ")
						.append(entity.chunkCoordX).append(",").append(entity.chunkCoordZ)
						.append(" at ").append((long)entity.posX).append("/").append((long)entity.posY).append("/").append((long)entity.posZ);
					
					if (entity instanceof EntityItem) {
						sb.append(" with age of ").append(((EntityItem)entity).age);
					}
					else if (entity instanceof EntityXPOrb) {
						sb.append(" with age of ").append(((EntityXPOrb)entity).xpOrbAge);
					}
					
					sb.append("\n");
				}
				else {
					sb.append("Tracking non-entity: ").append(obj.getClass().getSimpleName()).append("\n");
				}
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(dump));
			writer.write(sb.toString());
			writer.flush();
			writer.close();
			sender.sendChatToPlayer("Dumped " + trackedEntityCount + " tracked entities.");
		} catch (IOException e) {
			sender.sendChatToPlayer("Failed to dump tracked entities: " + e.getMessage());
		}
	}

}
