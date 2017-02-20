# RxJava extension for Easy Checkout Library
[![Build Status](https://travis-ci.org/alessandrojp/easy-checkout.svg)](https://travis-ci.org/alessandrojp/easy-checkout)
[![Bintray](https://img.shields.io/bintray/v/alessandrojp/android/easy-checkout.svg)](https://bintray.com/alessandrojp/android/easy-checkout/view)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


### Installation
* For Eclipse users, download the latest jar version on [Bintray][] and add it as a dependency.

* For Gradle users, the library is available in the jcenter and mavenCentral. Add this into your build.gradle file:

```groovy
repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile 'jp.alessandro.android:easy-checkout:vX.X.X'
    compile 'jp.alessandro.android:easy-checkout-extension-rxjava:vX.X.X'

    // RxJava
    compile 'io.reactivex:rxjava:1.2.3'
    compie 'io.reactivex:rxandroid:1.2.1'
}
```
where vX.X.X is the your preferred version. For the latest version, please see the project's [Releases][]. You can also see more details on [Bintray][].

[Releases]: https://github.com/alessandrojp/easy-checkout/releases
[Bintray]: https://bintray.com/alessandrojp/android/easy-checkout/view

* This library requires `com.android.vending.BILLING` permission. Usually, permissions are merged automatically during the manifest merging step, but you can add it into the *AndroidManifest.xml* of your application.
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

* If you use Proguard to obfuscate your code, you must add the following lines to your Proguard configuration file:
```xml
-keep class com.android.vending.billing.**
```

### Creating a Billing Context
* You need a **BillingContext** object to execute actions in this library.
You can create one as follows:

```java
// Application context
Context context = getApplicationContext();

// Public key generated on the Google Play Console
String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

BillingContext context = new BillingContext(
            context,
            base64EncodedPublicKey,
            BillingApi.VERSION_3, // It also supports api version 5
            new SystemLogger() // If don't want to check the logs, you can just give null
```

### Sample (Sample App coming soon)
* See the sample of how to use it:

```java
public class SampleActivity extends Activity {

    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
      @Override
      public void call(PurchaseResponse response) {
          if (response.isSuccess()) {
              Purchase purchase = response.getPurchase();
              // Do your stuff with the purchased item
          } else {
              // Handle the error
          }
      }
    };
    private BillingProcessorObservable mBillingProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      initBillingProcessor();
    }

    private void initBillingProcessor() {
      // Your public key
      String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

      BillingContext context = new BillingContext(
              getApplicationContext(), // App context
              base64EncodedPublicKey, // Public key generated on the Google Play Console
              BillingApi.VERSION_3, // It also supports version 5
              new SystemLogger() // If don't want to check the logs, you can just send null
      );
      mBillingProcessor = new BillingProcessorObservable(context, mPurchaseHandler);
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      mBillingProcessor.release();
    }
}
```

* Don't forget to override Activity's onActivityResult method like this:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (mBillingProcessor.onActivityResult(requestCode, resultCode, data)) {
      // The response will be sent through PurchaseHandler
      return;
  }
  super.onActivityResult(requestCode, resultCode, data);
}
```

# Purchase Item
* Call `BillingProcessorObservable#startPurchase` method to purchase a consumable/non-consumable or a subscription item  like this:

```java
Activity activity = this;
int requestCode = 10; // YOUR REQUEST CODE
String itemId = "YOUR_ITEM_ID";
PurchaseType purchaseType = PurchaseType.IN_APP; // or PurchaseType.SUBSCRIPTION for subscriptions
String developerPayload = "YOUR_DEVELOPER_PAYLOAD";

mBillingProcessor.startPurchase(activity, requestCode, itemId, purchaseType, developerPayload)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Action1<Void>() {
          @Override
          public void call(Void aVoid) {
              // Billing activity started successfully
              // Do nothing
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
              // Handle the error
          }
      });
```
As a result you will get a [Purchase](#purchase-object) object through of the PurchaseHandler set in the BillingProcessorObservable's constructor.

# Consume Purchased Item
* You can consume a purchased item anytime and allow the user to buy the same item again. To do this call `BillingProcessorObservable#consume` like this:

```java
String itemId = "YOUR_ITEM_ID";

mBillingProcessor.consumePurchase(itemId)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .subscribe(new Action1<Void>() {
          @Override
          public void call(Void aVoid) {
              // Item was consumed successfully
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
              // Handle the error
          }
      });
```

# Upgrade/Downgrade Subscription
* You can upgrade/downgrade an existing subscription by calling `BillingProcessorObservable#updateSubscription` like this:

**This is only available in the api version 5.
You can still set the api version 3 for other actions in the library.
The api version 5 automatically will be used only in this case.**

```java
Activity activity = this;
int requestCode = 10; // YOUR REQUEST CODE
ArrayList<String> itemIdList = new ArrayList<>();
itemIdList.add("CURRENT_ITEM_ID");
String targetItemId = "TARGET_ITEM_ID";
String developerPayload = "YOUR_DEVELOPER_PAYLOAD";

mBillingProcessor.updateSubscription(activity, requestCode, itemIdList, targetItemId, developerPayload)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Action1<Void>() {
          @Override
          public void call(Void aVoid) {
              // Billing activity started successfully
              // Do nothing
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
              // Handle the error
          }
      });
```
As a result you will get a [Purchase](#purchase-object) object through of the PurchaseHandler set in the BillingProcessorObservable's constructor.

# Inventory of Purchases
* You can get the inventory of your purchases like this:

```java
PurchaseType purchaseType = PurchaseType.IN_APP; // PurchaseType.SUBSCRIPTIONS for subscriptions

mBillingProcessor.getPurchases(purchaseType)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .subscribe(new Action1<PurchaseList>() {
          @Override
          public void call(Purchases purchases) {
              // Do your stuff with the list of purchases
              List<Purchase> purchaseList = purchases.getAll();
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
              // Handle the error
          }
      });
```
As a result you will get a list of [Purchase](#purchase-object) objects.

# List of Item details
* You can get a list of your sku item details such as prices and descriptions

```java
PurchaseType purchaseType = PurchaseType.IN_APP; // PurchaseType.SUBSCRIPTIONS for subscriptions
ArrayList<String> itemIds = new ArrayList<>();
itemIds.add("item_id_1");
itemIds.add("item_id_2");

mBillingProcessor.getItemDetails(purchaseType, itemIds)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .subscribe(new Action1<ItemDetailList>() {
          @Override
          public void call(ItemDetails itemDetails) {
              // Do your stuff with the list of item details
              List<Item> items = itemDetails.getAll();
          }
      }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
              // Handle the error
          }
      });
```
As a result you will get a list of [Item](#item-object) detail objects.

# Cancel
* Cancel the all purchase flows. It will clear the pending purchase flows and ignore any event until a new request.<br />If you don't need the BillingProcessor instance any more, call directly [Release](#release) instead.
<br />**Note: By canceling it will not cancel the purchase process since the purchase process is not controlled by the app.**

```java
mBillingProcessor.cancel();
```

# Release
* Release the handlers. Once you release it, you **MUST** to create a new instance.
<br />**Note: By releasing it will not cancel the purchase process since the purchase process is not controlled by the app.**

```java
mBillingProcessor.release();
```

# Check In-App Billing service availability
* In some devices, In-App Billing may not be available.
Therefore, it is advisable to check whether it is available or not by calling `BillingProcessorObservable.isServiceAvailable` as follows:

```java
boolean isAvailable = BillingProcessorObservable.isServiceAvailable(getApplicationContext());
if (!isAvailable) {
    // Abort
}
```

# Purchase Object
* This object contains all the information of a purchase

```java
  public String getOriginalJson();
  public String getOrderId();
  public String getPackageName();
  public String getSku();
  public long getPurchaseTime();
  public int getPurchaseState();
  public String getDeveloperPayload();
  public String getToken();
  public boolean isAutoRenewing();
  public String getSignature();
```

# Item Object
* This object contains all the information of an item

```java
  public String getOriginalJson();
  public String getSku();
  public String getType();
  public String getTitle();
  public String getDescription();
  public String getCurrency();
  public String getPrice();
  public long getPriceMicros();
```