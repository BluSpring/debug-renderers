package dev.bithole.debugrenderers.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.bithole.debugrenderers.DebugRenderersClientMod;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class NetworkHelper {

    // For use with non-vanilla plugin channels, since there may be clients that don't support them which we want to avoid communicating with
    public static void sendToAll(MinecraftServer server, CustomPayload payload) {
        sendToAll(server.getPlayerManager().getPlayerList(), payload);
    }

    public static void sendToAll(List<ServerPlayerEntity> players, CustomPayload payload) {
        for(ServerPlayerEntity player: players) {
            if(ServerPlayNetworking.canSend(player, payload.id())) {
                var buf = PacketByteBufs.create();
                payload.write(buf);
                ServerPlayNetworking.send(player, payload.id(), buf);
            }
        }
    }

    public static class DebugRenderersCommand {

        private static final RendererSuggestionProvider rendererSuggestionProvider = new RendererSuggestionProvider();

        public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
            dispatcher.register(literal("dr")
                    .then(argument("renderer", string())
                            .suggests(rendererSuggestionProvider)
                            .executes(ctx -> toggleRenderer(ctx.getSource(), getString(ctx, "renderer"))))
                    .executes(ctx -> listRenderers(ctx.getSource())));
        }

        private static int listRenderers(FabricClientCommandSource source) {

            List<Text> enabled = new ArrayList<>(), disabled = new ArrayList<>();
            var renderers = DebugRenderersClientMod.getInstance().getRenderers();
            for (DebugRenderer.Renderer renderer : renderers.getRenderers()) {
                if (renderers.getRendererStatus(renderer))
                    enabled.add(renderers.getName(renderer));
                else
                    disabled.add(renderers.getName(renderer));
            }

            source.sendFeedback(Text.translatable("commands.dr.enabledHeader")
                    .styled(style -> style.withBold(true).withColor(Formatting.GREEN))
                    .append((enabled.size() > 0 ? Texts.join(enabled, Text.literal(" ")).copy() : Text.translatable("commands.dr.none")).styled(style -> style.withBold(false))));

            source.sendFeedback(Text.translatable("commands.dr.disabledHeader")
                    .styled(style -> style.withBold(true).withColor(Formatting.GRAY))
                    .append((disabled.size() > 0 ? Texts.join(disabled, Text.literal(" ")).copy() : Text.translatable("commands.dr.none")).styled(style->style.withBold(false))));

            return 0;
        }

        private static int toggleRenderer(FabricClientCommandSource source, String rendererName) {
            var renderers = DebugRenderersClientMod.getInstance().getRenderers();
            DebugRenderer.Renderer debugRenderer = renderers.getByName(rendererName);
            if(debugRenderer != null) {
                var text = renderers.getName(debugRenderer);
                Boolean status = renderers.getRendererStatus(debugRenderer);
                if(renderers.toggleRenderer(debugRenderer)) {
                    source.sendFeedback(Text.translatable("commands.dr.onDisabled", text));
                } else {
                    source.sendFeedback(Text.translatable("commands.dr.onEnabled", text));
                }
            } else {
                source.sendError(Text.translatable("commands.dr.noSuchRenderer", rendererName));
            }

            return 0;
        }

        private static class RendererSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {

            @Override
            public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
                var renderers = DebugRenderersClientMod.getInstance().getRenderers();
                for (DebugRenderer.Renderer renderer : renderers.getRenderers()) {
                    var name = renderers.getName(renderer);

                    builder.suggest("\"" + name.getString() + "\"");
                }

                return builder.buildFuture();
            }

        }

    }
}
