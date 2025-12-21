//? if fabric {
package net.arcana.friendsforlife;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "friendsforlife-config", wrapperName = "FriendsForLifeConfig")
public class ConfigModel {
    public int petRespawnTimer = 60;
}
//?}