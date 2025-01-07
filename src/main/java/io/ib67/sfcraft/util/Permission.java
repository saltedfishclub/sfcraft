package io.ib67.sfcraft.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;

import java.util.Objects;

public record Permission<T extends Entity>(String key, boolean byDefault) {
    /**
     * @param key 权限节点
     */
    public Permission(String key, boolean byDefault) {
        this.key = Objects.requireNonNull(key).trim().toLowerCase();
        this.byDefault = byDefault;
    }

    public boolean hasPermission(T t) {
        if (Permissions.check(t, SFConsts.SPECIAL_SUDO)) return true;
        if (byDefault) {
            return !Permissions.check(t, "-"+key);
        }
        return Permissions.check(t, key);
    }
}
