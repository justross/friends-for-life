package net.arcana.friendsforlife;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class Commands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.commands.Commands.literal("listpets")
                    // /listpets → lists your own pets
                    .executes(Commands::listPetsSelf)

                    // /listpets <player> → lists another player's pets
                    .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                            .executes(Commands::listPetsOther)));
        });
    }

    // /listpets (no argument)
    private static int listPetsSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        return listPetsInternal(context.getSource(), player);
    }

    // /listpets <player>
    private static int listPetsOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        return listPetsInternal(context.getSource(), player);
    }

    // Shared logic
    private static int listPetsInternal(CommandSourceStack source, ServerPlayer player) {
        MinecraftServer server = source.getServer();
        if (server == null)
            return 0;

        StringBuilder pets = new StringBuilder();
        List<PetData> petsList = PetTracker.get(server).getTrackedMap().values().stream()
                .filter(p -> p.ownerUuid.equals(player.getUUID()))
                .toList();

        for (int i = 0; i < petsList.size(); i++) {
            PetData pet = petsList.get(i);
            var pd = pet.getEntity(server).join();
            if (pd != null) {
                var tame = pd.entity();
                var nbt = pd.shoulderNbt();
                if (tame != null) {
                    // get custom name or default name if there is none
                    String name = tame.hasCustomName() ? tame.getCustomName().getString()
                            : tame.getType().getDescription().getString();

                    pets.append(name)
                            .append(": ");
                } else if (nbt != null) {

                    // name
                    String name = nbt.contains("CustomName")
                            ? Component.Serializer.fromJson(nbt.getString("CustomName")).getString()
                            : BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(nbt.getString("id"))).getDescription().getString();

                    pets.append(name)
                            .append(": ");
                }

                pets.append(pet.position.toShortString());

                if (i != petsList.size() - 1) {
                    pets.append("\n");
                }
            }
        }

        source.sendSuccess(() -> Component.literal(pets.toString()), false);
        return 1;
    }
}
