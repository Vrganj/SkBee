package com.shanebeestudios.skbee.elements.text.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import com.shanebeestudios.skbee.SkBee;
import com.shanebeestudios.skbee.api.NBT.NBTApi;
import com.shanebeestudios.skbee.api.reflection.McReflection;
import com.shanebeestudios.skbee.api.util.TextUtils;
import com.shanebeestudios.skbee.api.util.Util;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.KeybindComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Name("Text Component - New Text Component")
@Description({"Create a new text component. Can have hover and click events added to it. You can also create a translate component, ",
        "this will send to the client, and the client will translate based on their language. You can use either an item type or a ",
        "translate string, you can find these in your Minecraft jar 'assets/minecraft/lang/<lang file>.json'.",
        "As of Paper 1.17.1, several more objects can translate including GameRules, PotionEffectTypes, Attributes, Difficulty, Enchantments, ",
        "FireworkEffectTypes, Entities and Blocks. KeyBind components will be replaced with the actual key the client is using.",
        "Some components have extra objects, you can use strings or other text components here."})
@Examples({"set {_comp::1} to text component from \"hi player \"",
        "set {_comp::2} to text component of \"hover over me for a special message!\"",
        "set hover event of {_comp::2} to hover event to show \"OoO look ma I'm hovering!\"",
        "send component {_comp::*} to player", "",
        "set {_t} to translate component from player's tool",
        "set {_t} to translate component from \"item.minecraft.milk_bucket\"",
        "set {_death} to translate component from \"death.fell.accident.ladder\" using player's name",
        "set {_assist} to translate component \"death.fell.assist\" using victim's name and attacker's name",
        "set {_key} to keybind component \"key.jump\""})
@Since("1.5.0")
public class ExprTextComponent extends SimpleExpression<BaseComponent> {

    private static final NBTApi api = SkBee.getPlugin().getNbtApi();
    private static final boolean HAS_TRANSLATION = Skript.classExists("net.kyori.adventure.translation.Translatable");

    static {
        Skript.registerExpression(ExprTextComponent.class, BaseComponent.class, ExpressionType.COMBINED,
                "[a] [new] text component[s] (from|of) %strings%",
                "[a] [new] key[ ]bind component[s] (from|of) %strings%",
                "[a] [new] translate component[s] (from|of) %objects%",
                "[a] [new] translate component[s] (from|of) %string% (with|using) %objects%");
    }

    private int pattern;
    private Expression<Object> translation;
    private Expression<Object> objects;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?> @NotNull [] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull ParseResult parseResult) {
        pattern = matchedPattern;
        translation = (Expression<Object>) exprs[0];
        objects = pattern == 3 ? (Expression<Object>) exprs[1] : null;
        return true;
    }

    @Nullable
    @Override
    protected BaseComponent[] get(@NotNull Event e) {
        List<BaseComponent> components = new ArrayList<>();

        for (Object object : this.translation.getArray(e)) {
            if (pattern == 0) {
                BaseComponent[] baseComponents = TextComponent.fromLegacyText(Util.getColString((String) object));
                components.add(new TextComponent(baseComponents));
            } else if (pattern == 1) {
                components.add(new KeybindComponent((String) object));
            } else if (pattern == 2) {
                String translate = getTranslation(object);
                if (translate != null) {
                    components.add(new TranslatableComponent(translate));
                }
            } else {
                String string = ((String) translation.getSingle(e));
                Object[] objects = this.objects.getAll(e);
                components.add(new TranslatableComponent(string, objects));
            }
        }
        return components.toArray(new BaseComponent[0]);
    }

    public static String getTranslation(Object object) {
        if (object instanceof ItemType) {
            return translateItemType(((ItemType) object));
        } else if (object instanceof Slot) {
            ItemStack item = ((Slot) object).getItem();
            if (HAS_TRANSLATION && item != null) {
                TextUtils.getTranslationKey(item);
            }
        } else if (object instanceof String) {
            return ((String) object);
        } else if (HAS_TRANSLATION) {
            return TextUtils.getTranslationKey(object);
        }
        return null;
    }

    public static String translateItemType(ItemType itemType) {
        ItemStack itemStack = itemType.getRandom();
        assert itemStack != null;
        String trans = McReflection.getTranslateKey(itemStack);
        if (trans != null) {
            return trans;
        }
        Material material = itemStack.getType();
        String type = material.isBlock() ? "block" : "item";
        String raw = itemType.getRawNames().get(0).replace("minecraft:", "");
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof PotionMeta) {
            StringBuilder builder = new StringBuilder("item.minecraft.");
            String nbt = api.getNBT(itemType, NBTApi.ObjectType.ITEM_TYPE);
            if (nbt != null) {
                String pot = api.getTag("Potion", new NBTContainer(nbt)).toString();
                if (pot != null) {
                    if (material == Material.POTION) {
                        builder.append("potion");
                    } else if (material == Material.SPLASH_POTION) {
                        builder.append("splash_potion");
                    } else if (material == Material.LINGERING_POTION) {
                        builder.append("lingering_potion");
                    } else if (material == Material.TIPPED_ARROW) {
                        builder.append("tipped_arrow");
                    }
                    builder.append(".effect.").append(pot.replace("minecraft:", ""));
                    return builder.toString();
                }
            }
        }
        return type + ".minecraft." + raw;
    }

    @Override
    public boolean isSingle() {
        return this.translation.isSingle();
    }

    @Override
    public @NotNull Class<? extends BaseComponent> getReturnType() {
        return BaseComponent.class;
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean d) {
        String comp = pattern == 0 ? "text" : pattern == 1 ? "keybind" : "translate";
        String trans = translation.toString(e, d);
        String obj = objects != null ? "using " + objects.toString(e, d) : "";
        return String.format("a new %s component from %s %s", comp, trans, obj);
    }

}
