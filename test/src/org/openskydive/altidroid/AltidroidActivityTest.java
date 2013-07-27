package org.openskydive.altidroid;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.openskydive.altidroid.AltidroidActivityTest \
 * org.openskydive.altidroid.tests/android.test.InstrumentationTestRunner
 */
public class AltidroidActivityTest extends ActivityInstrumentationTestCase2<AltidroidActivity> {

    public AltidroidActivityTest() {
        super("org.openskydive.altidroid", AltidroidActivity.class);
    }

}
