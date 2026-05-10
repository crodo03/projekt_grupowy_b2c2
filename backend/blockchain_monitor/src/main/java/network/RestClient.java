package network;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import service.BlockAnalyzer;

@Slf4j
public class RestClient {
    private final RestController restController;

    public RestClient(BlockAnalyzer blockAnalyzer) {
        this.restController = new RestController(blockAnalyzer);
    }

    public void startServer() {
        Javalin.create(config -> {
            // cors
            config.bundledPlugins.enableCors(
                    cors -> cors.addRule(
                            rule -> rule.reflectClientOrigin = true
                    )
            );

            // routes
            config.routes.sse("/sse", restController::sendLatestBlocks);
            config.routes.get("/block/{block-number}", restController::sendBlockInfo);

        }).start(8080);
    }
}
