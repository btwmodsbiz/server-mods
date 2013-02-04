package btwmod.cubemail;

import java.io.UnsupportedEncodingException;

import btwmods.util.Base64;

public class BookData {
	public final String toUsername;
	public final String title;
	public final String author;
	public final String[] pages;
	
	public BookData(String toUsername, String title, String author, String[] pages) {
		this.toUsername = toUsername;
		this.title = title;
		this.author = author;
		this.pages = pages;
	}
	
	public String serialize() throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder()
			.append("to:").append(Base64.encodeToString(toUsername.getBytes("UTF-8"), false))
			.append(";")
			.append("title:").append(Base64.encodeToString(title.getBytes("UTF-8"), false))
			.append(";")
			.append("author").append(Base64.encodeToString(author.getBytes("UTF-8"), false))
			.append(";")
			.append("pages:");
		
		for (int i = 0; i < pages.length; i++) {
			if (i > 0) sb.append("|");
			sb.append(Base64.encodeToString(pages[i].getBytes("UTF-8"), false));
		}
		
		return sb.toString();
	}
	
	public static BookData deserialze(String data) throws IllegalArgumentException {
		if (data == null || data.length() == 0)
			throw new IllegalArgumentException("null or zero length");
		
		String to = null;
		String title = null;
		String author = null;
		String[] pages = null;
		
		String[] pairs = data.split(";");
		for (int i = 0; i < pairs.length; i++) {
			String[] pair = pairs[i].split(":", 2);
			if (pair.length == 2) {
				if (pair[0].equalsIgnoreCase("to")) {
					to = pair[1];
				}
				else if (pair[0].equalsIgnoreCase("title")) {
					title = pair[1];
				}
				else if (pair[0].equalsIgnoreCase("author")) {
					author = pair[1];
				}
				else if (pair[0].equalsIgnoreCase("pages")) {
					pages = pair[1].split("\\|");
					
					if (pages.length == 0)
						throw new IllegalArgumentException("Pages has zero length");
				}
				else {
					throw new IllegalArgumentException("Bad value pair name: " + pair[0]);
				}
			}
		}
		
		if (to == null)
			throw new IllegalArgumentException("to value was not found");
		
		if (title == null)
			throw new IllegalArgumentException("title value was not found");
		
		if (author == null)
			throw new IllegalArgumentException("to value was not found");
		
		if (pages == null)
			throw new IllegalArgumentException("pages value was not found");
		
		return new BookData(to, title, author, pages);
	}
}
