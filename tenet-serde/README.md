# Tenet Serde module

Tenet serde module provides a different way of serialization and deserialization.

## Features

1. No reflection needed, tenet serialization is based on compile-time source code generation
2. The generated source code are also readable and debuggable, which makes development much easier
3. The serialization performance should be much better than reflection-based mechanisms.

## How to use

Class, Record, Enum annotated with @Serde, will be processed by serdeproc module when compiling.

There are several requirements when using @Serde with your classes:

1. If annotated on class, the target class must be public final, top-level, and never extend any classes.
2. If annotated on class, the target class must have a public no-arg constructor.
3. If annotated on record, the target record must be public, top-level.
4. If annotated on enum, the target enum must be public, top-level, if the enum has fields, all the fields should be final.
5. If annotated on enum, the target enum must have at least one item.
6. If annotated on enum, fields are optional for serialization, if no fields are present, serialization will be based on enum's item.

Generics are supported for @Serde when using class or records. However, it's not so convenient to use Generics during serialization and deserialization.

Because java choose to implement generics using erasing, which results in `Refer<GBean> refer = SerdeContext.refer(GBean.class)` cannot be completely safely cast to `Refer<GBean<T>>`.

So, if you want to use @Serde on generic types during serialization and deserialization, you could choose to manually annotate them with `@SuppressWarning({"unchecked", "rawtypes"})` to ignore the warnings.

Tenet @Serde doesn't guarantee the safety of type casting, for example, annotating `GBean<A extends Number & List<A>>` won't provide any forced type checking at runtime.
Tenet serialization and deserialization will always treat `A` as `Object`, developers are the ones who should make sure `ClassCastException` are well handled.





