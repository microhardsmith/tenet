package cn.zorcc.app;

import cn.zorcc.app.config.AppConfig;
import cn.zorcc.common.Context;
import cn.zorcc.common.util.ConfigUtil;

public class App {
    private static final String CONFIG_FILE_NAME = "app.json";

    public static void start() {
        start(CONFIG_FILE_NAME);
    }

    public static void start(String configFileName) {
        AppConfig appConfig = ConfigUtil.loadJsonConfig(configFileName, AppConfig.class);
        Context.load(appConfig, AppConfig.class);
        // Context.init();
    }
}
