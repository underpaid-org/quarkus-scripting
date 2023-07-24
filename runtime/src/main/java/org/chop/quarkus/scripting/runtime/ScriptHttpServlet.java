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

        var scriptName = determineScriptName(req.getPathInfo());

        if (!scriptNameToScriptMap.containsKey(determineScriptName(req.getPathInfo()))) {
            resp.getWriter().write("Script not found: " + scriptName);
            resp.setStatus(404);

            return;
        }

        try {
            var script = scriptNameToScriptMap.get(scriptName);

            script.run();
        } catch (Exception exception) {
            resp.getWriter().write("Script failed: " + scriptName + "\n" + getFilteredStackTrace(exception));
            resp.setStatus(500);

            return;
        }

        resp.setStatus(200);
    }

    private String determineScriptName(String path) {
        if (path == null) {
            return null;
        }

        return path.substring(1);
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
        var filteredCauseStackTrace = Arrays.stream(exception.getCause().getStackTrace())
            .filter(
                stackTraceElement -> stackTraceElement.getClassName().startsWith("org.chop") ||
                    stackTraceElement.getClassName().startsWith("org.lucra")
            )
            .toArray(StackTraceElement[]::new);

        exception.setStackTrace(filteredStackTrace);
        exception.getCause().setStackTrace(filteredCauseStackTrace);
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
}
