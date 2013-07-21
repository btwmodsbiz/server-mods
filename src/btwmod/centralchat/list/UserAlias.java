package btwmod.centralchat.list;

import com.google.gson.JsonObject;

public class UserAlias extends Username {
	public final String alias;
	
	public UserAlias(String username, String alias) {
		super(username);
		this.alias = alias;
	}
	
	public UserAlias(JsonObject json) {
		super(json);
		alias = json.get("alias").getAsString();
	}
	
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("alias", alias);
		return json;
	}
}
