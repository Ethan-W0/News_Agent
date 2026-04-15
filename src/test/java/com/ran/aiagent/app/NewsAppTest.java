//package com.ran.aiagent.app;
//
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Test;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class NewsAppTest {
//    @Resource
//    private ChatModel dashscopeChatModel;
//    @Resource
//    private NewsApp newsApp;
//    @Test
//    void doChat() {
//            String message = "你是qwen-max吗？";
//        String response = newsApp.doChat(message, "1");
//        assertNotNull(response);
//        System.out.println(response);
//
//
//    }
//}