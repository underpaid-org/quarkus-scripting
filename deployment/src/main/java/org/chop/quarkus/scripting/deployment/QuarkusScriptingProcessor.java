package org.chop.quarkus.scripting.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.console.QuarkusCommand;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class QuarkusScriptingProcessor {

    private static final String FEATURE = "quarkus-scripting";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ConsoleCommandBuildItem runCommand() {
        var config = ConfigProvider.getConfig();

        String httpHost = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String httpPort = config.getOptionalValue("quarkus.http.port", String.class).orElse("8080");

        return new ConsoleCommandBuildItem(new RunCommand(httpHost, httpPort));
    }

    @CommandDefinition(name = "run", description = "Runs a application defined script", aliases = { "r" })
    public static class RunCommand extends QuarkusCommand {
        @Argument(required = true, description = "The script name")
        private String scriptName;

        final HttpClient httpClient;

        final String httpHost;

        final String httpPort;

        public RunCommand(
            String httpHost,
            String httpPort
        ) {
            this.httpClient = HttpClient.newHttpClient();
            this.httpHost = httpHost;
            this.httpPort = httpPort;
        }

        @Override
        public CommandResult doExecute(CommandInvocation commandInvocation) {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + httpHost + ":" + httpPort + "/script/" + scriptName))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var messageBuilder = new StringBuilder()
                    .append("Script completed with response code ")
                    .append(response.statusCode());

                if (response.body() != null && !response.body().isEmpty()) {
                    messageBuilder
                        .append(" and message: ")
                        .append(response.body());
                }

                commandInvocation
                    .getShell()
                    .writeln(messageBuilder.toString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            return CommandResult.SUCCESS;
        }
    }
}
