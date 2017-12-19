package ru.nekludov.chatfuel.lift.jdk;

import ru.nekludov.chatfuel.lift.model.Lift;

/**
 * Еденица измерения времени - секунды.
 *
 * Формат текстовых команд для лифта для пользователя:
 *      * нажатие кнопки на этаже: qN
 *      * нажатие кнопки в лифте: wN
 *      * выход: exit
 * Здесь N - число.
 * Примеры:
 *      * вызвать лифт на первый этаж: q1
 *      * поехать на лифте на пятый этаж: w5
 */
public class JdkLiftApp
{

    public static void main(String[] args)
    {
        new JdkLiftController(new LiftConfig(args)).start();
    }

    private static class LiftConfig implements Lift.Config
    {

        private final int floorCount;
        private final int floorHeight;
        private final int liftSpeed;
        private final long openCloseTime;

        public LiftConfig(String[] args)
        {
            this.floorCount = Integer.valueOf(args[0]);
            this.floorHeight = Integer.valueOf(args[1]);
            this.liftSpeed = Integer.valueOf(args[2]);
            this.openCloseTime = Long.valueOf(args[3]);
        }

        @Override
        public int getFloorCount()
        {
            return floorCount;
        }

        @Override
        public int getFloorHeight()
        {
            return floorHeight;
        }

        @Override
        public int getLiftSpeed()
        {
            return liftSpeed;
        }

        @Override
        public long getOpenCloseTime()
        {
            return openCloseTime;
        }
    }

}
