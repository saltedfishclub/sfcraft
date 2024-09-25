package io.ib67.sfcraft.module.chat;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public record ChatPrefix(
        Text prefix,
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
