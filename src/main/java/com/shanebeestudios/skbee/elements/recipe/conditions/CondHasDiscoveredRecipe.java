package com.shanebeestudios.skbee.elements.recipe.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import com.shanebeestudios.skbee.elements.recipe.util.RecipeUtil;

import javax.annotation.Nullable;

@Name("Recipe - Has Discovered")
@Description("Check if a player has discovered a recipe. Can check recipes you created, another plugin has created, or vanilla Minecraft recipes." +
        "When checking recipes that are not your own, make sure to include the namespace, ex \"minecraft:diamond_sword\", \"someplugin:some_recipe\". " +
        "This condition is only available on 1.16+")
@Examples({"player has discovered recipe \"minecraft:furnace\"",
        "if player has discovered recipe \"my_custom_sword\":",
        "if player has discovered recipe \"someplugin:fancy_shovel\":",
        "if all players have not discovered recipe \"minecraft:golden_shovel\":",
        "if player has not discovered recipe \"my_fancy_hoe\":"})
@Since("1.4.9")
public class CondHasDiscoveredRecipe extends Condition {

    static {
        if (Skript.methodExists(HumanEntity.class, "hasDiscoveredRecipe", NamespacedKey.class)) {
            Skript.registerCondition(CondHasDiscoveredRecipe.class,
                    "%players% (has|have) discovered recipe[s] %strings%",
                    "%players% (has|have) not discovered recipe[s] %strings%");
        }
    }

    @SuppressWarnings("null")
    private Expression<Player> players;
    @SuppressWarnings("null")
    private Expression<String> recipes;

    @SuppressWarnings({"unchecked", "null"})
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        players = (Expression<Player>) exprs[0];
        recipes = (Expression<String>) exprs[1];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return players.check(e, player -> recipes.check(e,
                recipe -> player.hasDiscoveredRecipe(RecipeUtil.getKeyFromString(recipe))), isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return players.toString(e, d) + (players.isSingle() ? " has" : " have") + (isNegated() ? " not" : "") +
                " discovered recipe(s) " + recipes.toString(e, d);
    }

}
