package btwmod.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.ServerConfigurationManager;
import btwmods.IMod;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IInstanceListener;
import btwmods.player.InstanceEvent;

public class mod_AutoOp implements IMod, IInstanceListener {

	@Override
	public String getName() {
		return "Auto Op";
	}

	@Override
	public void init(Settings settings) {
		PlayerAPI.addListener(this);
	}

	@Override
	public void unload() {
		
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void instanceAction(InstanceEvent event) {
		ServerConfigurationManager manager = MinecraftServer.getServer().getConfigurationManager();
		if (event.getType() == InstanceEvent.TYPE.LOGIN && !manager.getOps().contains(event.getPlayerInstance().username)) {
			manager.addOp(event.getPlayerInstance().username);
			CommandBase.notifyAdmins(event.getPlayerInstance(), "commands.op.success", new Object[] { event.getPlayerInstance().username });
		}
	}
}
