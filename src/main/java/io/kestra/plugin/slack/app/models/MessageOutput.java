package io.kestra.plugin.slack.app.models;

import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kestra.plugin.slack.services.MessageService.fromSlackTimestamp;

@Value
@Builder
@Jacksonized
public class MessageOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "Message type")
    String type;

    @Schema(title = "Message subtype")
    String subtype;

    @Schema(title = "Team ID")
    String team;

    @Schema(title = "Channel ID")
    String channel;

    @Schema(title = "User ID who posted")
    String user;

    @Schema(title = "Username who posted")
    String username;

    @Schema(title = "Message text")
    String text;

    @Schema(title = "Message payload")
    Map<String, Object> payload;

    @Schema(title = "Message timestamp")
    Instant timestamp;

    @Schema(title = "Thread parent timestamp")
    String threadTimestamp;

    @Schema(title = "Is intro")
    Boolean isIntro;

    @Schema(title = "Is starred")
    Boolean isStarred;

    @Schema(title = "Pinned channel IDs")
    List<String> pinnedTo;

    @Schema(title = "Reactions")
    List<ReactionOutput> reactions;

    @Schema(title = "App ID")
    String appId;

    @Schema(title = "Bot ID")
    String botId;

    @Schema(title = "Bot link")
    String botLink;

    @Schema(title = "Display as bot")
    Boolean isDisplayAsBot;

    @Schema(title = "Single attached file")
    FileOutput file;

    @Schema(title = "Attached files")
    List<FileOutput> files;

    @Schema(title = "Is upload message")
    Boolean isUpload;

    @Schema(title = "Parent user ID")
    String parentUserId;

    @Schema(title = "Inviter user ID")
    String inviter;

    @Schema(title = "Client message ID")
    String clientMsgId;

    @Schema(title = "Channel topic text")
    String topic;

    @Schema(title = "Channel purpose text")
    String purpose;

    @Schema(title = "Edit information")
    Edited edited;

    @Schema(title = "Unfurl links")
    Boolean isUnfurlLinks;

    @Schema(title = "Unfurl media")
    Boolean isUnfurlMedia;

    @Schema(title = "Is thread broadcast")
    Boolean isThreadBroadcast;

    @Schema(title = "Is locked")
    Boolean isLocked;

    @Schema(title = "Reply count")
    Integer replyCount;

    @Schema(title = "User IDs who replied")
    List<String> replyUsers;

    @Schema(title = "Reply user count")
    Integer replyUsersCount;

    @Schema(title = "Latest reply timestamp")
    String latestReply;

    @Schema(title = "Is subscribed to thread")
    Boolean isSubscribed;

    @Schema(title = "Remote file IDs (x_files)")
    List<String> xFiles;

    @Schema(title = "Is hidden")
    Boolean isHidden;

    @Schema(title = "Last read timestamp")
    String lastRead;

    @Schema(title = "Item type")
    String itemType;

    @Schema(title = "No notifications")
    Boolean isNoNotifications;

    public static MessageOutput of(com.slack.api.model.Message message) {
        Map<String, Object> payload = new HashMap<>();

        if (message.getBlocks() != null) {
            payload.put("blocks", message.getBlocks());
        }

        if (message.getAttachments() != null) {
            payload.put("attachments", message.getAttachments());
        }

        return MessageOutput.builder()
            .type(message.getType())
            .subtype(message.getSubtype())
            .team(message.getTeam())
            .channel(message.getChannel())
            .user(message.getUser())
            .username(message.getUsername())
            .text(message.getText())
            .payload(!payload.isEmpty() ? payload : null)
            .timestamp(MessageService.fromSlackTimestamp(message.getTs()))
            .threadTimestamp(message.getThreadTs())
            .pinnedTo(message.getPinnedTo())
            .reactions(message.getReactions() != null ? message.getReactions().stream().map(ReactionOutput::of).collect(Collectors.toList()) : null)
            .appId(message.getAppId())
            .botId(message.getBotId())
            .botLink(message.getBotLink())
            .file(FileOutput.of(message.getFile()))
            .files(message.getFiles() != null ? message.getFiles().stream().map(FileOutput::of).collect(Collectors.toList()) : null)
            .parentUserId(message.getParentUserId())
            .inviter(message.getInviter())
            .clientMsgId(message.getClientMsgId())
            .topic(message.getTopic())
            .purpose(message.getPurpose())
            .edited(Edited.of(message.getEdited()))
            .replyCount(message.getReplyCount())
            .replyUsers(message.getReplyUsers())
            .replyUsersCount(message.getReplyUsersCount())
            .latestReply(message.getLatestReply())
            .xFiles(message.getXFiles())
            .lastRead(message.getLastRead())
            .itemType(message.getItemType())
            .isDisplayAsBot(message.isDisplayAsBot())
            .isUnfurlLinks(message.isUnfurlLinks())
            .isUnfurlMedia(message.isUnfurlMedia())
            .isThreadBroadcast(message.isThreadBroadcast())
            .isLocked(message.isLocked())
            .isUpload(message.isUpload())
            .isIntro(message.isIntro())
            .isStarred(message.isStarred())
            .isSubscribed(message.isSubscribed())
            .isHidden(message.isHidden())
            .isNoNotifications(message.isNoNotifications())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Edited {
        @Schema(title = "Editor user ID")
        String user;

        @Schema(title = "Edited timestamp")
        String timestamp;

        public static Edited of(com.slack.api.model.Message.Edited edited) {
            if (edited == null) {
                return null;
            }

            return Edited.builder()
                .user(edited.getUser())
                .timestamp(edited.getTs())
                .build();
        }
    }

}
