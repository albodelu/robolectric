package org.robolectric.shadows;

import android.os.StatFs;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

@Implements(StatFs.class)
public class ShadowStatFs {
  public static final int BLOCK_SIZE = 4096;
  private static final Stats DEFAULT_STATS = new Stats(0, 0, 0);
  private static Map<String, Stats> stats = new HashMap<String, Stats>();
  private Stats stat;

  public void __constructor__(String path) {
    restat(path);
  }

  @Implementation
  public int getBlockSize() {
    return BLOCK_SIZE;
  }

  @Implementation
  public int getBlockCount() {
    return stat.blockCount;
  }

  @Implementation
  public int getFreeBlocks() {
    return stat.freeBlocks;
  }

  @Implementation
  public int getAvailableBlocks() {
    return stat.availableBlocks;
  }

  @Implementation
  public void restat(String path) {
    stat = stats.get(path);
    if (stat == null) {
      stat = DEFAULT_STATS;
    }
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  public long getBlockSizeLong() {
    return BLOCK_SIZE;
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  public long getBlockCountLong() {
    return stat.blockCount;
  }

  @Implementation(minSdk = JELLY_BEAN_MR2)
  public long getAvailableBlocksLong() {
    return stat.availableBlocks;
  }

  public static void registerStats(File path, int blockCount, int freeBlocks, int availableBlocks) {
    registerStats(path.getAbsolutePath(), blockCount, freeBlocks, availableBlocks);
  }

  public static void registerStats(String path, int blockCount, int freeBlocks, int availableBlocks) {
    stats.put(path, new Stats(blockCount, freeBlocks, availableBlocks));
  }

  @Resetter
  public static void reset() {
    stats.clear();
  }

  private static class Stats {
    Stats(int blockCount, int freeBlocks, int availableBlocks) {
      this.blockCount = blockCount;
      this.freeBlocks = freeBlocks;
      this.availableBlocks = availableBlocks;
    }
    int blockCount, freeBlocks, availableBlocks;
  }
}