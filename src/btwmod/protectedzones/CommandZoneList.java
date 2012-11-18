package btwmod.protectedzones;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.CommandBase;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.WrongUsageException;

public class CommandZoneList extends CommandBase {
	
	private final mod_ProtectedZones mod;
	
	public CommandZoneList(mod_ProtectedZones mod) {
		this.mod = mod;
	}

	@Override
	public String getCommandUsage(ICommandSender par1iCommandSender) {
		return "/" + getCommandName() + " [page]";
	}

	@Override
	public String getCommandName() {
		return "zonelist";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length > 1)
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		
		String headerShort = "Zones: ";
		String headerLong = "Zones (Page XX/YY): ";
		int page = args.length == 1 ? parseIntWithMin(sender, args[0], 1) : 1;
		int maxListSize = Packet3Chat.maxChatLength - Math.max(headerShort.length(), headerLong.length());
		
		List<String> names = mod.getZoneNames();
		ArrayList<String> pages = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		
		for (String name : names) {
			
			if (sb.length() + name.length() + (sb.length() > 0 ? 2 : 0) > maxListSize) {
				pages.add(sb.toString());
				sb.setLength(0);
			}
			
			if (sb.length() > 0) sb.append(", ");
			sb.append(name);
		}
		
		if (sb.length() > 0)
			pages.add(sb.toString());
		
		page = Math.min(page, pages.size());
		
		sender.sendChatToPlayer((pages.size() == 1 ? headerShort : headerLong.replaceAll("XX/YY", page + "/" + pages.size()))
				+ pages.get(Math.min(page, pages.size()) - 1));
	}

}
