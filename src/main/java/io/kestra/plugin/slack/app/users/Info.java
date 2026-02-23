package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.UserOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Get Slack user details",
    description = "Retrieves user profile metadata; locale can be included."
)
@Plugin(
    examples = {
        @Example(
            title = "Get information about a user",
            full = true,
            code = """
                id: slack_user_info
                namespace: company.team

                tasks:
                  - id: get_user_info
                    type: io.kestra.plugin.slack.app.users.Info
                    token: "{{ secret('SLACK_TOKEN') }}"
                    user: "U1234567890"
                """
        )
    }
)
public class Info extends AbstractSlackClientConnection implements RunnableTask<UserOutput> {
    @Schema(
        title = "User ID",
        description = "Slack user ID to describe."
    )
    @NotNull
    private Property<String> user;

    @Schema(
        title = "Include locale",
        description = "If true, locale is returned. Default false."
    )
    @Builder.Default
    private Property<Boolean> includeLocale = Property.ofValue(false);

    @Override
    public UserOutput run(RunContext runContext) throws Exception {
        var builder = UsersInfoRequest.builder();

        runContext.render(this.user).as(String.class).ifPresent(builder::user);
        runContext.render(this.includeLocale).as(Boolean.class).ifPresent(builder::includeLocale);

        UsersInfoResponse response = call(runContext, (client) -> client.usersInfo(builder.build()));

        return UserOutput.of(response.getUser());
    }
}
