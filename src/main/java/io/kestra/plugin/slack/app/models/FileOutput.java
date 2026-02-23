package io.kestra.plugin.slack.app.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

import static io.kestra.plugin.slack.services.MessageService.fromSlackTimestamp;

@Value
@Builder
@Jacksonized
public class FileOutput implements io.kestra.core.models.tasks.Output  {
    @Schema(title = "File ID")
    String id;

    @Schema(title = "Created at")
    Instant created;

    @Schema(title = "File timestamp")
    Instant timestamp;

    @Schema(title = "File name")
    String name;

    @Schema(title = "File title")
    String title;

    @Schema(title = "File subject")
    String subject;

    @Schema(title = "MIME type")
    String mimetype;

    @Schema(title = "File type")
    String filetype;

    @Schema(title = "Pretty file type")
    String prettyType;

    @Schema(title = "Uploader user ID")
    String user;

    @Schema(title = "Uploader team ID")
    String userTeamId;

    @Schema(title = "Source team ID")
    String sourceTeamId;

    @Schema(title = "File mode")
    String mode;

    @Schema(title = "Is editable")
    Boolean isEditable;

    @Schema(title = "File size (bytes)")
    Integer size;

    @Schema(title = "Private download URL")
    String urlPrivate;

    @Schema(title = "Private forced-download URL")
    String urlPrivateDownload;

    @Schema(title = "Public permalink")
    String permalink;

    @Schema(title = "Public permalink (alt)")
    String permalinkPublic;

    @Schema(title = "Is public")
    Boolean isPublic;

    @Schema(title = "Is external")
    Boolean isExternal;

    @Schema(title = "External file type")
    String externalType;

    @Schema(title = "External file ID")
    String externalId;

    @Schema(title = "External file URL")
    String externalUrl;

    @Schema(title = "Thumbnail")
    Thumbnail thumbnail;

    @Schema(title = "Channels where shared")
    List<String> channels;

    @Schema(title = "Groups where shared")
    List<String> groups;

    @Schema(title = "IMs where shared")
    List<String> ims;

    public static FileOutput of(com.slack.api.model.File file) {
        if (file == null) {
            return null;
        }

        return FileOutput.builder()
            .id(file.getId())
            .created(fromSlackTimestamp(file.getCreated()))
            .timestamp(fromSlackTimestamp(file.getTimestamp()))
            .name(file.getName())
            .title(file.getTitle())
            .subject(file.getSubject())
            .mimetype(file.getMimetype())
            .filetype(file.getFiletype())
            .prettyType(file.getPrettyType())
            .user(file.getUser())
            .userTeamId(file.getUserTeam())
            .sourceTeamId(file.getSourceTeam())
            .mode(file.getMode())
            .isEditable(file.isEditable())
            .size(file.getSize())
            .urlPrivate(file.getUrlPrivate())
            .urlPrivateDownload(file.getUrlPrivateDownload())
            .permalink(file.getPermalink())
            .permalinkPublic(file.getPermalinkPublic())
            .isPublic(file.isPublic())
            .isExternal(file.isExternal())
            .externalType(file.getExternalType())
            .externalId(file.getExternalId())
            .externalUrl(file.getExternalUrl())
            .thumbnail(Thumbnail.of(file))
            .channels(file.getChannels())
            .groups(file.getGroups())
            .ims(file.getIms())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Thumbnail {
        @Schema(title = "Thumbnail 64x64")
        String thumb64;

        @Schema(title = "Thumbnail 80x80")
        String thumb80;

        @Schema(title = "Thumbnail 160x160")
        String thumb160;

        @Schema(title = "Thumbnail 360x360")
        String thumb360;

        @Schema(title = "Thumbnail 480x480")
        String thumb480;

        public static Thumbnail of(com.slack.api.model.File file) {
            return Thumbnail.builder()
                .thumb64(file.getThumb64())
                .thumb80(file.getThumb80())
                .thumb160(file.getThumb160())
                .thumb360(file.getThumb360())
                .thumb480(file.getThumb480())
                .build();
        }
    }
}
