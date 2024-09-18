package io.ib67.sfcraft.room.data;

import com.mojang.serialization.Dynamic;
import lombok.Getter;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentState;

import java.util.Objects;

@Getter
public class RoomWorldDataLoader extends PersistentState {

    private static final Identifier ID = Identifier.of("sfcraft", "room_data");
    private static final Type<RoomWorldDataLoader> TYPE = new Type<>(RoomWorldDataLoader::new, RoomWorldDataLoader::readNbt, DataFixTypes.LEVEL);

    private static final String TAG_GAME_RULES = "gameRules";

    private final GameRules gameRules;

    public RoomWorldDataLoader() {
        this(new GameRules());
    }

    public RoomWorldDataLoader(GameRules gameRules) {
        this.gameRules = gameRules;
    }

    public static RoomWorldDataLoader get(ServerWorld world) {
        Objects.requireNonNull(world);
        var state = world.getPersistentStateManager().getOrCreate(TYPE, ID.toString());
        state.markDirty();
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registry) {
        nbt.put(TAG_GAME_RULES, gameRules.toNbt());
        return nbt;
    }

    private static RoomWorldDataLoader readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registry) {
        if (nbt.contains(TAG_GAME_RULES)) {
            var c = nbt.getCompound(TAG_GAME_RULES);
            var dynamic = new Dynamic<>(NbtOps.INSTANCE, c);
            var rule = new GameRules(dynamic);
            return new RoomWorldDataLoader(rule);
        }
        return new RoomWorldDataLoader();
    }
}
