package io.kestra.plugin.slack.app.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.response.files.FilesUploadV2Response;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.core.utils.FileUtils;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.services.MessageService;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Upload a file to Slack",
    description = "Uploads a file from Kestra internal storage to one or more channels (optionally threaded). Requires `files:write`; threads need the `chat:write` scope."
)
@Plugin(
    examples = {
        @Example(
            title = "Upload a file to a Slack channel",
            full = true,
            code = """
                id: slack_file_upload
                namespace: company.team

                tasks:
                  - id: upload_file
                    type: io.kestra.plugin.slack.app.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general"]
                    from: "{{ outputs.previous_task.uri }}"
                    filename: "report.pdf"
                    title: "Monthly Report"
                """
        ),
        @Example(
            title = "Upload a file with a title and alt text",
            full = true,
            code = """
                id: slack_file_upload_detailed
                namespace: company.team

                tasks:
                  - id: upload_file
                    type: io.kestra.plugin.slack.app.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general", "#reports"]
                    from: "{{ inputs.file }}"
                    filename: "analysis.png"
                    title: "Data Analysis Chart"
                    altTxt: "Chart showing sales trends over the last quarter"
                """
        ),
        @Example(
            title = "Upload a file as a thread reply",
            full = true,
            code = """
                id: slack_file_thread_upload
                namespace: company.team

                tasks:
                  - id: upload_file
                    type: io.kestra.plugin.slack.app.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general"]
                    from: "{{ outputs.generate_report.uri }}"
                    filename: "detailed_report.pdf"
                    title: "Detailed Report"
                    timestamp: "{{ outputs.send_message.timestamp }}"
                """
        )
    }
)
public class Upload extends AbstractSlackClientConnection implements RunnableTask<Upload.Output> {
    @Schema(
        title = "Source file URI",
        description = "Internal storage URI of the file to upload (inputs/outputs/other tasks)."
    )
    @NotNull
    @PluginProperty(internalStorageURI = true, group = "main")
    private Property<String> from;

    @Schema(
        title = "Target channels",
        description = "Channel IDs or names to share the file to; multiple allowed."
    )
    @PluginProperty(group = "advanced")
    private Property<List<String>> channels;

    @Schema(title = "Filename")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> filename;

    @Schema(
        title = "File title",
        description = "Optional display title shown in Slack."
    )
    @PluginProperty(group = "advanced")
    private Property<String> title;

    @Schema(
        title = "Alt text",
        description = "Accessibility text, useful for images or visual content."
    )
    @PluginProperty(group = "advanced")
    private Property<String> altTxt;

    @Schema(
        title = "Snippet language",
        description = "Syntax highlighting label for code snippets (e.g., python, java, javascript)."
    )
    @PluginProperty(group = "advanced")
    private Property<String> snippetType;

    @Schema(
        title = "Thread timestamp",
        description = "Slack `ts` to post the file as a thread reply; requires matching channel and `chat:write`."
    )
    @PluginProperty(group = "advanced")
    private Property<Instant> timestamp;

    @Override
    public Output run(RunContext runContext) throws Exception {
        URI rFrom = new URI(runContext.render(this.from).as(String.class).orElseThrow());
        String rFilename = runContext.render(this.filename).as(String.class).orElseThrow();

        File uploadFile = runContext.workingDir().createTempFile(FileUtils.getExtension(rFilename)).toFile();
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(uploadFile), FileSerde.BUFFER_SIZE)) {
            IOUtils.copyLarge(runContext.storage().getFile(rFrom), outputStream);
        }

        FilesUploadV2Request.UploadFile slackUploadFiles = FilesUploadV2Request.UploadFile.builder()
            .file(uploadFile)
            .filename(rFilename)
            .title(runContext.render(this.title).as(String.class).orElse(null))
            .altTxt(runContext.render(this.altTxt).as(String.class).orElse(null))
            .snippetType(runContext.render(this.snippetType).as(String.class).orElse(null))
            .build();

        FilesUploadV2Request.FilesUploadV2RequestBuilder builder = FilesUploadV2Request.builder()
            .channels(runContext.render(this.channels).asList(String.class))
            .uploadFiles(List.of(slackUploadFiles))
            .threadTs(runContext.render(this.timestamp).as(Instant.class).map(MessageService::toSlackTimestamp).orElse(null));

        FilesUploadV2Response response = call(runContext, (client) -> client.filesUploadV2(builder.build()));
        com.slack.api.model.File uploadedFile = response.getFiles().getFirst();

        return Upload.Output.builder()
            .id(uploadedFile.getId())
            .title(uploadedFile.getTitle())
            .name(uploadedFile.getName())
            .permalink(uploadedFile.getPermalink())
            .permalinkPublic(uploadedFile.getPermalinkPublic())
            .urlPrivate(uploadedFile.getUrlPrivate())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Uploaded file ID")
        @NotNull
        String id;

        @Schema(title = "File title")
        String title;

        @Schema(title = "Filename")
        String name;

        @Schema(title = "Permalink")
        String permalink;

        @Schema(title = "Public permalink")
        String permalinkPublic;

        @Schema(title = "Private URL")
        String urlPrivate;
    }
}
