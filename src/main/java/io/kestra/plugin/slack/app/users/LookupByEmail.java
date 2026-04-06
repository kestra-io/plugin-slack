package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.UsersLookupByEmailRequest;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;

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
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Find a Slack user by email",
    description = "Looks up a user by email address and returns their user object."
)
@Plugin(
    examples = {
        @Example(
            title = "Find a user by email",
            full = true,
            code = """
                id: slack_lookup_user
                namespace: company.team

                tasks:
                  - id: lookup_user
                    type: io.kestra.plugin.slack.app.users.LookupByEmail
                    token: "{{ secret('SLACK_TOKEN') }}"
                    email: "user@example.com"
                """
        )
    }
)
public class LookupByEmail extends AbstractSlackClientConnection implements RunnableTask<UserOutput> {
    @Schema(
        title = "Email address",
        description = "Workspace email to look up."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> email;

    @Override
    public UserOutput run(RunContext runContext) throws Exception {
        var builder = UsersLookupByEmailRequest.builder();

        runContext.render(this.email).as(String.class).ifPresent(builder::email);

        UsersLookupByEmailResponse response = call(runContext, (client) -> client.usersLookupByEmail(builder.build()));

        return UserOutput.of(response.getUser());
    }
}
