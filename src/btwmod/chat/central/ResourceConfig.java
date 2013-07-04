package btwmod.chat.central;

public class ResourceConfig {
	public final String mode;
	public final String id;
	public final String key;
	
	protected ResourceConfig(String mode, String id, String key) {
		this.mode = mode;
		this.id = id;
		this.key = key;
	}
	
	public static ResourceConfig parse(String resourceDescriptor) {
		String[] pathParts = resourceDescriptor.split("\\?")[0].replaceFirst("^/", "").split("/");
		if (pathParts.length == 3 && pathParts[0].equalsIgnoreCase("user")) {
			return new ResourceConfig(pathParts[0], pathParts[1], pathParts[2]);
		}
		else if (pathParts.length == 3 && pathParts[0].equalsIgnoreCase("server")) {
			return new ResourceConfig(pathParts[0], pathParts[1], pathParts[2]);
		}
		else {
			return null;
		}
	}
}