package io.kestra.plugin.slack.app.users;

import com.slack.api.methods.request.users.profile.UsersProfileGetRequest;
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Get Slack user profile",
    description = "Retrieves a user's profile; can include labels for structured fields."
)
@Plugin(
    examples = {
        @Example(
            title = "Get user profile",
            full = true,
            code = """
                id: slack_get_profile
                namespace: company.team

                tasks:
                  - id: get_profile
                    type: io.kestra.plugin.slack.app.users.ProfileGet
                    token: "{{ secret('SLACK_TOKEN') }}"
                    user: "U1234567890"
                """
        )
    }
)
public class ProfileGet extends AbstractSlackClientConnection implements RunnableTask<ProfileGet.ProfileOutput> {
    @Schema(
        title = "User ID (optional)",
        description = "Target user; defaults to the authenticated user."
    )
    private Property<String> user;

    @Schema(
        title = "Include field labels",
        description = "If true, includes labels for structured profile fields. Default false."
    )
    @Builder.Default
    private Property<Boolean> includeLabels = Property.ofValue(false);

    @Override
    public ProfileOutput run(RunContext runContext) throws Exception {
        var builder = UsersProfileGetRequest.builder();

        runContext.render(this.user).as(String.class).ifPresent(builder::user);
        runContext.render(this.includeLabels).as(Boolean.class).ifPresent(builder::includeLabels);

        UsersProfileGetResponse response = call(runContext, (client) -> client.usersProfileGet(builder.build()));

        return ProfileOutput.of(response.getProfile());
    }

    @Value
    @Builder
    @Jacksonized
    public static class ProfileOutput implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The user's job title.")
        String title;

        @Schema(title = "The user's phone number.")
        String phone;

        @Schema(title = "The user's real name.")
        String realName;

        @Schema(title = "The user's display name.")
        String displayName;

        @Schema(title = "The user's email address.")
        String email;

        @Schema(title = "The user's status text.")
        String statusText;

        @Schema(title = "The user's status emoji.")
        String statusEmoji;

        public static ProfileOutput of(com.slack.api.model.User.Profile profile) {
            if (profile == null) {
                return null;
            }

            return ProfileOutput.builder()
                .title(profile.getTitle())
                .phone(profile.getPhone())
                .realName(profile.getRealName())
                .displayName(profile.getDisplayName())
                .email(profile.getEmail())
                .statusText(profile.getStatusText())
                .statusEmoji(profile.getStatusEmoji())
                .build();
        }
    }
}
