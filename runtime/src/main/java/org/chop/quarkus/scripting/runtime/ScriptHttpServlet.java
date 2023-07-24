package org.chop.quarkus.scripting.runtime;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IfBuildProfile("dev")
@WebServlet
public class ScriptHttpServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Script> scriptNameToScriptMap;

        try {
            scriptNameToScriptMap = collectScriptNameToScriptMap();
        } catch (DuplicateScriptException duplicateScriptException) {
            resp
                .getWriter()
                .write("Duplicate scripts with same name found:\n" + duplicateScriptException.script1.getClass().getName() + "\n" + duplicateScriptException.script2.getClass().getName());
            resp.setStatus(500);

            return;
        }

        String scriptName;

        try {
            scriptName = determineScriptName(req.getPathInfo());
        } catch (ScriptNotSpecifiedException e) {
            resp.getWriter().write("Script not specified.");
            resp.setStatus(400);

            return;
        }

        if (!scriptNameToScriptMap.containsKey(scriptName)) {
            resp.getWriter().write("Script not found: " + scriptName);
            resp.setStatus(404);

            return;
        }

        try {
            var script = scriptNameToScriptMap.get(scriptName);

            script.run(determineScriptArgumentList(req.getPathInfo()));
        } catch (Exception exception) {
            resp.getWriter().write("Script failed: " + scriptName + "\n" + getFilteredStackTrace(exception));
            resp.setStatus(500);

            return;
        }

        resp.setStatus(200);
    }

    private String determineScriptName(String path) throws ScriptNotSpecifiedException {
        if (path == null) {
            throw new ScriptNotSpecifiedException();
        }

        var splitPath = path.split("/");

        if (splitPath.length < 2) {
            throw new ScriptNotSpecifiedException();
        }

        return splitPath[1];
    }

    private List<String> determineScriptArgumentList(String path) {
        var splitPath = path.split("/");

        if (splitPath.length < 3) {
            return List.of();
        }

        return Arrays.asList(splitPath).subList(2, splitPath.length);
    }

    private Map<String, Script> collectScriptNameToScriptMap() throws DuplicateScriptException {
        var scriptNameToScriptMap = new HashMap<String, Script>();

        for (var script : CDI.current().select(Script.class)) {
            var existingScript = scriptNameToScriptMap.get(script.getName());

            if (scriptNameToScriptMap.containsKey(script.getName())) {
                throw new DuplicateScriptException(
                    existingScript,
                    script
                );
            }

            scriptNameToScriptMap.put(script.getName(), script);
        }

        return scriptNameToScriptMap;
    }

    private String getFilteredStackTrace(Exception exception) {
        var stringWriter = new StringWriter();
        var printWriter = new PrintWriter(stringWriter);

        var filteredStackTrace = Arrays.stream(exception.getStackTrace())
            .filter(
                stackTraceElement -> stackTraceElement.getClassName().startsWith("org.chop") ||
                    stackTraceElement.getClassName().startsWith("org.lucra")
            )
            .toArray(StackTraceElement[]::new);

        exception.setStackTrace(filteredStackTrace);

        if (exception.getCause() != null) {
            var cause = exception.getCause();
            var filteredCauseStackTrace = Arrays.stream(cause.getStackTrace())
                .filter(
                    stackTraceElement -> stackTraceElement.getClassName().startsWith("org.chop") ||
                        stackTraceElement.getClassName().startsWith("org.lucra")
                )
                .toArray(StackTraceElement[]::new);

            cause.setStackTrace(filteredCauseStackTrace);
        }

        exception.printStackTrace(printWriter);

        return stringWriter.toString();
    }

    private static class DuplicateScriptException extends Exception {
        final Script script1;
        final Script script2;

        DuplicateScriptException(
            Script script1,
            Script script2
        ) {
            super("Duplicate scripts with same name found:\n" + script1.getClass().getName() + "\n" + script2.getClass().getName());

            this.script1 = script1;
            this.script2 = script2;
        }
    }

    private static class ScriptNotSpecifiedException extends Exception {
        ScriptNotSpecifiedException() {
            super("Script not specified.");
        }
    }
}
