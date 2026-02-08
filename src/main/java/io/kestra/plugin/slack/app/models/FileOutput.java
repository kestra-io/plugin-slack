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
    @Schema(title = "The file ID.")
    String id;

    @Schema(title = "The timestamp when the file was created.")
    Instant created;

    @Schema(title = "The file timestamp.")
    Instant timestamp;

    @Schema(title = "The file name.")
    String name;

    @Schema(title = "The file title.")
    String title;

    @Schema(title = "The file subject.")
    String subject;

    @Schema(title = "The MIME type of the file.")
    String mimetype;

    @Schema(title = "The file type.")
    String filetype;

    @Schema(title = "A human-readable file type (e.g., 'Plain Text').")
    String prettyType;

    @Schema(title = "The user ID who uploaded the file.")
    String user;

    @Schema(title = "The team ID of the user who uploaded the file.")
    String userTeamId;

    @Schema(title = "The source team ID.")
    String sourceTeamId;

    @Schema(title = "The file mode.")
    String mode;

    @Schema(title = "Whether the file is editable.")
    Boolean isEditable;

    @Schema(title = "The file size in bytes.")
    Integer size;

    @Schema(title = "The private download URL.")
    String urlPrivate;

    @Schema(title = "The private download URL with download forced.")
    String urlPrivateDownload;

    @Schema(title = "The public permalink to the file.")
    String permalink;

    @Schema(title = "The public permalink.")
    String permalinkPublic;

    @Schema(title = "Whether the file is public.")
    Boolean isPublic;

    @Schema(title = "Whether the file is external.")
    Boolean isExternal;

    @Schema(title = "The external file type.")
    String externalType;

    @Schema(title = "The external file ID.")
    String externalId;

    @Schema(title = "The external file URL.")
    String externalUrl;

    @Schema(title = "The Thumbnail.")
    Thumbnail thumbnail;

    @Schema(title = "The list of channels the file is shared in.")
    List<String> channels;

    @Schema(title = "The list of groups the file is shared in.")
    List<String> groups;

    @Schema(title = "The list of IMs the file is shared in.")
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
        @Schema(title = "Thumbnail 64x64.")
        String thumb64;

        @Schema(title = "Thumbnail 80x80.")
        String thumb80;

        @Schema(title = "Thumbnail 160x160.")
        String thumb160;

        @Schema(title = "Thumbnail 360x360.")
        String thumb360;

        @Schema(title = "Thumbnail 480x480.")
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
