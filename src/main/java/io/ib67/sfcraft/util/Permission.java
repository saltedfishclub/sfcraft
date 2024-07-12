package io.ib67.sfcraft.util;

import net.minecraft.entity.Entity;

import java.util.Objects;

/**
 * 基于 Command Tags 实现的权限系统
 * 权限节点通常以 `.` 切割并且总是在 `sfcraft` 分类下，例如 `sfcraft.back`
 * 用 `-权限节点` 表示禁止使用
 *
 * @param <T> 对象
 */
public record Permission<T extends Entity>(String key, boolean byDefault) {
    /**
     * @param key 权限节点
     */
    public Permission(String key, boolean byDefault) {
        this.key = Objects.requireNonNull(key).trim().toLowerCase();
        this.byDefault = byDefault;
    }

    public boolean hasPermission(T t) {
        if (t.getCommandTags().contains(SFConsts.SPECIAL_SUDO)) return true;
        if (byDefault) {
            return !t.getCommandTags().contains("-" + key);
        }
        return t.getCommandTags().contains(key);
    }

    public void grant(T t) {
        if (!hasPermission(t)) {
            t.getCommandTags().remove("-" + key);
            if (!byDefault) {
                t.addCommandTag(key);
            }
        }
    }

    public void revoke(T t) {
        if (hasPermission(t)) {
            t.removeCommandTag(key);
            if (byDefault) {
                t.addCommandTag("-" + key);
            }
        }
    }
}
