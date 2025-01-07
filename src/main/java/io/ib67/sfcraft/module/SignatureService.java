package io.ib67.sfcraft.module;

import com.google.inject.Inject;
import io.ib67.sfcraft.config.SFConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class SignatureService {
    private Key key;

    @SneakyThrows
    @Inject
    public SignatureService(SFConfig config) {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(config.serverSecret.toCharArray(), RandomStringUtils.random(16).getBytes(), 2048, 256);
        key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public Signature readSignature(ByteBuf buf) {
        var hash = new byte[28];
        buf.readBytes(hash);
        var sign = Signature.PACKET_CODEC.decode(buf);
        if (!Arrays.equals(sign.calcHash(key), hash)) {
            throw new IllegalArgumentException("Invalid signature.");
        }
        if (sign.validUntil < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Expired signature.");
        }
        return sign;
    }

    public byte[] createSignature(Signature signature) {
        var buf = Unpooled.buffer();
        writeSignature(buf, signature);
        var arr = new byte[buf.readableBytes()];
        buf.readBytes(arr);
        return arr;
    }

    @SneakyThrows
    public void writeSignature(ByteBuf buf, Signature signature) {
        buf.writeBytes(signature.calcHash(key));
        Signature.PACKET_CODEC.encode(buf, signature);
    }

    public record Signature(
            String topic,
            String issuer,
            int permission,
            long validUntil,
            byte[] data
    ) {
        public static final PacketCodec<ByteBuf, Signature> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, Signature::topic,
                PacketCodecs.STRING, Signature::issuer,
                PacketCodecs.VAR_INT, Signature::permission,
                PacketCodecs.VAR_LONG, Signature::validUntil,
                PacketCodecs.BYTE_ARRAY, Signature::data,
                Signature::new
        );

        @SneakyThrows
        private byte[] calcHash(Key key) {
            var hmac = MessageDigest.getInstance("SHA3-224");
            hmac.update(topic.getBytes());
            hmac.update(issuer.getBytes());
            hmac.update(ByteBuffer.allocate(4).putInt(permission).array());
            hmac.update(ByteBuffer.allocate(8).putLong(validUntil).array());
            hmac.update(key.getEncoded());
            return hmac.digest();
        }
    }
}
