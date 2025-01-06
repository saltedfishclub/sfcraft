package io.ib67.sfcraft.module.supervisor;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.LitematicConverter;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

@Log4j2
public class WebModule extends ServerModule {
    private static final int PERMISSION_UPLOAD_SCHEMATICS = 2;
    private static final Path SCHEMATIC_DIR = Path.of("config/worldedit/schematics/");
    @Inject
    SFConfig config;
    @Inject
    SignatureService signatureService;
    private Javalin javalin;

    @Override
    @SneakyThrows
    public void onInitialize() {
        Files.createDirectories(SCHEMATIC_DIR);
        javalin = Javalin.create(cfg -> cfg.useVirtualThreads = false)
                .put("/api/schematics", this::uploadSchematic);
        Thread.ofVirtual().name("SFCraft API Web").start(() -> {
            javalin.start(config.httpPort);
        });
    }

    @Override
    public void onDisable() {
        javalin.stop();
    }

    private void uploadSchematic(@NotNull Context context) {
        if (context.contentLength() > config.maxSchematicSize) {
            context.result("Content is too large!");
            return;
        }

//        var signRaw = Base64.getUrlDecoder().decode(context.pathParam("sign"));
//        var verifiedSign = signatureService.readSignature(Unpooled.wrappedBuffer(signRaw));
//        if ((verifiedSign.permission() & PERMISSION_UPLOAD_SCHEMATICS) == 0) {
//            context.result("Permission denied!");
//            return;
//        }
        var files = context.uploadedFileMap();
        files.forEach((k, v) -> handleUploadSchematic(context, k, v));
    }

    @SneakyThrows
    private void handleUploadSchematic(@NotNull Context context, String fileName, List<UploadedFile> v) {
        if (v.size() != 1) {
            return;
        }
        var file = v.getFirst();
        if (file.size() > config.maxSchematicSize) {
            context.result("Content is too large!");
            return;
        }
        if (fileName.length() <= 10) return;
        var baseFileName = Helper.cleanFileName(fileName.substring(0, fileName.length() - 10));
        log.info("Handling new file: {}", fileName);
        if (fileName.endsWith(".schematic")) {
            Files.write(SCHEMATIC_DIR.resolve(baseFileName+".schematic"), file.content().readAllBytes());
            log.info("Saved " + fileName + " as a schematic.");
        } else if (fileName.endsWith(".litematic")) {
            log.error("Handling new {}", fileName);
            var converter = new LitematicConverter(
                    file.content(),
                    new NbtSizeTracker(config.maxSchematicSize, 16)
            );
            try {
                converter.read((name, nbt) -> {
                    name = baseFileName + "-" + name + ".schematic";
                    try {
                        NbtIo.writeCompressed(nbt, SCHEMATIC_DIR.resolve(name));
                        log.info("Schematic " + name + " has been saved!");
                        context.result("Success!");
                    } catch (Exception e) {
                        log.error("Error occurred when serializing .litematic to disk.", e);
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred when converting .litematic.", e);
            }
            converter.close();
        }
    }
}
