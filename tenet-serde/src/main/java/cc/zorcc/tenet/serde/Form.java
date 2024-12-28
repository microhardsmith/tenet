package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public sealed interface Form permits Form.Left, Form.Right, Form.Concrete {

    /**
     *   Global L form
     */
    Form L = new Left();

    /**
     *   Global R form
     */
    Form R = new Right();

    record Left() implements Form {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Left;
        }
    }

    record Right() implements Form {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Right;
        }
    }

    record Concrete(@NonNull Class<?> cls) implements Form {
        @Override
        public boolean equals(Object o) {
            return o instanceof Concrete concrete && cls.equals(concrete.cls());
        }

        @Override
        public int hashCode() {
            return cls.hashCode();
        }
    }

    static @NonNull List<Form> of(@NonNull Object... elements) {
        return Arrays.stream(elements).map(element -> switch (element) {
            case Left _ -> L;
            case Right _ -> R;
            case Class<?> cls -> new Concrete(cls);
            default -> throw new SerdeException("Unrecognized element: %s".formatted(elements));
        }).toList();
    }

}
