package com.ai.spring_ai.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Service
public class RagService {

    @Autowired
    private JedisPool jedisPool;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    // ====================== 【存入文档】 ======================
    public void addDocument(String text) {
        System.out.println("文本：" + text);
        System.out.println("✅ 生成模拟向量并存入Redis");

        // 生成模拟向量（相同文本永远生成相同向量）
        float[] vector = generateMockVector(text);

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "doc:" + UUID.randomUUID();
            Map<String, String> doc = new HashMap<>();
            doc.put("text", text);
            doc.put("vector", objectMapper.writeValueAsString(vector));
            jedis.hmset(key, doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ====================== 【相似度搜索 + 排序】 ======================
    public List<String> search(String question, int topK) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("doc:*");
            List<Map.Entry<String, Float>> scoredDocs = new ArrayList<>();

            for (String key : keys) {
                Map<String, String> map = jedis.hgetAll(key);
                String text = map.get("text");

                // ✅ 核心：混合排序算法（关键词匹配占70%，语义相似度占30%）
                float score = calculateHybridScore(question, text);

                scoredDocs.add(new AbstractMap.SimpleEntry<>(key, score));
            }

            // 按分数从高到低排序
            scoredDocs.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

            List<String> result = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, scoredDocs.size()); i++) {
                String key = scoredDocs.get(i).getKey();
                result.add(jedis.hget(key, "text"));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ====================== 【混合排序算法】 ======================
    private float calculateHybridScore(String question, String document) {
        // 1. 关键词匹配得分（权重70%）
        float keywordScore = calculateKeywordScore(question, document);

        // 2. 模拟语义相似度得分（权重30%）
        float semanticScore = calculateMockSemanticScore(question, document);

        // 3. 加权求和
        return keywordScore * 0.7f + semanticScore * 0.3f;
    }

    // 关键词匹配得分计算
    private float calculateKeywordScore(String question, String document) {
        // 把问题和文档都转成小写，按空格分词
        Set<String> questionWords = new HashSet<>(Arrays.asList(question.toLowerCase().split("")));
        Set<String> documentWords = new HashSet<>(Arrays.asList(document.toLowerCase().split("")));

        // 计算交集大小
        int commonWords = 0;
        for (String word : questionWords) {
            if (documentWords.contains(word)) {
                commonWords++;
            }
        }

        // 归一化到0-1之间
        return (float) commonWords / questionWords.size();
    }

    // 模拟语义相似度得分
    private float calculateMockSemanticScore(String question, String document) {
        // 用文本哈希作为种子，相同文本对永远生成相同的相似度
        long seed = (question + document).hashCode();
        Random seededRandom = new Random(seed);

        // 生成0.3-0.7之间的随机数，模拟语义相似度
        return 0.3f + seededRandom.nextFloat() * 0.4f;
    }

    // 生成模拟向量
    private float[] generateMockVector(String text) {
        float[] vector = new float[1024];
        // 用文本哈希作为随机种子，相同文本生成相同向量
        Random seededRandom = new Random(text.hashCode());

        for (int i = 0; i < vector.length; i++) {
            vector[i] = seededRandom.nextFloat() * 2 - 1;
        }
        return vector;
    }




}