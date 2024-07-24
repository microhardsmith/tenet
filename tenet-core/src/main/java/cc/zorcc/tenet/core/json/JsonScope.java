package cc.zorcc.tenet.core.json;

import java.util.List;
import java.util.Map;

/**
 *   JsonScope represents a Json parsing state-machine
 */
public sealed interface JsonScope {

    final class JsonObjScope implements JsonScope {

    }

    final class JsonArrScope implements JsonScope {

    }
}
