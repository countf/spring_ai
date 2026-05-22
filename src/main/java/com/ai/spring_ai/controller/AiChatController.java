package com.ai.spring_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class AiChatController {
    private final ChatClient chatClient;
    // 存储对话历史：key=会话ID，value=该会话的所有消息
    private final Map<String, List<Message>> chatHistory = new ConcurrentHashMap<>();
    // 全局系统提示词
    private static final SystemMessage SYSTEM_PROMPT = new SystemMessage("你是一个友好的AI助手，全程用中文回答");

    public AiChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 带记忆的多轮对话接口
    @GetMapping("/chat")
    public String chat(
            @RequestParam String sessionId,  // 新增：每个用户用不同的sessionId
            @RequestParam String message
    ) {
        // 1. 获取或创建这个会话的历史
        List<Message> messages = chatHistory.computeIfAbsent(sessionId, k -> {
            List<Message> init = new ArrayList<>();
            init.add(SYSTEM_PROMPT);
            return init;
        });

        // 2. 把用户的问题加入历史
        messages.add(new UserMessage(message));

        // 3. 调用AI（会带上所有历史消息）
        String response = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        // 4. 把AI的回答也加入历史
        messages.add(new AssistantMessage(response));

        return response;
    }

    // 清空指定会话的历史
    @GetMapping("/chat/clear")
    public String clearHistory(@RequestParam String sessionId) {
        chatHistory.remove(sessionId);
        return "会话" + sessionId + "的历史已清空";
    }
}
