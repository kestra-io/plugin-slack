package io.kestra.plugin.slack.app.files;

import com.slack.api.methods.request.files.FilesDeleteRequest;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.slack.AbstractSlackClientConnection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a Slack file",
    description = "Permanently removes a file by ID. Requires `files:write`; deletion cannot be undone."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a file from Slack",
            full = true,
            code = """
                id: slack_file_delete
                namespace: company.team

                tasks:
                  - id: delete_file
                    type: io.kestra.plugin.slack.app.files.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "F1234567890"
                """
        ),
        @Example(
            title = "Delete a file after uploading it",
            full = true,
            code = """
                id: slack_file_upload_and_delete
                namespace: company.team

                tasks:
                  - id: upload_file
                    type: io.kestra.plugin.slack.app.files.Upload
                    token: "{{ secret('SLACK_TOKEN') }}"
                    channels: ["#general"]
                    from: "{{ inputs.file }}"
                    filename: "document.pdf"
                    title: "Document"

                  - id: delete_file
                    type: io.kestra.plugin.slack.app.files.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "{{ outputs.upload_file.id }}"
                """
        )
    }
)
public class Delete extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "File ID",
        description = "Slack file ID to delete (e.g., F123...). Use Upload output or Slack API to obtain."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> fileId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String renderedFileId = runContext.render(this.fileId).as(String.class).orElseThrow();

        var builder = FilesDeleteRequest.builder()
            .file(renderedFileId);

        call(runContext, (client) -> client.filesDelete(builder.build()));

        return null;
    }
}
