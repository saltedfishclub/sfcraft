package io.ib67.sfcraft.module.hint;

/**
 * 一条提示的静态定义:id 同时作为实体 tag 后缀({@code sfhint_seen.<id>} / {@code sfhint_used.<id>})。
 * 所有提示对同一玩家均为一次性:emit 后打 seen tag,永不重发,故无需冷却字段。
 */
public enum Hint {
    ELYTRA_PORTAL_RUSH("elytra_portal_rush", "message.sfcraft.hint.elytra_portal_rush"),
    MOUNT_TAME_NEARBY("mount_tame_nearby", "message.sfcraft.hint.mount_tame_nearby"),
    RAVAGER_DASH("ravager_dash", "message.sfcraft.hint.ravager_dash"),
    LUNCHBOX_EAT("lunchbox_eat", "message.sfcraft.hint.lunchbox_eat"),
    REVERSE_TOKEN_AVAILABLE("reverse_token_available", "message.sfcraft.hint.reverse_token_available", true),
    STANDING_FIRM_TOTEM("standing_firm_totem", "message.sfcraft.hint.standing_firm_totem");

    private final String id;
    private final String langKey;
    private final boolean autoUsedOnEmit;

    Hint(String id, String langKey) {
        this(id, langKey, false);
    }

    Hint(String id, String langKey, boolean autoUsedOnEmit) {
        this.id = id;
        this.langKey = langKey;
        this.autoUsedOnEmit = autoUsedOnEmit;
    }

    public String id() {
        return id;
    }

    public String langKey() {
        return langKey;
    }

    /** true 表示一旦提示过即视作「已使用」,永远不再发(适用瞬时一次性事件)。 */
    public boolean autoUsedOnEmit() {
        return autoUsedOnEmit;
    }

    public static Hint byId(String id) {
        for (Hint hint : values()) {
            if (hint.id.equals(id)) return hint;
        }
        return null;
    }
}
