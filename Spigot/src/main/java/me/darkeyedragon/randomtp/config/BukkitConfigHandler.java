package me.darkeyedragon.randomtp.config;

import me.darkeyedragon.randomtp.RandomTeleport;
import me.darkeyedragon.randomtp.SpigotImpl;
import me.darkeyedragon.randomtp.api.config.Dimension;
import me.darkeyedragon.randomtp.api.config.RandomDimensionData;
import me.darkeyedragon.randomtp.api.config.section.*;
import me.darkeyedragon.randomtp.api.config.section.subsection.SectionWorldDetail;
import me.darkeyedragon.randomtp.api.world.RandomParticle;
import me.darkeyedragon.randomtp.api.world.RandomWorld;
import me.darkeyedragon.randomtp.api.world.location.RandomOffset;
import me.darkeyedragon.randomtp.common.config.Blacklist;
import me.darkeyedragon.randomtp.common.config.CommonConfigHandler;
import me.darkeyedragon.randomtp.common.config.DimensionData;
import me.darkeyedragon.randomtp.common.util.TimeUtil;
import me.darkeyedragon.randomtp.common.world.CommonParticle;
import me.darkeyedragon.randomtp.common.world.WorldConfigSection;
import me.darkeyedragon.randomtp.common.world.location.Offset;
import me.darkeyedragon.randomtp.config.section.*;
import me.darkeyedragon.randomtp.util.WorldUtil;
import me.darkeyedragon.randomtp.world.SpigotBiome;
import me.darkeyedragon.randomtp.world.SpigotBlockType;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BukkitConfigHandler extends CommonConfigHandler {

    private final SpigotImpl plugin;
    private ConfigMessage configMessage;
    private ConfigQueue configQueue;
    private ConfigWorld configWorld;
    private ConfigTeleport configTeleport;
    private ConfigEconomy configEconomy;
    private ConfigDebug configDebug;
    private ConfigBlacklist configBlacklist;

    public BukkitConfigHandler(RandomTeleport instance) {
        super(instance);
        this.plugin = instance.getPlugin();
    }

    /**
     * (re)loads the config.
     * When invalid fiels are found, they will be defaulted to prevent errors.
     */
    public boolean reload() {
        populateConfigMessage();
        populateConfigQueue();
        populateWorldConfigSection();
        populateConfigTeleport();
        populateConfigDebug();
        populateConfigEconomy();
        populateBlacklist();
        return true;
    }

    public void populateBlacklist() {
        configBlacklist = new ConfigBlacklist(getBlacklist());
    }

    public void populateConfigMessage() {
        configMessage = new ConfigMessage()
                .init(getInitTeleportMessage())
                .initTeleportDelay(getInitTeleportDelay())
                .teleportCanceled(getCancelMessage())
                .teleport(getTeleportMessage())
                .depletedQueue(getDepletedQueueMessage())
                .countdown(getCountdownRemainingMessage())
                .noWorldPermission(getNoWorldPermissionMessage())
                .emptyQueue(getEmptyQueueMessage())
                .invalidDefaultWorld(getInvalidDefaultWorld());
        configMessage.getEconomy()
                .insufficientFunds(getInsufficientFundsMessage())
                .payment(getPaymentMessage());
        configMessage.getSign().setComponents(getSignLines());
    }

    public void populateConfigQueue() {
        configQueue = new ConfigQueue()
                .size(getQueueSize())
                .initDelay(getInitDelay());
    }

    public void populateWorldConfigSection() {
        configWorld = new ConfigWorld(plugin, getOffsets());
    }

    public void populateConfigTeleport() {
        configTeleport = new ConfigTeleport(getCooldown(), getTeleportDelay(), isCanceledOnMove(), getTeleportDeathTimer(), getParticle(), getUseDefault(), getDefaultWorld());
    }

    public void populateConfigDebug() {
        configDebug = new ConfigDebug()
                .showQueuePopulation(getDebugShowQueuePopulation());
    }


    public void populateConfigEconomy() {
        configEconomy = new ConfigEconomy()
                .price(getPrice());
    }

    @Override
    public SectionMessage getSectionMessage() {
        return configMessage;
    }

    @Override
    public SectionQueue getSectionQueue() {
        return configQueue;
    }

    @Override
    public SectionWorld getSectionWorld() {
        return configWorld;
    }

    @Override
    public SectionBlacklist getSectionBlacklist() {
        return configBlacklist;
    }

    @Override
    public void saveConfig() {
        plugin.saveConfig();
    }

    @Override
    public SectionTeleport getSectionTeleport() {
        return configTeleport;
    }

    @Override
    public SectionDebug getSectionDebug() {
        return configDebug;
    }

    @Override
    public SectionEconomy getSectionEconomy() {
        return configEconomy;
    }

    private String getInitTeleportMessage() {
        return plugin.getConfig().getString("message.initteleport");
    }

    private String getNoWorldPermissionMessage() {
        return plugin.getConfig().getString("message.no_world_permission");
    }

    private String getTeleportMessage() {
        return plugin.getConfig().getString("message.teleport");
    }

    private String getDepletedQueueMessage() {
        return plugin.getConfig().getString("message.depleted_queue", "&6Locations queue depleted... Forcing generation of a new location");
    }

    private String getCountdownRemainingMessage() {
        return plugin.getConfig().getString("message.countdown");
    }

    private Set<SectionWorldDetail> getOffsets() {
        final ConfigurationSection section = plugin.getConfig().getConfigurationSection("worlds");
        Set<String> keys = Objects.requireNonNull(section).getKeys(false);
        Set<SectionWorldDetail> sectionWorldDetailSet = new HashSet<>(keys.size());
        for (String key : keys) {
            World world = Bukkit.getWorld(key);
            if (world == null) {
                randomTeleportPlugin.getLogger().warn("World " + key + " does not exist! Skipping...");
                continue;
            }
            boolean useWorldBorder = section.getBoolean(key + ".use_worldborder");
            boolean needsWorldPermission = section.getBoolean(key + ".needs_world_permission");
            int radius = section.getInt(key + ".radius");
            int offsetX = section.getInt(key + ".offsetX");
            int offsetZ = section.getInt(key + ".offsetZ");
            double price = section.getDouble(key + ".price");
            plugin.getLogger().info(ChatColor.GREEN + key + " found! Loading...");
            RandomWorld randomWorld = WorldUtil.toRandomWorld(world);
            RandomOffset offset;
            if (useWorldBorder) {
                offset = RandomWorld.getOffset(randomWorld);
            } else {
                offset = new Offset(offsetX, offsetZ, radius);
            }
            sectionWorldDetailSet.add(new WorldConfigSection(offset, randomWorld, price, useWorldBorder, needsWorldPermission));
        }
        return sectionWorldDetailSet;
    }

    private String getInsufficientFundsMessage() {
        return plugin.getConfig().getString("message.economy.insufficient_funds", "&cYou do not have enough money to rtp!");
    }

    private double getPrice() {
        return plugin.getConfig().getDouble("economy.default_price", 0);
    }

    private String getPaymentMessage() {
        String message = plugin.getConfig().getString("message.economy.payment", "&aYou just paid &b%price &ato rtp!");
        message = ChatColor.translateAlternateColorCodes('&', message);
        message = message.replaceAll("%price", getPrice() + "");
        return message;
    }

    private long getCooldown() throws NumberFormatException {
        String message = plugin.getConfig().getString("teleport.cooldown", "60m");
        return TimeUtil.stringToLong(message);
    }

    private long getTeleportDelay() {
        String message = plugin.getConfig().getString("teleport.delay", "0s");
        if (message != null) {
            return TimeUtil.stringToLong(message);
        }
        throw new NumberFormatException("Not a valid number");
    }

    private int getInitDelay() {
        return plugin.getConfig().getInt("queue.init_delay", 60);
    }

    private boolean getDebugShowQueuePopulation() {
        return plugin.getConfig().getBoolean("debug.show_queue_population", true);
    }

    private List<String> getPlugins() {
        return plugin.getConfig().getStringList("plugins");
    }

    private int getQueueSize() {
        return plugin.getConfig().getInt("queue.size", 5);
    }

    private String getCancelMessage() {
        return plugin.getConfig().getString("message.teleport_canceled", "&cYou moved! Teleportation canceled");
    }

    private String getInitTeleportDelay() {
        return plugin.getConfig().getString("message.initteleport_delay", "&aYou will be teleported in &6%s seconds. Do not move");
    }

    private boolean isCanceledOnMove() {
        return plugin.getConfig().getBoolean("teleport.cancel_on_move", false);
    }

    private String getEmptyQueueMessage() {
        return plugin.getConfig().getString("message.empty_queue", "&cThere are no locations available for this world! Try again in a bit or ask an admin to reload the config.");
    }

    private long getTeleportDeathTimer() {
        String message = plugin.getConfig().getString("teleport.death_timer", "10s");
        if (message != null) {
            return TimeUtil.stringToLong(message);
        }
        throw new NumberFormatException("Not a valid number");
    }

    public List<String> getSignLines() {
        return plugin.getConfig().getStringList("message.sign");
    }

    public Blacklist getBlacklist() {
        Blacklist blacklist = new Blacklist();
        for (Dimension dimension : Dimension.values()) {
            blacklist.addDimensionData(dimension, getDimData(dimension));
        }
        return blacklist;
    }

    private RandomDimensionData getDimData(Dimension dimension) {
        ConfigurationSection blacklistSec = plugin.getConfig().getConfigurationSection("blacklist");
        if (blacklistSec == null) return null;
        RandomDimensionData dimensionData = new DimensionData();

        ConfigurationSection section;
        switch (dimension) {
            case GLOBAL:
                section = blacklistSec.getConfigurationSection("global");
                break;
            case OVERWORLD:
                section = blacklistSec.getConfigurationSection("overworld");
                break;
            case NETHER:
                section = blacklistSec.getConfigurationSection("nether");
                break;
            case END:
                section = blacklistSec.getConfigurationSection("end");
                break;
            default:
                section = null;
                break;
        }
        if (section == null) return null;
        List<String> blockStrings = section.getStringList("block");
        Material[] materials = Material.values();
        for (String s : blockStrings) {
            if (s.startsWith("$")) {
                Iterable<Tag<Material>> tags = Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material.class);
                for (Tag<Material> tag : tags) {
                    if (tag.getKey().getKey().equalsIgnoreCase(s.substring(1))) {
                        for (Material value : tag.getValues()) {
                            dimensionData.addBlockType(new SpigotBlockType(value));
                        }
                    }
                }
            } else {
                Pattern pattern = Pattern.compile(s);
                for (Material material : materials) {
                    Matcher matcher = pattern.matcher(material.name());
                    while (matcher.find()) {
                        try {
                            dimensionData.addBlockType(new SpigotBlockType(Material.valueOf(matcher.group(0))));
                        } catch (IllegalArgumentException ex) {
                            super.randomTeleportPlugin.getLogger().warn(s + " is not a valid block.");
                        }
                    }
                }
            }
        }
        if (dimension == Dimension.GLOBAL) {
            return dimensionData;
        }
        List<String> biomeStrings = section.getStringList("biome");
        for (String s : biomeStrings) {
            Pattern pattern = Pattern.compile(s);
            for (Material material : materials) {
                Matcher matcher = pattern.matcher(material.name());
                while (matcher.find()) {
                    try {
                        dimensionData.addBiome(new SpigotBiome(Biome.valueOf(s.toUpperCase())));
                    } catch (IllegalArgumentException ex) {
                        randomTeleportPlugin.getLogger().warn(s + " is not a valid biome.");
                    }
                }
            }
        }
        return dimensionData;
    }

    @Nullable
    private RandomParticle getParticle() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("teleport");
        String particleString = section.getString("particle");
        if (particleString == null || particleString.equalsIgnoreCase("none")) {
            return new CommonParticle(null, 0);
        }
        String[] split = particleString.split(":");
        try {
            String particleId = split[0].toUpperCase();
            int amount = Integer.parseInt(split[1]);
            return new CommonParticle(particleId, amount);
        } catch (IllegalArgumentException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private boolean getUseDefault() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("teleport");
        return section.getBoolean("use_default_world", false);
    }

    private String getDefaultWorld() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("teleport");
        return section.getString("default_world", "world");
    }

    private String getInvalidDefaultWorld() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("message");
        return section.getString("invalid_default_world");
    }

    public ConfigBlacklist getConfigBlacklist() {
        return configBlacklist;
    }
}
