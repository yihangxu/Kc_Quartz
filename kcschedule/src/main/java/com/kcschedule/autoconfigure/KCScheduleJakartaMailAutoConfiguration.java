package com.kcschedule.autoconfigure;

import com.kcschedule.core.msg.MailNotifyChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "jakarta.mail.Session")
public class KCScheduleJakartaMailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(com.kcschedule.core.msg.NotifyChannel.class)
    public MailNotifyChannel mailNotifyChannel() {
        return new MailNotifyChannel();
    }
}
