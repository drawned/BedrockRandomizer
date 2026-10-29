package anarchy.minesky.bedrock;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public final class BedrockRandomizer extends JavaPlugin implements Listener {

    private static final BlockData BEDROCK_DATA = Material.BEDROCK.createBlockData();
    private static final BlockData DEEPSLATE_DATA = Material.DEEPSLATE.createBlockData();

    /** Per-column noise chance (Y offsets +1 … +4 from world min height). */
    private static final int[] NOISE_THRESHOLD = {80, 60, 35, 15};

    /** 256 columns × 4 noise rolls, filled once per new chunk. */
    private static final ThreadLocal<byte[]> ROLL_BYTES = ThreadLocal.withInitial(() -> new byte[256 * 4]);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }

        final World world = event.getWorld();
        final String worldName = world.getName();
        if (!worldName.equals("world") && !worldName.equals("world_nether")) {
            return;
        }

        final Chunk chunk = event.getChunk();
        final int minY = world.getMinHeight();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final byte[] rolls = ROLL_BYTES.get();
        random.nextBytes(rolls);

        int rollIndex = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int layer = 0; layer <= 5; layer++) {
                    final Block block = chunk.getBlock(x, minY + layer, z);
                    final Material current = block.getType();

                    if (layer == 0) {
                        if (current != Material.BEDROCK) {
                            block.setBlockData(BEDROCK_DATA, false);
                        }
                        continue;
                    }

                    if (layer == 5) {
                        if (current == Material.BEDROCK) {
                            block.setBlockData(DEEPSLATE_DATA, false);
                        }
                        continue;
                    }

                    final boolean bedrock = chance(rolls[rollIndex++], NOISE_THRESHOLD[layer - 1]);
                    if (bedrock) {
                        if (current != Material.BEDROCK) {
                            block.setBlockData(BEDROCK_DATA, false);
                        }
                    } else if (current == Material.BEDROCK) {
                        block.setBlockData(DEEPSLATE_DATA, false);
                    }
                }
            }
        }
    }

    /** Maps a uniform byte to 0–99 and compares to a percentage threshold. */
    private static boolean chance(byte roll, int thresholdPercent) {
        return ((roll & 0xFF) * 100 >>> 8) < thresholdPercent;
    }
}
