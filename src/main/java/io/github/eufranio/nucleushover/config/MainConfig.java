package io.github.eufranio.nucleushover.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.eufranio.nucleushover.NucleusHover;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Frani on 04/11/2018.
 */
@ConfigSerializable
public class MainConfig {

    @Setting
    public Map<String, PlaceholderText> placeholders = ImmutableMap.of("default", new PlaceholderText());

    public String getKey(Object obj) {
        for (Map.Entry<String, PlaceholderText> entry : placeholders.entrySet()) {
            if (entry.getValue() == obj) return entry.getKey();
        }
        return null;
    }

    @ConfigSerializable
    public static class PlaceholderText {

        @Setting
        private String text = "&6hello";

        @Setting
        private List<String> hover = Lists.newArrayList("hi!");

        @Setting
        private String click = "/say hello";

        @Setting
        private String suggest = "hi!";

        public Text build(CommandSource source) {
            String key = "nh_"+NucleusHover.getInstance().getConfig().getKey(PlaceholderText.this);
            return NucleusHover.fillPlaceholders(source, text, key)
                    .toBuilder()
                    .onHover(TextActions.showText(
                            Text.joinWith(Text.NEW_LINE, hover.stream()
                                            .map(s -> NucleusHover.fillPlaceholders(source, s, key))
                                            .collect(Collectors.toList())
                            )
                    ))
                    .onClick(click.isEmpty() ?
                            TextActions.suggestCommand(NucleusHover.getService().replaceSourcePlaceholders(suggest, source).toPlain()) :
                            TextActions.runCommand(NucleusHover.getService().replaceSourcePlaceholders(click, source).toPlain()))
                    .build();
        }

    }

}
