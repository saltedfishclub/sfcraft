package io.ib67.sfcraft.module.supervisor.web;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.module.supervisor.WebHandler;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.util.litematic.LitematicConverterV3;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

@Log4j2
public class SchematicUploader extends WebHandler {
    private static final String TOPIC_SCHEMATIC_UPLOADER = "upload_schematic";
    private static final int PERMISSION_UPLOAD_SCHEMATICS = 2;
    private static final Path SCHEMATIC_DIR = Path.of("config/worldedit/schematics/");
    @Inject
    SFConfig config;
    @Inject
    SignatureService signatureService;
    @Inject
    MinecraftServerSupplier serverSupplier;

    @Override
    @SneakyThrows
    public void onInitialize() {
        Files.createDirectories(SCHEMATIC_DIR);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry, Commands.CommandSelection env) {
        dispatcher.register(Commands.literal("upload").then(
                Commands.literal("schematic")
                        .executes(this::onRequestSchematic)
        ));
    }

    private int onRequestSchematic(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        if (source.isPlayer() && !SFConsts.COMMAND_UPLOAD_SCHEMATIC.hasPermission(source.getPlayer())) {
            source.sendSystemMessage(Component.translatable("message.sfcraft.schem.no_permission"));
            return Command.SINGLE_SUCCESS;
        }
        var url = generateSchematicUrl(source.isPlayer() ? source.getPlayer().getName().tryCollapseToString() : "CONSOLE");
        source.sendSystemMessage(Component.translatable("message.sfcraft.schem.click_to_upload").withColor(Color.GREEN.getRGB()));
        source.sendSystemMessage(Component.literal(url).withStyle(it -> it.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))));
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public void register(JavalinConfig javalin) {
        javalin.routes.options("/api/schematics/{name}", this::onUploadOptions).put("/api/schematics/{name}", this::uploadSchematic);
    }

    public String generateSchematicUrl(String issuer) {
        var name = issuer;
        var expireAt = System.currentTimeMillis() + 1000 * 300;
        var sign = signatureService.createSignature(new SignatureService.Signature(
                TOPIC_SCHEMATIC_UPLOADER,
                name,
                PERMISSION_UPLOAD_SCHEMATICS,
                expireAt,
                new byte[0]
        ));
        return config.webApiBase + "/schematics/upload?token=" + Base64.getUrlEncoder().encodeToString(sign) + "&expireAt=" + expireAt;
    }

    private void onUploadOptions(@NotNull Context context) {
        context.res().addHeader("Access-Control-Allow-Origin", "*");
        context.res().addHeader("Access-Control-Allow-Methods", "OPTIONS, PUT");
        context.res().addHeader("Access-Control-Allow-Headers", context.header("Access-Control-Request-Headers"));
        context.res().addHeader("Access-Control-Max-Age", "86400");
    }

    private void uploadSchematic(@NotNull Context context) {
        onUploadOptions(context);
        if (context.contentLength() > config.maxSchematicSize) {
            context.result("Content is too large!");
            context.res().setStatus(400);
            return;
        }
        var signRaw = Base64.getUrlDecoder().decode(context.queryParam("sign"));
        try {
            var verifiedSign = signatureService.readSignature(Unpooled.wrappedBuffer(signRaw));
            if ((verifiedSign.permission() & PERMISSION_UPLOAD_SCHEMATICS) == 0) {
                context.result("Permission denied!");
                context.status(403);
                return;
            }
            context.attribute("sign", verifiedSign);
        } catch (Exception e) {
            context.result("Invalid signature. Please re-run /upload schematic in game.");
            context.status(400);
            return;
        }
        var files = context.uploadedFileMap();
        var name = context.pathParam("name");
        files.forEach((k, v) -> handleUploadSchematic(context, name, v));
    }

    @SneakyThrows
    private void handleUploadSchematic(@NotNull Context context, String fileName, List<UploadedFile> v) {
        if (v.size() != 1) {
            return;
        }
        var file = v.getFirst();
        if (file.size() > config.maxSchematicSize) {
            context.result("Content is too large!");
            context.res().setStatus(400);
            return;
        }
        if (fileName.length() <= 10) return;
        var baseFileName = Helper.cleanFileName(fileName.substring(0, fileName.length() - 10));
        var sign = context.<SignatureService.Signature>attribute("sign");
        var player = serverSupplier.get().getPlayerList().getPlayerByName(sign.issuer());
        log.info("Handling file {} from player {}", fileName, sign.issuer());
        if (fileName.endsWith(".schematic") || fileName.endsWith(".schem")) {
            Files.write(SCHEMATIC_DIR.resolve(baseFileName + ".schematic"), file.content().readAllBytes());
            log.info("Saved " + fileName + " as a schematic.");
            sendSuccess(player, baseFileName);
        } else if (fileName.endsWith(".litematic")) {
            try (var converter = new LitematicConverterV3(file.content(), new NbtAccounter(config.maxSchematicSize, 64))) {
                converter.read((name, nbt) -> {
                    name = baseFileName + "-" + Helper.cleanFileName(name) + ".schematic";
                    try {
                        NbtIo.writeCompressed(nbt, SCHEMATIC_DIR.resolve(name));
                        log.info(sign.issuer() + ": Schematic " + name + " has been saved!");
                        sendSuccess(player, name);
                        context.result("Success!");
                    } catch (Exception e) {
                        log.error("Error occurred when serializing .litematic to disk.", e);
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred when converting .litematic.", e);
            }
        } else {
            context.result("Invalid format");
            context.res().setStatus(403);
            return;
        }
        context.result("Uploaded!");
    }

    private static void sendSuccess(ServerPlayer player, String fileName) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.sfcraft.schem.saved", fileName).withColor(Color.GREEN.getRGB()));
        player.sendSystemMessage(Component.translatable("message.sfcraft.schem.or_click").withStyle(it -> it.withUnderlined(true).withClickEvent(
                new ClickEvent.SuggestCommand("//schem load " + fileName)
        )));
    }
}
