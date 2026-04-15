package com.ran.aiagent.controller;

import com.ran.aiagent.agent.NewsManus;
//import com.ran.aiagent.app.NewsApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {
//    @Resource
//    private NewsApp newsApp;
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ChatModel dashscopeChatModel;


//    @GetMapping("/news/chat/sync")
//    public String doChat(String message , String chatId){
//        return newsApp.doChat(message , chatId);
//    }
//
//    @GetMapping(value = "/news/chat/sse" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> doChatByStream(String message, String chatId){
//        return newsApp.doChatByStream(message , chatId);
//    }

    // 流式调用智能体
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message){
        NewsManus newsManus = new NewsManus(allTools , dashscopeChatModel);
        return newsManus.runStream(message);
    }
}
