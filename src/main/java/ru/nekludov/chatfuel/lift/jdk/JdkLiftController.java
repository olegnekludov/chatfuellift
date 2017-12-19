package ru.nekludov.chatfuel.lift.jdk;

import ru.nekludov.chatfuel.lift.model.Lift;
import ru.nekludov.chatfuel.lift.model.MoveStrategies;

import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class JdkLiftController
{

    private final Lift lift;

    private final Timer timer = new Timer();

    public JdkLiftController(Lift.Config liftConfig)
    {
        this.lift = new Lift(
                liftConfig,
                MoveStrategies.SIMPLE_NEAREST,
                new Lift.Scheduler()
                {
                    @Override
                    public void schedule(long delay, Runnable command)
                    {
                        timer.schedule(new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                synchronized (lift) {
                                    command.run();
                                }
                            }
                        }, delay * 1000);
                    }
                },
                new Lift.Listener()
                {
                    @Override
                    public void onDoorOpened()
                    {
                        log("DOOR OPEN");
                    }

                    @Override
                    public void onDoorClosed()
                    {
                        log("DOOR CLOSE");
                    }

                    @Override
                    public void onEnterFloor(int floor)
                    {
                        log("ENTER FLOOR " + floor);
                    }
                }
        );
    }

    private void log(String msg)
    {
        System.out.println(new Date() + ": " + msg);
    }

    public void start()
    {
        log("LIFT IS READY");
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String cmd = in.nextLine();
            if ("exit".equalsIgnoreCase(cmd)) {
                break;
            }
            if (cmd.isEmpty()) {
                continue;
            }
            try {
                if (cmd.startsWith("q")) {
                    synchronized (lift) {
                        lift.call(Integer.valueOf(cmd.substring(1)));
                    }
                }
                else if (cmd.startsWith("w")) {
                    synchronized (lift) {
                        lift.go(Integer.valueOf(cmd.substring(1)));
                    }
                }
                else {
                    log("Unknown command: " + cmd);
                }
            }
            catch (Exception e) {
                log("Error: " + e.getMessage());
            }
        }
        timer.cancel();
        log("GOODBYE");
    }

}
