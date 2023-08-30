package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.*;

public final class CompressUtil {
    private CompressUtil() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public static byte[] compressUsingGzip(final byte[] rawData) {
        return compressUsingGzip(rawData, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compressUsingGzip(final byte[] rawData, final int level) {
        try(ResizableByteArray resizableByteArray = new ResizableByteArray(Math.max(rawData.length >> 4, 32)); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(resizableByteArray){{
            def.setLevel(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        }}) {
            gzipOutputStream.write(rawData);
            gzipOutputStream.finish();
            return resizableByteArray.toArray();
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip compression", e);
        }
    }

    public static byte[] decompressUsingGzip(final byte[] compressedData) {
        try (ResizableByteArray resizableByteArray = new ResizableByteArray(compressedData.length); GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[4 * Constants.KB];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                resizableByteArray.write(buffer, 0, bytesRead);
            }
            return resizableByteArray.toArray();
        }catch (IOException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform gzip decompression", e);
        }
    }

    @SuppressWarnings("unused")
    public static byte[] compressUsingDeflate(final byte[] rawData) {
        return compressUsingDeflate(rawData, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compressUsingDeflate(final byte[] rawData, final int level) {
        Deflater deflater = new Deflater(level >= Deflater.BEST_SPEED && level <= Deflater.BEST_COMPRESSION ? level : Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(rawData);
        byte[] buffer = new byte[4 * Constants.KB];
        try(ResizableByteArray resizableByteArray = new ResizableByteArray()) {
            while (!deflater.finished()) {
                int len = deflater.deflate(buffer);
                resizableByteArray.write(buffer, 0, len);
            }
            return resizableByteArray.toArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompressUsingDeflate(final byte[] compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        try (ResizableByteArray resizableByteArray = new ResizableByteArray(compressedData.length)) {
            byte[] buffer = new byte[4 * Constants.KB];
            while (!inflater.finished()) {
                int decompressLen = inflater.inflate(buffer);
                resizableByteArray.write(buffer, 0, decompressLen);
            }
            return resizableByteArray.toArray();
        }catch (DataFormatException e) {
            throw new FrameworkException(ExceptionType.COMPRESS, "Unable to perform deflate decompression", e);
        }finally {
            inflater.end();
        }
    }
}
