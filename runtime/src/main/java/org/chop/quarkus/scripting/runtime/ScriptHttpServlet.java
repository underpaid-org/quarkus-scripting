package org.chop.quarkus.scripting.runtime;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@IfBuildProfile("dev")
@WebServlet
public class ScriptHttpServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var scriptName = determineScriptName(req.getPathInfo());
        var scriptNameToScriptMap = collectScriptNameToScriptMap();

        if (!scriptNameToScriptMap.containsKey(scriptName)) {
            resp.getWriter().write("Script not found: " + scriptName);
            resp.setStatus(404);
        }

        scriptNameToScriptMap.get(scriptName).run();

        resp.setStatus(200);
    }

    private String determineScriptName(String path) {
        if (path == null) {
            return null;
        }

        return path.substring(1);
    }

    private Map<String, Script> collectScriptNameToScriptMap() {
        var scriptNameToScriptMap = new HashMap<String, Script>();

        CDI.current().select(Script.class).stream()
            .forEach(
                script -> {
                    if (scriptNameToScriptMap.containsKey(script.getName())) {
                        throw new IllegalStateException("Duplicate script name: " + script.getName());
                    }

                    scriptNameToScriptMap.put(script.getName(), script);
                }
            );

        return scriptNameToScriptMap;
    }
}
