package com.shanebeestudios.skbee.elements.other.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import com.shanebeestudios.skbee.api.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Name("Block Data - Block")
@Description({"Get block data from a block. You can get a string of block data, all the tags in a block data or a specific tag. ",
        "You can also set a block data for a block or set a specific tag for block data. This syntax is only available for MC 1.13+ ",
        "Skript 2.5 added a new block data system that differs slightly from SkBee. That said, when using that version of Skript ",
        "the only available syntax here will be 'block data tags', 'block data tag \"tag here\"' and 'block data without updates', ",
        "these will not be optional."})
@Examples({"set {_data} to block data of target block of player", "set {_data::*} to block data tags of target block of player",
        "set {_water} to block data tag \"waterlogged\" of event-block", "set block data of target block to \"minecraft:carrots[age=7]\"",
        "set block data tag \"waterlogged\" of event-block to true"})
@Since("1.0.0")
public class ExprBlockDataBlock extends SimpleExpression<Object> {

    static {
        if (Skript.isRunningMinecraft(1, 13)) {
            if (Skript.classExists("ch.njol.skript.expressions.ExprBlockData")) {
                PropertyExpression.register(ExprBlockDataBlock.class, Object.class,
                        "block[ ](data|state) (1¦tags|2¦tag %-string%|3¦without update[s])", "blocks");
            } else {
                PropertyExpression.register(ExprBlockDataBlock.class, Object.class,
                        "block[ ](data|state) [(1¦tags|2¦tag %-string%|3¦without update[s])]", "blocks");
            }
        }
    }

    private Expression<String> tag;
    private Expression<Block> blocks;
    private int parse;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, ParseResult parseResult) {
        this.tag = (Expression<String>) exprs[0];
        this.blocks = (Expression<Block>) exprs[1];
        this.parse = parseResult.mark;
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        List<Object> list = new ArrayList<>();
        for (Block block : blocks.getAll(event)) {
            if (parse == 2) {
                String tag = getTag(block.getBlockData().getAsString(), this.tag.getSingle(event));
                if (tag == null) return null;

                if (isBoolean(tag)) {
                    list.add(Boolean.valueOf(tag));
                } else if (isNumber(tag)) {
                    list.add(Integer.parseInt(tag));
                } else {
                    list.add(tag);
                }
            } else if (parse == 1) {
                String[] data = getData(block.getBlockData().getAsString());
                if (data != null) {
                    list.addAll(Arrays.asList(data));
                }
            } else {
                list.add(block.getBlockData().getAsString());
            }
        }
        return list.toArray();
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Object.class);
        }
        return null;
    }

    @Override
    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        String obj = delta == null ? "" : delta[0].toString();
        for (Block block : blocks.getAll(e)) {
            BlockData blockData;
            switch (parse) {
                // Tag "string"
                case 2:
                    BlockData oldData = block.getBlockData();
                    if (oldData.getAsString().contains("[")) { // only attempt to change data for a block that has possible data
                        String tag = this.tag != null ? this.tag.getSingle(e) : null;
                        if (tag == null) continue;

                        String newData = block.getType().getKey() + "[" + tag.toLowerCase(Locale.ROOT) + "=" + obj + "]";
                        try {
                            blockData = Bukkit.createBlockData(newData);
                            blockData = oldData.merge(blockData);
                            block.setBlockData(blockData);
                        } catch (IllegalArgumentException ex) {
                            Util.debug("Could not parse block data: %s", newData);
                        }
                    }
                    break;
                // Tags
                case 1:
                    // Dont think this will work, so we shall ignore it
                    return;
                // Block Data
                default:
                    try {
                        blockData = Bukkit.createBlockData(obj);
                        block.setBlockData(blockData, parse != 3);
                    } catch (IllegalArgumentException ex) {
                        Util.debug("Could not parse block data: %s", obj);
                    }
            }
        }

    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return parse != 1;
    }

    @Override
    public String toString(Event e, boolean d) {
        return "block data" + (parse == 1 ? " tags" : parse == 2 ? " tag " +
                tag.toString(e, d) : "") + " of block " + blocks.toString(e, d);
    }

    // Utils
    private String getTag(String data, String tag) {
        String[] sp = getData(data);
        if (sp != null) {
            for (String string : sp) {
                String[] s = string.split("=");
                if (s[0].equals(tag)) {
                    return s[1];
                }
            }
        }
        return null;
    }

    private String[] getData(String data) {
        String[] splits1 = data.split("\\[");
        if (splits1.length >= 2) {
            String[] splits2 = splits1[1].split("]");

            return splits2[0].split(",");
        }
        return null;
    }

    private boolean isNumber(String string) {
        return string.matches("\\d+");
    }

    private boolean isBoolean(String string) {
        return string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false");
    }

}