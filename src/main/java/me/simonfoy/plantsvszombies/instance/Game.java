package me.simonfoy.plantsvszombies.instance;

import me.simonfoy.plantsvszombies.Plants_vs_Zombies;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {

    private Plants_vs_Zombies plantsVsZombies;
    private Location hub;
    private Location spawn;
    private GameState state;
    private List<UUID> players;
    private int requiredPlayers;
    private Countdown countdown;
    private GameTimer gameTimer;
    private Plants_vs_ZombiesGame plantsVsZombiesGame;
    private ScoreboardManager scoreBoardManager;

    public Game(Plants_vs_Zombies plantsVsZombies) {
        this.plantsVsZombies = plantsVsZombies;
        this.hub = new Location(Bukkit.getWorld("world"), 0, 70, 0);
        this.spawn = new Location(Bukkit.getWorld("world"), 0, 250, 0);
        this.state = GameState.PREPARING;
        this.players = new ArrayList<>();
        this.requiredPlayers = 1;
        this.countdown = new Countdown(plantsVsZombies, this);
        this.gameTimer = new GameTimer(plantsVsZombies, this);
        this.plantsVsZombiesGame = new Plants_vs_ZombiesGame(plantsVsZombies, this);
        this.scoreBoardManager = new ScoreboardManager(plantsVsZombies);
    }

    /* BASE GAME INSTANCE LOGIC */

    public void start() {
        if (state != GameState.PREPARING && state != GameState.PRE_START) {
            return;
        }
        plantsVsZombiesGame.start();
        setState(GameState.IN_PROGRESS);

        for (Player players : Bukkit.getOnlinePlayers()) {
            getScoreBoardManager().clearScoreboard(players);
            getScoreBoardManager().setupGameStartScoreboard(players);
        }
        sendMessage(ChatColor.YELLOW + "Game is now in IN_PROGRESS State");
    }

    public void end() {
        if (state != GameState.IN_PROGRESS) {
            return;
        }
        setState(GameState.ENDING);
        sendMessage(ChatColor.YELLOW + "Game is now in ENDING State");

        cleanUp();
    }

    public void cleanUp() {
        if (state != GameState.ENDING) {
            return;
        }
        setState(GameState.CLEANING_UP);
        sendMessage(ChatColor.YELLOW + "Game is now in CLEANING_UP State");

        reset(true);
    }

    public void reset(boolean removePlayers) {
        if (removePlayers) {
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                player.teleport(hub);
            }
            players.clear();
        }
        sendTitle("", "");
        plantsVsZombiesGame.unregister();
        setState(GameState.PREPARING);

        for (Player players : Bukkit.getOnlinePlayers()) {
            getScoreBoardManager().clearScoreboard(players);
            getScoreBoardManager().setupGamePreparingScoreboard(players);
        }

        if (countdown.isRunning()) {
            countdown.stop();
        }

        if (gameTimer.isRunning()) {
            gameTimer.stop();
        }

        countdown = new Countdown(plantsVsZombies, this);
        gameTimer = new GameTimer(plantsVsZombies, this);
        plantsVsZombiesGame = new Plants_vs_ZombiesGame(plantsVsZombies, this);
    }

    /* UTILITIES */

    public void restart() {
        int countdownTime = 10;
        new BukkitRunnable() {
            int remaining = countdownTime;

            @Override
            public void run() {
                if (remaining > 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Server is restarting in " + remaining + " seconds...");
                    remaining--;
                } else {
                    cancel();
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
                }
            }
        }.runTaskTimer(plantsVsZombies, 0L, 20L);
    }

    public void sendMessage(String message) {
        for (UUID uuid : players) {
            Bukkit.getPlayer(uuid).sendMessage(message);
        }
    }

    public void sendTitle(String title, String subtitle) {
        for (UUID uuid : players) {
            Bukkit.getPlayer(uuid).sendTitle(title, subtitle);
        }
    }

    /* PLAYER LOGIC */

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        player.teleport(spawn);
        player.closeInventory();
        player.getInventory().clear();
        getScoreBoardManager().clearScoreboard(player);
        getScoreBoardManager().setupGamePreparingScoreboard(player);

        if (state.equals(GameState.PREPARING) && players.size() == requiredPlayers) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                getScoreBoardManager().clearScoreboard(players);
                getScoreBoardManager().setupGamePreStartScoreboard(players);
            }
            countdown.start();
            setState(GameState.PRE_START);
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        player.teleport(hub);
        getScoreBoardManager().clearScoreboard(player);
        player.sendTitle("", "");


        if (state == GameState.PRE_START && players.size() < requiredPlayers) {
            sendMessage(ChatColor.RED + "There is not enough players. Countdown stopped.");
            reset(false);
            return;
        }

        if (state == GameState.IN_PROGRESS && players.size() < requiredPlayers) {
            sendMessage(ChatColor.RED + "The game has ended as too many players have left.");
            end();
        }
    }

    public Location getSpawn() { return spawn; }
    public GameState getState() { return state; }
    public List<UUID> getPlayers() { return players; }
    public int getRequiredPlayers() { return requiredPlayers; }
    public Plants_vs_ZombiesGame getPlantsVsZombiesGame() { return plantsVsZombiesGame; }
    public Countdown getCountdown() { return countdown; }
    public GameTimer getGameTimer() { return gameTimer; }
    public ScoreboardManager getScoreBoardManager() { return scoreBoardManager; }
    public void setState(GameState state) { this.state = state; }

}
