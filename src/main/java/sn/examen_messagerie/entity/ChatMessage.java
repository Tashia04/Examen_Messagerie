package sn.examen_messagerie.entity;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private String action;
    private String sender;
    private String receiver;
    private String content;
    private MessageStatus status;

    // constructeurs, getters et setters


    public ChatMessage() {
    }

    public ChatMessage(String action, String sender, String receiver, String content, MessageStatus status) {
        this.action = action;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}