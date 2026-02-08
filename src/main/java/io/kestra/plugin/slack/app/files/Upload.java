package io.kestra.plugin.slack.app.files;

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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Upload a file to Slack."
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
                    type: io.kestra.plugin.slack.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general"]
                    file: "{{ outputs.previous_task.uri }}"
                    filename: "report.pdf"
                    title: "Monthly Report"
                    initialComment: "Here is the monthly report"
                """
        )
    }
)
public class Upload extends AbstractSlackClientConnection implements RunnableTask<Upload.Output> {
    @Schema(
        title = "The file from Kestra's internal storage to upload.",
        description = "URI of the file in Kestra's internal storage. Can be from inputs, outputs, or other tasks."
    )
    @NotNull
    @PluginProperty(internalStorageURI = true)
    private Property<String> from;

    @Schema(
        title = "Channel IDs or names where the file will be shared.",
        description = "To get the ID of a channel, right click on the channel name in Slack and select 'Copy Link'. The ID is the last part of the URL."
    )
    private Property<List<String>> channels;

    @Schema(title = "Filename of the file.")
    @NotNull
    private Property<String> filename;

    @Schema(title = "Title of the file.")
    private Property<String> title;

    @Schema(title = "Initial comment to add with the file.")
    private Property<String> altTxt;

    @Schema(title = "Initial comment to add with the file.")
    private Property<String> snippetType;

    @Schema(title = "Thread timestamp to upload file as a reply.")
    private Property<String> timestamp;

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
            .threadTs(runContext.render(this.timestamp).as(String.class).orElse(null));

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
        @Schema(title = "The ID of the uploaded file.")
        @NotNull
        String id;

        @Schema(title = "The title of the file.")
        String title;

        @Schema(title = "The name of the file.")
        String name;

        @Schema(title = "The permanent link to the file.")
        String permalink;

        @Schema(title = "The public permanent link to the file.")
        String permalinkPublic;

        @Schema(title = "The private URL to the file.")
        String urlPrivate;
    }
}
