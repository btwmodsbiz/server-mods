package btwmod.centralchat.struct;

import com.google.gson.JsonObject;

public class Username {
	public final String username;
	
	public Username(String username) {
		if (username == null)
			throw new NullPointerException();
		
		this.username = username;
	}
	
	public Username(JsonObject json) {
		username = json.get("username").getAsString();
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("username", username);
		return json;
	}
	
	@Override
	public int hashCode() {
		return username.toLowerCase().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null || obj.getClass() != getClass())
			return false;
		
		Username other = (Username)obj;
		return username.equalsIgnoreCase(other.username);
	}
}
