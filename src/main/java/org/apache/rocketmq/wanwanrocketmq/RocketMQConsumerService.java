package org.apache.rocketmq.wanwanrocketmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RocketMQConsumerService {
    @Autowired
    private RocketMQConfig rocketMQConfig;

    @Autowired
    private DingdingConfig dingdingConfig;

    @Bean
    public DefaultMQProducer getProducer() throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setProducerGroup(rocketMQConfig.getProducerGroup());
        producer.setNamesrvAddr(rocketMQConfig.getNamesrv());
        producer.start();
        return producer;
    }

    @Bean(destroyMethod = "shutdown")
    private DefaultMQPushConsumer buildConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(rocketMQConfig.getNamesrv());
        consumer.setConsumerGroup(rocketMQConfig.getConsumerGroup());
        consumer.subscribe(rocketMQConfig.getTopic(), "*");
        consumer.setConsumeMessageBatchMaxSize(1);

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                if (list.isEmpty()) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                return onMessage(list.get(0));
            }
        });

        consumer.start();
        return consumer;
    }

    private ConsumeConcurrentlyStatus onMessage(MessageExt messageExt) {
        String eventType = messageExt.getUserProperty("x-github-event");
        JSONObject jsonObject = (JSONObject) JSON.parse(new String(messageExt.getBody(), StandardCharsets.UTF_8));

        String url = "";
        String action = jsonObject.getString("action");
        String writer = "";
        String content = "";
        if ("issues".equalsIgnoreCase(eventType)) {
            url = jsonObject.getJSONObject("issue").getString("html_url");
            writer = jsonObject.getJSONObject("issue").getJSONObject("user").getString("login");
            content = jsonObject.getJSONObject("issue").getString("title");
        } else if ("issue_comment".equalsIgnoreCase(eventType)) {
            url = jsonObject.getJSONObject("comment").getString("html_url");
            writer = jsonObject.getJSONObject("comment").getJSONObject("user").getString("login");
            content = jsonObject.getJSONObject("comment").getString("body");
        } else if ("pull_request".equalsIgnoreCase(eventType)) {
            url = jsonObject.getJSONObject("pull_request").getString("html_url");
            writer = jsonObject.getJSONObject("pull_request").getJSONObject("user").getString("login");
            content = jsonObject.getJSONObject("pull_request").getString("body");
        } else {
            if (jsonObject.containsKey(eventType)) {
                url = jsonObject.getJSONObject(eventType).getString("html_url");
                if (jsonObject.containsKey("user")) {
                    writer = jsonObject.getJSONObject(eventType).getJSONObject("user").getString("login");
                }
            }
            content = "未处理的github事件";
        }

        try {
            String str = String.format("[%s] %s",
                    dingdingConfig.getKeyword(),
                    String.format("@%s %s %s, content is : %s, more detail: %s", writer, action, eventType, content, url));

            System.out.println("onMessage=" + str + "\n");
            System.out.println("onMessage=" + HttpUtil.send(dingdingConfig.getWebhookUrl(), str));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
