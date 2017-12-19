package ru.nekludov.chatfuel.lift.model;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Набор тестов может быть не полным (например, возможно, следует полнее протестировать взаимодействие лифта с MoveStrategy).
 */
public class LiftTest
{

    IMocksControl mocks = EasyMock.createStrictControl();

    static final int HEIGHT = 275;
    static final int SPEED = 90;
    static final int OCTIME = 15;

    static final int FLOORTIME = HEIGHT / SPEED;
    static final int STARTTIME = 0;

    Lift.Config cfg = new Lift.Config()
    {
        @Override
        public int getFloorCount()
        {
            return 10;
        }

        @Override
        public int getFloorHeight()
        {
            return HEIGHT;
        }

        @Override
        public int getLiftSpeed()
        {
            return SPEED;
        }

        @Override
        public long getOpenCloseTime()
        {
            return OCTIME;
        }
    };

    long currentTime = STARTTIME;

    TestScheduler scheduler = new TestScheduler();
    class TestScheduler implements Lift.Scheduler {

        private final SortedSet<Scheduled> scheduled = new TreeSet<>();

        @Override
        public void schedule(long delay, Runnable command)
        {
            Scheduled s = new Scheduled(currentTime + delay, command);
            scheduled.add(s);
        }

        void executeScheduled()
        {
            while (!scheduled.isEmpty()) {
                Scheduled next = scheduled.first();
                scheduled.remove(next);
                currentTime = Math.max(currentTime, next.time);
                next.command.run();
            }
        }

        class Scheduled implements Comparable<Scheduled>
        {
            long time;
            Runnable command;

            public Scheduled(long time, Runnable command)
            {
                this.time = time;
                this.command = command;
            }

            @Override
            public int compareTo(Scheduled o)
            {
                return (int) (time - o.time); // предполагаем, что у нас в тестах значения времени "за int" не выйдут
            }
        }
    };

    Lift.Listener listener = new Lift.Listener()
    {
        @Override
        public void onDoorOpened()
        {
            System.out.println("*** OPEN");
            timedListener.onDoorOpened(currentTime);
        }

        @Override
        public void onDoorClosed()
        {
            System.out.println("*** CLOSE");
            timedListener.onDoorClosed(currentTime);
        }

        @Override
        public void onEnterFloor(int floor)
        {
            System.out.println("*** FLOOR " + floor);
            timedListener.onEnterFloor(currentTime, floor);
        }
    };

    Lift lift = new Lift(cfg, MoveStrategies.SIMPLE_NEAREST, scheduler, listener);

    @Test
    public void testFloorBoundaries()
    {
        checkIllegalFloor(() -> lift.call(0));
        checkIllegalFloor(() -> lift.call(cfg.getFloorCount() + 1));
        checkIllegalFloor(() -> lift.go(0));
        checkIllegalFloor(() -> lift.go(cfg.getFloorCount() + 1));
    }

    private void checkIllegalFloor(Runnable action)
    {
        try {
            action.run();
        }
        catch (IllegalArgumentException e) {
            return;
        }
        Assert.fail("There is no floor invalid exception");
    }

    @Test
    public void testCallForLift_SameFloor_OpenCloseDoor()
    {
        checkDoor();
        test(() -> lift.call(1));

        checkDoor();
        test(() -> {
            lift.call(1);
            lift.call(1);
            lift.call(1);
        });
    }

    @Test
    public void testCallForLift()
    {
        checkMove(2, 3);
        test(() -> lift.call(3));
    }

    @Test
    public void testCallForLift_Multiples_UpAndDown()
    {
        checkMove(2, 3);
        checkMove(4, 5);
        checkMove(6);

        test(() -> {
            lift.call(3);
            lift.call(5);
            lift.call(6);
        });

        checkMove(7);

        test(() -> {
            lift.call(7);
        });

        checkMove(6, 5, 4);
        checkMove(3, 2);

        test(() -> {
            lift.call(4);
            lift.call(2);
        });
    }

    @Test
    public void testGo_SameFloor_OpenCloseDoor()
    {
        checkDoor();
        test(() -> lift.go(1));

        checkDoor();
        test(() -> {
            lift.go(1);
            lift.go(1);
            lift.go(1);
        });
    }

    @Test
    public void testGo()
    {
        checkMove(2, 3);
        test(() -> lift.go(3));
    }

    @Test
    public void testGo_Multiples_UpAndDown()
    {
        checkMove(2, 3);
        checkMove(4, 5);
        checkMove(6);

        test(() -> {
            lift.go(3);
            lift.go(5);
            lift.go(6);
        });

        checkMove(7);

        test(() -> {
            lift.go(7);
        });

        checkMove(6, 5, 4);
        checkMove(3, 2);

        test(() -> {
            lift.go(4);
            lift.go(2);
        });
    }

    @Test
    public void testCallAndGo()
    {
        checkDoor();
        checkMove(2, 3);

        test(() -> {
            lift.call(1);
            lift.go(3);
        });
    }

    @Test
    public void testCallThenGo()
    {
        checkMove(2, 3, 4);

        test(() -> {
            lift.call(4);
        });

        checkMove(3, 2, 1);

        test(() -> {
            lift.go(1);
        });
    }

    @Test
    public void testCallOpenLift()
    {
        checkDoor();
        checkMove(2, 3);

        test(() -> {
            lift.call(1);
            lift.call(3);
        });
    }

    @Test
    public void testUpThenDownAndUp()
    {
        checkMove(2, 3, 4, 5);

        test(() -> lift.call(5));

        checkMove(4, 3, 2, 1);
        checkMove(2, 3, 4, 5);

        test(() -> {
            lift.call(1);
            lift.call(5);
        });
    }

    @Test
    public void testUpThenDownAndUpAnother()
    {
        checkMove(2, 3, 4, 5);

        test(() -> lift.call(5));

        checkMove(4, 3, 2, 1);
        checkMove(2, 3, 4, 5, 6);

        test(() -> {
            lift.call(1);
            lift.call(6);
        });
    }

    private void test(Runnable action)
    {
        mocks.replay();
        action.run();
        scheduler.executeScheduled();
        mocks.verify();
        mocks.reset();
    }

    private TimedListener timedListener = mocks.createMock(TimedListener.class);

    private long checkTime = STARTTIME;

    private void checkMove(int... floors)
    {
        for (int f : floors) {
            checkEnterFloor(f);
        }
        checkDoor();
    }

    private void checkDoor()
    {
        checkDoorOpened();
        checkDoorClosed();
    }

    private void checkDoorOpened()
    {
        timedListener.onDoorOpened(checkTime);
    }

    private void checkDoorClosed()
    {
        checkTime += OCTIME;
        timedListener.onDoorClosed(checkTime);
    }

    private void checkEnterFloor(int floor)
    {
        checkTime += FLOORTIME;
        timedListener.onEnterFloor(checkTime, floor);
    }

    private interface TimedListener
    {
        void onDoorOpened(long time);
        void onDoorClosed(long time);
        void onEnterFloor(long time, int floor);
    }
}
