package ru.nekludov.chatfuel.lift.model;

import java.util.BitSet;

/**
 * В ТЗ реализуем одну простую стратегию, но если нужно - могу реализовать и более сложные
 * (например, учитывающие приоритет нажатия кнопок в лифте нажатиям вызовов лифта с этажа).
 */
public class MoveStrategies
{

    /**
     * Стратегия, которая отправляет лифт на ближайший этаж с приоритетом вверх.
     * Если этаж задан - оставляет его неизменным.
     */
    public static final Lift.MoveStrategy SIMPLE_NEAREST = new Lift.MoveStrategy()
    {
        @Override
        public Integer getTargetFloor(BitSet callBitSet, BitSet goBitSet, int currentFloor, Integer targetFloor)
        {
            if (targetFloor != null && targetFloor != currentFloor) {
                return targetFloor;
            }
            BitSet join = ((BitSet) callBitSet.clone());
            join.or(goBitSet);
            int targetUp = join.nextSetBit(currentFloor);
            int targetDown = join.previousSetBit(currentFloor);
            if (targetUp != -1 && targetDown != -1) {
                int dup = targetUp - currentFloor;
                int ddown = currentFloor - targetDown;
                return ddown < dup ? targetDown : targetUp;
            }
            else if (targetUp != -1) {
                return targetUp;
            }
            else if (targetDown != -1) {
                return targetDown;
            }
            return null;
        }
    };

}
