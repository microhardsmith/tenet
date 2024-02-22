# Json 

In the Tenet project, there is an extremely simple JSON serialization tool designed for seamless conversion between byte stream data and Java objects. 
While specialized libraries are typically employed for such tasks, JSON parsing itself is a straightforward operation. 
We aim to keep the source code as simple as possible, aligning it closely with our framework. 
This approach also facilitates developers in customizing and enhancing its functionality through the use of annotations.

# Reflection

When it comes to serialization and deserialization, using reflection is an unavoidable approach. In Java, there are currently several reflection invocation methods:

- Invocation using Method.invoke().
- Invocation using MethodHandle.invokeExact().
- Invocation using LambdaMetaFactory.

During serialization, we primarily utilize get(), set(), and constructor methods, which inherently have low overhead.
In actual testing scenarios, the efficiency of invoking methods using Method.invoke() is highest when there is no caching involved. 
On the other hand, when caching is employed, the efficiency of using LambdaMetaFactory is optimal.
However, it's worth noting that the dynamically generated performance of LambdaMetaFactory is significantly poor. 
In comparison, MethodHandle.invokeExact() strikes a better balance between generation time and runtime performance. 
Therefore, we have decided to use the Method.invoke() approach for ordinary reflection invocations and the MethodHandle.invokeExact() approach for cases where object information is pre-cached.