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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a file from Slack.",
    description = "Delete a file from Slack using the Slack API. This action is permanent and cannot be undone. " +
        "You need the `files:write` scope in your Slack app to use this task."
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
        title = "The ID of the file to delete.",
        description = "The file ID can be obtained from the Upload task output or from the Slack API. " +
            "File IDs typically start with 'F' (e.g., F1234567890)."
    )
    @NotNull
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
