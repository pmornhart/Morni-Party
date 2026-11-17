package de.moinmorni.party.net;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Morni Party ohne Netzwerk: deutsch, keine Party, keine Statistik nach außen (lokal zählt Morni Party trotzdem).
 * Niemand fragt nach „spielt gerade“ oder einem freien Platz – wer die Verbindung verliert, kann trotzdem rechtzeitig
 * auf diesen Server zurückkommen und weiterspielen (das regelt Morni Party selbst).
 */
final class Standalone implements Network {

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public String language(UUID player) {
        return "de";
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        // ohne Netzwerk nur lokal
    }

    @Override
    public void state(String state, int players) {
        // niemand fragt
    }

    @Override
    public void toLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void askToLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void playing(Player player, boolean playing) {
        // kein Netzwerk, das nachfragen könnte
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        // keine Lobby, die „Zurück ins Spiel“ anbietet
    }

    @Override
    public void forget(UUID player) {
        // nichts gemerkt
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public String serverName() {
        return "";
    }
}
