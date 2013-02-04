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
	
	public String toBase64() throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		
		sb
			.append("toUsername:").append(Base64.encodeToString(toUsername.getBytes("UTF-8"), false))
			.append(";")
			.append("title:").append(Base64.encodeToString(title.getBytes("UTF-8"), false))
			.append(";")
			.append("author").append(Base64.encodeToString(author.getBytes("UTF-8"), false))
			.append(";")
			.append("pages");
		
		for (int i = 0; i < pages.length; i++) {
			if (i > 0) sb.append("|");
			sb.append(Base64.encodeToString(pages[i].getBytes("UTF-8"), false));
		}
		
		return sb.toString();
	}
}
