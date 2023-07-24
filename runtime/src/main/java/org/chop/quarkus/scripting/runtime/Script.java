package org.chop.quarkus.scripting.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Script {
    public abstract void run(@NotNull List<String> argumentList);

    public abstract String getName();
}
