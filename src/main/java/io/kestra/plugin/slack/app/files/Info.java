package io.kestra.plugin.slack.app.files;


import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.response.files.FilesInfoResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;
import io.kestra.plugin.slack.app.models.FileOutput;
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
    title = "Get information about a file in Slack.",
    description = "Retrieve detailed metadata about a specific file from Slack, including its name, title, " +
        "timestamps, user who uploaded it, and other properties. You need the `files:read` scope in your Slack app to use this task."
)
@Plugin(
    examples = {
        @Example(
            title = "Get information about a file",
            full = true,
            code = """
                id: slack_file_info
                namespace: company.team

                tasks:
                  - id: get_file_info
                    type: io.kestra.plugin.slack.app.files.Info
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "F1234567890"
                """
        ),
        @Example(
            title = "Get file info after uploading",
            full = true,
            code = """
                id: slack_upload_and_get_info
                namespace: company.team

                tasks:
                  - id: upload_file
                    type: io.kestra.plugin.slack.app.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general"]
                    from: "{{ inputs.file }}"
                    filename: "report.pdf"
                    title: "Report"

                  - id: get_file_info
                    type: io.kestra.plugin.slack.app.files.Info
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "{{ outputs.upload_file.id }}"
                """
        )
    }
)
public class Info extends AbstractSlackClientConnection implements RunnableTask<FileOutput> {
    @Schema(
        title = "The ID of the file.",
        description = "The file ID can be obtained from the Upload task output or from the Slack API. " +
            "File IDs typically start with 'F' (e.g., F1234567890)."
    )
    @NotNull
    private Property<String> fileId;

    @Override
    public FileOutput run(RunContext runContext) throws Exception {
        var builder = FilesInfoRequest.builder();

        runContext.render(this.fileId).as(String.class).ifPresent(builder::file);

        FilesInfoResponse response = call(runContext, (client) -> client.filesInfo(builder.build()));

        return FileOutput.of(response.getFile());
    }
}
