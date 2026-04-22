package io.kestra.plugin.slack;

import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiTextResponse;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Rethrow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSlackClientConnection extends Task {
    @Schema(
        title = "Slack token"
    )
    @NotNull
    @PluginProperty(group = "main", secret = true)
    protected Property<String> token;

    // we only set in tests to redirect API calls to a mock server.
    protected transient String methodsEndpointUrlPrefix;

    protected <R extends SlackApiTextResponse> R call(RunContext runContext, Rethrow.FunctionChecked<MethodsClient, R, Exception> call) {
        SlackConfig config = new SlackConfig();
        if (methodsEndpointUrlPrefix != null) {
            config.setMethodsEndpointUrlPrefix(methodsEndpointUrlPrefix);
            config.setStatsEnabled(false);
        }
        try (Slack slack = Slack.getInstance(config)) {
            R response = call.apply(slack.methods(runContext.render(this.token).as(String.class).orElseThrow()));

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
