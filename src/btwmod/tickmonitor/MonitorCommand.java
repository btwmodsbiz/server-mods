package btwmod.tickmonitor;

import java.util.Arrays;
import java.util.List;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.WrongUsageException;

public class MonitorCommand extends CommandBase {
	
	private mod_TickMonitor mod;
	
	public MonitorCommand(mod_TickMonitor mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandName() {
		return "monitor";
	}
	
	public String getCommandUsage(ICommandSender sender)
    {
        return "/" + getCommandName() + " [<on|off|status|hidecoords|showcoords>]";
    }

	@Override
	public List getCommandAliases() {
        return Arrays.asList(new String[] { "tick", "tickmonitor" });
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
			sender.sendChatToPlayer(mod.getName() + " is " + (mod.isRunning() ? "on" : "off") + ".");
		}
		else if (args[0].equalsIgnoreCase("on")) {
			if (mod.isRunning()) {
				sender.sendChatToPlayer(mod.getName() + " is already on.");
			}
			else {
				mod.setIsRunning(true);
				sender.sendChatToPlayer(mod.getName() + " is now on.");
			}
		}
		else if (args[0].equalsIgnoreCase("off")) {
			if (mod.isRunning()) {
				mod.setIsRunning(false);
				sender.sendChatToPlayer(mod.getName() + " is now off.");
			}
			else {
				sender.sendChatToPlayer(mod.getName() + " is already off.");
			}
		}
		else if (args[0].equalsIgnoreCase("hidecoords")) {
			if (mod.hideChunkCoords) {
				sender.sendChatToPlayer(mod.getName() + " is already hiding coords.");
			}
			else {
				mod.hideChunkCoords = true;
				sender.sendChatToPlayer(mod.getName() + " is now hiding coords.");
			}
		}
		else if (args[0].equalsIgnoreCase("showcoords")) {
			if (!mod.hideChunkCoords) {
				sender.sendChatToPlayer(mod.getName() + " is already showing coords.");
			}
			else {
				mod.hideChunkCoords = false;
				sender.sendChatToPlayer(mod.getName() + " is now showing coords.");
			}
		}
		else {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}
	}
}
