package io.ib67.sfcraft.module.chat;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public record ChatPrefix(
        Component prefix,
        String id,
        boolean temporary,
        int priority
) implements Comparable<ChatPrefix> {
    public ChatPrefix {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(prefix, "prefix cannot be null");
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof ChatPrefix && ((ChatPrefix) obj).id.equals(id);
    }

    @Override
    public int compareTo(@NotNull ChatPrefix o) {
        return Integer.compare(priority, o.priority);
    }
}
