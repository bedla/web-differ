package cz.bedla.differ.testsupport

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class MyClock() : Clock() {
    private val zoneId = ZoneId.of("Europe/Prague")!!
    private val baseNow = ZonedDateTime.of(2019, 9, 18, 20, 35, 5, 0, zoneId)!!
    private lateinit var now: ZonedDateTime
    private lateinit var clock: Clock

    init {
        reset()
    }

    override fun withZone(zone: ZoneId?): Clock = clock.withZone(zoneId)

    override fun getZone(): ZoneId = clock.zone

    override fun instant(): Instant = clock.instant()

    fun reset(): MyClock {
        setupClock(baseNow)
        return this
    }

    fun modify(modifyAction: (ZonedDateTime) -> ZonedDateTime): MyClock {
        setupClock(modifyAction(now))
        return this
    }

    private fun setupClock(value: ZonedDateTime) {
        now = value
        clock = fixed(Instant.from(now), zoneId)
    }

    companion object {
        fun <T> invokeWith(clock: Clock, modifyAction: (ZonedDateTime) -> ZonedDateTime, invoker: () -> T): T {
            val myClock = clock as MyClock
            val old = myClock.now
            try {
                myClock.modify(modifyAction)
                return invoker()
            } finally {
                myClock.modify { _ -> old }
            }
        }
    }
}
