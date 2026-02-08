package io.kestra.plugin.slack.app.conversations;


import com.slack.api.methods.request.conversations.ConversationsSetTopicRequest;
import com.slack.api.methods.response.conversations.ConversationsSetTopicResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.ConversationTopicOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Set the topic of a Slack conversation (channel)."
)
@Plugin(
    examples = {
        @Example(
            title = "Set a channel topic",
            full = true,
            code = """
                id: slack_set_topic
                namespace: company.team

                tasks:
                  - id: set_topic
                    type: io.kestra.plugin.slack.conversations.SetTopic
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channel: "C1234567890"
                    topic: "Discussion about the new project"
                """
        )
    }
)
public class SetTopic extends AbstractSlackClientConnection implements RunnableTask<ConversationTopicOutput> {
    @Schema(
        title = "The ID of the channel.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    @NotNull
    private Property<String> channel;

    @Schema(
        title = "The new topic for the channel.",
        description = "The topic can be up to 250 characters."
    )
    @NotNull
    private Property<String> topic;

    @Override
    public ConversationTopicOutput run(RunContext runContext) throws Exception {
        var builder = ConversationsSetTopicRequest.builder();

        runContext.render(this.channel).as(String.class).ifPresent(builder::channel);
        runContext.render(this.topic).as(String.class).ifPresent(builder::topic);

        ConversationsSetTopicResponse response = call(runContext, (client) -> client.conversationsSetTopic(builder.build()));

        return ConversationTopicOutput.of(response.getChannel().getTopic());
    }
}
