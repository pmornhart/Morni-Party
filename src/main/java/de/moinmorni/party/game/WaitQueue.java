package de.moinmorni.party.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Warteschlange für die nächste Partie. Eine Party ist eine Einheit: sie spielt immer zusammen und wird nie getrennt.
 */
public final class WaitQueue {

    private final List<List<UUID>> units = new ArrayList<>();

    /** Einheit anmelden (Mitglieder, die schon wo anders warten, ziehen in diese Einheit um). */
    public void add(List<UUID> unit) {
        if (unit.isEmpty()) {
            return;
        }
        unit.forEach(this::removeQuietly);
        units.add(new ArrayList<>(unit));
    }

    /** Einzelne Person zu einer bestehenden Einheit dazu (z. B. Party-Mitglied kommt später an). */
    public boolean join(UUID member, UUID unitMember) {
        for (List<UUID> u : units) {
            if (u.contains(unitMember)) {
                if (!u.contains(member)) {
                    removeQuietly(member);
                    u.add(member);
                }
                return true;
            }
        }
        return false;
    }

    public boolean remove(UUID uuid) {
        return removeQuietly(uuid);
    }

    private boolean removeQuietly(UUID uuid) {
        boolean found = false;
        Iterator<List<UUID>> it = units.iterator();
        while (it.hasNext()) {
            List<UUID> u = it.next();
            if (u.remove(uuid)) {
                found = true;
                if (u.isEmpty()) {
                    it.remove();
                }
            }
        }
        return found;
    }

    public boolean contains(UUID uuid) {
        return units.stream().anyMatch(u -> u.contains(uuid));
    }

    public int size() {
        return units.stream().mapToInt(List::size).sum();
    }

    /** Alle Wartenden in der Reihenfolge der Anmeldung. */
    public List<UUID> all() {
        List<UUID> out = new ArrayList<>();
        units.forEach(out::addAll);
        return out;
    }

    /** Wie viele kämen in die nächste Partie (höchstens {@code max}, Partys nicht getrennt)? */
    public int fitting(int max) {
        int n = 0;
        for (List<UUID> u : units) {
            if (n + u.size() <= max) {
                n += u.size();
            }
        }
        return n;
    }

    /**
     * Die nächste Partie herausnehmen: Einheiten der Reihe nach, solange sie hineinpassen (eine zu große Party wartet,
     * die Nächsten rücken vor). Eine Party größer als {@code max} wird ausnahmsweise geteilt.
     */
    public List<UUID> take(int max) {
        List<UUID> out = new ArrayList<>();
        takeGroups(max).forEach(out::addAll);
        return out;
    }

    /** Wie {@link #take}, aber in Einheiten (Partys bleiben als Gruppe erkennbar, z. B. für Seilschaften). */
    public List<List<UUID>> takeGroups(int max) {
        List<List<UUID>> out = new ArrayList<>();
        int n = 0;
        Iterator<List<UUID>> it = units.iterator();
        while (it.hasNext()) {
            List<UUID> u = it.next();
            if (n + u.size() <= max) {
                out.add(new ArrayList<>(u));
                n += u.size();
                it.remove();
            } else if (u.size() > max && out.isEmpty()) {
                out.add(new ArrayList<>(u.subList(0, max)));
                u.subList(0, max).clear();
                break;
            }
        }
        return out;
    }

    /** Einheiten vorn wieder einreihen (wenn eine Partie sie doch nicht braucht). */
    public void putBack(List<List<UUID>> groups) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            units.addFirst(new ArrayList<>(groups.get(i)));
        }
    }

    public void clear() {
        units.clear();
    }
}
