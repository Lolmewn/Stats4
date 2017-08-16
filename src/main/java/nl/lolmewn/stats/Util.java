package nl.lolmewn.stats;

import org.bukkit.inventory.ItemStack;

public class Util {

    public static String serialiseItemStack(ItemStack stack) {
        return stack == null ? "None" : stack.serialize().toString();
    }
}
