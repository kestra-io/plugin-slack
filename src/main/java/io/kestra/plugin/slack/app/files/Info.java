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
    title = "Get information about a file in Slack."
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
                    type: io.kestra.plugin.slack.files.Info
                    token: "{{ secret('SLACK_TOKEN') }}"
                    fileId: "F1234567890"
                """
        )
    }
)
public class Info extends AbstractSlackClientConnection implements RunnableTask<FileOutput> {
    @Schema(
        title = "The ID of the file.",
        description = "The file ID can be obtained from the Upload task output or from the Slack API."
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
