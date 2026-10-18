package ai.ignition.android.obd;

import ai.ignition.android.util.RedactionUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class RedactionUtilTest {

    @Test
    public void testMacRedaction() {
        assertEquals("00:1D:A5:**:**:**", RedactionUtil.redactMac("00:1D:A5:12:34:56"));
        assertEquals("AA-BB-CC:**:**:**", RedactionUtil.redactMac("AA-BB-CC-DD-EE-FF"));
    }

    @Test
    public void testVinRedaction() {
        assertEquals("1HG**********3456", RedactionUtil.redactVin("1HGCR2F84HA123456"));
        assertEquals("UNAVAILABLE", RedactionUtil.redactVin(null));
        assertEquals("UNAVAILABLE", RedactionUtil.redactVin(""));
    }
}
