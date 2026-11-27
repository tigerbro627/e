package dev.serverhub;

/** Editable at config/serverhub/config.json (restart or reload the server after editing). */
public final class Config {
    public String currencySymbol = "$";
    public long startingBalance = 1000;
    public int auctionDurationHours = 48;
    public int auctionTaxPercent = 5;
    public long auctionMinPrice = 1;
    public int maxListingsPerPlayer = 10;
    public int tpaTimeoutSeconds = 60;
    public int tpaCooldownSeconds = 10;
    public boolean mobRewardsEnabled = true;
    public int healthPerDollar = 8; // 8 health = 4 hearts = $1
    public boolean sidebarEnabled = true;
    public String sidebarTitle = "SERVER";

    private static Config instance;

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void save() {
        if (instance != null) Storage.write("config.json", instance);
    }

    public static void load() {
        Config c = Storage.read("config.json", Config.class);
        if (c == null) c = new Config();
        instance = c;
        Storage.write("config.json", c);
    }
}
