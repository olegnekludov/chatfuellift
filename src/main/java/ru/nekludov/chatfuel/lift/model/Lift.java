package ru.nekludov.chatfuel.lift.model;

import java.util.BitSet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Первоначальное состояние лифта: 1 этаж, двери закрыты.
 * Считаем, что открытие и закрытие дверей происходит мгновенно.
 *
 * Повторное нажатие на кнопку текущего этажа при открытом лифте игнорируется
 * (в соответствии с "Считаем, что пользователь не может помешать лифту закрыть двери").
 *
 * При достижении этажа, на который лифт запрашивали любым способом, лифт открывает там двери.
 *
 * Модель лифта однопоточная (для возможности использования в разных моделях параллельных вычислений,
 * а также для лучшей декомпозиции/связности - выделяем отдельно логику работы лифта).
 * Для корретной работы модели при использовании из нескольких потоков нужна внешняя синхронизация.
 */
public class Lift
{

    public static final int MIN_FLOORS = 5;
    public static final int MAX_FLOORS = 20;

    /**
     * Параметры лифта.
     * В арифметике для упрощения используем только целочисленные значения - соответственно, возможны ошибки округления.
     * Учитывайте это при выборе значений параметров и их единиц измерения.
     */
    public interface Config
    {
        /**
         * Количество этажей в доме.
         */
        int getFloorCount();

        /**
         * Высота этажа.
         */
        int getFloorHeight();

        /**
         * Скорость движения лифта.
         */
        int getLiftSpeed();

        /**
         * Время между открытием и закрытием дверей.
         */
        long getOpenCloseTime();
    }

    public interface Listener
    {
        void onDoorOpened();
        void onDoorClosed();
        void onEnterFloor(int floor);
    }

    /**
     * Стратегия определения направления движения лифта.
     * При движении вызывается на каждом этаже.
     */
    public interface MoveStrategy
    {
        /**
         * Возвращает этаж, на который следует двигаться лифту.
         * @param callBitSet нажатые кнопки вызова на этажах
         * @param goBitSet нажатые кнопки в самом лифте
         * @param currentFloor этаж, на котором находится сейчас лифт
         * @param targetFloor текущий целевой этаж лифта (задан, если лифт уже находится в движении)
         * @return целевой этаж для лифта (на который лифту следует ехать без остановок) или null, если никуда не ехать
         */
        Integer getTargetFloor(BitSet callBitSet, BitSet goBitSet, int currentFloor, Integer targetFloor);
    }

    public interface Scheduler
    {
        void schedule(long delay, Runnable command);
    }

    private final Config cfg;
    private final MoveStrategy moveStrategy;
    private final Scheduler scheduler;
    private final Listener listener;

    private final long floorTime;

    private int currentFloor = 1;
    private Integer targetFloor;

    private final BitSet callBitSet;
    private final BitSet goBitSet;

    public Lift(Config cfg, MoveStrategy moveStrategy, Scheduler scheduler, Listener listener)
    {
        checkArgument(cfg.getFloorCount() >= MIN_FLOORS && cfg.getFloorCount() <= MAX_FLOORS, "Wrong floor count");
        this.cfg = cfg;
        this.moveStrategy = moveStrategy;
        this.scheduler = scheduler;
        this.listener = listener;

        floorTime = cfg.getFloorHeight() / cfg.getLiftSpeed();

        // будем использовать биты, начиная с 1-го (нулевой не используем - для тестового задания пусть так будет)
        callBitSet = new BitSet(cfg.getFloorCount() + 1);
        goBitSet = new BitSet(cfg.getFloorCount() + 1);
    }

    /**
     * Вызвать лифт на этаж из подъезда.
     */
    public void call(int floor)
    {
        checkFloor(floor);

        callBitSet.set(floor);
        operate();
    }

    /**
     * Нажать на кнопку этажа внутри лифта.
     */
    public void go(int floor)
    {
        checkFloor(floor);

        goBitSet.set(floor);
        operate();
    }

    private enum State
    {
        ONFLOOR, OPEN, MOVING
    }

    private State state = State.ONFLOOR;

    private void openDoor()
    {
        state = State.OPEN;
        listener.onDoorOpened();
        scheduler.schedule(cfg.getOpenCloseTime(), this::closeDoor);
    }

    private void closeDoor()
    {
        state = State.ONFLOOR;
        listener.onDoorClosed();
        operate();
    }

    private void operate()
    {
        if (State.MOVING.equals(state)) {
            return;
        }

        boolean currentFloorBit = getAndClearFloorBits();

        if (State.OPEN.equals(state)) {
            return;
        }

        if (currentFloorBit) {
            openDoor();
            return;
        }

        targetFloor = moveStrategy.getTargetFloor(callBitSet, goBitSet, currentFloor, targetFloor);

        if (targetFloor == null) {
            return;
        }

        int delta = targetFloor > currentFloor ? 1 : -1;

        state = State.MOVING;

        scheduler.schedule(floorTime, () -> {
            currentFloor += delta;
            listener.onEnterFloor(currentFloor);
            state = State.ONFLOOR;
            operate();
        });
    }

    private boolean getAndClearFloorBits()
    {
        boolean bit = callBitSet.get(currentFloor) || goBitSet.get(currentFloor);
        callBitSet.clear(currentFloor);
        goBitSet.clear(currentFloor);
        return bit;
    }

    private void checkFloor(int floor)
    {
        checkArgument(floor >= 1 && floor <= cfg.getFloorCount(), "Invalid floor");
    }

}
