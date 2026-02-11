package io.kestra.plugin.slack;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiTextResponse;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Rethrow;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSlackClientConnection extends Task {
    @Schema(
        title = "The slack token"
    )
    @NotNull
    protected Property<String> token;

    @Getter(value = AccessLevel.PRIVATE)
    protected transient Slack slack;

    protected MethodsClient client(RunContext runContext) throws IllegalVariableEvaluationException {
        if (slack == null) {
            SlackConfig slackConfig = SlackConfig.DEFAULT;

            slack = Slack.getInstance(slackConfig);
        }

        return slack
            .methods(runContext.render(this.token).as(String.class).orElseThrow());
    }

    protected <R extends SlackApiTextResponse> R call(RunContext runContext, Rethrow.FunctionChecked<MethodsClient, R, Exception> call) {
        try {
            R response = call.apply(this.client(runContext));

            if (response.getWarning() != null) {
                runContext.logger().warn(response.getWarning());
            }

            if (response.isOk()) {
                return response;
            }

            throw new IllegalStateException(response.getError() + ": " + response);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
