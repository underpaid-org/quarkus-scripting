package org.chop.quarkus.scripting.runtime;

import java.util.List;
import java.util.Map;

public interface Script {
    public abstract void run(List<String> argumentList);

    public abstract String getName();
}
