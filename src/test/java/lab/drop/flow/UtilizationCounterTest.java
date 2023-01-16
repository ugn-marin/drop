package lab.drop.flow;

import lab.drop.concurrent.Concurrent;
import lab.drop.function.Reducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilizationCounterTest {
    private static final double delta = 0.05;

    @Test
    void validations() throws InterruptedException {
        var counter = new UtilizationCounter(2);
        Assertions.assertEquals(Double.NaN, counter.getAverageUtilization());
        Assertions.assertEquals(0, counter.getCurrentUtilization());
        try {
            counter.stop();
            Assertions.fail();
        } catch (IllegalStateException e) {
            Assertions.assertEquals("Utilization measurement was stopped or not started.", e.getMessage());
        }
        counter.start();
        Thread.sleep(50);
        Assertions.assertEquals(Double.NaN, counter.getAverageUtilization());
        Assertions.assertEquals(0, counter.getCurrentUtilization());
        try {
            counter.idle();
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("-1 is smaller than the minimum 0.", e.getMessage());
        }
        counter.busy();
        counter.busy();
        counter.busy();
        try {
            counter.busy();
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("3 is greater than the maximum 2.", e.getMessage());
        }
        try {
            counter.start();
            Assertions.fail();
        } catch (IllegalStateException e) {
            Assertions.assertEquals("Utilization measurement was already started.", e.getMessage());
        }
    }

    @Test
    void flow1() throws Exception {
        var counter = new UtilizationCounter(3);
        counter.start();
        var f1 = Concurrent.run(() -> {
            counter.busy();
            Thread.sleep(50);
            counter.idle();
            Thread.sleep(350);
            counter.busy();
            Thread.sleep(100);
            counter.idle();
        });
        var f2 = Concurrent.run(() -> {
            counter.busy();
            Thread.sleep(200);
            counter.idle();
            Thread.sleep(100);
            counter.busy();
            Thread.sleep(200);
            counter.idle();
        });
        Concurrent.getAll(Reducer.suppressor(), f1, f2);
        double middle = counter.getAverageUtilization();
        Thread.sleep(100);
        counter.stop();
        Assertions.assertEquals(0.306, counter.getAverageUtilization(), delta);
        Assertions.assertEquals(0.366, middle, delta);
    }

    @Test
    void flow2() throws Exception {
        var counter = new UtilizationCounter(3);
        counter.start();
        Assertions.assertEquals(0, counter.getCurrentUtilization());
        Thread.sleep(100);
        var f1 = Concurrent.run(() -> {
            counter.busy();
            Thread.sleep(500);
            counter.idle();
        });
        var f2 = Concurrent.run(() -> {
            Thread.sleep(100);
            counter.busy();
            Assertions.assertEquals(0.66, counter.getCurrentUtilization(), 0.01);
            Thread.sleep(400);
            counter.idle();
        });
        var f3 = Concurrent.run(() -> {
            Thread.sleep(150);
            counter.busy();
            Assertions.assertEquals(1, counter.getCurrentUtilization());
            Thread.sleep(300);
            counter.idle();
        });
        Concurrent.getAll(Reducer.suppressor(), f1, f2, f3);
        counter.stop();
        Assertions.assertEquals(0.667, counter.getAverageUtilization(), delta);
    }

    @Test
    void flow2_plus1() throws Exception {
        var counter = new UtilizationCounter(4);
        counter.start();
        Assertions.assertEquals(0, counter.getCurrentUtilization());
        Thread.sleep(100);
        var f1 = Concurrent.run(() -> {
            counter.busy();
            Thread.sleep(500);
            counter.idle();
        });
        var f2 = Concurrent.run(() -> {
            Thread.sleep(100);
            counter.busy();
            Assertions.assertEquals(0.5, counter.getCurrentUtilization());
            Thread.sleep(400);
            counter.idle();
        });
        var f3 = Concurrent.run(() -> {
            Thread.sleep(150);
            counter.busy();
            Assertions.assertEquals(0.75, counter.getCurrentUtilization());
            Thread.sleep(300);
            counter.idle();
        });
        Concurrent.getAll(Reducer.suppressor(), f1, f2, f3);
        counter.stop();
        Assertions.assertEquals(0.5, counter.getAverageUtilization(), delta);
    }
}
