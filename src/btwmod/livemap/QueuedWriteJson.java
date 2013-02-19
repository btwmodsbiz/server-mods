package btwmod.livemap;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import btwmods.io.QueuedWrite;

public class QueuedWriteJson extends QueuedWrite {
	
	public final Gson gson;
	public final JsonElement json;

	public QueuedWriteJson(File file, JsonElement json) {
		this(file, null, json);
	}

	public QueuedWriteJson(File file, Gson gson, JsonElement json) {
		super(file, TYPE.OVERWRITE_SAFE);
		this.gson = gson;
		this.json = json;
	}

	@Override
	public void write(Writer writer) throws IOException {
		writer.write(toString());
	}

	@Override
	public String toString() {
		return gson == null ? json.toString() : gson.toJson(json);
	}
}