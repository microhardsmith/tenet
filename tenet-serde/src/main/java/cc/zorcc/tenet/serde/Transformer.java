package cc.zorcc.tenet.serde;

public interface Transformer<A, B> {

    B from(A a);

    A to(B b);

}
