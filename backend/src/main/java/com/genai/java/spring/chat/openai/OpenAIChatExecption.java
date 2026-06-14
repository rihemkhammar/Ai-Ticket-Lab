package com.genai.java.spring.chat.openai;

public class OpenAIChatExecption extends Exception {
    public OpenAIChatExecption(String message){
        super(message);
    }
    public OpenAIChatExecption(String message ,Throwable cause){
        super(message,cause);
    }
}
