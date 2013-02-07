package btwmod.cubemail;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;

import net.minecraft.src.ItemEditableBook;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.NBTTagString;

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
			.append("to:").append(toUsername)
			.append(";")
			.append("title:").append(Base64.encodeToString(title.getBytes("UTF-8"), false))
			.append(";")
			.append("author").append(author)
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
					try {
						title = new String(Base64.decode(pair[1]), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new IllegalArgumentException("Invalid UTF-8 characters in title");
					}
				}
				else if (pair[0].equalsIgnoreCase("author")) {
					author = pair[1];
				}
				else if (pair[0].equalsIgnoreCase("pages")) {
					pages = pair[1].split("\\|");
					
					if (pages.length == 0)
						throw new IllegalArgumentException("Pages has zero length");
					
					for (int p = 0; p < pages.length; p++) {
						try {
							pages[p] = new String(Base64.decode(pages[p]), "UTF-8");
						} catch (UnsupportedEncodingException e) {
							throw new IllegalArgumentException("Invalid UTF-8 characters in page " + (i+1));
						}
					}
				}
				else {
					throw new IllegalArgumentException("Unknown value pair name: " + pair[0]);
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
	
	public static BookData fromItemStack(ItemStack itemStack) {
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (ItemEditableBook.validBookTagContents(tagCompound)) {
			
			NBTTagString title = (NBTTagString)tagCompound.getTag("title");
			NBTTagString author = (NBTTagString)tagCompound.getTag("author");
			NBTTagList pages = (NBTTagList)tagCompound.getTag("pages");
			
			if (pages.tagCount() > 0) {
				NBTTagString firstPage = (NBTTagString)pages.tagAt(0);
				if (firstPage != null && firstPage.data != null) {
					
					Matcher matcher = mod_CubeMail.toLine.matcher(firstPage.data.trim());
					if (matcher.find(0) && matcher.group(1) != null) {
						
						String[] pagesArr = new String[pages.tagCount()];
						for (int i = 0, count = pages.tagCount(); i < count; i++) {
							pagesArr[i] = ((NBTTagString)pages.tagAt(i)).data;
						}
						
						return new BookData(matcher.group(1), title.data, author.data, pagesArr);
					}
				}
			}
		}
		
		return null;
	}
}
