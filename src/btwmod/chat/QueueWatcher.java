package btwmod.chat;

import java.util.concurrent.BlockingQueue;

import btwmod.chat.central.Message;

public class QueueWatcher implements Runnable {
	
	private final BlockingQueue<Message> queue;
	private final IMessageHandler handler;
	private volatile boolean isRunning = false;
	private volatile boolean stop = false;
	
	public QueueWatcher(BlockingQueue<Message> queue, IMessageHandler handler) {
		this.queue = queue;
		this.handler = handler;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void stop() {
		stop = true;
	}

	@Override
	public void run() {
		isRunning = true;
		
		try {
			while (!stop) {
				handler.handleMesage(queue.take());
			}
		} catch (InterruptedException e) {
			
		}
		
		isRunning = false;
	}
	
}