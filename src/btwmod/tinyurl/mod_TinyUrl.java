package btwmod.tinyurl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.logging.Level;

import btwmods.IMod;
import btwmods.ModLoader;
import btwmods.PlayerAPI;
import btwmods.io.Settings;
import btwmods.player.IPlayerChatListener;
import btwmods.player.PlayerChatEvent;
import btwmods.util.Base64;

public class mod_TinyUrl implements IMod, IPlayerChatListener {
	
	private static final String redirectHTML = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
		+ "<title>Redirecting...</title>"
		+ "<script>location.replace(\"@@@\");</script></head>"
		+ "<body><p>Redirecting to <a href=\"###\">###</a></p></body></html>";
	
	private Random rand = new Random();
	private String url = null;
	private File redirectDirectory = null;

	@Override
	public String getName() {
		return "Tiny URL";
	}

	@Override
	public void init(Settings settings, Settings data) throws Exception {
		if (settings.hasKey("directory"))
			redirectDirectory = new File(settings.get("directory"));
		else {
			ModLoader.outputError(getName() + "'s directory setting is not set.");
			return;
		}
		
		if (settings.hasKey("url"))
			url = settings.get("url");
		else {
			ModLoader.outputError(getName() + "'s url setting is not set.");
			return;
		}
		
		if (!redirectDirectory.isDirectory()) {
			ModLoader.outputError(getName() + "'s directory setting does not point to a directory.", Level.SEVERE);
			return;
		}
		
		PlayerAPI.addListener(this);
	}

	@Override
	public void unload() throws Exception {
		PlayerAPI.removeListener(this);
	}

	@Override
	public IMod getMod() {
		return this;
	}

	@Override
	public void onPlayerChatAction(PlayerChatEvent event) {
		String message = event.getMessage();
		if (event.type == PlayerChatEvent.TYPE.AUTO_COMPLETE && !message.startsWith("/")) {
			String[] split = message.split(" ", -1);
			String last = split[split.length - 1];
			if (last.length() >= url.length() && (last.startsWith("http://") || last.startsWith("https://")) && !last.startsWith(url)) {
				try {
					File redirectFile;
					do {
						redirectFile = new File(redirectDirectory, getRandHash());
					} while (redirectFile.exists());
					
					BufferedWriter bw = new BufferedWriter(new FileWriter(redirectFile));
					bw.write(redirectHTML
						.replace("@@@", last.replace("'", "\\'").replace("\"", "\\\"").replace("\0", "\\0"))
						.replace("###", last.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;").replace("<", "&lt;").replace(">", "&gt;")));
					bw.close();
					
					event.addCompletion(url + redirectFile.getName());
					event.markHandled();
				} catch (Exception e) {
					ModLoader.outputError(e, getName() + " failed to create a redirect file:" + e.getMessage());
				}
			}
		}
	}
	
	private String getRandHash() {
		String hash = null;
		byte[] bytes = new byte[3];
		do {
			rand.nextBytes(bytes);
			hash = Base64.encodeToString(bytes, false);
			
			if (hash.indexOf("/") >= 0 || hash.indexOf("+") >= 0)
				System.out.println(hash);
		} while (hash.indexOf("/") >= 0 || hash.indexOf("+") >= 0);
		
		return hash.replace("=", "");
	}

}
