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
    title = "Delete a file from Slack."
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
                    type: io.kestra.plugin.slack.files.Delete
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "F1234567890"
                """
        )
    }
)
public class Delete extends AbstractSlackClientConnection implements RunnableTask<VoidOutput> {
    @Schema(
        title = "The ID of the file to delete.",
        description = "The file ID can be obtained from the Upload task output or from the Slack API."
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
