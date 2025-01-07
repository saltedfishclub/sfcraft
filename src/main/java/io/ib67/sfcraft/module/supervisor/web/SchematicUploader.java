package io.ib67.sfcraft.module.supervisor.web;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.module.supervisor.WebHandler;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.Permission;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.util.litematic.LitematicConverterV3;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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

    @Override
    @SneakyThrows
    public void onInitialize() {
        Files.createDirectories(SCHEMATIC_DIR);
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal("upload").then(
                CommandManager.literal("schematic")
                        .requires(it -> !it.isExecutedByPlayer() || SFConsts.COMMAND_UPLOAD_SCHEMATIC.hasPermission(it.getPlayer()))
                        .executes(this::onRequestSchematic)
        ));
    }

    private int onRequestSchematic(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var url = generateSchematicUrl(source.isExecutedByPlayer() ? source.getPlayer().getName().getLiteralString() : "CONSOLE");
        source.sendMessage(Text.literal("Click this URL to upload schematic files.").withColor(Color.GREEN.getRGB()));
        source.sendMessage(Text.literal(url).styled(it -> it.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public void register(Javalin javalin) {
        javalin.options("/api/schematics/{name}", this::onUploadOptions).put("/api/schematics/{name}", this::uploadSchematic);
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
        var verifiedSign = signatureService.readSignature(Unpooled.wrappedBuffer(signRaw));
        if ((verifiedSign.permission() & PERMISSION_UPLOAD_SCHEMATICS) == 0) {
            context.result("Permission denied!");
            return;
        }
        context.attribute("sign", verifiedSign);
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
        //var player = serverSupplier.get().getPlayerManager().getPlayer(sign.issuer());
        ServerPlayerEntity player = null;
        if (fileName.endsWith(".schematic") || fileName.endsWith(".schem")) {
            Files.write(SCHEMATIC_DIR.resolve(baseFileName + ".schematic"), file.content().readAllBytes());
            log.info("Saved " + fileName + " as a schematic.");
            if (player != null) {
                player.sendMessage(Text.literal("Schematic " + baseFileName + " has been saved!").withColor(Color.GREEN.getRGB()));
            }
        } else if (fileName.endsWith(".litematic")) {
            log.info("Handling new litematic: {}", fileName);
            try (var converter = new LitematicConverterV3(file.content(), new NbtSizeTracker(config.maxSchematicSize, 16))) {
                converter.read((name, nbt) -> {
                    name = baseFileName + "-" + name + ".schematic";
                    try {
                        NbtIo.writeCompressed(nbt, SCHEMATIC_DIR.resolve(name));
                        log.info("Schematic " + name + " has been saved!");
                        if (player != null) {
                            player.sendMessage(Text.literal("Schematic " + name + " has been saved!").withColor(Color.GREEN.getRGB()));
                        }
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
}
