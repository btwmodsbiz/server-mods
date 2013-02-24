package btwmod.livemap;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class JsonClassName extends JsonElement {
	
	private Class clazz = null;
	private Object obj = null;
	
	public JsonClassName(Object obj) {
		this.obj = obj;
	}
	
	public JsonClassName(Class clazz) {
		this.clazz = clazz;
	}

	@Override
	public boolean isJsonPrimitive() {
		return !isJsonNull();
	}

	@Override
	public boolean isJsonNull() {
		return clazz == null && obj == null;
	}

	@Override
	public JsonPrimitive getAsJsonPrimitive() {
		if (isJsonNull())
			throw new IllegalStateException("This is not a JSON Primitive.");
		
		return new JsonPrimitive(clazz == null ? obj.getClass().getSimpleName() : clazz.getSimpleName());
	}
	
}
