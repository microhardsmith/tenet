package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.*;

public final class CompressUtil {
    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    public static byte[] compressUsingGzip(final byte[] rawData) {
        return compressUsingGzip(rawData, Deflater.DEFAULT_COMPRESSION);
    }
    public static byte[] compressUsingGzip(final byte[] rawData, final int level) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream){{
            if(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION) {
                def.setLevel(level);
            }
        }}) {
            gzipOutputStream.write(rawData);
            gzipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to perform gzip compression", e);
        }
    }

    public static byte[] decompressUsingGzip(final byte[] compressedData) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[4 * Constants.KB];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to perform gzip decompression", e);
        }
    }

    public static byte[] compressUsingDeflate(final byte[] rawData, final int level) {
        Deflater deflater = new Deflater(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        byte[] buffer = new byte[4 * Constants.KB];
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            while (!deflater.finished()) {
                int len = deflater.deflate(buffer);
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to perform deflate compression", e);
        }finally {
            deflater.end();
        }
    }

    public static byte[] decompressUsingDeflate(final byte[] compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4 * Constants.KB];
            while (!inflater.finished()) {
                int decompressLen = inflater.inflate(buffer);
                byteArrayOutputStream.write(buffer, 0, decompressLen);
            }
            return byteArrayOutputStream.toByteArray();
        }catch (IOException | DataFormatException e) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
