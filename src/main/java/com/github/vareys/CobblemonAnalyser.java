package com.github.vareys;

import com.github.vareys.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CobblemonAnalyser implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("cobblemon_analyser");
    public static final String MOD_ID = "cobblepacks";

    public class DataPackFile {
        FileType type;
        String filePath;
        byte[] bytes;

        String source;
        boolean isBase;

        public DataPackFile(FileType type, String name, byte[] bytes, String source, boolean isBase) {
            this.type = type;
            this.filePath = name;
            this.bytes = bytes;
            this.source = source;
            this.isBase = isBase;
        }

        @Override
        public String toString() {
            return String.format("%s: %s - %s", type, source, filePath);
        }

        public String getPokemon() {
            List<String> dexHits = new ArrayList<>();
            for (String s : dex) {
                if (filePath.contains(s)) dexHits.add(s);
            }
            if (dexHits.size() > 1) {
                String longest = "";
                for (String dex : dexHits) {
                    if (dex.length() > longest.length()) {
                        longest = dex;
                    }
                }
                return longest;
            } else if (dexHits.size() == 1) {
                return dexHits.get(0);
            }
            return "other";
        }

        public boolean hasMultipleSources() {
            return false;
        }
    }

    public static List<String> dex = new ArrayList<>();
    public static Map<String, List<DataPackFile>> pokeData = new HashMap<>();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    @Override
    public void onInitialize() {


        try {
            // get all pokemon for the dex
            dex = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("data/cobblepacks/pokedex.txt").toURI()));
            dex = dex.stream().map(String::toLowerCase).collect(Collectors.toList());

            // read all zips
            File cobblepacks = new File(MOD_ID);
            if (!cobblepacks.exists()) cobblepacks.mkdir();
            File[] zips = cobblepacks.listFiles();
            // for each zip get all files

            List<String> data = new ArrayList<>(Arrays.asList(
                    "assets/cobblemon/bedrock/pokemon",
                    "assets/cobblemon/textures/pokemon",
                    "data/cobblemon/spawn_detail_presets",
                    "data/cobblemon/spawn_pool_world",
                    "data/cobblemon/species_features",
                    "data/cobblemon/species_feature_assignments",
                    "data/cobblemon/species"
            ));

            ZipFile cobbleZip = new ZipFile("outside/Cobblemon-fabric-1.4.1+1.20.1.jar");
            Enumeration<? extends ZipEntry> cobbleEntries = cobbleZip.entries();
            while (cobbleEntries.hasMoreElements()) {
                ZipEntry entry = cobbleEntries.nextElement();
                boolean found = false;
                for (String datum : data) {
                    if (entry.getName().startsWith(datum)) {
                        found = true;
                        break;
                    }
                }
                if (!found) continue;
                addPoke(entry, cobbleZip, true);
            }

            CONFIG_MANAGER.initPriorities(zips);


            for (File zip : zips) {


                ZipFile zipFile = new ZipFile(zip.getPath());
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    // assign file type
                    ZipEntry entry = entries.nextElement();
                    addPoke(entry, zipFile, false);
                }
            }


            pokeData.forEach((p, f) -> {
                String source = null;
                boolean multiSource = false;
                for (DataPackFile file : f) {
                    if (file.isBase) continue;
                    if (file.type == FileType.SPAWNER) continue;
                    if (source == null) source = file.source;
                    else if (!source.equals(file.source)) {
                        multiSource = true;
                    }
                }
                if (multiSource || p.equals("other")) {
                    LOGGER.info(p);
//                    for (DataPackFile file : f) {
//                        LOGGER.info(file.toString());
//                    }
                }
            });


        } catch (IOException e) {
            LOGGER.error("something went horribly wrong: " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
    }

    private void addPoke(ZipEntry entry, ZipFile zipFile, boolean isBase) throws IOException {
        if (entry.isDirectory()) return;
        FileType type = getFileType(entry);

        InputStream stream = zipFile.getInputStream(entry);
        byte[] bytes = stream.readAllBytes();

        DataPackFile file = new DataPackFile(type, entry.getName(), bytes, zipFile.getName(), isBase);

        String pokemon = file.getPokemon();
        if (!pokeData.containsKey(pokemon)) {
            pokeData.put(pokemon, new ArrayList<>());
        }
        pokeData.get(pokemon).add(file);
    }

    private FileType getFileType(ZipEntry entry) {
        FileType type = FileType.OTHER;
        if (entry.getName().contains("resolvers")) type = FileType.OVERRIDE;
        else if (entry.getName().contains("models")) type = FileType.MODEL;
        else if (entry.getName().contains("textures")) type = FileType.TEXTURE;
        else if (entry.getName().contains("animations")) type = FileType.ANIMATION;
        else if (entry.getName().contains("posers")) type = FileType.POSER;
        else if (entry.getName().contains("resolvers")) type = FileType.MODEL;
        else if (entry.getName().contains("species_additions")) type = FileType.SPECIES_ADDITION;
        else if (entry.getName().contains("species_features_assignments"))
            type = FileType.SPECIES_FEATURE_ASSIGNMENT;
        else if (entry.getName().contains("species_features")) type = FileType.SPECIES_FEATURE;
        else if (entry.getName().contains("species")) type = FileType.SPECIES;
        else if (entry.getName().contains("spawn_pool_world")) type = FileType.SPAWNER;
        else if (entry.getName().contains("spawn_detail_presets")) type = FileType.SPAWN_PRESET;
        else if (entry.getName().contains("sounds")) type = FileType.SOUND;
        else if (entry.getName().contains("lang")) type = FileType.LANG;
        return type;
    }
}