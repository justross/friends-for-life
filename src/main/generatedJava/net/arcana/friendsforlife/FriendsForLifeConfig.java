package net.arcana.friendsforlife;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FriendsForLifeConfig extends ConfigWrapper<net.arcana.friendsforlife.ConfigModel> {

    public final Keys keys = new Keys();



    private FriendsForLifeConfig() {
        super(net.arcana.friendsforlife.ConfigModel.class);
    }

    private FriendsForLifeConfig(Consumer<Jankson.Builder> janksonBuilder) {
        super(net.arcana.friendsforlife.ConfigModel.class, janksonBuilder);
    }

    public static FriendsForLifeConfig createAndLoad() {
        var wrapper = new FriendsForLifeConfig();
        wrapper.load();
        return wrapper;
    }

    public static FriendsForLifeConfig createAndLoad(Consumer<Jankson.Builder> janksonBuilder) {
        var wrapper = new FriendsForLifeConfig(janksonBuilder);
        wrapper.load();
        return wrapper;
    }



    public static class Keys {

    }
}

