package com.shanebeestudios.skbee.elements.other.type;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.JavaFunction;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Timespan;
import com.shanebeestudios.skbee.api.particle.ParticleUtil;
import com.shanebeestudios.skbee.api.particle.VibrationBee;
import com.shanebeestudios.skbee.api.util.EnumParser;
import com.shanebeestudios.skbee.api.util.EnumUtils;
import com.shanebeestudios.skbee.api.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Vibration;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.jetbrains.annotations.Nullable;

public class Types {

    static {
        // == TYPES ==

        // Only register if no other addons have registered this class
        if (Classes.getExactClassInfo(State.class) == null) {
            EnumUtils<State> FISH_STATE_ENUM = new EnumUtils<>(State.class);
            Classes.registerClass(new ClassInfo<>(State.class, "fishstate")
                    .user("fish ?states?")
                    .name("Fish Event State")
                    .usage(FISH_STATE_ENUM.getAllNames())
                    .since("1.15.2")
                    .parser(new EnumParser<>(FISH_STATE_ENUM)));
        }

        // Only register if no other addons have registered this class
        if (Classes.getExactClassInfo(Particle.class) == null) {
            Classes.registerClass(new ClassInfo<>(Particle.class, "particle")
                    .user("particles?")
                    .name("Particle")
                    .description("Represents a particle which can be used in the 'Particle Spawn' effect.",
                            "Some particles require extra data, these are distinguished by their data type within the square brackets.",
                            "DustOption, DustTransition and Vibration each have their own functions to build the appropriate data for these particles.")
                    .usage(ParticleUtil.getNamesAsString())
                    .examples("play 1 of soul at location of player",
                            "play 10 of dust using dustOption(green, 10) at location of player",
                            "play 3 of item using player's tool at location of player",
                            "play 1 of block using dirt at location of player",
                            "play 1 of dust_color_transition using dustTransition(blue, green, 3) at location of player",
                            "play 1 of vibration using vibration({loc1}, {loc2}, 1 second) at {loc1}")
                    .since("1.9.0")
                    .parser(new Parser<Particle>() {

                        @Nullable
                        @Override
                        public Particle parse(String s, ParseContext context) {
                            return ParticleUtil.parse(s.replace(" ", "_"));
                        }

                        @Override
                        public String toString(Particle particle, int flags) {
                            return ParticleUtil.getName(particle);
                        }

                        @Override
                        public String toVariableNameString(Particle particle) {
                            return "particle:" + toString(particle, 0);
                        }

                        public String getVariableNamePattern() {
                            return "particle://s";
                        }
                    }));
        } else {
            Util.log("It looks like another addon registered 'particle' already. ");
            Util.log("You may have to use their particles in SkBee's 'particle spawn' effect.");
        }

        Classes.registerClass(new ClassInfo<>(DustOptions.class, "dustoption")
                .name(ClassInfo.NO_DOC).user("dust ?options?"));

        if (Skript.isRunningMinecraft(1, 17)) {
            Classes.registerClass(new ClassInfo<>(DustTransition.class, "dusttransition")
                    .name(ClassInfo.NO_DOC).user("dust ?transitions?"));
            Classes.registerClass(new ClassInfo<>(Vibration.class, "vibration")
                    .name(ClassInfo.NO_DOC).user("vibrations?"));
        }

        // == FUNCTIONS ==

        // Function to create DustOptions
        Functions.registerFunction(new JavaFunction<DustOptions>("dustOption", new Parameter[]{
                new Parameter<>("color", Classes.getExactClassInfo(Color.class), true, null),
                new Parameter<>("size", Classes.getExactClassInfo(Number.class), true, null)
        }, Classes.getExactClassInfo(DustOptions.class), true) {
            @Nullable
            @Override
            public DustOptions[] execute(FunctionEvent e, Object[][] params) {
                org.bukkit.Color color = ((Color) params[0][0]).asBukkitColor();
                float size = ((Number) params[1][0]).floatValue();
                return new DustOptions[]{new DustOptions(color, size)};
            }
        }.description("Creates a new dust option to be used with 'dust' particle. Color can either be a regular color or an RGB color using",
                        "Skript's rgb() function. Size is the size the particle will be.")
                .examples("set {_c} to dustOption(red, 1.5)", "set {_c} to dustOption(rgb(1, 255, 1), 3)")
                .since("1.9.0"));

        if (Skript.isRunningMinecraft(1, 17)) {
            // Function to create DustTransition
            Functions.registerFunction(new JavaFunction<DustTransition>("dustTransition", new Parameter[]{
                    new Parameter<>("fromColor", Classes.getExactClassInfo(Color.class), true, null),
                    new Parameter<>("toColor", Classes.getExactClassInfo(Color.class), true, null),
                    new Parameter<>("size", Classes.getExactClassInfo(Number.class), true, null)
            }, Classes.getExactClassInfo(DustTransition.class), true) {
                @Nullable
                @Override
                public DustTransition[] execute(FunctionEvent e, Object[][] params) {
                    org.bukkit.Color fromColor = ((Color) params[0][0]).asBukkitColor();
                    org.bukkit.Color toColor = ((Color) params[1][0]).asBukkitColor();
                    float size = ((Number) params[2][0]).floatValue();
                    return new DustTransition[]{
                            new DustTransition(fromColor, toColor, size)
                    };
                }
            }.description("Creates a new dust transition to be used with 'dust_color_transition' particle.",
                            "Color can either be a regular color or an RGB color using Skript's rgb() function.",
                            "Size is the size the particle will be. Requires MC 1.17+")
                    .examples("set {_d} to dustTransition(red, green, 10)", "set {_d} to dustTransition(blue, rgb(1,1,1), 5)")
                    .since("1.11.1"));

            // Function to create vibration
            Functions.registerFunction(new JavaFunction<Vibration>("vibration", new Parameter[]{
                    new Parameter<>("from", Classes.getExactClassInfo(Location.class), true, null),
                    new Parameter<>("to", Classes.getExactClassInfo(Location.class), true, null),
                    new Parameter<>("arrivalTime", Classes.getExactClassInfo(Timespan.class), true, null)
            }, Classes.getExactClassInfo(Vibration.class), true) {
                @Nullable
                @Override
                public Vibration[] execute(FunctionEvent e, Object[][] params) {
                    if (params[0].length == 0 || params[1].length == 0) {
                        return null;
                    }
                    Location origin = (Location) params[0][0];
                    Location destination = (Location) params[1][0];
                    int arrivalTime = (int) ((Timespan) params[2][0]).getTicks_i();
                    VibrationBee vibration = new VibrationBee(origin, destination, arrivalTime);
                    return new Vibration[]{vibration.get()};
                }
            }.description("Creates a new vibration to be used with 'vibration' particle.",
                            "FROM = the origin location the particle will start at.",
                            "TO = the destination location the particle will travel to.",
                            "ARRIVAL TIME = the time it will take to arrive at the destination location. Requires MC 1.17+")
                    .examples("set {_v} to vibration({loc1}, {loc2}, 10 seconds)")
                    .since("1.11.1"));
        }
    }

}
