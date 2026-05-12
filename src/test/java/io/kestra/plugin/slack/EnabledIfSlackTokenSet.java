package io.kestra.plugin.slack;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfEnvironmentVariable(named = "SLACK_BOT_TOKEN", matches = ".+")
public @interface EnabledIfSlackTokenSet {
}
