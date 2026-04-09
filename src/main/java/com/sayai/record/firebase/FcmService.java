package com.sayai.record.firebase;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    /**
     * Sends a push notification to a specific topic.
     *
     * @param topic The topic to send to (e.g., "transactions")
     * @param title The title of the notification
     * @param body  The body text of the notification
     */
    public void sendTopicMessage(String topic, String title, String body) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setFcmOptions(WebpushFcmOptions.builder()
                                    .setLink("/fantasy/trade")
                                    .build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setContentAvailable(true)
                                    .build())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM topic message: {}", response);
        } catch (FirebaseMessagingException | RuntimeException e) {
            log.error("Failed to send FCM message to topic: {}", topic, e);
        }
    }

    /**
     * Subscribes a list of client registration tokens to a topic.
     * Web clients must be subscribed via backend because the JS SDK does not support topic subscription.
     */
    public void subscribeToTopic(List<String> tokens, String topic) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(tokens, topic);
            log.info("Subscribed {} tokens to topic {}", tokens.size(), topic);
        } catch (FirebaseMessagingException | RuntimeException e) {
            log.error("Failed to subscribe to topic {}", topic, e);
        }
    }
}
