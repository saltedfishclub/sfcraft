package io.ib67.sfcraft.access.impl;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.ib67.sfcraft.access.AccessController;
import io.ib67.sfcraft.access.GrantedEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SimpleAccessController implements AccessController {
    private final Multimap<String, String> whitelist = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

    public SimpleAccessController(List<GrantedEntry> entries) {
        entries.forEach(i -> whitelist.put(i.name(), i.condition()));
    }

    @Override
    public boolean checkAccess(String profile, String address) {
        return whitelist.containsEntry(profile, address) || whitelist.containsEntry(profile, null);
    }

    @Override
    public void grantAccess(String profile, String condition) {
        whitelist.put(profile, condition);
    }

    @Override
    public void revokeAccess(String profile) {
        whitelist.removeAll(profile);
    }

    @Override
    public Collection<? extends GrantedEntry> getEntries() {
        return whitelist.asMap().entrySet()
                .stream().flatMap(it -> it.getValue().stream().map(i -> new GrantedEntry(it.getKey(), i)))
                .collect(Collectors.toSet());
    }
}
