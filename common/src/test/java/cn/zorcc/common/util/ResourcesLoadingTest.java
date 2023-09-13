package cn.zorcc.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class ResourcesLoadingTest {
    @Test
    public void testSelfClass() {
        try(InputStream inputStream = ResourcesLoadingTest.class.getResourceAsStream("/large.json")) {
            Assertions.assertNotNull(inputStream);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testProjectClass() {
        try(InputStream inputStream = ConfigUtil.class.getResourceAsStream("/large.json")) {
            Assertions.assertNotNull(inputStream);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testThirdPartyClass() {
        try(InputStream inputStream = Assertions.class.getResourceAsStream("/large.json")) {
            Assertions.assertNotNull(inputStream);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testJDKClass() {
        try(InputStream inputStream = Collection.class.getResourceAsStream("/large.json")) {
            Assertions.assertNull(inputStream);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
