package com.maryhairsalon.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BookingStatusTest {
    @Test
    fun `known statuses map to their display labels`() {
        assertEquals("Pending", bookingStatusLabel("pending"))
        assertEquals("Upcoming", bookingStatusLabel("upcoming"))
        assertEquals("Completed", bookingStatusLabel("completed"))
        assertEquals("Cancelled", bookingStatusLabel("cancelled"))
    }

    @Test
    fun `unknown status falls back to capitalized raw value`() {
        assertEquals("Rescheduled", bookingStatusLabel("rescheduled"))
    }
}
