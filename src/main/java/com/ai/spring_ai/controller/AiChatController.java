package com.ai.spring_ai.controller;


import com.ai.spring_ai.Service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin(origins = "*") // 跨域
public class AiChatController {
    private final ChatClient chatClient;
    private final RagService ragService;
    // 存储对话历史：key=会话ID，value=该会话的所有消息
    private final Map<String, List<Message>> chatHistory = new ConcurrentHashMap<>();
    // 全局系统提示词
    private static final SystemMessage SYSTEM_PROMPT = new SystemMessage("你是一个友好的AI助手，全程用中文回答.");

    public AiChatController(ChatClient.Builder builder,RagService ragService) {
        this.chatClient = builder.build();
        this.ragService = ragService;
    }

    // ====================== Day3：Function Calling 工具 ======================
    public static class MyTools {
        // AI 自动调用：查天气
        @Tool(description = "查询指定城市的当前天气")
        public String getWeather(
                @ToolParam(description = "城市名称") String city
        ) {
            System.out.println("✅ AI 调用工具：查询 " + city + " 天气");
            return city + " 今天晴天，25度，非常舒适";
        }

        // AI 自动调用：计算器
        @Tool(description = "进行加法计算")
        public Integer add(
                @ToolParam(description = "数字1") Integer a,
                @ToolParam(description = "数字2") Integer b
        ) {
            System.out.println("✅ AI 调用工具：计算 " + a + " + " + b);
            return a + b;
        }
    }

    // 带记忆的多轮对话接口
    @GetMapping("/chat")
    public String chat(
            @RequestParam String sessionId,  // 新增：每个用户用不同的sessionId
            @RequestParam String message
    ) {


        // 从Redis搜索最相关的1条文本
        List<String> referenceList = ragService.search(message, 1);
        String referenceText = referenceList.isEmpty() ? "可以自由回答" : referenceList.get(0);

        SystemMessage ragPrompt = new SystemMessage("""
                根据参考资料回答问题。
                如果没有参考资料，你可以自由回答。
                参考资料：%s
                """.formatted(referenceText));


        // 1. 获取或创建这个会话的历史
        List<Message> messages = chatHistory.computeIfAbsent(sessionId, k -> {
            List<Message> init = new ArrayList<>();
            init.add(SYSTEM_PROMPT);
            return init;
        });


        messages.add(SYSTEM_PROMPT);
        messages.add(ragPrompt);


        // 2. 把用户的问题加入历史
        messages.add(new UserMessage(message));

        // 3. 调用AI（会带上所有历史消息）
        String response = chatClient.prompt()
                .messages(messages)
                .tools(new MyTools())
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

    @GetMapping("/add")
    public String add(@RequestParam String text) {
        ragService.addDocument(text);
        return "✅ 存入Redis成功：" + text;
    }
//
//    @GetMapping("/search")
//    public List<Map<String, String>> search(@RequestParam String question) {
//        return ragService.search(question, 3);
//    }
    @GetMapping("/search")
    public List<String> search(@RequestParam String question) {
        //数字代表显示答案的数量
        return ragService.search(question, 1);
    }
}


