package lab.drop.swing;

import lab.drop.concurrent.Concurrent;
import lab.drop.functional.Reducer;
import lab.drop.runtime.JVM;

public class SwingDemoJVM {

    public static void main(String[] args) throws Exception {
        Concurrent.getAll(Reducer.last(), Concurrent.physical().run(new JVM(SwingDemo.class)),
                Concurrent.physical().run(new JVM(SwingDemo.class)));
    }
}
