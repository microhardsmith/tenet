package cc.zorcc.tenet.serdeproc.example;

public final class Example {
    
    public static void main(String[] args) {
        try {
            Class.forName("cc.zorcc.tenet.serdeproc.example.BeanExample");
            Class.forName("cc.zorcc.tenet.serdeproc.example.RBeanExample");
            Class.forName("cc.zorcc.tenet.serdeproc.example.EBeanExample");
            System.out.println("Loaded successfully");
        }catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
