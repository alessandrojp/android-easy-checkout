package jp.alessandro.android.iab;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/09/28.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class PurchaseTypeTest {

    @Test
    public void checkSize() {
        assertThat(PurchaseType.values().length).isEqualTo(2);
    }

    @Test
    public void valueOfInApp() {
        assertThat(PurchaseType.valueOf(PurchaseType.IN_APP.name())).isNotNull();
    }

    @Test
    public void valueOfSubscription() {
        assertThat(PurchaseType.valueOf(PurchaseType.SUBSCRIPTION.name())).isNotNull();
    }
}