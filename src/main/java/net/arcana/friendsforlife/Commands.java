package net.arcana.friendsforlife;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.util.CompatComponent;
import net.arcana.friendsforlife.util.CompatNbt;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;

public class Commands {

    public static void register() {

    }

    // /listpets (no argument)
    public static int listPetsSelf(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        return listPetsInternal(context.getSource(), player);
    }

    // /listpets <player>
    public static int listPetsOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        return listPetsInternal(context.getSource(), player);
    }

    // Shared logic
    private static int listPetsInternal(CommandSourceStack source, ServerPlayer player) {
        MinecraftServer server = source.getServer();

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
                    // get custom customName or default name if there is none
                    String name = tame.hasCustomName() ? Objects.requireNonNull(tame.getCustomName()).getString()
                            : tame.getType().getDescription().getString();

                    pets.append(name)
                            .append(": ");
                } else if (nbt != null) {
                    String customName = CompatNbt.getCustomName(nbt);
                    String name = customName != null
                            ? customName
                            : CompatNbt.entityType(CompatNbt.getString(nbt, "id")).getDescription().getString();

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
