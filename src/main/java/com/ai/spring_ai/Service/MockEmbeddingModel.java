package com.ai.spring_ai.Service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
@Primary
public class MockEmbeddingModel implements EmbeddingModel {

    // 你原来的模拟向量生成方法
    public float[] generateVectorByMiMoV25(String text) {
        System.out.println("✅ 模拟生成 1024 维向量（用于RAG测试）");

        float[] vector = new float[1024];
        Random random = new Random();
        for (int i = 0; i < vector.length; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }
        return vector;
    }

    // 1. 实现 call 方法（批量处理请求）
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return new EmbeddingResponse(
                request.getInstructions().stream()
                        .map(doc -> {
                            float[] vector = generateVectorByMiMoV25(doc);
                            return new Embedding(vector, 0);
                        })
                        .toList()
        );
    }

    // 2. 实现 embed 方法（单个文档处理）
    @Override
    public float[] embed(Document document) {
        // 直接调用你的模拟方法生成向量
        return generateVectorByMiMoV25(document.getText());
    }
}