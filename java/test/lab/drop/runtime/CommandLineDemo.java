package lab.drop.runtime;

import lab.drop.Sugar;

public class CommandLineDemo {

    public static void main(String[] args) throws Exception {
        var hostnameCommand = new CommandLine("hostname");
        hostnameCommand.setNoPrints();
        var hostname = hostnameCommand.call();
        System.out.printf("%s, %s%n", Sugar.first(hostname.getOutput()), hostname);

        System.out.println(new Ping("google.com").call());

        System.out.println(new JVM(Ping.class, "speedtest.net").attempt());
        System.out.println(new JVM(Ping.class, "unknownhost").attempt());
        System.out.println(new JVM(Ping.class).attempt());

        System.out.println(new CommandLine("java", "-version").call());

        System.out.println(new JVM(Both.class).attempt());
    }

    private static class Ping extends CommandLine {

        public static void main(String[] args) throws Exception {
            System.exit(new Ping(Sugar.first(args)).call().getExitStatus());
        }

        Ping(String host) {
            super("ping", host);
        }
    }

    private static class Both {

        public static void main(String[] args) throws InterruptedException {
            System.out.println(1);
            Thread.sleep(500);
            System.err.println(2);
            Thread.sleep(500);
            System.out.println(3);
            Thread.sleep(500);
            System.err.println(4);
        }
    }
}
