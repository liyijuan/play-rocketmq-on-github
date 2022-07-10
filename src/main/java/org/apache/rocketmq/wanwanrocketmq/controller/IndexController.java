package org.apache.rocketmq.wanwanrocketmq.controller;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.wanwanrocketmq.RocketMQConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class IndexController {
    @Autowired
    private RocketMQConfig rocketMQConfig;

    @Autowired
    private DefaultMQProducer producer;

    @RequestMapping(value = "/payload", method = RequestMethod.POST)
    public String get(@RequestBody String eventString, @RequestHeader Map<String, String> headers) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        System.out.println("收到一条消息" + eventString);
        Message message = new Message();
        if (headers != null) {
            for (String key : headers.keySet()) {
                message.putUserProperty(key, headers.get(key));
                System.out.println("一个header=" + key + ", value=" + message.getProperty(key));
            }
        }

        message.setTopic(rocketMQConfig.getTopic());
        message.setBody(eventString.getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(message);
        System.out.println(result.toString());
        return result.toString();
    }
}
