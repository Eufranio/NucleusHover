package io.github.eufranio.nucleushover;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import io.github.eufranio.nucleushover.config.MainConfig;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.exceptions.PluginAlreadyRegisteredException;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import me.rojo8399.placeholderapi.*;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Plugin(
        id = "nucleushover",
        name = "NucleusHover",
        description = "Adds hover chat extensions for Nucleus",
        authors = {
                "Eufranio"
        },
        dependencies = {
                @Dependency(id = "nucleus"),
                @Dependency(id = "placeholderapi")
        }
)
public class NucleusHover {

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    private MainConfig config;

    private static PlaceholderService service;
    private static NucleusHover instance;
    private static NucleusMessageTokenService messageService;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        instance = this;
        service = Sponge.getServiceManager().provideUnchecked(PlaceholderService.class);
        messageService = NucleusAPI.getMessageTokenService();

        this.loadConfig();
        this.registerNucleusTokens();
        this.registerPlaceholders();
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        this.loadConfig();
        this.registerNucleusTokens();
    }

    private void loadConfig() {
        try {
            ConfigurationNode node = this.loader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
            config = node.getValue(TypeToken.of(MainConfig.class), (Supplier<MainConfig>) MainConfig::new);
            this.loader.save(node);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    private void registerNucleusTokens() {
        PluginContainer container = Sponge.getPluginManager().fromInstance(this).get();
        getMessageService().unregister(container);
        try {
            getMessageService().register(container, (token, source, variables) -> {
                if (!token.startsWith("nh_")) return Optional.empty();
                String id = token.replace("nh_", "");

                MainConfig.PlaceholderText text = this.getConfig().placeholders.get(id);
                if (text == null) return Optional.empty();

                return Optional.of(text.build(source));
            });
        } catch (PluginAlreadyRegisteredException e) {
            e.printStackTrace();
        }

        this.getConfig().placeholders.keySet().forEach(s -> getMessageService()
                .registerPrimaryToken("nh_" + s, container, "nh_" + s)
        );
    }

    private void registerPlaceholders() {
        service.loadAll(this, this).stream().map(builder -> {
            switch (builder.getId()) {
                case "meta":
                    return builder.description("Returns the meta value associated with the key");
            }
            return builder;
        }).map(builder -> builder.author("Eufranio").plugin(this).version("1.0")).forEach(builder -> {
            try {
                builder.buildAndRegister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MainConfig getConfig() {
        return this.config;
    }

    public static String fromText(Text t) {
        return TextSerializers.FORMATTING_CODE.serialize(t);
    }

    public static PlaceholderService getService() {
        return service;
    }

    public static NucleusMessageTokenService getMessageService() {
        return messageService;
    }

    public static NucleusHover getInstance() {
        return instance;
    }

    public static Text fillPlaceholders(CommandSource src, String str, String excluding) {
        List<String> tokens = NucleusHover.getMessageService().getPrimaryTokens();
        for (String token : tokens) {
            if (!token.equalsIgnoreCase(excluding)) {
                if (str.contains("{{" + token + "}}")) {
                    str = str.replace("{{" + token + "}}", fromText(NucleusHover.getMessageService().applyPrimaryToken(token, src).orElse(Text.of(token))));
                }
            }
        }
        return NucleusHover.getService().replaceSourcePlaceholders(str, src);
    }

    @Placeholder(id = "meta")
    public String meta(@Source CommandSource src, @Token @Nullable String key) {
        /*return TextSerializers.FORMATTING_CODE.serialize(messageService
                .getPrimaryTokenParserAndIdentifier("displayname")
                .map(Tuple::getFirst)
                .map(p -> p.parse("displayname", src, Maps.newHashMap()).get())
                .get());*/
        String str = src.getOption(key).orElse(null);
        return str == null ? "" : str + " ";
    }

}
