package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.UsersGetPresenceRequest;
import com.slack.api.methods.response.users.UsersGetPresenceResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Get Slack user presence",
    description = "Retrieves presence (active/away) and connection details for a user ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get presence for a user",
            full = true,
            code = """
                id: slack_get_presence
                namespace: company.team

                tasks:
                  - id: check_presence
                    type: io.kestra.plugin.slack.app.users.GetPresence
                    token: "{{ secret('SLACK_TOKEN') }}"
                    user: "U1234567890"
                """
        )
    }
)
public class GetPresence extends AbstractSlackClientConnection implements RunnableTask<GetPresence.Output> {
    @Schema(
        title = "User ID",
        description = "User whose presence is requested."
    )
    @NotNull
    private Property<String> user;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var builder = UsersGetPresenceRequest.builder();

        runContext.render(this.user).as(String.class).ifPresent(builder::user);

        UsersGetPresenceResponse response = call(runContext, (client) -> client.usersGetPresence(builder.build()));

        return Output.builder()
            .presence(response.getPresence())
            .online(response.isOnline())
            .autoAway(response.isAutoAway())
            .manualAway(response.isManualAway())
            .connectionCount(response.getConnectionCount())
            .lastActivity(response.getLastActivity())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Presence status")
        String presence;

        @Schema(title = "Is online")
        Boolean online;

        @Schema(title = "Is auto away")
        Boolean autoAway;

        @Schema(title = "Is manually away")
        Boolean manualAway;

        @Schema(title = "Active connection count")
        Integer connectionCount;

        @Schema(title = "Last activity timestamp")
        Integer lastActivity;
    }
}
