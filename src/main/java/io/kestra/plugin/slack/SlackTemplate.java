package io.kestra.plugin.slack;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class SlackTemplate extends SlackIncomingWebhook {
    @Schema(title = "Slack channel to send the message to.", description = "This property works only with legacy webhook URLs, new Slack incoming webhook URLs are already tied to a specific channel. "
            +
            "For more details, see: [Legacy Webhooks](https://api.slack.com/legacy/custom-integrations/messaging/webhooks#legacy-customizations) and "
            +
            "[Current Webhooks](https://api.slack.com/messaging/webhooks).")
    @Deprecated
    protected Property<String> channel;

    @Schema(title = "Author of the slack message", description = "This property works only with legacy webhook URLs, new Slack incoming webhook URLs are already tied to a specific username.")
    @Deprecated
    protected Property<String> username;

    @Schema(title = "Url of the icon to use", description = "This property works only with legacy webhook URLs, new Slack incoming webhook URLs are already tied to a specific icon URL.")
    @Deprecated
    protected Property<String> iconUrl;

    @Schema(title = "Emoji icon to use", description = "This property works only with legacy webhook URLs, new Slack incoming webhook URLs are already tied to a specific icon.")
    @Deprecated
    protected Property<String> iconEmoji;

    @Schema(title = "Template to use", hidden = true)
    protected Property<String> templateUri;

    @Schema(title = "Map of variables to use for the message template")
    protected Property<Map<String, Object>> templateRenderMap;

    @SuppressWarnings("unchecked")
    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        Map<String, Object> map = new HashMap<>();

        // Render templateUri once with 'r' prefix
        final var rTemplateUri = runContext.render(this.templateUri).as(String.class);
        if (rTemplateUri.isPresent()) {
            String template = IOUtils.toString(
                    Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(rTemplateUri.get())),
                    StandardCharsets.UTF_8);

            String render = runContext.render(template,
                    templateRenderMap != null ? runContext.render(templateRenderMap).asMap(String.class, Object.class)
                            : Map.of());
            map = (Map<String, Object>) JacksonMapper.ofJson().readValue(render, Object.class);
        }

        // Render all properties once with 'r' prefix - runContext.render is
        // null-friendly
        var rChannel = runContext.render(this.channel).as(String.class);
        if (rChannel.isPresent()) {
            map.put("channel", rChannel.get());
        }

        var rUsername = runContext.render(this.username).as(String.class);
        if (rUsername.isPresent()) {
            map.put("username", rUsername.get());
        }

        var rIconUrl = runContext.render(this.iconUrl).as(String.class);
        if (rIconUrl.isPresent()) {
            map.put("icon_url", rIconUrl.get());
        }

        var rIconEmoji = runContext.render(this.iconEmoji).as(String.class);
        if (rIconEmoji.isPresent()) {
            map.put("icon_emoji", rIconEmoji.get());
        }

        this.payload = Property.ofValue(JacksonMapper.ofJson().writeValueAsString(map));

        return super.run(runContext);
    }
}
