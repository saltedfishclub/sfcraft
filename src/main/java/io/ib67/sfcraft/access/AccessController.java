package io.ib67.sfcraft.access;

import com.mojang.authlib.GameProfile;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

public interface AccessController {
    boolean checkAccess(String name, String address);

    void grantAccess(String name, String condition);

    void revokeAccess(String name);

    Collection<? extends GrantedEntry> getEntries();
}
