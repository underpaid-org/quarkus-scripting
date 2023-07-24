package org.chop.quarkus.scripting.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.console.QuarkusCommand;
import io.quarkus.undertow.deployment.ServletBuildItem;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.chop.quarkus.scripting.runtime.ScriptHttpServlet;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

class QuarkusScriptingProcessor {

    private static final String FEATURE = "scripting";
    private static final String SCRIPTS_PATH = "/scripts";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem createServlet() {
        return ServletBuildItem.builder("greeting-extension", ScriptHttpServlet.class.getName())
            .addMapping(SCRIPTS_PATH + "/*")
            .build();
    }

    @BuildStep
    ConsoleCommandBuildItem runCommand() {
        var config = ConfigProvider.getConfig();

        String httpHost = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String httpPort = config.getOptionalValue("quarkus.http.port", String.class).orElse("8080");

        return new ConsoleCommandBuildItem(new RunCommand(httpHost, httpPort, SCRIPTS_PATH));
    }

    @CommandDefinition(name = "run", description = "Runs a application defined script", aliases = { "r" })
    public static class RunCommand extends QuarkusCommand {
        @Arguments(required = true, description = "Script name and arguments")
        private List<String> arguments;

        final HttpClient httpClient;
        final String httpHost;
        final String httpPort;
        final String scriptsPath;

        public RunCommand(
            String httpHost,
            String httpPort,
            String scriptsPath
        ) {
            this.httpClient = HttpClient.newHttpClient();
            this.httpHost = httpHost;
            this.httpPort = httpPort;
            this.scriptsPath = scriptsPath;
        }

        @Override
        public CommandResult doExecute(CommandInvocation commandInvocation) {
            try {
                var request = HttpRequest.newBuilder()
                    .uri(buildUri())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    commandInvocation.getShell().writeln("SUCCESS! Script completed successfully.");

                    return CommandResult.SUCCESS;
                } else {
                    var messageBuilder = new StringBuilder()
                        .append("FAILURE! Script failed.");

                    if (response.body() != null && !response.body().isEmpty()) {
                        messageBuilder
                            .append("\n")
                            .append(response.body());
                    }

                    commandInvocation.getShell().writeln(messageBuilder.toString());

                    return CommandResult.FAILURE;
                }
            } catch (IOException | InterruptedException exception) {
                commandInvocation.getShell().writeln("FAILURE! Script failed." + "\n" + exception.getMessage());

                return CommandResult.FAILURE;
            } catch (ScriptNotSpecifiedException e) {
                commandInvocation.getShell().writeln("FAILURE! Script failed." + "\n" + "Script not specified.");

                return CommandResult.FAILURE;
            }
        }

        private URI buildUri() throws ScriptNotSpecifiedException {
            if (arguments.size() < 1) {
                throw new ScriptNotSpecifiedException();
            }

            var scriptName = arguments.get(0);

            return URI.create("http://" + httpHost + ":" + httpPort + scriptsPath + "/" + scriptName + "/" + String.join("/", arguments.subList(1, arguments.size())));
        }

        private static class ScriptNotSpecifiedException extends Exception {
            public ScriptNotSpecifiedException() {
                super("Script not specified.");
            }
        }
    }
}
