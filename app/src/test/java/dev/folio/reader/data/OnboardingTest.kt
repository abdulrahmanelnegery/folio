package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    @Test fun aFreshInstallShowsTheTour() = assertEquals(Welcome.Tour, Onboarding.decide("", false, false, "0.12.0"))

    @Test fun theSameVersionShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.12.0", true, true, "0.12.0"))

    @Test fun anUpdateShowsTheNotes() = assertEquals(Welcome.WhatsNew, Onboarding.decide("0.11.2", true, true, "0.12.0"))

    @Test fun aDowngradeShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.12.0", true, true, "0.11.2"))

    @Test fun anExistingUserFromAnOlderVersionGetsTheNotesNotTheTour() = assertEquals(Welcome.WhatsNew, Onboarding.decide("", false, true, "0.12.0"))

    @Test fun versionsCompareByNumberNotText() {
        assertTrue(Onboarding.compare("0.10.0", "0.9.0") > 0)
        assertTrue(Onboarding.compare("0.12.0", "0.12") == 0)
        assertTrue(Onboarding.compare("1.0.0", "0.99.9") > 0)
    }

    @Test fun notesFallBackToTheNewestOlderVersion() {
        assertEquals(ReleaseNotes.forVersion("0.11.2"), ReleaseNotes.forVersion("0.11.5"))
        assertTrue(ReleaseNotes.forVersion("0.12.0").size in 2..4)
        assertTrue(ReleaseNotes.forVersion("0.1.0").isEmpty())
    }
}
