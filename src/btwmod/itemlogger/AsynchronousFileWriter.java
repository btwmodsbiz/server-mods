package btwmod.itemlogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import btwmods.ModLoader;

public class AsynchronousFileWriter {
	
	private final mod_ItemLogger mod;
	private final ConcurrentLinkedQueue<QueuedLine> writeQueue = new ConcurrentLinkedQueue<QueuedLine>();
	private volatile WriterThread writerThread = null;
	private int queuedCount = 0;
	
	public AsynchronousFileWriter(mod_ItemLogger mod) {
		this.mod = mod;
	}
	
	public int getWriteCount() {
		return queuedCount;
	}
	
	public boolean isThreadRunning() {
		return writerThread != null && writerThread.isRunning();
	}
	
	public void queueWrite(File file, String line) {
		writeQueue.add(new QueuedLine(file, line));
		
		if (queuedCount % 50 == 0) {
			if (!isThreadRunning()) {
				new Thread(writerThread = new WriterThread()).start();
			}
		}
		
		queuedCount++;
	}
	
	private class QueuedLine {
		public final File file;
		public final String line;
		
		public QueuedLine(File file, String line) {
			this.file = file;
			this.line = line;
		}
	}
	
	private class WriterThread implements Runnable {
		private boolean isRunning = true;
		
		public boolean isRunning() {
			return isRunning;
		}
		
		@Override
		public void run() {
			Map<File, List<String>> writesByFile = new LinkedHashMap<File, List<String>>();
			
			while (writerThread == this) {
				
				// Dequeue the lines and group by file.
				QueuedLine write = null;
				while ((write = writeQueue.poll()) != null) {
					List lines = writesByFile.get(write.file);
					if (lines == null)
						writesByFile.put(write.file, lines = new ArrayList<String>());
					
					lines.add(write.line);
				}
				
				// Write the lines to each file.
				writeToFiles(writesByFile);
				
				// Clear the dequeued lines.
				writesByFile.clear();
				
				try {
					Thread.sleep(40L);
				} catch (InterruptedException e) {
					
				}
			}
			
			isRunning = false;
		}
		
		private void writeToFiles(Map<File, List<String>> writesByFile) {
			BufferedWriter writer = null;
			for (Entry<File, List<String>> entry : writesByFile.entrySet()) {
				try {
					writer = new BufferedWriter(new FileWriter(entry.getKey(), true));
					for (String line : entry.getValue()) {
						writer.write(line);
						writer.newLine();
					}
				} catch (IOException e) {
					StringBuffer sb = new StringBuffer();
					for (String line : entry.getValue()) {
						sb.append("\n\t");
						sb.append(line);
					}
					
					ModLoader.outputError(e, mod.getName() + " failed to write the following to \"" + entry.getKey().getPath() + "\":", Level.SEVERE);
				}
				finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							
						}
					}
				}
			}
		}
	}
}
