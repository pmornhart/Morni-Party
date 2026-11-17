package de.moinmorni.party.net;

import de.moinmorni.api.MoinMorni;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Morni Party im Netzwerk: alles über die Schnittstelle des Netzwerk-Plugins. Die Schnittstelle wird bei jedem Aufruf neu
 * geholt – lädt jemand das Netzwerk-Plugin neu, stimmt sie trotzdem.
 *
 * <p>{@code askToLobby}, {@code playing} und {@code holdSlot} gibt es erst ab Version 2 der Schnittstelle. Ein älteres
 * Netzwerk-Plugin kennt sie nicht (der Aufruf endet dann mit einem {@link LinkageError}): Morni Party merkt sich das,
 * lässt sie danach weg und schickt „zur Lobby“ wie bisher sofort.
 */
final class Connected implements Network {

    private static final String GERMAN = "de";

    private final Logger log;
    /** Wem das Netzwerk gerade „spielt“ glaubt – gemeldet wird nur, was sich ändert. */
    private final Set<UUID> playing = new HashSet<>();
    private boolean v2 = true;

    private Connected(Logger log) {
        this.log = log;
    }

    /** null, wenn das Netzwerk-Plugin keine Schnittstelle anbietet. */
    static Network create(Logger log) {
        return MoinMorni.get().isPresent() ? new Connected(log) : null;
    }

    private static Optional<MoinMorni> api() {
        return MoinMorni.get();
    }

    /** Methode ab Version 2 aufrufen; false, wenn das Netzwerk-Plugin sie nicht kennt (oder nicht da ist). */
    private boolean v2(Consumer<MoinMorni> call) {
        Optional<MoinMorni> api = api();
        if (!v2 || api.isEmpty()) {
            return false;
        }
        try {
            call.accept(api.get());
            return true;
        } catch (LinkageError e) {
            v2 = false;
            log.info("Das Netzwerk-Plugin ist älter (" + e.getClass().getSimpleName() + "): „zur Lobby“ ohne Nachfrage "
                    + "und ohne „Zurück ins Spiel“ – sonst läuft alles wie bisher.");
            return false;
        }
    }

    @Override
    public boolean connected() {
        return true;
    }

    @Override
    public String language(UUID player) {
        String lang = api().map(a -> a.language(player)).orElse(GERMAN);
        return "en".equals(lang) ? "en" : GERMAN;
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        api().ifPresent(a -> a.stats(MODE, player, add, best, false));
    }

    @Override
    public void state(String state, int players) {
        api().ifPresent(a -> a.state(MODE, state, players));
    }

    @Override
    public void toLobby(Player player, Component message) {
        api().ifPresentOrElse(a -> a.toLobby(player), () -> player.kick(message));
    }

    @Override
    public void askToLobby(Player player, Component message) {
        if (!v2(a -> a.askToLobby(player))) {
            toLobby(player, message);
        }
    }

    @Override
    public void playing(Player player, boolean now) {
        UUID u = player.getUniqueId();
        if (now == playing.contains(u)) {
            return;
        }
        if (now) {
            playing.add(u);
        } else {
            playing.remove(u);
        }
        v2(a -> a.playing(player, now));
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        v2(a -> a.holdSlot(player, Math.max(0, seconds)));
    }

    @Override
    public void forget(UUID player) {
        playing.remove(player);
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        Optional<MoinMorni> api = api();
        if (api.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.get().party(player).thenApply(p -> p.map(Connected::convert))
                .exceptionally(e -> Optional.empty());
    }

    private static Party convert(MoinMorni.Party p) {
        return new Party(p.leader(), p.members().stream()
                .map(m -> new Party.Member(m.uuid(), m.name(), m.server()))
                .toList());
    }

    @Override
    public String serverName() {
        return api().map(MoinMorni::serverName).orElse("");
    }
}
