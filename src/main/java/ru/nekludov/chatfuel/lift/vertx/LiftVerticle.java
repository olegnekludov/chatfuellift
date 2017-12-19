package ru.nekludov.chatfuel.lift.vertx;

import io.vertx.core.AbstractVerticle;
import ru.nekludov.chatfuel.lift.model.Lift;
import ru.nekludov.chatfuel.lift.model.MoveStrategies;

import java.util.Date;

public class LiftVerticle extends AbstractVerticle
{

    public static final String LIFT_ADDRESS = "lift";

    private final Lift lift;

    public LiftVerticle(Lift.Config liftConfig)
    {
        lift = new Lift(
                liftConfig,
                MoveStrategies.SIMPLE_NEAREST,
                (delay, command) -> getVertx().setTimer(delay, l -> command.run()),
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

                    private void log(String msg)
                    {
                        System.out.println(new Date() + ": " + msg);
                    }
                }
        );

        getVertx().eventBus().consumer(LIFT_ADDRESS, message -> {

        });
    }
}
