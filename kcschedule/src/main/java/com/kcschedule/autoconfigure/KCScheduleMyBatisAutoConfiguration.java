package com.kcschedule.autoconfigure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.mybatis.spring.annotation.MapperScan")
@MapperScan("com.kcschedule.core.dao")
public class KCScheduleMyBatisAutoConfiguration {
}
