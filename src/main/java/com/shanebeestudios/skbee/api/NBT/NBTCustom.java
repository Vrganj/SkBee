package com.shanebeestudios.skbee.api.NBT;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.NamespacedKey;
import com.shanebeestudios.skbee.SkBee;

public interface NBTCustom {

    NamespacedKey OLD_KEY = new NamespacedKey(SkBee.getPlugin(), "custom-nbt");

    NBTCompound getCustomNBT();

    void deleteCustomNBT();

}
