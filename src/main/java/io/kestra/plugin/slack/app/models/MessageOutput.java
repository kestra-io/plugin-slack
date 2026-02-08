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
    @Schema(title = "The message type.")
    String type;

    @Schema(title = "The message subtype.")
    String subtype;

    @Schema(title = "The team ID.")
    String team;

    @Schema(title = "The channel ID.")
    String channel;

    @Schema(title = "The user ID who posted the message.")
    String user;

    @Schema(title = "The user name who posted the message.")
    String username;

    @Schema(title = "The message text.")
    String text;

    @Schema(title = "The message payload.")
    Map<String, Object> payload;

    @Schema(title = "The timestamp of the message.")
    Instant timestamp;

    @Schema(title = "The timestamp of the parent message if this is a thread reply.")
    String threadTimestamp;

    @Schema(title = "Whether this is an intro message.")
    Boolean isIntro;

    @Schema(title = "Whether the message is starred by the calling user.")
    Boolean isStarred;

    @Schema(title = "List of channel IDs where this message is pinned.")
    List<String> pinnedTo;

    @Schema(title = "List of reactions to this message.")
    List<ReactionOutput> reactions;

    @Schema(title = "The app ID if the message was posted by an app.")
    String appId;

    @Schema(title = "The bot ID if the message was posted by a bot.")
    String botId;

    @Schema(title = "The bot link.")
    String botLink;

    @Schema(title = "Whether the message should be displayed as if posted by a bot.")
    Boolean isDisplayAsBot;

    @Schema(title = "A single file attached to the message.")
    FileOutput file;

    @Schema(title = "List of files attached to the message.")
    List<FileOutput> files;

    @Schema(title = "Whether this is an upload message.")
    Boolean isUpload;

    @Schema(title = "The parent user ID.")
    String parentUserId;

    @Schema(title = "The user ID of the person who invited.")
    String inviter;

    @Schema(title = "The client message ID.")
    String clientMsgId;

    @Schema(title = "The topic for channel_topic subtype messages.")
    String topic;

    @Schema(title = "The purpose for channel_purpose subtype messages.")
    String purpose;

    @Schema(title = "Edit information if the message was edited.")
    Edited edited;

    @Schema(title = "Whether links should be unfurled.")
    Boolean isUnfurlLinks;

    @Schema(title = "Whether media should be unfurled.")
    Boolean isUnfurlMedia;

    @Schema(title = "Whether this is a thread broadcast message.")
    Boolean isThreadBroadcast;

    @Schema(title = "Whether the message is locked.")
    Boolean isLocked;

    @Schema(title = "The number of replies in the thread.")
    Integer replyCount;

    @Schema(title = "List of user IDs who replied in the thread.")
    List<String> replyUsers;

    @Schema(title = "The number of users who replied in the thread.")
    Integer replyUsersCount;

    @Schema(title = "The timestamp of the latest reply in the thread.")
    String latestReply;

    @Schema(title = "Whether the user is subscribed to the thread.")
    Boolean isSubscribed;

    @Schema(title = "List of remote file IDs (x_files).")
    List<String> xFiles;

    @Schema(title = "Whether the message is hidden.")
    Boolean isHidden;

    @Schema(title = "The timestamp of the last read message.")
    String lastRead;

    @Schema(title = "The item type.")
    String itemType;

    @Schema(title = "Whether there are no notifications for this message.")
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
        @Schema(title = "The user ID who edited the message.")
        String user;

        @Schema(title = "The timestamp when the message was edited.")
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
