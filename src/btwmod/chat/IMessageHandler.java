package btwmod.chat;

import btwmod.chat.central.Message;

public interface IMessageHandler {
	public void handleMesage(Message message);
}