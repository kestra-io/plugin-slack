package io.kestra.plugin.slack.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.util.QueryStringParser;
import com.slack.api.bolt.util.SlackRequestParser;
import com.slack.api.model.event.*;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.EncryptedString;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.queues.QueueException;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.core.trigger.AbstractWebhookTrigger;
import io.kestra.plugin.core.trigger.WebhookContext;
import io.kestra.plugin.core.trigger.WebhookResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.lang.reflect.Field;
import java.net.http.HttpHeaders;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "TODO",
    description = "TODO"
)
@Plugin(
    examples = {
        @Example(
            title = "TODO",
            code = "TODO",
            full = true
        ),
    }
)
//@WebhookValidation
public class Trigger extends AbstractWebhookTrigger implements TriggerOutput<Trigger.Output> {
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @PluginProperty
    @Schema(
        title = "The bot token to use to receive the events.",
        description = "Bot tokens represent a bot associated with an app installed in a workspace. Starting wtih `xoxb-`"
    )
    private Property<String> botToken;

    @PluginProperty
    @Schema(
        title = "The application signing secret.",
        description = """
            Slack signs the requests we send you using this secret. Confirm that each request comes from Slack by verifying its unique signature.
           """
    )
    private Property<String> signingSecret;

    @Override
    public HttpResponse<?> evaluate(WebhookContext context) throws Exception {
        // Reject path since not expected
        if (context.getPath() != null || context.getRequest().getUri().getPath().endsWith("/")) {
            return HttpResponse.of(HttpResponse.Status.NOT_FOUND);
        }

        RunContext runContext = context.getWebhookService().runContext(context.getFlow(), this);

        Request<?> slackReq = this.parseRequest(context.getRequest(), context, runContext);

        if (slackReq != null) {
            App app = app(context, runContext);

            try {
                Response slackResp = app.run(slackReq);

                return HttpResponse.builder()
                    .status(HttpResponse.Status.valueOf(slackResp.getStatusCode()))
                    .body(slackResp.getBody() != null ? slackResp.getBody() : null)
                    .headers(httpHeaders(slackResp))
                    .build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to handle a slack request", e);
            }
        }

        return HttpResponse.of(HttpResponse.Status.BAD_REQUEST, "Invalid Request");
    }

    private static HttpHeaders httpHeaders(Response response) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", List.of(response.getContentType()));
        headers.putAll(response.getHeaders());

