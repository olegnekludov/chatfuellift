package ru.nekludov.chatfuel.lift.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

public class MoveStrategiesTest
{

    Lift.MoveStrategy moveStrategy;

    BitSet callbs = new BitSet(10);
    BitSet gobs = new BitSet(10);

    @Test
    public void testSimpleNearest()
    {
        moveStrategy = MoveStrategies.SIMPLE_NEAREST;
        check(5, null, null);
        check(5, 5, null);
        check(5, 6, 6);

        callbs.set(6);
        check(5, null, 6);
        callbs.set(5);
        check(5, null, 5);

        gobs.set(4);
        callbs.clear();
        callbs.set(6);
        check(5, null, 6);

        callbs.clear();
        callbs.set(7);
        check(5, null, 4);
    }

    private void check(int current, Integer target, Integer expected)
    {
        Assert.assertEquals("Wrong target floor", expected,
                moveStrategy.getTargetFloor(callbs, gobs, current, target));
    }


}
