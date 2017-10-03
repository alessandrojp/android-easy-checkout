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
public class BillingApiTest {

    @Test
    public void checkSize() {
        assertThat(BillingApi.values().length).isEqualTo(2);
    }

    @Test
    public void valueOfVersion3() {
        assertThat(BillingApi.valueOf(BillingApi.VERSION_3.name())).isNotNull();
    }

    @Test
    public void valueOfVersion5() {
        assertThat(BillingApi.valueOf(BillingApi.VERSION_5.name())).isNotNull();
    }
}