package cc.zorcc.tenet.serde;

import java.util.List;

public record TransformData(
        List<Form> f,
        List<Form> t,
        Transformer<?, ?> transformer
) {

}
