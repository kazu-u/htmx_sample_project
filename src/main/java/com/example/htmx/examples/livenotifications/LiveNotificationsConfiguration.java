package com.example.htmx.examples.livenotifications;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 通知ストリームの送信処理を小さな専用スレッドプールで実行する。 */
@Configuration
class LiveNotificationsConfiguration {
    @Bean
    ThreadPoolTaskScheduler liveNotificationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(LiveNotificationStream.MAX_ACTIVE_STREAMS);
        scheduler.setThreadNamePrefix("live-notification-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
