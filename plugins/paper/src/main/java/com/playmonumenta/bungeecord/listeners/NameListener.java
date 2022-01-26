package com.playmonumenta.bungeecord.listeners;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.yaml.snakeyaml.Yaml;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class NameListener implements Listener {
	private static NameListener globalNameListener = null;

	private final Yaml mYaml = new Yaml();
	private final File file = new File("uuid2name.yml");
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final Plugin plugin;
	private final Logger logger;

	/* This is the "real" table that is saved / loaded */
	private final HashMap<String, String> uuid2name;
	/* This is a convenience table that is not saved - case insensitive, always lowercase names */
	private final HashMap<String, String> name2uuid;

	@SuppressWarnings("unchecked")
	public NameListener(Plugin plugin) {
		this.plugin = plugin;
		logger = plugin.getLogger();

		uuid2name = new HashMap<String, String>();

		try {
			file.createNewFile();
			try (Reader rd = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				Map<String, String> map = mYaml.loadAs(rd, Map.class);
				if (map != null) {
					uuid2name.putAll(map);
				}
			}
		} catch (Exception ex) {
			file.renameTo(new File("uuid2name.yml.old"));
			logger.severe("Could not load uuid2name, resetting them");
		}

		name2uuid = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : uuid2name.entrySet()) {
			name2uuid.put(entry.getValue().toLowerCase(), entry.getKey());
		}

		logger.info("Loaded uuid2name database with " + uuid2name.size() + " entries");

		/* Keep a global static reference to the most recently instantiated object */
		if (globalNameListener != null) {
			/* Unregister any existing listener when re-instantiating this class */
			plugin.getProxy().getPluginManager().unregisterListener(globalNameListener);
		}
		globalNameListener = this;

		/* Register this object as an event handler */
		plugin.getProxy().getPluginManager().registerListener(plugin, this);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void postLoginEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getName();

		lock.readLock().lock();
		String storedName = uuid2name.get(uuid.toString());
		lock.readLock().unlock();

		if (storedName == null || !storedName.equals(name)) {
			lock.writeLock().lock();

			if (storedName != null) {
				/* Name changed! */
				name2uuid.remove(storedName);
			}

			uuid2name.put(uuid.toString(), name);
			name2uuid.put(name.toLowerCase(), uuid.toString());

			lock.writeLock().unlock();

			/* Log messages outside the locked section */
			if (storedName != null) {
				logger.info("Detected name change for player " + uuid.toString() + ": " + storedName + " -> " + name);
			}
			logger.info("Updated name for player " + uuid.toString() + ": " + name);

			/* Since the data was changed, schedule an async task to save it */
			plugin.getProxy().getScheduler().schedule(plugin, () -> {
				save();
			}, 0, 0, TimeUnit.SECONDS);
		}
	}

	/* Case insensitive! */
	public static @Nullable UUID name2uuid(String name) {
		if (globalNameListener == null) {
			return null;
		}

		globalNameListener.lock.readLock().lock();
		String uuidStr = globalNameListener.name2uuid.get(name.toLowerCase());
		globalNameListener.lock.readLock().unlock();

		if (uuidStr == null) {
			return null;
		}
		return UUID.fromString(uuidStr);
	}

	public static @Nullable String uuid2name(UUID uuid) {
		if (globalNameListener == null) {
			return null;
		}

		globalNameListener.lock.readLock().lock();
		String name = globalNameListener.uuid2name.get(uuid.toString());
		globalNameListener.lock.readLock().unlock();
		return name;
	}

	private void save() {
		Map<String, String> copy = new HashMap<String, String>();
		lock.readLock().lock();
		try {
			copy.putAll(uuid2name);
		} finally {
			lock.readLock().unlock();
		}

		try (Writer wr = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			mYaml.dump(copy, wr);
			logger.info("Successfully saved uuid2name mappings");
		} catch (IOException ex) {
			logger.log(Level.WARNING, "Could not save uuid2name", ex);
		}
	}
}