        return HttpHeaders.of(headers, (s1, s2) -> true);
    }

    private AppConfig appConfig(WebhookContext context, RunContext runContext) throws IllegalVariableEvaluationException {
        return AppConfig.builder()
            .singleTeamBotToken(runContext.render(botToken).as(String.class).orElse(null))
            .signingSecret(runContext.render(signingSecret).as(String.class).orElseThrow())
            .build();
    }

    private com.slack.api.bolt.response.Response createExecution(WebhookContext context, RunContext runContext, EventsApiPayload<?> eventsApiPayload, com.slack.api.bolt.context.Context slackContext)  {
        Map<String, Object> body = MAPPER.convertValue(eventsApiPayload.getEvent(), MAP_TYPE_REFERENCE);

        if (runContext.logger().isTraceEnabled()) {
            runContext.logger().trace("Slack Event: {}", body);
        }

        return this.createExecution(
            context,
            runContext,
            body,
            slackContext
        );
    }


    private com.slack.api.bolt.response.Response createExecution(WebhookContext context, RunContext runContext, com.slack.api.bolt.request.Request<?> request, com.slack.api.bolt.context.Context slackContext) {
        Map<String, Object> body = extractPayload(request);

        if (runContext.logger().isTraceEnabled()) {
            runContext.logger().trace("Slack Request: {}", request);
        }

        return this.createExecution(
            context,
            runContext,
            body,
            slackContext
        );
    }

    private static Map<String, Object> extractPayload(com.slack.api.bolt.request.Request<?> request) {
        try {
            Field field = request.getClass().getDeclaredField("payload");
            field.setAccessible(true);
            Object value = field.get(request);

            Map<String, Object> result = MAPPER.convertValue(value, MAP_TYPE_REFERENCE);
            result.remove("token");

            return result;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private com.slack.api.bolt.response.Response createExecution(WebhookContext context, RunContext runContext, Map<String, Object> body, com.slack.api.bolt.context.Context slackContext) {
        EncryptedString token = null;
        if (slackContext.getBotToken() != null) {
            try {
                token = EncryptedString.from(slackContext.getBotToken(), runContext);
            } catch (GeneralSecurityException e) {
                runContext.logger().error("Failed to encrypt token", e);
                throw new RuntimeException(e);
            }
        }

        Output output = Output.builder()
            .body(body)
            .headers(context.getRequest().getHeaders().map())
            .parameters(context.getWebhookService().parseParameters(context))
            .token(token)
            .build();

        Optional<Execution> maybeExecution = context.getWebhookService().newExecution(
            context,
            context.getFlow(),
            this,
            output
        );

        if (maybeExecution.isEmpty()) {
            return slackContext.ack();
        } else {
            try {
                context.getWebhookService().startExecution(maybeExecution.get());
            } catch (QueueException e) {
                runContext.logger().error("Failed to start execution for slack webhook", e);
                throw new RuntimeException(e);
            }

            WebhookResponse webhookResponse = context.getWebhookService().executionResponse(maybeExecution.get());

            try {
                return Response.json(200, JacksonMapper.ofJson().writeValueAsString(webhookResponse));
            } catch (JsonProcessingException e) {
                runContext.logger().error("Failed to serialize response", e);
                throw new RuntimeException(e);
            }
        }
    }

    private Request<?> parseRequest(HttpRequest request, WebhookContext context, RunContext runContext) throws IllegalVariableEvaluationException {
        var requestParser = new SlackRequestParser(this.appConfig(context, runContext));

        RequestHeaders headers = new RequestHeaders(request.getHeaders().map());

        SlackRequestParser.HttpRequest rawRequest = SlackRequestParser.HttpRequest.builder()
            .requestUri(request.getUri().toString())
            .queryString(QueryStringParser.toMap(request.getUri().getQuery()))
            .headers(headers)
            .requestBody(body(request.getBody()))
            .remoteAddress(remoteAddress(request))
            .build();

        return requestParser.parse(rawRequest);
    }

    private static String body(@Nullable HttpRequest.RequestBody body) throws IllegalVariableEvaluationException {
        if (body == null) {
            return "";
        } else if (body instanceof HttpRequest.JsonRequestBody) {
            try {
                return JacksonMapper.ofJson().writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new IllegalVariableEvaluationException(e.getMessage(), e);
            }
        } else {
            return (String) body.getContent();
        }
    }

    private static String remoteAddress(HttpRequest request) {
        if (request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null) {
            return null;
        }

        byte[] rawBytes = request.getRemoteAddress().getAddress().getAddress();

        int i = 4;
        StringBuilder ipAddress = new StringBuilder();
        for (byte raw : rawBytes) {
            ipAddress.append(raw & 0xFF);
            if (--i > 0) {
                ipAddress.append(".");
            }
        }
        return ipAddress.toString();
    }

    @SuppressWarnings("deprecation")
    private App app(WebhookContext context, RunContext runContext) throws IllegalVariableEvaluationException {
        App app = App.builder()
            .status(App.Status.Stopped)
            .appConfig(this.appConfig(context, runContext))
            .build();

        app
            .event(AccountChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppHomeOpenedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppInstalledEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppMentionEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppRateLimitedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppRequestedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppUninstalledEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AppUninstalledTeamEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AssistantThreadContextChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(AssistantThreadStartedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(BotAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(BotChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(CallRejectedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelArchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelCreatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelHistoryChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelIdChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelJoinedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelLeftEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelMarkedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelRenameEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelSharedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelUnarchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ChannelUnsharedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(CommandsChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(DndUpdatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(DndUpdatedUserEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(EmailDomainChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(EmojiChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ErrorEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ExternalOrgMigrationFinishedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ExternalOrgMigrationStartedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileCommentAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileCommentDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileCommentEditedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileCreatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FilePublicEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileSharedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FileUnsharedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(FunctionExecutedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GoodbyeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GridMigrationFinishedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GridMigrationStartedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupArchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupCloseEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupHistoryChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupJoinedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupLeftEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupMarkedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupOpenEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupRenameEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(GroupUnarchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(HelloEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ImCloseEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ImCreatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ImHistoryChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ImMarkedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ImOpenEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(InviteRequestedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(LinkSharedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ManualPresenceChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MemberJoinedChannelEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MemberLeftChannelEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageBotEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelArchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelConvertToPublicEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelJoinEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelLeaveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelNameEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelPostingPermissionsEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelPurposeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelTopicEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageChannelUnarchiveEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageEkmAccessDeniedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageFileShareEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageGroupTopicEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageMeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageMetadataDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageMetadataPostedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageMetadataUpdatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageRepliedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(MessageThreadBroadcastEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(PinAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(PinRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(PongEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(PrefChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(PresenceChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ReactionAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ReactionRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ReconnectUrlEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ResourcesAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ResourcesRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ScopeDeniedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(ScopeGrantedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SharedChannelInviteAcceptedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SharedChannelInviteApprovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SharedChannelInviteDeclinedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SharedChannelInviteReceivedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SharedChannelInviteRequestedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(StarAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(StarRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SubteamCreatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SubteamMembersChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SubteamSelfAddedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SubteamSelfRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(SubteamUpdatedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamAccessGrantedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamAccessRevokedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamDomainChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamJoinEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamMigrationStartedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamPlanChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamPrefChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamProfileChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamProfileDeleteEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamProfileReorderEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TeamRenameEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(TokensRevokedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserChangeEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserHuddleChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserProfileChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserResourceDeniedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserResourceGrantedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserResourceRemovedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserStatusChangedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(UserTypingEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(WorkflowDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(WorkflowPublishedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(WorkflowStepDeletedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(WorkflowStepExecuteEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .event(WorkflowUnpublishedEvent.class, (req, ctx) -> this.createExecution(context, runContext, req, ctx))

            .command(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .attachmentAction(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .blockAction(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .blockSuggestion(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .globalShortcut(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .messageShortcut(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .dialogSubmission(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .dialogCancellation(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .dialogSuggestion(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .dialogSuggestion(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .viewSubmission(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .viewClosed(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .workflowStepEdit(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .workflowStepSave(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .workflowStepExecute(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx))
            .workflowStepExecute(Pattern.compile("^.*$"), (req, ctx) -> this.createExecution(context, runContext, req, ctx));

        return app;
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The full body for the webhook request"
        )
        @NotNull
        private Object body;

        @Schema(title = "The headers for the webhook request")
        @NotNull
        private Map<String, List<String>> headers;

        @Schema(title = "The parameters for the webhook request")
        @NotNull
        private Map<String, List<String>> parameters;

        @Schema(title = "The bot token used to receive the event")
        @NotNull
        private EncryptedString token;
    }
}
